package com.alvexcore.repo.bcal;

import com.alvexcore.repo.workflow.activiti.graph.ProcessGraph;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.workflow.activiti.ActivitiScriptNode;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.csv.CSVParser;
import org.mozilla.javascript.NativeArray;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultBusinessCalendarHandler extends AbstractBusinessCalendarHandler {

    class TaskKeyComparator
    {
        RuntimeService runtimeService;

        Map<String, Map<String, Integer>> distances = new HashMap<>();

        public TaskKeyComparator(RuntimeService runtimeService) {
            this.runtimeService = runtimeService;

            for (ProcessDefinition definition: repositoryService.createProcessDefinitionQuery().latestVersion().list())
            {
                String definitionKey = definition.getKey();
                BpmnModel model = repositoryService.getBpmnModel(definition.getId());

                Process process = model.getMainProcess();

                ProcessGraph graph = new ProcessGraph(process);

                Set<String> userTasks = graph.getUserTasks().stream()
                        .map(org.activiti.bpmn.model.Task::getId)
                        .collect(Collectors.toSet());

                distances.put(
                        definitionKey,
                        graph.bfs(null, null, null).entrySet().stream()
                           .filter(entry -> userTasks.contains(entry.getKey()))
                            .collect(Collectors.toMap(
                                    entry -> ((UserTask)graph.getElementById(entry.getKey())).getFormKey(),
                                    Map.Entry::getValue,
                                    (e1, e2) -> e1,
                                    LinkedHashMap::new
                            ))
                 );
            }
        }

        public int compare(String processKey, String formKey1, String formKey2)
        {
            String definitionKey = getProcessDefinitionKey(processKey);
            Map<String, Integer> d = distances.get(definitionKey);

            if (d == null)
                return formKey1.compareTo(formKey2);

            return d.getOrDefault(formKey1, 0) - d.getOrDefault(formKey2, 0);
        }
    }


    private String businessCalendarPath;
    private TaskKeyComparator comparator;

    @Required
    public void setBusinessCalendarPath(String businessCalendarPath) {
        this.businessCalendarPath = businessCalendarPath;
    }

    @Override
    public KeyInfo getTaskKeyInfo(DelegateTask delegateTask) {
        String processName = delegateTask.getProcessDefinitionId().split(":")[0];
        return getTaskKeyInfo(processName, delegateTask.getFormKey());
    }

    public String getProcessDefinitionKey(String processKey)
    {
        return processKey;
    }

    @Override
    public Map<String, Integer> getDefaultLimits() {
        HashMap<String, Integer> limitsMap = new HashMap<>();


        Map<String, List<String>> tasks = getTasks(processDefinition -> true, true);

        for (Map.Entry<String, List<String>> entry: tasks.entrySet())
        {
            String workflow = entry.getKey();
            entry.getValue().forEach(
                formKey -> limitsMap.putIfAbsent(getTaskKeyInfo(workflow, formKey).toString(), BusinessCalendar.DEFAULT_TASK_TIME_LIMIT)
            );
        }

        return limitsMap;
    }

    public static Set<LocalDate> loadHolidaysListFromCsv(URL businessCalendarPath) throws IOException
    {
        CSVParser parser = new CSVParser(new InputStreamReader(businessCalendarPath.openStream()));

        Set<LocalDate> holidays = new HashSet<>();
        LocalDate startFrom = LocalDate.now();

        boolean skip = true;
        for (String[] line: parser.getAllValues()) {
            if (skip)
            {
                skip = false;
                continue;
            }
            Integer year = Integer.valueOf(line[0]);
            if (startFrom.getYear() > year)
                continue;
            for (int month = startFrom.getMonthValue(); month <= 12; month++) {
                for (String holiday : line[month].split(",")) {
                    holiday = holiday.trim();
                    if (holiday.endsWith("*"))
                        continue;
                    Integer day = Integer.valueOf(holiday);
                    if (month == startFrom.getMonthValue() && day < startFrom.getDayOfMonth())
                        continue;
                    holidays.add(LocalDate.of(year, month, day));
                }
            }
        }

        return holidays;
    }

    @Override
    public int compareTaskKeys(String processKey, String key1, String key2) {
        return comparator.compare(processKey, key1, key2);
    }

    @Override
    public int compareProcessKeys(String key1, String key2) {
        return key1.compareTo(key2);
    }

    @Override
    public Map<String, Object> buildEmailModel(Task task, NodeRef personRef) {
        HashMap<String, Object> model = new HashMap<>();

        model.put("shareUrl", shareUrl);

        model.put("taskTitle", task.getDescription());
        model.put("taskId", "activiti$" + task.getId());
        model.put("taskDueDate", task.getDueDate().toInstant().atOffset(businessCalendar.getZoneOffset()).toLocalDate().toString());

        String pid = task.getProcessInstanceId();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(pid).singleResult();
        if (processInstance == null)
            processInstance = ((TaskEntity)task).getProcessInstance();

        Map<String, Object> variables = runtimeService.getVariables(task.getExecutionId());

        String workflowDescription = (String) variables.get("bpm_workflowDescription");
        if (workflowDescription != null && !workflowDescription.isEmpty())
            model.put("workflowDescription", workflowDescription);

        String workflowName = repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId()).getName();
        model.put("workflowTitle", workflowName);

        ActivitiScriptNode workflowPackage = (ActivitiScriptNode) variables.get("bpm_package");

        List<Map> workflowDocuments = new ArrayList<>();

        // Without this check workflowPackage.getChildren() fails with NPE if package is empty
        if (workflowPackage.getHasChildren()) {
            NativeArray children = (NativeArray) workflowPackage.getChildren();

            for (Object child : children) {
                ScriptNode node = (ScriptNode) child;

                HashMap<String, String> doc = new HashMap<>();
                doc.put("id", node.getId());
                doc.put("name", node.getName());

                workflowDocuments.add(doc);
            }
        }

        if (!workflowDocuments.isEmpty())
            model.put("workflowDocuments", workflowDocuments);

        return model;
    }

    @Override
    public Set<LocalDate> initialize(Configuration configuration, StringTemplateLoader templateLoader) throws Exception {
        comparator = new TaskKeyComparator(runtimeService);

        return loadHolidaysListFromCsv(new URL(businessCalendarPath));
    }
}

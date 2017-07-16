package com.alvexcore.repo.bcal;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.task.Task;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.beans.factory.annotation.Required;

import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class CustomBusinessCalendarHandler extends AbstractBusinessCalendarHandler {

    List<Integer> WORKFLOW_KINDS = Arrays.asList(1, 2, 3, 4, 5);

    private String businessCalendarPath;

    @Required
    public void setBusinessCalendarPath(String businessCalendarPath) {
        this.businessCalendarPath = businessCalendarPath;
    }

    @Override
    public KeyInfo getTaskKeyInfo(DelegateTask delegateTask) {
        String processName = delegateTask.getProcessDefinitionId().split(":")[0];
        Integer wfKind = (Integer) delegateTask.getVariable("someWorkflowKindVariable");
        if (!WORKFLOW_KINDS.contains(wfKind))
            wfKind = WORKFLOW_KINDS.get(0);
        return getTaskKeyInfo(wfKind, processName, delegateTask.getFormKey());
    }

    public KeyInfo getTaskKeyInfo(Integer workflowKind, String processKey, String taskKey)
    {
        return getTaskKeyInfo(workflowKind + "_" + processKey, taskKey);
    }

    @Override
    public Map<String, Integer> getDefaultLimits() {
        HashMap<String, Integer> limitsMap = new HashMap<>();


        Map<String, List<String>> tasks = getTasks(processDefinition -> true, true);

        for (Map.Entry<String, List<String>> entry: tasks.entrySet())
        {
            String workflow = entry.getKey();
            WORKFLOW_KINDS.forEach(kind -> {
                entry.getValue().forEach(
                    formKey -> limitsMap.putIfAbsent(getTaskKeyInfo(kind, workflow, formKey).toString(), 10)
                );
            });
        }

        return limitsMap;

    }

    @Override
    public int compareTaskKeys(String processKey, String key1, String key2) {
        return -key1.compareTo(key2);
    }

    @Override
    public int compareProcessKeys(String key1, String key2) {
        return -key1.compareTo(key2);
    }

    @Override
    public Map<String, Object> buildEmailModel(Task task, NodeRef personRef) {
        // TODO implement
        return null;
    }

    @Override
    public Set<LocalDate> initialize(Configuration configuration, StringTemplateLoader templateLoader) throws Exception {
        templateLoader.putTemplate("custom", "Custom template body");

        return DefaultBusinessCalendarHandler.loadHolidaysListFromCsv(new URL(businessCalendarPath));
    }
}

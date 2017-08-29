package com.alvexcore.repo.bcal;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.task.Task;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AbstractBusinessCalendarHandler implements InitializingBean {

    protected ServiceRegistry serviceRegistry;
    protected BusinessCalendar businessCalendar;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected List<String> matches;
    protected TaskDueDateSetter dueDateSetter;
    protected Properties properties;

    protected String shareUrl;

    @Required
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Required
    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Required
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Required
    public void setBusinessCalendar(BusinessCalendar businessCalendar) {
        this.businessCalendar = businessCalendar;
        this.businessCalendar.registerHandler(this);
    }

    @Required
    public void setProperties(Properties properties) {
        this.properties = properties;

        shareUrl = String.format("%s://%s:%s/%s",
            properties.getProperty("share.protocol"),
            properties.getProperty("share.host"),
            properties.getProperty("share.port"),
            properties.getProperty("share.context")
        );
    }

    @Required
    public void setMatches(List<String> matches) {
        this.matches = matches;
    }

    public List<String> getMatches() {
        return matches;
    }

    @Required
    public void setDueDateSetter(TaskDueDateSetter dueDateSetter) {
        this.dueDateSetter = dueDateSetter;
    }

    protected Map<String, List<String>> getTasks(Predicate<ProcessDefinition> predicate, boolean latestOnly)
    {
        Map<String, List<String>> result = new HashMap<>();

        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        if (latestOnly)
            query = query.latestVersion();

        for (ProcessDefinition def: query.list())
        {
            if (!predicate.test(def))
                continue;

            List<String> formKeys = new ArrayList<>();

            BpmnModel model = repositoryService.getBpmnModel(def.getId());
            Process process = model.getProcesses().get(0);

            process.findFlowElementsOfType(UserTask.class).forEach(userTask -> formKeys.add(userTask.getFormKey()));

            result.put(def.getKey(), formKeys);
        }

        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this == businessCalendar.getHandler()) {
            dueDateSetter.setMatches(
                matches.stream()
                    .map(s -> Arrays.asList("task-create:.*@" + s, "task-assign-after-change:.*@" + s, "process-start@" + s))
                    .flatMap(l -> l.stream())
                .collect(Collectors.toList())
            );
        }
    }

    public boolean isRealAssignee(String assignee)
    {
        return assignee != null;
    }

    public KeyInfo getTaskKeyInfo(String procecessKey, String formKey)
    {
        return new KeyInfo(procecessKey, formKey);
    }

    public abstract KeyInfo getTaskKeyInfo(DelegateTask delegateTask);

    public abstract Map<String, Integer> getDefaultLimits();

    public abstract int compareTaskKeys(String processKey, String key1, String key2);

    public abstract int compareProcessKeys(String key1, String key2);

    public abstract Map<String, Object> buildEmailModel(Task task, NodeRef personRef);

    public abstract Set<LocalDate> initialize(Configuration configuration, StringTemplateLoader templateLoader) throws Exception;
}

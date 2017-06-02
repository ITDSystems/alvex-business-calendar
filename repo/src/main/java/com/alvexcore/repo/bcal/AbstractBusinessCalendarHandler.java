package com.alvexcore.repo.bcal;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.alfresco.service.ServiceRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class AbstractBusinessCalendarHandler implements BusinessCalendarHandler, InitializingBean{

    protected ServiceRegistry serviceRegistry;
    protected BusinessCalendar businessCalendar;
    protected RepositoryService repositoryService;
    protected List<String> matches;
    protected TaskDueDateSetter dueDateSetter;

    @Required
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Required
    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Required
    public void setBusinessCalendar(BusinessCalendar businessCalendar) {
        this.businessCalendar = businessCalendar;
        this.businessCalendar.registerHandler(this);
    }

    @Required
    public void setMatches(List<String> matches) {
        this.matches = matches;
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
        if (this == businessCalendar.getHandler())
            dueDateSetter.setMatches(matches);
    }

    public KeyInfo getTaskKeyInfo(String procecessKey, String formKey)
    {
        return new KeyInfo(procecessKey, formKey);
    }
}

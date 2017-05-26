package com.alvexcore.repo.bcal;

import org.alfresco.service.ServiceRegistry;
import org.springframework.beans.factory.annotation.Required;

public abstract class AbstractDefaultLimitsProvider implements DefaultLimitsProvider {
    protected ServiceRegistry serviceRegistry;

    protected TaskLimitsInitializer taskLimitsInitializer;

    @Required
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Required
    public void setTaskLimitsInitializer(TaskLimitsInitializer taskLimitsInitializer) {
        this.taskLimitsInitializer = taskLimitsInitializer;
        taskLimitsInitializer.setDefaultLimitsProvider(this);
    }
}

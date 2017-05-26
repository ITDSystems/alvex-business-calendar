package com.alvexcore.repo.bcal;

import org.alfresco.service.ServiceRegistry;

public abstract class AbstractTaskMapper implements TaskMapper {
    protected ServiceRegistry serviceRegistry;

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }
}

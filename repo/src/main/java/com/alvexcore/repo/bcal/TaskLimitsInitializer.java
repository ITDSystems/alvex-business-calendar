package com.alvexcore.repo.bcal;

import org.springframework.context.ApplicationEvent;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;

public class TaskLimitsInitializer extends AbstractLifecycleBean {

    private BusinessCalendar businessCalendar;

    private DefaultLimitsProvider defaultLimitsProvider;

    public void setDefaultLimitsProvider(DefaultLimitsProvider defaultLimitsProvider) {
        this.defaultLimitsProvider = defaultLimitsProvider;
    }


    @Override
    protected void onBootstrap(ApplicationEvent applicationEvent) {
        if (defaultLimitsProvider != null)
            businessCalendar.setDefaultLimits(defaultLimitsProvider.getLimits());
    }

    @Override
    protected void onShutdown(ApplicationEvent applicationEvent) {
        //
    }

    public void setBusinessCalendar(BusinessCalendar businessCalendar) {
        this.businessCalendar = businessCalendar;
    }
}

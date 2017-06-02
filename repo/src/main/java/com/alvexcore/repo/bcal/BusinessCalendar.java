package com.alvexcore.repo.bcal;

import com.alvexcore.repo.kvstore.KeyValueStoreAware;
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar;
import net.objectlab.kit.datecalc.common.HolidayCalendar;
import org.alfresco.error.AlfrescoRuntimeException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class BusinessCalendar extends KeyValueStoreAware implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    public static final Integer DEFAULT_TASK_TIME_LIMIT = 2;
    public static final String MAP_NAME = "BusinessCalendar";

    private HolidayCalendar<LocalDate> holidayCalendar;

    private ConcurrentMap<String, Integer> limitsMap;
    private ApplicationContext applicationContext;
    private BusinessCalendarHandler handler;

    public BusinessCalendarHandler getHandler() {
        return handler;
    }

    public HolidayCalendar<LocalDate> getHolidayCalendar() {
        return holidayCalendar;
    }

    public Integer getTaskTimeLimit(String key) {
        return limitsMap.getOrDefault(key, DEFAULT_TASK_TIME_LIMIT);
    }

    public Map<String, Integer> getLimitsMap() {
        return Collections.unmodifiableMap(limitsMap);
    }

    public void setLimit(String formKey, Integer limit) {
        limitsMap.put(formKey, limit);
    }

    public void setLimits(Map<String, Integer> limits) {
        limitsMap.clear();
        updateLimits(limits);
    }

    public void updateLimits(Map<String, Integer> limits) {
        limitsMap.putAll(limits);
    }

    public void setDefaultLimits(Map<String, Integer> limits)
    {
        for (Map.Entry<String, Integer> entry: limits.entrySet())
            limitsMap.putIfAbsent(entry.getKey(), entry.getValue());
    }

    @Override
    protected void onKeyValueStoreReady() {
        limitsMap = keyValueStore.getStore(MAP_NAME);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        ApplicationContext closedContext = contextRefreshedEvent.getApplicationContext();
        if(closedContext != null && closedContext.equals(this.applicationContext)) {
            try {
                initialize();
            } catch (Exception e) {
                throw new AlfrescoRuntimeException("Failed to initialize business calendar", e);
            }
        }
    }

    private void initialize() throws IOException{
        holidayCalendar = new DefaultHolidayCalendar<>();
        holidayCalendar.setHolidays(handler.loadHolidaysList());

        setDefaultLimits(handler.getDefaultLimits());
    }

    public void registerHandler(BusinessCalendarHandler handler)
    {
        if (this.handler == null)
            this.handler = handler;
        else
            if (this.handler.getClass().equals(DefaultBusinessCalendarHandler.class))
                this.handler = handler;
            else
                if (!handler.getClass().equals(DefaultBusinessCalendarHandler.class))
                    throw new AlfrescoRuntimeException("More than one business calendar custom handler specified");
    }
}
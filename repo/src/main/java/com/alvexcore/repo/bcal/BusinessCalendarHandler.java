package com.alvexcore.repo.bcal;

import org.activiti.engine.delegate.DelegateTask;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public interface BusinessCalendarHandler {

    KeyInfo getTaskKeyInfo(DelegateTask delegateTask);
    Map<String, Integer> getDefaultLimits();
    Set<LocalDate> loadHolidaysList() throws IOException;

    int compareTaskKeys(String processKey, String key1, String key2);
    int compareProcessKeys(String key1, String key2);
}

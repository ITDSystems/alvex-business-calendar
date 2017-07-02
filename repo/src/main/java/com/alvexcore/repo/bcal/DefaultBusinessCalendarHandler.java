package com.alvexcore.repo.bcal;

import org.activiti.engine.delegate.DelegateTask;
import org.apache.commons.csv.CSVParser;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class DefaultBusinessCalendarHandler extends AbstractBusinessCalendarHandler {

    private String businessCalendarPath;

    @Required
    public void setBusinessCalendarPath(String businessCalendarPath) {
        this.businessCalendarPath = businessCalendarPath;
    }

    @Override
    public KeyInfo getTaskKeyInfo(DelegateTask delegateTask) {
        String processName = delegateTask.getProcessDefinitionId().split(":")[0];
        return getTaskKeyInfo(processName, delegateTask.getFormKey());
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

    @Override
    public Set<LocalDate> loadHolidaysList() throws IOException {
        return loadHolidaysListFromCsv(new URL(businessCalendarPath));
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
        return key1.compareTo(key2);
    }

    @Override
    public int compareProcessKeys(String key1, String key2) {
        return key1.compareTo(key2);
    }
}

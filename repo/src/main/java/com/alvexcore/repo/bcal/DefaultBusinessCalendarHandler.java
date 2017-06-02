package com.alvexcore.repo.bcal;

import org.activiti.engine.delegate.DelegateTask;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class DefaultBusinessCalendarHandler extends AbstractBusinessCalendarHandler {

    public static final String DEFAULT_BC_RESOURCE = "/russia-business-calendar-2019.csv";

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
        File file = new File(businessCalendarPath);
        URL url = file.exists() ? file.toURI().toURL() : this.getClass().getResource(DEFAULT_BC_RESOURCE);
        return loadHolidaysListFromCsv(url);
    }

    public static Set<LocalDate> loadHolidaysListFromCsv(URL businessCalendarPath) throws IOException
    {
        CSVParser parser = CSVParser.parse(businessCalendarPath, StandardCharsets.UTF_8, CSVFormat.RFC4180);
        List<CSVRecord> records = parser.getRecords();
        Set<LocalDate> holidays = new HashSet<>();
        LocalDate startFrom = LocalDate.now();
        for (int i = 1; i < records.size(); i++) {
            CSVRecord csvRecord = records.get(i);
            Integer year = Integer.valueOf(csvRecord.get(0));
            if (startFrom.getYear() > year)
                continue;
            for (int month = startFrom.getMonthValue(); month <= 12; month++) {
                for (String holiday : csvRecord.get(month).split(",")) {
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

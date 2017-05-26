package com.alvexcore.repo.bcal;

import com.alvexcore.repo.kvstore.KeyValueStoreAware;
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar;
import net.objectlab.kit.datecalc.common.HolidayCalendar;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class BusinessCalendar extends KeyValueStoreAware implements InitializingBean {

    public static final Integer DEFAULT_TASK_TIME_LIMIT = 2;
    public static final String MAP_NAME = "BusinessCalendar";

    private static final String DEFAULT_BC_RESOURCE = "/russia-business-calendar-2019.csv";

    private HolidayCalendar<LocalDate> holidayCalendar;

    String businessCalendarPath;

    private ConcurrentMap<String, Integer> limitsMap;

    public void setBusinessCalendarPath(String businessCalendarPath) {
        this.businessCalendarPath = businessCalendarPath;
    }

    public HolidayCalendar<LocalDate> getHolidayCalendar() {
        return holidayCalendar;
    }

    public Integer getTaskTimeLimit(String key) {
        return limitsMap.getOrDefault(key, DEFAULT_TASK_TIME_LIMIT);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        loadHolidayCalendar();
    }

    private void loadHolidayCalendar() throws IOException {
        File file = new File(businessCalendarPath);
        URL url = file.exists() ? file.toURI().toURL() : this.getClass().getResource(DEFAULT_BC_RESOURCE);
        CSVParser parser = CSVParser.parse(url, StandardCharsets.UTF_8, CSVFormat.RFC4180);
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
        holidayCalendar = new DefaultHolidayCalendar<>();
        holidayCalendar.setHolidays(holidays);
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
}
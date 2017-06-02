package com.alvexcore.repo.bcal;

import org.activiti.engine.delegate.DelegateTask;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class CustomBusinessCalendarHandler extends AbstractBusinessCalendarHandler {

    List<Integer> WORKFLOW_KINDS = Arrays.asList(1, 2, 3, 4, 5);

    private String businessCalendarPath;

    @Required
    public void setBusinessCalendarPath(String businessCalendarPath) {
        this.businessCalendarPath = businessCalendarPath;
    }

    @Override
    public KeyInfo getTaskKeyInfo(DelegateTask delegateTask) {
        String processName = delegateTask.getProcessDefinitionId().split(":")[0];
        Integer wfKind = (Integer) delegateTask.getVariable("someWorkflowKindVariable");
        if (!WORKFLOW_KINDS.contains(wfKind))
            wfKind = WORKFLOW_KINDS.get(0);
        return getTaskKeyInfo(wfKind, processName, delegateTask.getFormKey());
    }

    public KeyInfo getTaskKeyInfo(Integer workflowKind, String processKey, String taskKey)
    {
        return getTaskKeyInfo(workflowKind + "_" + processKey, taskKey);
    }

    @Override
    public Map<String, Integer> getDefaultLimits() {
        HashMap<String, Integer> limitsMap = new HashMap<>();


        Map<String, List<String>> tasks = getTasks(processDefinition -> true, true);

        for (Map.Entry<String, List<String>> entry: tasks.entrySet())
        {
            String workflow = entry.getKey();
            WORKFLOW_KINDS.forEach(kind -> {
                entry.getValue().forEach(
                    formKey -> limitsMap.putIfAbsent(getTaskKeyInfo(kind, workflow, formKey).toString(), 10)
                );
            });
        }

        return limitsMap;

    }

    @Override
    public Set<LocalDate> loadHolidaysList() throws IOException {
        File file = new File(businessCalendarPath);
        URL url = file.exists() ? file.toURI().toURL() : this.getClass().getResource(DefaultBusinessCalendarHandler.DEFAULT_BC_RESOURCE);
        return DefaultBusinessCalendarHandler.loadHolidaysListFromCsv(url);
    }

    @Override
    public int compareTaskKeys(String processKey, String key1, String key2) {
        return -key1.compareTo(key2);
    }

    @Override
    public int compareProcessKeys(String key1, String key2) {
        return -key1.compareTo(key2);
    }
}

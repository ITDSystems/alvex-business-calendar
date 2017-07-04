package com.alvexcore.repo.bcal.jobs;

import com.alvexcore.repo.RepositoryExtensionRegistry;
import com.alvexcore.repo.bcal.AbstractBusinessCalendarHandler;
import com.alvexcore.repo.bcal.BusinessCalendar;
import com.alvexcore.repo.kvstore.KeyValueStoreAware;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class SendTaskNotificationsJobExecuter extends KeyValueStoreAware {

    protected final static Log logger = LogFactory.getLog(SendTaskNotificationsJobExecuter.class);
    public static final String LAST_RUN = "alvex.bcal.notifications.lastRun";

    private RepositoryService repositoryService;
    private TaskService taskService;

    private BusinessCalendar businessCalendar;
    private AbstractBusinessCalendarHandler handler;

    private int deadline;
    private ConcurrentMap<String, LocalDate> alvexGlobalKVS;

    private LocalDate lastRun;

    @Required
    public void setBusinessCalendar(BusinessCalendar businessCalendar) {
        this.businessCalendar = businessCalendar;
        handler = businessCalendar.getHandler();
    }

    @Required
    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Required
    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }

    @Required
    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    void execute() {
        LocalDate now = LocalDate.now();

        if (lastRun == null)
            lastRun = alvexGlobalKVS.get(LAST_RUN);

        if (now.equals(lastRun))
        {
            logger.info("All notifications already sent, skipping");
            return;
        }

        logger.info("Sending email notifications");


        List<String> matches = handler.getMatches();

        int counter = 0;

        for (ProcessDefinition def: repositoryService.createProcessDefinitionQuery().list())
        {
            if (!matches.stream().anyMatch(p -> def.getKey().matches(p)))
                continue;

            for (Task task: taskService.createTaskQuery().processDefinitionId(def.getId()).list())
            {
                try{
                    Date _dueDate = task.getDueDate();
                    if (_dueDate == null)
                        continue;

                    LocalDate dueDate = _dueDate.toInstant().atOffset(businessCalendar.getZoneOffset()).toLocalDate();
                    if (dueDate.isBefore(now)) {
                        businessCalendar.onTaskOverdue(task);
                        counter++;
                    }
                    else
                        if (dueDate.equals(now))
                        {
                            businessCalendar.onTaskDeadlineToday(task);
                            counter++;
                        }
                        else
                        {
                            LocalDate deadlineDate = businessCalendar.getDeadlineDate(dueDate, deadline);
                            if (deadlineDate.isBefore(now)) {
                                businessCalendar.onTaskDeadlineApproaching(task);
                                counter++;
                            }
                        }

                } catch (ActivitiObjectNotFoundException e)
                {
                    logger.error("Error occurred while sending notifications", e);
                }
            }
        }

        logger.info(String.format("%d email notifications sent", counter));

        alvexGlobalKVS.put(LAST_RUN, now);
    }

    @Override
    protected void onKeyValueStoreReady() {
        alvexGlobalKVS = keyValueStore.getStore(RepositoryExtensionRegistry.ALVEX_GLOBAL_KEY_VALUE_STORE);
    }
}
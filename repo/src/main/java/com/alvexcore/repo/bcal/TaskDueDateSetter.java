package com.alvexcore.repo.bcal;

import com.alvexcore.repo.workflow.activiti.AlvexActivitiListener;
import net.objectlab.kit.datecalc.jdk8.LocalDateCalculator;
import net.objectlab.kit.datecalc.jdk8.LocalDateForwardHandler;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.alfresco.repo.workflow.WorkflowNotificationUtils;
import org.springframework.beans.factory.annotation.Required;

import java.time.LocalDate;
import java.util.Date;

public class TaskDueDateSetter extends AlvexActivitiListener implements TaskListener, ExecutionListener {

    private static final String TASK_CREATED = "TASK_CREATED";
    private BusinessCalendar businessCalendar;
    private int lastHour;
    private int lastMinute;
    private int lastSecond;

    @Required
    public void setLastHour(int lastHour) {
        this.lastHour = lastHour;
    }

    @Required
    public void setLastMinute(int lastMinute) {
        this.lastMinute = lastMinute;
    }

    @Required
    public void setLastSecond(int lastSecond) {
        this.lastSecond = lastSecond;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();

        if (eventName.equals(TaskListener.EVENTNAME_CREATE)) {
            setTaskDueDate(delegateTask);
            delegateTask.setVariableLocal(TASK_CREATED, true);
            businessCalendar.onTaskAssigned(delegateTask);
        }
        else if (eventName.equals(TaskListener.EVENTNAME_ASSIGNMENT)) {
            if (businessCalendar.getHandler().isRealAssignee(delegateTask.getAssignee()))
                if (Boolean.TRUE.equals(delegateTask.getVariableLocal(TASK_CREATED)))
                    businessCalendar.onTaskAssigned(delegateTask);
        }
    }

    private void setTaskDueDate(DelegateTask delegateTask) {
        AbstractBusinessCalendarHandler handler = businessCalendar.getHandler();
        LocalDate now = LocalDate.now();
        LocalDateCalculator calculator = new LocalDateCalculator(null, now, businessCalendar.getHolidayCalendar(), new LocalDateForwardHandler());

        Integer timeLimit = businessCalendar.getTaskTimeLimit(handler.getTaskKeyInfo(delegateTask).toString());

        if (timeLimit < 0)
            return;

        if (calculator.getCurrentBusinessDate() != now)
            timeLimit--;

        calculator.moveByBusinessDays(timeLimit);

        Date date = Date.from(calculator.getCurrentBusinessDate().atTime(lastHour, lastMinute, lastSecond).toInstant(businessCalendar.getZoneOffset()));

        delegateTask.setDueDate(date);
    }

    public void setBusinessCalendar(BusinessCalendar businessCalendar) {
        this.businessCalendar = businessCalendar;
    }


    @Override
    public void notify(DelegateExecution execution) throws Exception {
        // Disable out-of-the-box notifications
        execution.setVariableLocal(WorkflowNotificationUtils.PROP_SEND_EMAIL_NOTIFICATIONS, false);
    }
}

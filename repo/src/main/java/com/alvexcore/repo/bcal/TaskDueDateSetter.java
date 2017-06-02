package com.alvexcore.repo.bcal;

import com.alvexcore.repo.workflow.activiti.AlvexActivitiListener;
import net.objectlab.kit.datecalc.jdk8.LocalDateCalculator;
import net.objectlab.kit.datecalc.jdk8.LocalDateForwardHandler;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.springframework.beans.factory.annotation.Required;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class TaskDueDateSetter extends AlvexActivitiListener implements TaskListener {

    private BusinessCalendar businessCalendar;

    @Override
    public void notify(DelegateTask delegateTask) {
        BusinessCalendarHandler handler = businessCalendar.getHandler();
        LocalDateCalculator calculator = new LocalDateCalculator(null, LocalDate.now(), businessCalendar.getHolidayCalendar(), new LocalDateForwardHandler());

        Integer timeLimit = businessCalendar.getTaskTimeLimit(handler.getTaskKeyInfo(delegateTask).toString());
        if (timeLimit < 0)
            return;

        calculator.moveByBusinessDays(timeLimit);

        Date date = Date.from(calculator.getCurrentBusinessDate().atStartOfDay(ZoneId.systemDefault()).toInstant());

        delegateTask.setDueDate(date);
    }

    public void setBusinessCalendar(BusinessCalendar businessCalendar) {
        this.businessCalendar = businessCalendar;
    }


}

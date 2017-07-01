package com.alvexcore.repo.bcal;

import com.alvexcore.repo.workflow.activiti.AlvexActivitiListener;
import net.objectlab.kit.datecalc.jdk8.LocalDateCalculator;
import net.objectlab.kit.datecalc.jdk8.LocalDateForwardHandler;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.springframework.beans.factory.annotation.Required;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

public class TaskDueDateSetter extends AlvexActivitiListener implements TaskListener {

    private BusinessCalendar businessCalendar;
    private int lastHour;
    private int lastMinute;
    private int lastSecond;
    private ZoneOffset zoneOffset;

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
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        Instant instant = Instant.now();
        ZoneId systemZone = ZoneId.systemDefault();
        zoneOffset = systemZone.getRules().getOffset(instant);

    }

    @Override
    public void notify(DelegateTask delegateTask) {
        BusinessCalendarHandler handler = businessCalendar.getHandler();
        LocalDateCalculator calculator = new LocalDateCalculator(null, LocalDate.now(), businessCalendar.getHolidayCalendar(), new LocalDateForwardHandler());

        Integer timeLimit = businessCalendar.getTaskTimeLimit(handler.getTaskKeyInfo(delegateTask).toString());
        if (timeLimit < 0)
            return;

        calculator.moveByBusinessDays(timeLimit);

        Date date = Date.from(calculator.getCurrentBusinessDate().atTime(lastHour, lastMinute, lastSecond).toInstant(zoneOffset));

        delegateTask.setDueDate(date);
    }

    public void setBusinessCalendar(BusinessCalendar businessCalendar) {
        this.businessCalendar = businessCalendar;
    }


}

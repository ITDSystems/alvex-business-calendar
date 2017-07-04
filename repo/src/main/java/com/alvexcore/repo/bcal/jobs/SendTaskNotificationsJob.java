package com.alvexcore.repo.bcal.jobs;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.schedule.AbstractScheduledLockedJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

public class SendTaskNotificationsJob extends AbstractScheduledLockedJob implements StatefulJob {

    @Override
    public void executeJob(JobExecutionContext jobContext) throws JobExecutionException {
        JobDataMap jobData = jobContext.getJobDetail().getJobDataMap();
        SendTaskNotificationsJobExecuter executer = (SendTaskNotificationsJobExecuter) jobData.get("executer");

        AuthenticationUtil.runAsSystem(() -> {
            executer.execute();
            return null;
        });
    }
}
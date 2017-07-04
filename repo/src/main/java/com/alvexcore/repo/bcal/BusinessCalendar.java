package com.alvexcore.repo.bcal;

import com.alvexcore.repo.kvstore.KeyValueStoreAware;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar;
import net.objectlab.kit.datecalc.common.HolidayCalendar;
import net.objectlab.kit.datecalc.jdk8.LocalDateBackwardHandler;
import net.objectlab.kit.datecalc.jdk8.LocalDateCalculator;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.task.Task;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class BusinessCalendar extends KeyValueStoreAware implements InitializingBean, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private final Log logger = LogFactory.getLog(BusinessCalendar.class);

    public static final Integer DEFAULT_TASK_TIME_LIMIT = 2;
    public static final String DUEDATE_LIMITS = "DuedateLimits";

    public static final String ON_ASSIGN_TEMPLATE_KEY = "onAssign";
    public static final String ON_DEADLINE_TEMPLATE_KEY = "onDeadline";
    public static final String ON_DEADLINE_TODAY_TEMPLATE_KEY = "onDeadlineToday";
    public static final String ON_OVERDUE_TEMPLATE_KEY = "onOverdue";
    public static final String BODY_TEMPLATE_KEY = "Body";
    public static final String SUBJECT_TEMPLATE_KEY = "Subject";

    private ZoneOffset zoneOffset;

    private HolidayCalendar<LocalDate> holidayCalendar;

    private ConcurrentMap<String, Integer> limitsMap;
    private ApplicationContext applicationContext;
    private AbstractBusinessCalendarHandler handler;

    private ActionService actionService;
    private PersonService personService;
    private NodeService nodeService;

    private String emailFrom;
    private Configuration freemarkerCfg;
    private Map<String, String> templateUrls = new HashMap<>();

    public BusinessCalendar() {
        Instant instant = Instant.now();
        ZoneId systemZone = ZoneId.systemDefault();
        zoneOffset = systemZone.getRules().getOffset(instant);
    }

    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }

    @Required
    public void setServices(ServiceRegistry services)
    {
        actionService = services.getActionService();
        personService = services.getPersonService();
        nodeService = services.getNodeService();
    }

    @Required
    public void setOnAssignBodyTemplatePath(String path) throws MalformedURLException {
        templateUrls.put(ON_ASSIGN_TEMPLATE_KEY + BODY_TEMPLATE_KEY, path);
    }

    @Required
    public void setOnDeadlineBodyTemplatePath(String path) throws MalformedURLException {
        templateUrls.put(ON_DEADLINE_TEMPLATE_KEY + BODY_TEMPLATE_KEY, path);
    }

    @Required
    public void setOnDeadlineTodayBodyTemplatePath(String path) throws MalformedURLException {
        templateUrls.put(ON_DEADLINE_TODAY_TEMPLATE_KEY + BODY_TEMPLATE_KEY, path);
    }

    @Required
    public void setOnOverdueBodyTemplatePath(String path) throws MalformedURLException {
        templateUrls.put(ON_OVERDUE_TEMPLATE_KEY + BODY_TEMPLATE_KEY, path);
    }

    @Required
    public void setOnAssignSubjectTemplatePath(String path) throws MalformedURLException {
        templateUrls.put(ON_ASSIGN_TEMPLATE_KEY + SUBJECT_TEMPLATE_KEY, path);
    }

    @Required
    public void setOnDeadlineSubjectTemplatePath(String path) throws MalformedURLException {
        templateUrls.put(ON_DEADLINE_TEMPLATE_KEY + SUBJECT_TEMPLATE_KEY, path);
    }

    @Required
    public void setOnDeadlineTodaySubjectTemplatePath(String path) throws MalformedURLException {
        templateUrls.put(ON_DEADLINE_TODAY_TEMPLATE_KEY + SUBJECT_TEMPLATE_KEY, path);
    }

    @Required
    public void setOnOverdueSubjectTemplatePath(String path) throws MalformedURLException {
        templateUrls.put(ON_OVERDUE_TEMPLATE_KEY + SUBJECT_TEMPLATE_KEY, path);
    }

    public AbstractBusinessCalendarHandler getHandler() {
        return handler;
    }

    public HolidayCalendar<LocalDate> getHolidayCalendar() {
        return holidayCalendar;
    }

    public Integer getTaskTimeLimit(String key) {
        return limitsMap.getOrDefault(key, DEFAULT_TASK_TIME_LIMIT);
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
        limitsMap = keyValueStore.getStore(DUEDATE_LIMITS);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        ApplicationContext closedContext = contextRefreshedEvent.getApplicationContext();
        if(closedContext != null && closedContext.equals(this.applicationContext)) {
            try {
                initialize();
            } catch (Exception e) {
                throw new AlfrescoRuntimeException("Failed to initialize business calendar", e);
            }
        }
    }

    private void initialize() throws IOException{
        holidayCalendar = new DefaultHolidayCalendar<>();
        holidayCalendar.setHolidays(handler.loadHolidaysList());

        setDefaultLimits(handler.getDefaultLimits());
    }

    public void registerHandler(AbstractBusinessCalendarHandler handler)
    {
        if (this.handler == null)
            this.handler = handler;
        else
            if (this.handler.getClass().equals(DefaultBusinessCalendarHandler.class))
                this.handler = handler;
            else
                if (!handler.getClass().equals(DefaultBusinessCalendarHandler.class))
                    throw new AlfrescoRuntimeException("More than one business calendar custom handler specified");
    }

    public void onTaskAssigned(DelegateTask task) {
        Task taskInfo = (Task) task;
        try {
            sendEmail(taskInfo, ON_ASSIGN_TEMPLATE_KEY);
        } catch (Exception e) {
            logger.error("Failed to send email", e);
        }
    }

    public void onTaskDeadlineApproaching(Task task) {
        try {
            sendEmail(task, ON_DEADLINE_TEMPLATE_KEY);
        } catch (Exception e) {
            logger.error("Failed to send email", e);
        }
    }

    public void onTaskDeadlineToday(Task task) {
        try {
            sendEmail(task, ON_DEADLINE_TODAY_TEMPLATE_KEY);
        } catch (Exception e) {
            logger.error("Failed to send email", e);
        }
    }

    public void onTaskOverdue(Task task) {
        try {
            sendEmail(task, ON_OVERDUE_TEMPLATE_KEY);
        } catch (Exception e) {
            logger.error("Failed to send email", e);
        }
    }

    protected void sendEmail(Task task, String templateKey) throws IOException, TemplateException {
        Action mailAction = actionService.createAction(MailActionExecuter.NAME);

        NodeRef personRef = personService.getPerson(task.getAssignee());
        String emailTo = (String) nodeService.getProperty(personRef, ContentModel.PROP_EMAIL);

        Object model = handler.buildEmailModel(task, personRef);

        StringWriter writer = new StringWriter();

        Template bodyTemplate = freemarkerCfg.getTemplate(templateKey + BODY_TEMPLATE_KEY);
        bodyTemplate.process(model, writer);
        String emailBody = writer.toString();

        writer.getBuffer().setLength(0);

        Template subjectTemplate = freemarkerCfg.getTemplate(templateKey + SUBJECT_TEMPLATE_KEY);
        subjectTemplate.process(model, writer);
        String subject = writer.toString();


        mailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT, subject);
        mailAction.setParameterValue(MailActionExecuter.PARAM_TO, emailTo);
        mailAction.setParameterValue(MailActionExecuter.PARAM_FROM, emailFrom);
        mailAction.setParameterValue(MailActionExecuter.PARAM_TEXT, emailBody);
        mailAction.setParameterValue(MailActionExecuter.PARAM_HTML, true);
        mailAction.setParameterValue(MailActionExecuter.PARAM_IGNORE_SEND_FAILURE, true);
        mailAction.setExecuteAsynchronously(true);

        actionService.executeAction(mailAction, null);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        freemarkerCfg = new Configuration();
        freemarkerCfg.setDefaultEncoding("UTF-8");

        StringTemplateLoader templateLoader = new StringTemplateLoader();
        freemarkerCfg.setTemplateLoader(templateLoader);

        for (String key1: Arrays.asList(ON_ASSIGN_TEMPLATE_KEY, ON_DEADLINE_TEMPLATE_KEY, ON_OVERDUE_TEMPLATE_KEY))
            for (String key2: Arrays.asList(BODY_TEMPLATE_KEY, SUBJECT_TEMPLATE_KEY)) {
                String key = key1 + key2;
                templateLoader.putTemplate(key, IOUtils.toString(new URL(templateUrls.get(key))));
            }
    }

    @Required
    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public LocalDate getDeadlineDate(LocalDate dueDate, int deadline) {
        LocalDateCalculator calculator = new LocalDateCalculator(null, dueDate, getHolidayCalendar(), new LocalDateBackwardHandler());

        deadline = Math.abs(deadline) - 1;

        if (deadline <= 0)
            return dueDate;

        calculator.moveByBusinessDays(-deadline);

        return calculator.getCurrentBusinessDate();
    }
}
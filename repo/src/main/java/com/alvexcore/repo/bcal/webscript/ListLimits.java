package com.alvexcore.repo.bcal.webscript;

import com.alvexcore.repo.bcal.TaskMapper;
import org.alfresco.repo.i18n.MessageService;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import com.alvexcore.repo.bcal.BusinessCalendar;

import java.io.IOException;
import java.util.Map;

public class ListLimits extends AbstractWebScript {
    private BusinessCalendar businessCalendar;
    private MessageService messageService;
    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;

    @Required
    public void setBusinessCalendar(BusinessCalendar businessCalendar) {
        this.businessCalendar = businessCalendar;
    }

    @Required
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.messageService = serviceRegistry.getMessageService();
        this.dictionaryService = serviceRegistry.getDictionaryService();
        this.namespaceService = serviceRegistry.getNamespaceService();
    }


    protected Object maybeEmptyString(String s)
    {
        return s == null ? "" : s;
    }

    @Override
    public void execute(WebScriptRequest webScriptRequest, WebScriptResponse webScriptResponse) throws IOException {
        Map<String, Integer> limits = businessCalendar.getLimitsMap();

        JSONObject resp = new JSONObject();

        try {
            for (Map.Entry<String, Integer> entry: limits.entrySet())
            {
                TaskMapper.KeyInfo info = TaskMapper.KeyInfo.fromString(entry.getKey());

                TypeDefinition typeDefinition = dictionaryService.getType(QName.createQName(info.getTaskKey(), namespaceService));
                String modelName = typeDefinition.getModel().getName().toPrefixString();

                JSONObject proc;
                if (resp.has(info.getProcessKey()))
                    proc = resp.getJSONObject(info.getProcessKey());
                else
                {
                    proc = new JSONObject();
                    String processLabel = messageService.getMessage(String.format("alvex.workflow.duedates.process.%s", info.getFilteredProcessKey()));
                    proc.put("processLabel", maybeEmptyString(processLabel));
                    proc.put("limits", new JSONArray());
                    resp.put(info.getProcessKey(), proc);
                }

                JSONObject limitInfo = new JSONObject();
                limitInfo.put("taskKey", info.getTaskKey());
                limitInfo.put("limitKey", entry.getKey());
                limitInfo.put("limit", entry.getValue());
                String taskLabel = messageService.getMessage(String.format("%s.type.%s.title", modelName.replace(":", "_"), info.getFilteredTaskKey()));
                limitInfo.put("taskLabel", maybeEmptyString(taskLabel));

                proc.getJSONArray("limits").put(limitInfo);
            }
        } catch (JSONException e) {
            int i = 0;
        }

        webScriptResponse.setContentEncoding("UTF-8");
        webScriptResponse.setContentType("application/json");
        webScriptResponse.getWriter().write(resp.toString());
    }
}

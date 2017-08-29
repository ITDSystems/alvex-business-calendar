package com.alvexcore.repo.bcal.webscript;

import com.alvexcore.repo.bcal.AbstractBusinessCalendarHandler;
import com.alvexcore.repo.bcal.BusinessCalendar;
import com.alvexcore.repo.bcal.KeyInfo;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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


    protected String getMessage(String key)
    {
        String msg = messageService.getMessage(key);
        return msg == null ? key : msg;
    }

    @Override
    public void execute(WebScriptRequest webScriptRequest, WebScriptResponse webScriptResponse) throws IOException {
        AbstractBusinessCalendarHandler handler = businessCalendar.getHandler();

        Map<String, Integer> limits = businessCalendar.getLimitsMap();

        JSONObject resp = new JSONObject();
        JSONArray jsonLimits = new JSONArray();
        try {
            resp.put("limits", jsonLimits);
        } catch (JSONException e) {
            //
        }

        List<String> sortedProcessKeys = limits.keySet().stream()
            .map(key -> KeyInfo.getProcessKey(key))
            .distinct()
            .sorted((k1, k2) -> handler.compareProcessKeys(k1, k2))
        .collect(Collectors.toList());

        Map<String, List<String>> sortedTaskKeys = new HashMap<>();
        sortedProcessKeys.forEach(key -> sortedTaskKeys.put(key,  new ArrayList<>()));

        limits.keySet().forEach(key -> {
            KeyInfo keyInfo = KeyInfo.fromString(key);
            sortedTaskKeys.get(keyInfo.getProcessKey()).add(keyInfo.getTaskKey());
        });
        sortedTaskKeys.entrySet().forEach(entry -> entry.getValue().sort((k1, k2) -> handler.compareTaskKeys(entry.getKey(), k1, k2)));

        try {
            for (String processKey: sortedProcessKeys)
            {
                JSONObject proc = new JSONObject();
                String processLabelKey = String.format("%s.workflow.title", processKey).replace(":", "_");
                proc.put("processLabel", getMessage(processLabelKey));
                JSONArray jsonTaskLimits = new JSONArray();
                proc.put("limits", jsonTaskLimits);
                jsonLimits.put(proc);

                for (String taskKey: sortedTaskKeys.get(processKey)) {
                    TypeDefinition typeDefinition = dictionaryService.getType(QName.createQName(taskKey, namespaceService));
                    String modelName = typeDefinition.getModel().getName().toPrefixString();

                    KeyInfo keyInfo = new KeyInfo(processKey, taskKey);
                    JSONObject limitInfo = new JSONObject();
                    limitInfo.put("taskKey", taskKey);
                    String limitKey = keyInfo.toString();
                    limitInfo.put("limitKey", limitKey);
                    limitInfo.put("limit", limits.get(limitKey));
                    String taskLabelKey = String.format("%s.type.%s.title", modelName, taskKey).replace(":", "_");
                    limitInfo.put("taskLabel", getMessage(taskLabelKey));

                    jsonTaskLimits.put(limitInfo);
                }
            }
        } catch (JSONException e) {
            int i = 0;
        }

        webScriptResponse.setContentEncoding("UTF-8");
        webScriptResponse.setContentType("application/json");
        webScriptResponse.getWriter().write(resp.toString());
    }
}

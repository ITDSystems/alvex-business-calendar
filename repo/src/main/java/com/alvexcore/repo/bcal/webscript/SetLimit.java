package com.alvexcore.repo.bcal.webscript;

import com.alvexcore.repo.bcal.BusinessCalendar;
import org.alfresco.error.AlfrescoRuntimeException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class SetLimit extends AbstractWebScript {
    private BusinessCalendar businessCalendar;

    @Override
    public void execute(WebScriptRequest webScriptRequest, WebScriptResponse webScriptResponse) {
        String limitKey = webScriptRequest.getServiceMatch().getTemplateVars().get("limitKey");

        JSONObject req = (JSONObject) webScriptRequest.parseContent();

        Integer limit;
        try {
            limit = req.getInt("limit");
        } catch (JSONException e) {
            throw new AlfrescoRuntimeException("Cannot parse request", e);
        }

        businessCalendar.setLimit(limitKey, limit);
    }

    public void setBusinessCalendar(BusinessCalendar businessCalendar) {
        this.businessCalendar = businessCalendar;
    }
}

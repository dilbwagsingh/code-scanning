package com.mykaarma.kcommunications_model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageRedactResponse extends Response {

    private String redactedMessageBody;
    private String requestUUID;

    public String getRedactedMessageBody() {
        return redactedMessageBody;
    }

    public String getRequestUUID() {
        return requestUUID;
    }

    public void setRequestUUID(String requestUUID) {
        this.requestUUID = requestUUID;
    }
    
    public void setRedactedMessageBody(String redactedMessageBody) {
        this.redactedMessageBody = redactedMessageBody;
    }

}
package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;
import java.util.List;

public class MessageRedactListResponse implements Serializable {
    
    private String responseUUID;
    private List<MessageRedactResponse> messages;

    public void setMessages(List<MessageRedactResponse> messages) {
        this.messages= messages;
    }

    public List<MessageRedactResponse> getMessages() {
        return messages;
    }

    public String getResponseUUID() {
        return responseUUID;
    }

    public void setResponseUUID(String responseUUID) {
        this.responseUUID = responseUUID;
    }

}
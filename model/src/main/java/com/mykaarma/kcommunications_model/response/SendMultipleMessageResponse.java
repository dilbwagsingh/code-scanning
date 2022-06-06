package com.mykaarma.kcommunications_model.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SendMultipleMessageResponse extends Response {

    private List<SendMessageResponse> sendMessageResponses;
    private String requestUUID;

    public List<SendMessageResponse> getSendMessageResponses() {
        return sendMessageResponses;
    }

    public String getRequestUUID() {
        return requestUUID;
    }

    public void setRequestUUID(String requestUUID) {
        this.requestUUID = requestUUID;
    }

    public void setSendMessageResponses(List<SendMessageResponse> sendMessageResponses) {
        this.sendMessageResponses = sendMessageResponses;
    }
    
}

package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageRedactResponse implements Serializable {

    @JsonProperty(value = "messageID")
    private String messageID;

    @JsonProperty(value = "messageBody")
    private String messageBody;

    public String getMessageID() {
        return messageID;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

}
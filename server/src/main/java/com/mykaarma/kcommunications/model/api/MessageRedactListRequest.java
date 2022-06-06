package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageRedactListRequest implements Serializable {

    @JsonProperty(value = "campaignID")
    private Long campaignID;

    @JsonProperty(value = "messages")
    private List<MessageRedactRequest> messages;

    public Long getCampaignID() {
        return campaignID;
    }

    public void setCampaignID(Long campaignID) {
        this.campaignID = campaignID;
    }

    public List<MessageRedactRequest> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageRedactRequest> messages) {
        this.messages = messages;
    }
}
package com.mykaarma.kcommunications_model.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SendDraftRequest implements Serializable {

    @JsonProperty("sendSynchronously")
    private Boolean sendSynchronously = false;

    public Boolean getSendSynchronously() {
        return sendSynchronously;
    }

    public void setSendSynchronously(Boolean sendSynchronously) {
        this.sendSynchronously = sendSynchronously;
    }

}

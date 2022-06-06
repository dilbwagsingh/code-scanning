package com.mykaarma.kcommunications_model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PredictPreferredCommunicationModeResponse extends Response {

    private String requestUUID;
    private Boolean isProcessed;

    public String getRequestUUID() {
        return requestUUID;
    }

    public Boolean getIsProcessed() {
        return isProcessed;
    }

    public void setIsProcessed(Boolean isProcessed) {
        this.isProcessed = isProcessed;
    }

    public void setRequestUUID(String requestUUID) {
        this.requestUUID = requestUUID;
    }

}

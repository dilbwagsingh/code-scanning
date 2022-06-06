package com.mykaarma.kcommunications_model.response;

public class UpdateMessagePredictionFeedbackResponse extends Response {

    private Boolean isThreadStatusUpdated;

    public Boolean getIsThreadStatusUpdated() {
        return isThreadStatusUpdated;
    }

    public void setIsThreadStatusUpdated(Boolean isThreadStatusUpdated) {
        this.isThreadStatusUpdated = isThreadStatusUpdated;
    }
}

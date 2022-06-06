package com.mykaarma.kcommunications_model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.MultipleMessageSendingAttributes;

public class MultipleMessageRequest {

    @JsonProperty("messageAttributes")
    private MessageAttributes messageAttributes;

    @JsonProperty("multipleMessageSendingAttributes")
    private MultipleMessageSendingAttributes multipleMessageSendingAttributes;

    public MessageAttributes getMessageAttributes() {
        return messageAttributes;
    }

    public MultipleMessageSendingAttributes getMultipleMessageSendingAttributes() {
        return multipleMessageSendingAttributes;
    }

    public void setMultipleMessageSendingAttributes(MultipleMessageSendingAttributes multipleMessageSendingAttributes) {
        this.multipleMessageSendingAttributes = multipleMessageSendingAttributes;
    }

    public void setMessageAttributes(MessageAttributes messageAttributes) {
		this.messageAttributes = messageAttributes;
	}

}

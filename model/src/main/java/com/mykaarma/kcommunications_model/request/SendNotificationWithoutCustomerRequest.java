package com.mykaarma.kcommunications_model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.NotificationWithoutCustomerAttributes;

public class SendNotificationWithoutCustomerRequest {
	
	@JsonProperty("messageAttributes")
	private MessageAttributes messageAttributes;
	
	@JsonProperty("notificationAttributes")
	private NotificationWithoutCustomerAttributes notificationAttributes;

	public MessageAttributes getMessageAttributes() {
		return messageAttributes;
	}

	public void setMessageAttributes(MessageAttributes messageAttributes) {
		this.messageAttributes = messageAttributes;
	}

	public NotificationWithoutCustomerAttributes getNotificationAttributes() {
		return notificationAttributes;
	}

	public void setNotificationAttributes(NotificationWithoutCustomerAttributes notificationAttributes) {
		this.notificationAttributes = notificationAttributes;
	}
}

package com.mykaarma.kcommunications_model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mykaarma.kcommunications_model.enums.Status;

@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendNotificationWithoutCustomerResponse extends Response {
	private String notificationMessageUUID;
	private Status status;
	private String requestUUID;
	
	public String getNotificationMessageUUID() {
		return notificationMessageUUID;
	}

	public void setNotificationMessageUUID(String notificationMessageUUID) {
		this.notificationMessageUUID = notificationMessageUUID;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getRequestUUID() {
		return requestUUID;
	}

	public void setRequestUUID(String requestUUID) {
		this.requestUUID = requestUUID;
	}
	
}

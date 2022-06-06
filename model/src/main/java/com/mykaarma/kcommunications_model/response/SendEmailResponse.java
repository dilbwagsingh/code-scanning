package com.mykaarma.kcommunications_model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mykaarma.kcommunications_model.enums.Status;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SendEmailResponse extends Response {
	private Status status;
	
	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
}

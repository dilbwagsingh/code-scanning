package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommunicationStatusResponse implements Serializable{

	@JsonProperty(value = "response")
	private Response response;
	
	@JsonProperty(value = "optoutStatus")
	private String optoutStatus;
	
	public Response getResponse() {
		return response;
	}
	
	public void setResponse(Response response) {
		this.response = response;
	}

	public String getOptoutStatus() {
		return optoutStatus;
	}

	public void setOptoutStatus(String optoutStatus) {
		this.optoutStatus = optoutStatus;
	}
	
	public CommunicationStatusResponse() {
		
	}
	
}


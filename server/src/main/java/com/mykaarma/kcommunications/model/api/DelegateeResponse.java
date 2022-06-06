package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DelegateeResponse implements Serializable {
	
	@JsonProperty(value = "response")
	private Response response;
	
	@JsonProperty(value = "delegateeDAID")
	private Long delegateeDAID=null;

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

	public Long getDelegateeDAID() {
		return delegateeDAID;
	}

	public void setDelegateeDAID(Long delegateeDAID) {
		this.delegateeDAID = delegateeDAID;
	}

}
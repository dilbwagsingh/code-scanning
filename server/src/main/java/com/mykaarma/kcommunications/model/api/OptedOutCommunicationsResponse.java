package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OptedOutCommunicationsResponse implements Serializable {

	@JsonProperty(value = "response")
	private Response response;
	
	@JsonProperty(value = "optoutCommunicationList")
	private List<String> optoutCommunicationList=new ArrayList<String>();
	
	public List<String> getOptoutCommunicationList() {
		return optoutCommunicationList;
	}

	public void setOptoutCommunicationList(List<String> optoutCommunicationList) {
		this.optoutCommunicationList = optoutCommunicationList;
	}

	public Response getResponse() {
		return response;
	}
	
	public void setResponse(Response response) {
		this.response = response;
	}
	
	public OptedOutCommunicationsResponse() {
		
	}
}
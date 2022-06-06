package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommunicationListRequest implements Serializable {
	
	@JsonProperty(value = "communicationValueList")
	private List<String> communicationValueList=new ArrayList<String>();
	
	public CommunicationListRequest() {
		
	}

	public List<String> getCommunicationValueList() {
		return communicationValueList;
	}

	public void setCommunicationValueList(List<String> communicationValueList) {
		this.communicationValueList = communicationValueList;
	}
}


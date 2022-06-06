package com.mykaarma.kcommunications_model.response;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommunicationHistoryResponse extends Response{

	@JsonProperty("communicationHistoryPdfUrl")
	String communicationHistoryPdfUrl = null;
	
	@JsonProperty("requestSubmitted")
	public String requestSubmitted;
	
	public String getRequestSubmitted() {
		return requestSubmitted;
	}

	public void setRequestSubmitted(String requestSubmitted) {
		this.requestSubmitted = requestSubmitted;
	}

	public String getcommunicationHistoryPdfUrl(){
		return communicationHistoryPdfUrl;
	}
	
	public void setcommunicationHistoryPdfUrl(String communicationHistoryHtml){
		this.communicationHistoryPdfUrl=communicationHistoryHtml;
	}
}

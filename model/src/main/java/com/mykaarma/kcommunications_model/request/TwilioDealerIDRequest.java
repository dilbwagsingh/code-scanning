package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TwilioDealerIDRequest {

	@JsonProperty("dealerIDList")
	private List<Long> dealerIDList;

	@JsonProperty("url")
	private String url;
	
	@JsonProperty("operationType")
	private String operationType;
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<Long> getDealerIDList() {
		return dealerIDList;
	}

	public void setDealerIDList(List<Long> dealerIDList) {
		this.dealerIDList = dealerIDList;
	}
	
	public String getOperationType() {
		return operationType;
	}
	
	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}
	
}

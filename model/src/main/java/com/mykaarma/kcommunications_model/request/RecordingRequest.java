package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecordingRequest {

	@JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;
    
	@JsonProperty("dealerIDs")
	private List<Long> dealerIDs;

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public List<Long> getDealerIDs() {
		return dealerIDs;
	}

	public void setDealerIDs(List<Long> dealerIDs) {
		this.dealerIDs = dealerIDs;
	}
	
}

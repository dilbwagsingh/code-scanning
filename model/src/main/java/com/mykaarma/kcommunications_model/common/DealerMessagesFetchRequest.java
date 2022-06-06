package com.mykaarma.kcommunications_model.common;

import java.util.Date;

public class DealerMessagesFetchRequest {

	private Long dealerID;
	private String startDate;
	private String endDate;
	private Boolean deleteRecordings;
	private Boolean verifyRecordings;
	
	public Boolean getVerifyRecordings() {
		return verifyRecordings;
	}

	public void setVerifyRecordings(Boolean verifyRecordings) {
		this.verifyRecordings = verifyRecordings;
	}

	public Boolean getDeleteRecordings() {
		return deleteRecordings;
	}

	public void setDeleteRecordings(Boolean deleteRecordings) {
		this.deleteRecordings = deleteRecordings;
	}

	private Integer expiration;
	
	public Integer getExpiration() {
		return expiration;
	}

	public void setExpiration(Integer expiration) {
		this.expiration = expiration;
	}

	public Long getDealerID() {
		return dealerID;
	}
	
	public void setDealerID(Long dealerID) {
		this.dealerID = dealerID;
	}
	
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
	
}

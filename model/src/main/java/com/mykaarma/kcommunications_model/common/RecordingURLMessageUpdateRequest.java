package com.mykaarma.kcommunications_model.common;

public class RecordingURLMessageUpdateRequest {

	private Long messageID;

	private Long dealerID;

	private Integer expiration;
	
	private Boolean deleteRecordings;
	
	private Boolean verifyRecording;
	
	public Boolean getVerifyRecording() {
		return verifyRecording;
	}

	public void setVerifyRecording(Boolean verifyRecording) {
		this.verifyRecording = verifyRecording;
	}

	public Boolean getDeleteRecordings() {
		return deleteRecordings;
	}

	public void setDeleteRecordings(Boolean deleteRecordings) {
		this.deleteRecordings = deleteRecordings;
	}

	public Integer getExpiration() {
		return expiration;
	}

	public void setExpiration(Integer expiration) {
		this.expiration = expiration;
	}
	
	public Long getMessageID() {
		return messageID;
	}

	public void setMessageID(Long messageID) {
		this.messageID = messageID;
	}
	
	public Long getDealerID() {
		return dealerID;
	}

	public void setDealerID(Long dealerID) {
		this.dealerID = dealerID;
	}
	
}

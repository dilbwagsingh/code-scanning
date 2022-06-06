package com.mykaarma.kcommunications_model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateMessagePredictionFeedbackRequest {
	
	@JsonProperty("messagePredictionID")
	private Long messagePredictionID;

	@JsonProperty("userFeedback")
	private String userFeedback;

	@JsonProperty("reason")
	private String reason;
	
	public Long getMessagePredictionID() {
		return messagePredictionID;
	}
	public void setMessagePredictionID(Long messagePredictionID) {
		this.messagePredictionID = messagePredictionID;
	}
	public String getUserFeedback() {
		return userFeedback;
	}
	public void setUserFeedback(String userFeedback) {
		this.userFeedback = userFeedback;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
}

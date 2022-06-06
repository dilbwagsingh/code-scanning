package com.mykaarma.kcommunications_model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateMessagePredictionFeedbackRequestNew {

	@JsonProperty("customerUUID")
	private String customerUUID;
	
	@JsonProperty("messageUUID")
	private String messageUUID;
	
	@JsonProperty("dealerAssociateUUID")
	private String dealerAssociateUUID;
	
	@JsonProperty("messageDepartmentUUID")
	private String messageDepartmentUUID;
	
	@JsonProperty("predictionFeature")
	private String predictionFeature;

	@JsonProperty("userFeedback")
	private String userFeedback;
	
	public String getCustomerUUID() {
		return customerUUID;
	}
	public void setCustomerUUID(String customerUUID) {
		this.customerUUID = customerUUID;
	}
	
	public String getMessageUUID() {
		return messageUUID;
	}
	public void setMessageUUID(String messageUUID) {
		this.messageUUID = messageUUID;
	}
	
	public String getDealerAssociateUUID() {
		return dealerAssociateUUID;
	}
	public void setDealerAssociateUUID(String dealerAssociateUUID) {
		this.dealerAssociateUUID = dealerAssociateUUID;
	}
	
	public String getMessageDepartmentUUID() {
		return messageDepartmentUUID;
	}
	public void setMessageDepartmentUUID(String messageDepartmentUUID) {
		this.messageDepartmentUUID = messageDepartmentUUID;
	}
	
	public String getPredictionFeature() {
		return predictionFeature;
	}
	public void setPredictionFeature(String predictionFeature) {
		this.predictionFeature = predictionFeature;
	}
	
	public String getUserFeedback() {
		return userFeedback;
	}
	public void setUserFeedback(String userFeedback) {
		this.userFeedback = userFeedback;
	}
}

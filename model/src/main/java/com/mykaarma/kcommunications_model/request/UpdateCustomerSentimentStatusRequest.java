package com.mykaarma.kcommunications_model.request;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateCustomerSentimentStatusRequest {

	@JsonProperty("customerIDAndThreadID")
	private HashMap<Long,Long> customerIDAndThreadID;

	@JsonProperty("dealerID")
	private Long dealerID;

	@JsonProperty("customerSentiment")
	private String customerSentiment;

	@JsonProperty("dealerAssociateID")
	private Long dealerAssociateID;
	
	public HashMap<Long,Long> getCustomerIDAndThreadID() {
		return customerIDAndThreadID;
	}
	public void setCustomerIDAndThreadID(HashMap<Long,Long> customerIDAndThreadID) {
		this.customerIDAndThreadID = customerIDAndThreadID;
	}
	public Long getDealerID() {
		return dealerID;
	}
	public void setDealerID(Long dealerID) {
		this.dealerID = dealerID;
	}
	public String getCustomerSentiment() {
		return customerSentiment;
	}
	public void setCustomerSentiment(String customerSentiment) {
		this.customerSentiment = customerSentiment;
	}
	public Long getDealerAssociateID() {
		return dealerAssociateID;
	}
	public void setDealerAssociateID(Long dealerAssociateID) {
		this.dealerAssociateID = dealerAssociateID;
	}
}

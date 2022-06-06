package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteSubscriptionsRequest {

	@JsonProperty("dealerAssociates")
	private List<Long> dealerAssociates;

	public List<Long> getDealerAssociates() {
		return dealerAssociates;
	}

	public void setDealerAssociates(List<Long> dealerAssociates) {
		this.dealerAssociates = dealerAssociates;
	}
	
}

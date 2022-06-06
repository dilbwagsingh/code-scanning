package com.mykaarma.kcommunications_model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubscriptionUpdateRequest {

	@JsonProperty("fromDealerId")
	private Long fromDealerId;

	@JsonProperty("toDealerId")
	private Long toDealerId;

	@JsonProperty("batchSize")
	private Long batchSize;

	public Long getFromDealerId() {
		return fromDealerId;
	}

	public void setFromDealerId(Long fromDealerId) {
		this.fromDealerId = fromDealerId;
	}
	
	public Long getToDealerId() {
		return toDealerId;
	}

	public void setToDealerId(Long toDealerId) {
		this.toDealerId = toDealerId;
	}
	
	public Long getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(Long batchSize) {
		this.batchSize = batchSize;
	}
}

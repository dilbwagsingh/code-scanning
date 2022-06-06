package com.mykaarma.kcommunications.model.rabbit;

import java.io.Serializable;

public class FetchCustomersDealer implements Serializable{

	Long dealerId;
	Long batchSize;
	Long offSet;
	Integer expiration;
	
	public Long getDealerId() {
		return dealerId;
	}
	public void setDealerId(Long dealerId) {
		this.dealerId = dealerId;
	}
	
	public Long getBatchSize() {
		return batchSize;
	}
	public void setBatchSize(Long batchSize) {
		this.batchSize = batchSize;
	}
	
	public Long getOffSet() {
		return offSet;
	}
	public void setOffSet(Long offSet) {
		this.offSet = offSet;
	}
	
	public Integer getExpiration() {
		return expiration;
	}
	public void setExpiration(Integer expiration) {
		this.expiration = expiration;
	} 
}

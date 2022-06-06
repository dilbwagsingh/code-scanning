package com.mykaarma.kcommunications.model.rabbit;

import java.io.Serializable;

public class CustomerSubscriptionsUpdate implements Serializable{

	Long customerId;
	Integer expiration;
	
	public Long getCustomerId() {
		return customerId;
	}
	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}
	public Integer getExpiration() {
		return expiration;
	}
	public void setExpiration(Integer expiration) {
		this.expiration = expiration;
	}
}

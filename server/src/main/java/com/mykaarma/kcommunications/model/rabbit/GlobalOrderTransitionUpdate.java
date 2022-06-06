package com.mykaarma.kcommunications.model.rabbit;

import com.mykaarma.korder_model.v3.common.GlobalOrderTransitionDTO;

public class GlobalOrderTransitionUpdate extends GlobalOrderTransitionDTO{
	private Integer expiration;

	public Integer getExpiration() {
		return expiration;
	}

	public void setExpiration(Integer expiration) {
		this.expiration = expiration;
	}
	
}

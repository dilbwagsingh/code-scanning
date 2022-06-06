package com.mykaarma.kcommunications.utils;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mykaarma.kcommunications.model.api.Response;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RetryMessageResponse extends Response implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private Boolean isSuccess;

	public Boolean getIsSuccess() {
		return isSuccess;
	}

	public void setIsSuccess(Boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

}
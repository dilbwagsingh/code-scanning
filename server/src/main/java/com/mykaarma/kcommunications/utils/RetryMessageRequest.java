package com.mykaarma.kcommunications.utils;

import java.io.Serializable;

public class RetryMessageRequest implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String twilioErrorCode;

	public String getTwilioErrorCode() {
		return twilioErrorCode;
	}

	public void setTwilioErrorCode(String twilioErrorCode) {
		this.twilioErrorCode = twilioErrorCode;
	}

}
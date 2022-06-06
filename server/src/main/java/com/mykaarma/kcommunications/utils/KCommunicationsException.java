package com.mykaarma.kcommunications.utils;

import com.mykaarma.kcommunications_model.enums.ErrorCode;

@SuppressWarnings("serial")
public class KCommunicationsException extends Exception{

	private ErrorCode customError;
	private String data;

	
	public KCommunicationsException(ErrorCode customError) {
		super();
		this.customError = customError;
	}
	
	public KCommunicationsException(ErrorCode customError, Exception e) {
		super(e);
		this.customError = customError;
	}
	
	public KCommunicationsException(ErrorCode customError,String data) {
		super();
		this.customError = customError;
		this.data= data;
	}

	public ErrorCode getCustomError() {
		return customError;
	}

	public void setCustomError(ErrorCode customError) {
		this.customError = customError;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	
}

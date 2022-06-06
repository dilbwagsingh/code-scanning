package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;


public class Error implements Serializable {


    private static final long serialVersionUID = 1L;
    private Integer errorCode;
    private String errorTitle;
    private String errorMessage;
    
	public Error() {
		super();
	}

	public Error(ErrorCodes errorCodes) {
		super();
		this.errorCode = errorCodes.getErrorCode();
		this.errorTitle = errorCodes.getErrorTitle();
		this.errorMessage = errorCodes.getErrorMessage();
	}

	public Integer getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(Integer errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorTitle() {
		return errorTitle;
	}

	public void setErrorTitle(String errorTitle) {
		this.errorTitle = errorTitle;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	
}

package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.mykaarma.kcommunications_model.common.SendEmailRequestBody;

public class SendEmailRequest {
	
	List<SendEmailRequestBody> sendEmailRequest;

	public List<SendEmailRequestBody> getSendEmailRequest() {
		return sendEmailRequest;
	}

	public void setSendEmailRequest(List<SendEmailRequestBody> sendEmailRequest) {
		this.sendEmailRequest = sendEmailRequest;
	}
	
}

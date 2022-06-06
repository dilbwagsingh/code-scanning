package com.mykaarma.kcommunications_model.response;

import java.util.List;

public class FailedMessageResponse extends Response{
	
	private List<String> failedMessageSIDsList;

	private List<String> messageSidsWithNoCustomer;
	
	public List<String> getMessageSidsWithNoCustomer() {
		return messageSidsWithNoCustomer;
	}

	public void setMessageSidsWithNoCustomer(List<String> messageSidsWithNoCustomer) {
		this.messageSidsWithNoCustomer = messageSidsWithNoCustomer;
	}

	public List<String> getFailedMessageSIDsList() {
		return failedMessageSIDsList;
	}

	public void setFailedMessageSIDsList(List<String> failedMessageSIDsList) {
		this.failedMessageSIDsList = failedMessageSIDsList;
	}

	
}
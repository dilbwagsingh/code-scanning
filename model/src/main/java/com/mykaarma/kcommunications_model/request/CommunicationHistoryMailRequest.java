package com.mykaarma.kcommunications_model.request;

public class CommunicationHistoryMailRequest {

	private CommunicationHistoryRequest commHistoryRequest;

	private String customerUUID;

	private String departmentUUID;
	
	private Integer expiration;

	public CommunicationHistoryRequest getCommHistoryRequest() {
		return commHistoryRequest;
	}

	public void setCommHistoryRequest(CommunicationHistoryRequest commHistoryRequest) {
		this.commHistoryRequest = commHistoryRequest;
	}
	
	public String getCustomerUUID() {
		return customerUUID;
	}

	public void setCustomerUUID(String customerUUID) {
		this.customerUUID = customerUUID;
	}
	
	public String getDepartmentUUID() {
		return departmentUUID;
	}

	public void setDepartmentUUID(String departmentUUID) {
		this.departmentUUID = departmentUUID;
	}
	
	public Integer getExpiration() {
		return expiration;
	}

	public void setExpiration(Integer expiration) {
		this.expiration = expiration;
	}
}

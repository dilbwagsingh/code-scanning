package com.mykaarma.kcommunications.model.rabbit;

import java.util.List;

import com.mykaarma.kcommunications_model.request.SaveMessageRequest;
import com.mykaarma.kcustomer_model.dto.Customer;
import com.mykaarma.kcustomer_model.lombokresponse.CustomerWithVehiclesResponse;

public class SaveHistoricalMessageRequest {

	private SaveMessageRequest saveMessageRequest;
	private String customerUuid;
	private Long threadID;
	private String departmentUuid;
	private String callBackPathUrl;
	private Customer customer;
	private Boolean logInMongo;
	private Integer expiration;
	 
	public Long getThreadID() {
		return threadID;
	}

	public void setThreadID(Long threadID) {
		this.threadID = threadID;
	}
	
	public Integer getExpiration() {
		return expiration;
	}

	public void setExpiration(Integer expiration) {
		this.expiration = expiration;
	}

	public Boolean getLogInMongo() {
		return logInMongo;
	}

	public void setLogInMongo(Boolean logInMongo) {
		this.logInMongo = logInMongo;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public String getCallBackPathUrl() {
		return callBackPathUrl;
	}

	public void setCallBackPathUrl(String callBackPathUrl) {
		this.callBackPathUrl = callBackPathUrl;
	}

	public SaveMessageRequest getSaveMessageRequest() {
		return saveMessageRequest;
	}

	public void setSaveMessageRequest(SaveMessageRequest saveMessageRequest) {
		this.saveMessageRequest = saveMessageRequest;
	}
	
	public String getCustomerUuid() {
		return customerUuid;
	}

	public void setCustomerUuid(String customerUuid) {
		this.customerUuid = customerUuid;
	}

	public String getDepartmentUuid() {
		return departmentUuid;
	}

	public void setDepartmentUuid(String departmentUuid) {
		this.departmentUuid = departmentUuid;
	}
	
}

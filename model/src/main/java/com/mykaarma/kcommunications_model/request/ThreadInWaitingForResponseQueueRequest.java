package com.mykaarma.kcommunications_model.request;

public class ThreadInWaitingForResponseQueueRequest {

	private Long dealerID;
	private Long dealerDepartmentID;
	private Long customerID;
	private Long threadID;
	
	public Long getDealerID() {
		return dealerID;
	}
	public void setDealerID(Long dealerID) {
		this.dealerID = dealerID;
	}
	public Long getDealerDepartmentID() {
		return dealerDepartmentID;
	}
	public void setDealerDepartmentID(Long dealerDepartmentID) {
		this.dealerDepartmentID = dealerDepartmentID;
	}
	public Long getCustomerID() {
		return customerID;
	}
	public void setCustomerID(Long customerID) {
		this.customerID = customerID;
	}
	public Long getThreadID() {
		return threadID;
	}
	public void setThreadID(Long threadID) {
		this.threadID = threadID;
	}
}

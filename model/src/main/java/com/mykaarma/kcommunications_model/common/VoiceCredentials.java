package com.mykaarma.kcommunications_model.common;

public class VoiceCredentials {

	private String dealerSubAccount;
	private String brokerNumber;
	private Long deptID;
	
	public String getDealerSubAccount() {
		return dealerSubAccount;
	}
	public void setDealerSubAccount(String dealerSubAccount) {
		this.dealerSubAccount = dealerSubAccount;
	}
	public String getBrokerNumber() {
		return brokerNumber;
	}
	public void setBrokerNumber(String brokerNumber) {
		this.brokerNumber = brokerNumber;
	}
	public Long getDeptID() {
		return deptID;
	}
	public void setDeptID(Long deptID) {
		this.deptID = deptID;
	}
	
}

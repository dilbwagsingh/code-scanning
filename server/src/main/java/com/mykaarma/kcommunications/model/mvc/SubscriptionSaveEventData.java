package com.mykaarma.kcommunications.model.mvc;

import java.io.Serializable;

public class SubscriptionSaveEventData implements Serializable{

	private String eventName;
	private Long dealerID;
	private Long customerID;
	private String customerUUID;
	private Long dealerDepartmentID;
	private Long subscriberDAID;
	private Long revokedDAID;
	private String deviceID;
	private Long eventRaisedBy;
	private Boolean isHistoricalMessage = false;
	private Boolean isAssignee = false;
	
	public Boolean getIsAssignee() {
		return isAssignee;
	}
	public void setIsAssignee(Boolean isAssignee) {
		this.isAssignee = isAssignee;
	}
	public Boolean getIsHistoricalMessage() {
		return isHistoricalMessage;
	}
	public void setIsHistoricalMessage(Boolean isHistoricalMessage) {
		this.isHistoricalMessage = isHistoricalMessage;
	}
	public String getEventName() {
		return eventName;
	}
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	public String getDeviceID() {
		return deviceID;
	}
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}
	
	
	public Long getDealerID() {
		return dealerID;
	}
	public void setDealerID(Long dealerID) {
		this.dealerID = dealerID;
	}
	public Long getCustomerID() {
		return customerID;
	}
	public void setCustomerID(Long customerID) {
		this.customerID = customerID;
	}
	public String getCustomerUUID() {
		return customerUUID;
	}
	public void setCustomerUUID(String customerUUID) {
		this.customerUUID = customerUUID;
	}
	public Long getDealerDepartmentID() {
		return dealerDepartmentID;
	}
	public void setDealerDepartmentID(Long dealerDepartmentID) {
		this.dealerDepartmentID = dealerDepartmentID;
	}
	public Long getSubscriberDAID() {
		return subscriberDAID;
	}
	public void setSubscriberDAID(Long subscriberDAID) {
		this.subscriberDAID = subscriberDAID;
	}
	public Long getRevokedDAID() {
		return revokedDAID;
	}
	public void setRevokedDAID(Long revokedDAID) {
		this.revokedDAID = revokedDAID;
	}
	public Long getEventRaisedBy() {
		return eventRaisedBy;
	}
	public void setEventRaisedBy(Long eventRaisedBy) {
		this.eventRaisedBy = eventRaisedBy;
	}
	
	
}

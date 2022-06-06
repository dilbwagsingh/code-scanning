package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
public class NotificationWithoutCustomerEventData implements Serializable {
	
	private String eventName;
	private Long dealerID;
	private Long dealerDepartmentID;
	private Long eventRaisedBy;
	private String notificationMessageUUID;
	private Date date;
	private MessageAttributes messageAttributes;
	private NotificationWithoutCustomerAttributes notificationAttributes;
	
	public NotificationWithoutCustomerEventData() {
		
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

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

	public Long getEventRaisedBy() {
		return eventRaisedBy;
	}

	public void setEventRaisedBy(Long eventRaisedBy) {
		this.eventRaisedBy = eventRaisedBy;
	}

	public String getNotificationMessageUUID() {
		return notificationMessageUUID;
	}

	public void setNotificationMessageUUID(String notificationMessageUUID) {
		this.notificationMessageUUID = notificationMessageUUID;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public MessageAttributes getMessageAttributes() {
		return messageAttributes;
	}

	public void setMessageAttributes(MessageAttributes messageAttributes) {
		this.messageAttributes = messageAttributes;
	}

	public NotificationWithoutCustomerAttributes getNotificationAttributes() {
		return notificationAttributes;
	}

	public void setNotificationAttributes(NotificationWithoutCustomerAttributes notificationAttributes) {
		this.notificationAttributes = notificationAttributes;
	}
}

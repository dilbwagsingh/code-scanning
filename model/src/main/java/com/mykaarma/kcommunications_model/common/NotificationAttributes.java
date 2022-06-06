package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("serial")
public class NotificationAttributes implements Serializable {
	
	@JsonProperty("internalSubscribersNotifierPop")
	protected Boolean internalSubscribersNotifierPop;
	
	@JsonProperty("externalSubscribersNotifierPop")
	protected Boolean externalSubscribersNotifierPop;
	
	@JsonProperty("threadOwnerNotifierPop")
	protected Boolean threadOwnerNotifierPop;
	
	@JsonProperty("sendPhoneNotification")
	protected Boolean sendPhoneNotification = false;
	
	@JsonProperty("additionalNotifierNotificationDAUUIDs")
	protected List<String> additionalNotifierNotificationDAUUIDs;
	
	@JsonProperty("notificationButtons")
	protected List<NotificationButton> notificationButtons;
	
	public NotificationAttributes() {
		
	}

	public Boolean getInternalSubscribersNotifierPop() {
		return internalSubscribersNotifierPop;
	}

	public void setInternalSubscribersNotifierPop(Boolean internalSubscribersNotifierPop) {
		this.internalSubscribersNotifierPop = internalSubscribersNotifierPop;
	}

	public Boolean getExternalSubscribersNotifierPop() {
		return externalSubscribersNotifierPop;
	}

	public void setExternalSubscribersNotifierPop(Boolean externalSubscribersNotifierPop) {
		this.externalSubscribersNotifierPop = externalSubscribersNotifierPop;
	}

	public Boolean getThreadOwnerNotifierPop() {
		return threadOwnerNotifierPop;
	}

	public void setThreadOwnerNotifierPop(Boolean threadOwnerNotifierPop) {
		this.threadOwnerNotifierPop = threadOwnerNotifierPop;
	}

	public Boolean getSendPhoneNotification() {
		return sendPhoneNotification;
	}

	public void setSendPhoneNotification(Boolean sendPhoneNotification) {
		this.sendPhoneNotification = sendPhoneNotification;
	}

	public List<String> getAdditionalNotifierNotificationDAUUIDs() {
		return additionalNotifierNotificationDAUUIDs;
	}

	public void setAdditionalNotifierNotificationDAUUIDs(List<String> additionalNotifierNotificationDAUUIDs) {
		this.additionalNotifierNotificationDAUUIDs = additionalNotifierNotificationDAUUIDs;
	}

	public List<NotificationButton> getNotificationButtons() {
		return notificationButtons;
	}

	public void setNotificationButtons(List<NotificationButton> notificationButtons) {
		this.notificationButtons = notificationButtons;
	}
}

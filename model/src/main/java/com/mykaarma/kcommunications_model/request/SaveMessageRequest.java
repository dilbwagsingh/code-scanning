package com.mykaarma.kcommunications_model.request;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.NotificationAttributes;
import com.mykaarma.kcommunications_model.common.VoiceCallAttributes;

public class SaveMessageRequest {

	private String sourceUuid;
	
	private String callBackPathUrl;
	
	private Date sentOn;
	
	private Date receivedOn;

	private String userUuid;
	
	private String messageUuid;

	private String communicationUid;

	@JsonProperty("messageAttributes")
	private MessageAttributes messageAttributes;
	
	@JsonProperty("notificationAttributes")
	private NotificationAttributes notificationAttributes;
	
	@JsonProperty("voiceCallAttributes")
	private VoiceCallAttributes voiceCallAttributes;

	public String getSourceUuid() {
		return sourceUuid;
	}

	public void setSourceUuid(String sourceUuid) {
		this.sourceUuid = sourceUuid;
	}

	public String getCallBackPathUrl() {
		return callBackPathUrl;
	}

	public void setCallBackPathUrl(String callBackPathUrl) {
		this.callBackPathUrl = callBackPathUrl;
	}

	public MessageAttributes getMessageAttributes() {
		return messageAttributes;
	}

	public void setMessageAttributes(MessageAttributes messageAttributes) {
		this.messageAttributes = messageAttributes;
	}

	public NotificationAttributes getNotificationAttributes() {
		return notificationAttributes;
	}

	public void setNotificationAttributes(NotificationAttributes notificationAttributes) {
		this.notificationAttributes = notificationAttributes;
	}

	public VoiceCallAttributes getVoiceCallAttributes() {
		return voiceCallAttributes;
	}

	public void setVoiceCallAttributes(VoiceCallAttributes voiceCallAttributes) {
		this.voiceCallAttributes = voiceCallAttributes;
	}
	
	public Date getSentOn() {
		return sentOn;
	}

	public void setSentOn(Date sentOn) {
		this.sentOn = sentOn;
	}

	public Date getReceivedOn() {
		return receivedOn;
	}

	public void setReceivedOn(Date receivedOn) {
		this.receivedOn = receivedOn;
	}
	
	public String getUserUuid() {
		return userUuid;
	}

	public void setUserUuid(String userUuid) {
		this.userUuid = userUuid;
	}
	
	public String getMessageUuid() {
		return messageUuid;
	}

	public void setMessageUuid(String messageUuid) {
		this.messageUuid = messageUuid;
	}
	
	public String getCommunicationUid() {
		return communicationUid;
	}

	public void setCommunicationUid(String communicationUid) {
		this.communicationUid = communicationUid;
	}
}
 
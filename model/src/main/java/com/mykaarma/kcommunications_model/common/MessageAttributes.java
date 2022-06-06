package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessagePurpose;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.Tag;

@SuppressWarnings("serial")
public class MessageAttributes implements Serializable{
	
	@JsonProperty("body")
	private String body;
	
	@JsonProperty("type")
	private MessageType type;
	
	@JsonProperty("protocol")
	private MessageProtocol protocol;
	
	@JsonProperty("purpose")
	private MessagePurpose purpose;
	
	@JsonProperty("isManual")
	private Boolean isManual;
	
	@JsonProperty("subject")
	private String subject;
	
	@JsonProperty("metaData")
	private HashMap<String, String> metaData;
	
	@JsonProperty("tags")
	private List<Tag> tags;
	
	@JsonProperty("draftAttributes")
	private DraftAttributes draftAttributes;
	
	@JsonProperty("attachments")
	private ArrayList<AttachmentAttributes> attachments;

	@JsonProperty("defaultReplyAction")
	private String defaultReplyAction;
	
	@JsonProperty("updateThreadTimestamp")
	private Boolean updateThreadTimestamp;
	
	@JsonProperty("updateTotalMessageCount")
	private Boolean updateTotalMessageCount = true;
	
	@JsonProperty("showInCustomerConversation")
	private Boolean showInCustomerConversation = true;

	public String getBody() {
		return body;
	}

	public void setBody(String messageBody) {
		this.body = messageBody;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType messageType) {
		this.type = messageType;
	}

	public MessageProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(MessageProtocol messageProtocol) {
		this.protocol = messageProtocol;
	}

	public MessagePurpose getPurpose() {
		return purpose;
	}

	public void setPurpose(MessagePurpose messagePurpose) {
		this.purpose = messagePurpose;
	}

	public Boolean getIsManual() {
		return isManual;
	}

	public void setIsManual(Boolean isManualMessage) {
		this.isManual = isManualMessage;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public HashMap<String, String> getMetaData() {
		return metaData;
	}

	public void setMetaData(HashMap<String, String> metaData) {
		this.metaData = metaData;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public DraftAttributes getDraftAttributes() {
		return draftAttributes;
	}

	public void setDraftAttributes(DraftAttributes draftAttributes) {
		this.draftAttributes = draftAttributes;
	}

	public List<AttachmentAttributes> getAttachments() {
		return attachments;
	}

	public void setAttachments(ArrayList<AttachmentAttributes> attachments) {
		this.attachments = attachments;
	}

	public String getDefaultReplyAction() {
		return defaultReplyAction;
	}

	public void setDefaultReplyAction(String defaultReplyAction) {
		this.defaultReplyAction = defaultReplyAction;
	}

	public Boolean getUpdateThreadTimestamp() {
		return updateThreadTimestamp;
	}

	public void setUpdateThreadTimestamp(Boolean updateThreadTimestamp) {
		this.updateThreadTimestamp = updateThreadTimestamp;
	}

	public Boolean getUpdateTotalMessageCount() {
		return updateTotalMessageCount;
	}

	public void setUpdateTotalMessageCount(Boolean updateTotalMessageCount) {
		this.updateTotalMessageCount = updateTotalMessageCount;
	}

	public Boolean getShowInCustomerConversation() {
		return showInCustomerConversation;
	}

	public void setShowInCustomerConversation(Boolean showInCustomerConversation) {
		this.showInCustomerConversation = showInCustomerConversation;
	}
	
}

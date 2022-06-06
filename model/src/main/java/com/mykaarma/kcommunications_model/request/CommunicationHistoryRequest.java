package com.mykaarma.kcommunications_model.request;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommunicationHistoryRequest {
	
	@JsonProperty("fromDate")
	private Date fromDate;
	
	@JsonProperty("toDate")
	private Date toDate;
	
	@JsonProperty("lastNMessages")
	private Integer lastNMessages;
	
	@JsonProperty("messageType")
	private String messageType;
	
	@JsonProperty("messageProtocol")
	private String messageProtocol;
	
	@JsonProperty("toEmailList")
	private List<String> toEmailList;
	
	@JsonProperty("sendPdf")
	private Boolean sendPdf;
	
	@JsonProperty("showInternalComments")	
	private Boolean showInternalComments;	

	public Boolean getShowInternalComments() {	
		return showInternalComments;	
	}	
	public void setShowInternalComments(Boolean showInternalComments) {	
		this.showInternalComments = showInternalComments;	
	}
	
	public Boolean getSendPdf() {
		return sendPdf;
	}
	public void setSendPdf(Boolean sendPdf) {
		this.sendPdf = sendPdf;
	}
	public List<String> getToEmailList() {
		return toEmailList;
	}
	public void setToEmailList(List<String> toEmailList) {
		this.toEmailList = toEmailList;
	}
	public Date getFromDate() {
		return fromDate;
	}
	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}
	public Date getToDate() {
		return toDate;
	}
	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}
	public Integer getLastNMessages() {
		return lastNMessages;
	}
	public void setLastNMessages(Integer lastNMessages) {
		this.lastNMessages = lastNMessages;
	}
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public String getMessageProtocol() {
		return messageProtocol;
	}
	public void setMessageProtocol(String messageProtocol) {
		this.messageProtocol = messageProtocol;
	}
}

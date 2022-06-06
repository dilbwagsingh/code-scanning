package com.mykaarma.kcommunications.model.kre;

import java.util.Date;
import java.util.List;


public class InboundTextRequest {

private String accountSid;
	
	private String sid;
	private String to;
	private String from;
	private String body;
	private String numMedia;
	private Integer expiration;
	private String uuid;
	private Date receivedDate;
	
	private Date sentDate;
	
	public Integer getExpiration() {
		return expiration;
	}
	public void setExpiration(Integer expiration) {
		this.expiration = expiration;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getAccountSid() {
		return accountSid;
	}
	public void setAccountSid(String accountSid) {
		this.accountSid = accountSid;
	}
	public String getSid() {
		return sid;
	}
	public void setSid(String sid) {
		this.sid = sid;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getNumMedia() {
		return numMedia;
	}
	public void setNumMedia(String numMedia) {
		this.numMedia = numMedia;
	}
	
	public Date getReceivedDate() {
		return receivedDate;
	}
	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}
	public Date getSentDate() {
		return sentDate;
	}
	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

}

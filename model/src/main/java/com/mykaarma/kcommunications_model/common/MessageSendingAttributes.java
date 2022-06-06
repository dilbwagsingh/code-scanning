package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageSendingAttributes implements Serializable {
	
	@JsonProperty("addSignature")
	private Boolean addSignature = false;  // default false
	
	@JsonProperty("addTCPAFooter")
	private Boolean addTCPAFooter; // default value of the DSO
	
	@JsonProperty("overrideOptoutRules")
	private Boolean overrideOptoutRules = false; // false for everyone except km-api-v2 subscriber
	
	@JsonProperty("sendVCard")
	private Boolean sendVCard = false; // default false
	
	@JsonProperty("communicationValueOfCustomer")
	private String communicationValueOfCustomer; // default preferred communication of customer
	
	@JsonProperty("overrideHolidays")
	private Boolean overrideHolidays = true;      // default true
	
	@JsonProperty("listOfEmailsToBeCCed")  
	private List<String> listOfEmailsToBeCCed ; // default null
	
	@JsonProperty("listOfEmailsToBeBCCed") 
	private List<String> listOfEmailsToBeBCCed; // default null
	
	@JsonProperty("callbackURL")
	private String callbackURL; // null

	@JsonProperty("callbackMetaData")
	private Map<String, String> callbackMetaData;

	@JsonProperty("addFooter")
	private Boolean addFooter; // default true
	
	@JsonProperty("sendSynchronously") 
	private Boolean sendSynchronously=false; // default false
	
	@JsonProperty("delay")
	private Integer delay=0; // default 0

	@JsonProperty("queueIfOptedOut")
	private Boolean queueIfOptedOut = false;
	
	public Boolean getAddSignature() {
		return addSignature;
	}

	public void setAddSignature(Boolean addSignature) {
		this.addSignature = addSignature;
	}

	public Boolean getAddTCPAFooter() {
		return addTCPAFooter;
	}

	public void setAddTCPAFooter(Boolean addTCPAFooter) {
		this.addTCPAFooter = addTCPAFooter;
	}

	public Boolean getOverrideOptoutRules() {
		return overrideOptoutRules;
	}

	public void setOverrideOptoutRules(Boolean overrideOptoutRules) {
		this.overrideOptoutRules = overrideOptoutRules;
	}

	public Boolean getSendVCard() {
		return sendVCard;
	}

	public void setSendVCard(Boolean sendVCard) {
		this.sendVCard = sendVCard;
	}

	public String getCommunicationValueOfCustomer() {
		return communicationValueOfCustomer;
	}

	public void setCommunicationValueOfCustomer(String communicationValueOfCustomer) {
		this.communicationValueOfCustomer = communicationValueOfCustomer;
	}

	public Boolean getOverrideHolidays() {
		return overrideHolidays;
	}

	public void setOverrideHolidays(Boolean overrideHolidays) {
		this.overrideHolidays = overrideHolidays;
	}

	public List<String> getListOfEmailsToBeCCed() {
		return listOfEmailsToBeCCed;
	}

	public void setListOfEmailsToBeCCed(List<String> listOfEmailsToBeCCed) {
		this.listOfEmailsToBeCCed = listOfEmailsToBeCCed;
	}

	public List<String> getListOfEmailsToBeBCCed() {
		return listOfEmailsToBeBCCed;
	}

	public void setListOfEmailsToBeBCCed(List<String> listOfEmailsToBeBCCed) {
		this.listOfEmailsToBeBCCed = listOfEmailsToBeBCCed;
	}

	public String getCallbackURL() {
		return callbackURL;
	}

	public void setCallbackURL(String callbackURL) {
		this.callbackURL = callbackURL;
	}

	public Map<String, String> getCallbackMetaData() {
		return callbackMetaData;
	}

	public void setCallbackMetaData(Map<String, String> callbackMetaData) {
		this.callbackMetaData = callbackMetaData;
	}

	public Boolean getAddFooter() {
		return addFooter;
	}

	public void setAddFooter(Boolean addFooter) {
		this.addFooter = addFooter;
	}

	public Boolean getSendSynchronously() {
		return sendSynchronously;
	}

	public void setSendSynchronously(Boolean sendSynchronously) {
		this.sendSynchronously = sendSynchronously;
	}

	public Integer getDelay() {
		return delay;
	}

	public void setDelay(Integer delay) {
		this.delay = delay;
	}

	public Boolean getQueueIfOptedOut() {
		return queueIfOptedOut;
	}

	public void setQueueIfOptedOut(Boolean queueIfOptedOut) {
		this.queueIfOptedOut = queueIfOptedOut;
	}
}

package com.mykaarma.kcommunications_model.common;

import java.util.Date;

public class VoiceCallAttributes {

	private String callingParty;
	private String party2;
	private String party1Prompt;
	private String party2prompt;
	private Long callStatus;
	private String callIdentifier;
	private Date callDateTime;
	private String callBroker;
	private String recordingUrl;
	private Boolean recordCall;
	private Boolean transcribeCall;
	private String transcribeText;
	
	public String getCallingParty() {
		return callingParty;
	}
	public void setCallingParty(String callingParty) {
		this.callingParty = callingParty;
	}
	public String getParty2() {
		return party2;
	}
	public void setParty2(String party2) {
		this.party2 = party2;
	}
	public String getParty1Prompt() {
		return party1Prompt;
	}
	public void setParty1Prompt(String party1Prompt) {
		this.party1Prompt = party1Prompt;
	}
	public String getParty2prompt() {
		return party2prompt;
	}
	public void setParty2prompt(String party2prompt) {
		this.party2prompt = party2prompt;
	}
	public Long getCallStatus() {
		return callStatus;
	}
	public void setCallStatus(Long callStatus) {
		this.callStatus = callStatus;
	}
	public String getCallIdentifier() {
		return callIdentifier;
	}
	public void setCallIdentifier(String callIdentifier) {
		this.callIdentifier = callIdentifier;
	}
	public Date getCallDateTime() {
		return callDateTime;
	}
	public void setCallDateTime(Date callDateTime) {
		this.callDateTime = callDateTime;
	}
	public String getCallBroker() {
		return callBroker;
	}
	public void setCallBroker(String callBroker) {
		this.callBroker = callBroker;
	}
	public String getRecordingUrl() {
		return recordingUrl;
	}
	public void setRecordingUrl(String recordingUrl) {
		this.recordingUrl = recordingUrl;
	}
	public Boolean getRecordCall() {
		return recordCall;
	}
	public void setRecordCall(Boolean recordCall) {
		this.recordCall = recordCall;
	}
	public Boolean getTranscribeCall() {
		return transcribeCall;
	}
	public void setTranscribeCall(Boolean transcribeCall) {
		this.transcribeCall = transcribeCall;
	}
	public String getTranscribeText() {
		return transcribeText;
	}
	public void setTranscribeText(String transcribeText) {
		this.transcribeText = transcribeText;
	}
	
}

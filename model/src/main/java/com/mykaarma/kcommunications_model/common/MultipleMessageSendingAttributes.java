package com.mykaarma.kcommunications_model.common;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MultipleMessageSendingAttributes {
    
    @JsonProperty("startTimeOfSendingMessages")
    private String startTimeOfSendingMessages;

    @JsonProperty("endTimeOfSendingMessages")
    private String endTimeOfSendingMessages;

    @JsonProperty("sendMessageToNewPhoneNumbers")
    private Boolean sendMessageToNewPhoneNumbers;

    @JsonProperty("addSignature")
    private Boolean addSignature = false; // default false

    @JsonProperty("addTCPAFooter")
    private Boolean addTCPAFooter; // default value of the DSO

    @JsonProperty("overrideHolidays")
    private Boolean overrideHolidays = true; // default true

    @JsonProperty("customers")
    private List<String> customerUUIDList;

    @JsonProperty("addFooter")
    private Boolean addFooter; // default true

    @JsonProperty("bulkText")
    private Boolean bulkText = true; // default true

    @JsonProperty("sendSynchronously")
    private Boolean sendSynchronously = false; // default false

    @JsonProperty("customerUUIDToCommunicationValues")
    private Map<String, String> customerUUIDToCommunicationValues;

    @JsonProperty("listOfEmailsToBeCCed")  
	private List<String> listOfEmailsToBeCCed ; // default null
	
	@JsonProperty("listOfEmailsToBeBCCed") 
	private List<String> listOfEmailsToBeBCCed; // default null
    
    public Map<String, String> getCustomerUUIDToCommunicationValues() {
        return customerUUIDToCommunicationValues;
    }

    public void setCustomerUUIDToCommunicationValues(Map<String, String> customerUUIDToCommunicationValues) {
        this.customerUUIDToCommunicationValues = customerUUIDToCommunicationValues;
    }

    public Boolean getSendSynchronously() {
        return sendSynchronously;
    }

    public void setSendSynchronously(Boolean sendSynchronously) {
        this.sendSynchronously = sendSynchronously;
    }

    public Boolean getBulkText() {
        return bulkText;
    }

    public void setBulkText(Boolean bulkText) {
        this.bulkText = bulkText;
    }

    
	public String getStartTimeOfSendingMessages() {
		return startTimeOfSendingMessages;
	}

	public void setStartTimeOfSendingMessages(String startTimeOfSendingMessages) {
		this.startTimeOfSendingMessages = startTimeOfSendingMessages;
	}

	public String getEndTimeOfSendingMessages() {
		return endTimeOfSendingMessages;
	}

	public void setEndTimeOfSendingMessages(String endTimeOfSendingMessages) {
		this.endTimeOfSendingMessages = endTimeOfSendingMessages;
	}

	public Boolean getSendMessageToNewPhoneNumbers() {
		return sendMessageToNewPhoneNumbers;
	}

	public void setSendMessageToNewPhoneNumbers(Boolean sendMessageToNewPhoneNumbers) {
		this.sendMessageToNewPhoneNumbers = sendMessageToNewPhoneNumbers;
	}

	public void setCustomerUUIDList(List<String> customerUUIDList) {
		this.customerUUIDList = customerUUIDList;
	}

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

	public Boolean getOverrideHolidays() {
		return overrideHolidays;
	}

	public void setOverrideHolidays(Boolean overrideHolidays) {
		this.overrideHolidays = overrideHolidays;
	}

	public List<String> getCustomerUUIDList() {
		return customerUUIDList;
	}

	public Boolean getAddFooter() {
		return addFooter;
	}

	public void setAddFooter(Boolean addFooter) {
		this.addFooter = addFooter;
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

}

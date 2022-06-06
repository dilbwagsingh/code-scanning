package com.mykaarma.kcommunications_model.request;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class AutoCsiLogEventRequest implements Serializable {
	  private static final long serialVersionUID = 1L;

	  @JsonProperty("dealerOrderUUID")
	  private String dealerOrderUUID = null;

	  @JsonProperty("isScheduled")
	  private Boolean isScheduled = null;

	  @JsonProperty("isSent")
	  private Boolean isSent = null;

	  @JsonProperty("messageProtocol")
	  private String messageProtocol = null;

	  @JsonProperty("messageUUID")
	  private String messageUUID = null;

	  @JsonProperty("scheduledFailureReason")
	  private String scheduledFailureReason = null;

	  @JsonProperty("sentFailureReason")
	  private String sentFailureReason = null;

	  @JsonProperty("tsUTC")
	  private String tsUTC = null;

	  public AutoCsiLogEventRequest dealerOrderUUID(String dealerOrderUUID) {
	    this.dealerOrderUUID = dealerOrderUUID;
	    return this;
	  }
	  

	  public String getDealerOrderUUID() {
	    return dealerOrderUUID;
	  }

	  public void setDealerOrderUUID(String dealerOrderUUID) {
	    this.dealerOrderUUID = dealerOrderUUID;
	  }

	  public AutoCsiLogEventRequest isScheduled(Boolean isScheduled) {
	    this.isScheduled = isScheduled;
	    return this;
	  }
	  

	  public Boolean isIsScheduled() {
	    return isScheduled;
	  }

	  public void setIsScheduled(Boolean isScheduled) {
	    this.isScheduled = isScheduled;
	  }

	  public AutoCsiLogEventRequest isSent(Boolean isSent) {
	    this.isSent = isSent;
	    return this;
	  }
	  

	  public Boolean isIsSent() {
	    return isSent;
	  }

	  public void setIsSent(Boolean isSent) {
	    this.isSent = isSent;
	  }

	  public AutoCsiLogEventRequest messageProtocol(String messageProtocol) {
	    this.messageProtocol = messageProtocol;
	    return this;
	  }
	  

	  public String getMessageProtocol() {
	    return messageProtocol;
	  }

	  public void setMessageProtocol(String messageProtocol) {
	    this.messageProtocol = messageProtocol;
	  }

	  public AutoCsiLogEventRequest messageUUID(String messageUUID) {
	    this.messageUUID = messageUUID;
	    return this;
	  }
	  

	  public String getMessageUUID() {
	    return messageUUID;
	  }

	  public void setMessageUUID(String messageUUID) {
	    this.messageUUID = messageUUID;
	  }

	  public AutoCsiLogEventRequest scheduledFailureReason(String scheduledFailureReason) {
	    this.scheduledFailureReason = scheduledFailureReason;
	    return this;
	  }
	  

	  public String getScheduledFailureReason() {
	    return scheduledFailureReason;
	  }

	  public void setScheduledFailureReason(String scheduledFailureReason) {
	    this.scheduledFailureReason = scheduledFailureReason;
	  }

	  public AutoCsiLogEventRequest sentFailureReason(String sentFailureReason) {
	    this.sentFailureReason = sentFailureReason;
	    return this;
	  }
	  

	  public String getSentFailureReason() {
	    return sentFailureReason;
	  }

	  public void setSentFailureReason(String sentFailureReason) {
	    this.sentFailureReason = sentFailureReason;
	  }

	  public AutoCsiLogEventRequest tsUTC(String tsUTC) {
	    this.tsUTC = tsUTC;
	    return this;
	  }
	  

	  public String getTsUTC() {
	    return tsUTC;
	  }

	  public void setTsUTC(String tsUTC) {
	    this.tsUTC = tsUTC;
	  }

	  @Override
	  public boolean equals(java.lang.Object o) {
	    if (this == o) {
	      return true;
	    }
	    if (o == null || getClass() != o.getClass()) {
	      return false;
	    }
	    AutoCsiLogEventRequest autocsiLogEventItem = (AutoCsiLogEventRequest) o;
	    return Objects.equals(this.dealerOrderUUID, autocsiLogEventItem.dealerOrderUUID) &&
	        Objects.equals(this.isScheduled, autocsiLogEventItem.isScheduled) &&
	        Objects.equals(this.isSent, autocsiLogEventItem.isSent) &&
	        Objects.equals(this.messageProtocol, autocsiLogEventItem.messageProtocol) &&
	        Objects.equals(this.messageUUID, autocsiLogEventItem.messageUUID) &&
	        Objects.equals(this.scheduledFailureReason, autocsiLogEventItem.scheduledFailureReason) &&
	        Objects.equals(this.sentFailureReason, autocsiLogEventItem.sentFailureReason) &&
	        Objects.equals(this.tsUTC, autocsiLogEventItem.tsUTC);
	  }

	  @Override
	  public int hashCode() {
	    return java.util.Objects.hash(dealerOrderUUID, isScheduled, isSent, messageProtocol, messageUUID, scheduledFailureReason, sentFailureReason, tsUTC);
	  }


	  @Override
	  public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("class AutocsiLogEventItem {\n");
	    
	    sb.append("    dealerOrderUUID: ").append(toIndentedString(dealerOrderUUID)).append("\n");
	    sb.append("    isScheduled: ").append(toIndentedString(isScheduled)).append("\n");
	    sb.append("    isSent: ").append(toIndentedString(isSent)).append("\n");
	    sb.append("    messageProtocol: ").append(toIndentedString(messageProtocol)).append("\n");
	    sb.append("    messageUUID: ").append(toIndentedString(messageUUID)).append("\n");
	    sb.append("    scheduledFailureReason: ").append(toIndentedString(scheduledFailureReason)).append("\n");
	    sb.append("    sentFailureReason: ").append(toIndentedString(sentFailureReason)).append("\n");
	    sb.append("    tsUTC: ").append(toIndentedString(tsUTC)).append("\n");
	    sb.append("}");
	    return sb.toString();
	  }

	  private String toIndentedString(java.lang.Object o) {
	    if (o == null) {
	      return "null";
	    }
	    return o.toString().replace("\n", "\n    ");
	  }

	}

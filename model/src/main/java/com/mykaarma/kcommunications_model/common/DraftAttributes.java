package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mykaarma.kcommunications_model.enums.DraftFailureReason;
import com.mykaarma.kcommunications_model.enums.DraftStatus;

public class DraftAttributes implements Serializable {
	
	@JsonProperty("draftStatus")
	private DraftStatus draftStatus;

	@JsonProperty("scheduledOn")
	private String scheduledOn;
	
	@JsonProperty("draftFailureReason")
	private DraftFailureReason draftFailureReason;

	public DraftStatus getDraftStatus() {
		return draftStatus;
	}

	public void setDraftStatus(DraftStatus draftStatus) {
		this.draftStatus = draftStatus;
	}

	public String getScheduledOn() {
		return scheduledOn;
	}

	public void setScheduledOn(String scheduledOn) {
		this.scheduledOn = scheduledOn;
	}

	public DraftFailureReason getDraftFailureReason() {
		return draftFailureReason;
	}

	public void setDraftFailureReason(DraftFailureReason draftFailureReason) {
		this.draftFailureReason = draftFailureReason;
	}

}

package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecordingUpdateRequest extends RecordingRequest{
    
	@JsonProperty("recordingDelete")
	private Boolean recordingDelete = false;

	public Boolean getRecordingDelete() {
		return recordingDelete;
	}

	public void setRecordingDelete(Boolean recordingDelete) {
		this.recordingDelete = recordingDelete;
	}
	
}

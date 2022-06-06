package com.mykaarma.kcommunications_model.response;

import java.util.List;

public class SaveMessageListResponse extends Response{

	List<SaveMessageResponse> saveMessageResponse;
	List<String> succesfullySubmittedSourceUuids;
	List<String> failedSourceUuids;
	
	public List<SaveMessageResponse> getSaveMessageResponse() {
		return saveMessageResponse;
	}
	public void setSaveMessageResponse(List<SaveMessageResponse> saveMessageResponse) {
		this.saveMessageResponse = saveMessageResponse;
	}
	public List<String> getSuccesfullySubmittedSourceUuids() {
		return succesfullySubmittedSourceUuids;
	}
	public void setSuccesfullySubmittedSourceUuids(List<String> succesfullySubmittedSourceUuids) {
		this.succesfullySubmittedSourceUuids = succesfullySubmittedSourceUuids;
	}
	public List<String> getFailedSourceUuids() {
		return failedSourceUuids;
	}
	public void setFailedSourceUuids(List<String> failedSourceUuids) {
		this.failedSourceUuids = failedSourceUuids;
	}
	
}

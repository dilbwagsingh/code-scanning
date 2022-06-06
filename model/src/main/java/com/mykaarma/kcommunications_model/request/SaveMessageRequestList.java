package com.mykaarma.kcommunications_model.request;

import java.util.List;

public class SaveMessageRequestList {

	private List<SaveMessageRequest> saveMessageRequestList;
	private String callBackPathUrl;
	
	public String getCallBackPathUrl() {
		return callBackPathUrl;
	}

	public void setCallBackPathUrl(String callBackPathUrl) {
		this.callBackPathUrl = callBackPathUrl;
	}

	public List<SaveMessageRequest> getSaveMessageRequestList() {
		return saveMessageRequestList;
	}

	public void setSaveMessageRequestList(List<SaveMessageRequest> saveMessageRequestList) {
		this.saveMessageRequestList = saveMessageRequestList;
	}
	
}

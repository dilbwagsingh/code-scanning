package com.mykaarma.kcommunications_model.response;

public class SaveMessageResponse extends Response{

	private String sourceUuid;
	private String messageUuid;
	
	public String getSourceUuid() {
		return sourceUuid;
	}
	public void setSourceUuid(String sourceUuid) {
		this.sourceUuid = sourceUuid;
	}
	public String getMessageUuid() {
		return messageUuid;
	}
	public void setMessageUuid(String messageUuid) {
		this.messageUuid = messageUuid;
	}
}

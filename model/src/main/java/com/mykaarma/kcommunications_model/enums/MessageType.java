package com.mykaarma.kcommunications_model.enums;


public enum MessageType {
	DRAFT("F"), // draft 
	DISCARDED_DRAFT("D"), // discarded draft
	OUTGOING("S"),  // outgoing message
	INCOMING("I"),
	NOTE("N") // Note (manual i.e internal note OR automatic i.e. system event/notification)
	;
	
	private String messageType;
	
	private MessageType(String messageType) {
		this.messageType = messageType;
	}
	
	public String getMessageType() {
		return this.messageType;
	}
	
	public static MessageType getMessageTypeForString(String messageTypeStr)
	{
		MessageType messageType = null;
		if(messageTypeStr != null) {
			for(MessageType type : MessageType.values()) {
				if(type.getMessageType().equalsIgnoreCase(messageTypeStr.trim())) {
					messageType = type;
					break;
				}
			}
		}

		return messageType;
	}
}

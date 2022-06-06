package com.mykaarma.kcommunications_model.enums;

public enum MessageProtocol {
	TEXT("X"),
	EMAIL("E"),
	VOICE_CALL("V"),
	NONE("Z"); // for Note (manual i.e internal note / automatic i.e. system event/notification)
	
	private String messageProtocol;
	
	private MessageProtocol(String messageProtocol) {
		this.messageProtocol = messageProtocol;
	}
	
	public String getMessageProtocol() {
		return this.messageProtocol;
	}
	
	public static MessageProtocol getMessageProtocolForString(String messageProtocolStr)
	{
		MessageProtocol messageProtocol = null;
		if(messageProtocolStr != null) {
			for(MessageProtocol type : MessageProtocol.values()) {
				if(type.getMessageProtocol().equalsIgnoreCase(messageProtocolStr.trim())) {
					messageProtocol = type;
					break;
				}
			}
		}

		return messageProtocol;
	}

	public static MessageProtocol fromString(String messageProtocolStr) {
		for(MessageProtocol mp : MessageProtocol.values()) {
			if(mp.name().equalsIgnoreCase(messageProtocolStr)) {
				return mp;
			}
		}
		return null;
	}
}

package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;

import com.mykaarma.global.MessageKeyword;

import lombok.Data;

@Data
public class CommunicationStatusRequest implements Serializable {

	private String messageUuid;
	
	private Integer optOutRank;
	
	private Double optOutScore;

	private Double optOutV2Score;

	private MessageKeyword messageKeyword;

	public void setMessageKeyword(com.mykaarma.kcommunications_model.enums.MessageKeyword messageKeyword) {
		for(MessageKeyword mk : MessageKeyword.values()) {
			if(mk.name().equalsIgnoreCase(messageKeyword.name())) {
				this.messageKeyword = mk;
			}
		}
	}

	public void setMessageKeyword(MessageKeyword messageKeyword) {
		this.messageKeyword = messageKeyword;
	}
	
}
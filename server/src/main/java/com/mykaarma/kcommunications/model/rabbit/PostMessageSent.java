package com.mykaarma.kcommunications.model.rabbit;

import com.mykaarma.kcommunications.model.jpa.Message;

import lombok.Data;

@Data
public class PostMessageSent {
	private Message message;
	private Long threadDelegatee;
	private Boolean postMessageProcessingToBeDone;
	private Integer expiration;
    private Boolean isEditedDraft;
    private Boolean updateThreadTimestamp;
    private Boolean isFailedMessage = false;
}

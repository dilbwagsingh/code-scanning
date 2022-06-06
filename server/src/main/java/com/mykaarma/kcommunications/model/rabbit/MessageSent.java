package com.mykaarma.kcommunications.model.rabbit;

import com.mykaarma.kcommunications.model.jpa.Message;

import lombok.Data;

@Data
public class MessageSent {
	private Message message;
	private Boolean isEditedDraft;
}

package com.mykaarma.kcommunications.model.mvc;

import java.util.Date;

import lombok.Data;

@Data
public class MessageSaveEventData extends MessageViewControllerEvent {

	private Long messageID;
	private Long messageDealerAssociateID;
	private Long threadID;
	private Long threadDealerAssociateID;
	private String messageType; 
	private Date messageDate;
	private Boolean isDelayed;
	private String messagePurpose;
	private String messageBody;
	private String messageSubject;
	private Boolean isManual;
	private String protocol;
	private Boolean status;
	private Boolean updateThreadTimestamp;
}

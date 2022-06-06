package com.mykaarma.kcommunications.model.mvc;

import java.util.Date;

import lombok.Data;

@Data
public class ThreadSaveEventData extends MessageViewControllerEvent{
	private Long threadID;
	private Date threadUpdatedDate;
	private Boolean isThreadDelegated;
	private Long previousThreadOwnerDAID;
	private Long currentThreadOwnerDAID;
	private Date lastMessageOn;
	private String assigneeName;
	private Boolean status;
}

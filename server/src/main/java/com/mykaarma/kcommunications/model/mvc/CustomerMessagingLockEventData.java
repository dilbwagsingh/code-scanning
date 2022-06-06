package com.mykaarma.kcommunications.model.mvc;

import lombok.Data;

@Data
public class CustomerMessagingLockEventData extends MessageViewControllerEvent {

	private Long lockedByDealerAssociateID;
	private Boolean isDealerAssociateSpecficEvent =true;
	
}

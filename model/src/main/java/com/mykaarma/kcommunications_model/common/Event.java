package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;

import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.NotificationType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class Event implements Serializable {

	@ApiModelProperty(notes = "Notification type, can be aither internal or external")
	private NotificationType type;
	
	private CommunicationCategory category;

}

package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UserEvent implements Serializable {

	@ApiModelProperty(notes = "User involved in subscription event")
	private User user;
	
	@ApiModelProperty(notes = "Events to which user is subscribed to")
	private List<Event> addedEvents;
	
	@ApiModelProperty(notes = "Events from which user is revoked")
	private List<Event> revokedEvents;
	
	@ApiModelProperty(notes = "Events triggered in mykaarma")
	private List<Event> events;

}

package com.mykaarma.kcommunications.model.rabbit;

import lombok.Data;

@Data
public class PostMessageReceived {
	
	private Long dealerID;
	private String messageUUID;
	private String departmentUUID;
	private Integer expiration;
	

}

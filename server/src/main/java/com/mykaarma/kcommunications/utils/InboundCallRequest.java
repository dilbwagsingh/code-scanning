package com.mykaarma.kcommunications.utils;


import java.util.Date;

import lombok.Data;

@Data
public class InboundCallRequest {

	private String accountSid;
	
	private String sid;
	private String to;
	private String from;
	private String body;
	private String numMedia;
	private Integer expiration;
	private String uuid;
	private Date receivedDate;
	
	private Date sentDate;
	
}

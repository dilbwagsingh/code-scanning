package com.mykaarma.kcommunications_model.common;

import java.util.HashMap;

import com.mykaarma.kcommunications_model.enums.MessagePurpose;

import lombok.Data;

@Data
public class SendEmailRequestBody {
	
	private String fromName;
	private String fromEmail;
	private String toList; 
	private String ccList; 
	private String bccList; 
	private String subject;
	private String message;
	private Boolean useDealerEmailCredentials=false;
	private HashMap<String,String> attachmentUrlAndNameMap;
	private String reference;
	private String messagePurposeUuid;
	
}

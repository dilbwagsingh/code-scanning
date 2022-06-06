package com.mykaarma.kcommunications_model.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class DraftMessageMetaDataDTO implements Serializable{

	private String messageUuid;
	
	private String status;
	
	private Date scheduledOn;
	
	private String reasonForLastFailure;
	
	private Boolean addSignature;
}

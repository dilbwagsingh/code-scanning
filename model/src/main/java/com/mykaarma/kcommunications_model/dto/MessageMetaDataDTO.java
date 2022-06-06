package com.mykaarma.kcommunications_model.dto;

import java.io.Serializable;

import lombok.Data;

@Data
public class MessageMetaDataDTO implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String messageUuid;
	
	private String metaData;

}
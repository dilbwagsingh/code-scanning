package com.mykaarma.kcommunications_model.request;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchMessagesForCustomerRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssZ")
	private Date lastMessageReceivedOn;
	
	private List<String> messageUuidsReceivedAtSameTime;
	
	private int maxResults=20;
	
	private boolean fetchDrafts=false;
	
	private int maxDraftsToBeFetched=100;
	
	private List<MessageProtocol> protocolsSupported;
	
}

package com.mykaarma.kcommunications.model.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FailedMessagesRequest {

	@JsonProperty(value = "messageSidList")
	private List<String> messageSidList=new ArrayList<String>();
	
	@JsonProperty(value = "accountSid")
	private String accountSid;

}

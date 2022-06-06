package com.mykaarma.kcommunications_model.response;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mykaarma.kcommunications_model.dto.MessageDTO;
import com.mykaarma.kcommunications_model.enums.FetchMessageTypes;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerMessagesFetchResponse extends Response implements Serializable{
	
	HashMap<FetchMessageTypes, List<MessageDTO>> messagesMap;
	
	String requestUuid;

}

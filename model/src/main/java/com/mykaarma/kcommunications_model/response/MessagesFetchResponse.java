package com.mykaarma.kcommunications_model.response;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mykaarma.kcommunications_model.dto.MessageDTO;
import com.mykaarma.kcommunications_model.response.Response;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessagesFetchResponse extends Response implements Serializable{
	
	List<MessageDTO> messageDTOList;
	
	String requestUuid;

}

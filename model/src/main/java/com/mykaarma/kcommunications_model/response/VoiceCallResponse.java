package com.mykaarma.kcommunications_model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mykaarma.kcommunications_model.enums.Status;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoiceCallResponse extends Response {

	String sid;
	Status status;
	
}

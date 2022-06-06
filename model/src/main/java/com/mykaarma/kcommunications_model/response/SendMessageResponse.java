package com.mykaarma.kcommunications_model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper=true)
public class SendMessageResponse extends SendMessageWithoutCustomerResponse {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String customerUUID;
	private Map<String, String> metaData;
}

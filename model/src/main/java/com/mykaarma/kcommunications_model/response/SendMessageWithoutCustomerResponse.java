package com.mykaarma.kcommunications_model.response;

import com.mykaarma.kcommunications_model.enums.Status;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class SendMessageWithoutCustomerResponse extends Response {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String messageUUID;
	private Status status;
	private String requestUUID;
}

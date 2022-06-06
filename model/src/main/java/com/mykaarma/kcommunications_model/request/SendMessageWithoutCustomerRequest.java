package com.mykaarma.kcommunications_model.request;

import java.io.Serializable;

import com.mykaarma.kcommunications_model.enums.MessagePurpose;

import lombok.Data;

@Data
public class SendMessageWithoutCustomerRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String messageBody;
	String messageSubject;
	String fromNumber;
	String toNumber;
	Boolean sendSynchronously = Boolean.FALSE;
	Integer delayInSeconds = 0;
	String messagePurposeUuid;
}

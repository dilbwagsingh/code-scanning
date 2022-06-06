package com.mykaarma.kcommunications.utils;

import org.springframework.stereotype.Component;

import com.mykaarma.global.CommunicationFailureReason;

import lombok.Data;

@Component
@Data
public class OutboundMessageDetail {
	String sid;
	String brokerNumber;
	private CommunicationFailureReason failureReason;
	
}

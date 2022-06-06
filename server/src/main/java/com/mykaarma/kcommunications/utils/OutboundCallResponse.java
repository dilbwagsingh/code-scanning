package com.mykaarma.kcommunications.utils;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class OutboundCallResponse {

	private String callRecordingURL;
	private int callRecordingDuration;
	
}

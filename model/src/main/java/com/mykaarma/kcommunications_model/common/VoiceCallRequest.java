package com.mykaarma.kcommunications_model.common;

import lombok.Data;

@Data
public class VoiceCallRequest {

	String userUUID;
	String party1Number;
	String party2Number;
	boolean supportCall;
	String twilioCallBackPath;	
	
}

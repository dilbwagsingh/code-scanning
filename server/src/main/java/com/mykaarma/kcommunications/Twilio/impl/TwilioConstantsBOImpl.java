package com.mykaarma.kcommunications.Twilio.impl;

import org.springframework.stereotype.Service;

@Service
public class TwilioConstantsBOImpl{

	public static enum TwilioOpsEnum {
		P1Connected, P2Connected, EndCall, P1ConnectedSupport, TranscribeCallBack, InboundCall, InboundCallForward, InboundSms, FallBack, MissedCall,IPadEndCall,
		RnConnected, StatusCallback, CIWVoiceOptInCallConnected, CIWMissedCall, CIWOptInCallConnected, CIWStatusCallback,
		P1ConnectedConference, P2ConnectedConference, EndCallConference, AnnounceConferenceGreeting, InboundEndCall, OutboundEndCall
	};
	

	public static final String InboundTw2CustOptionKey = "Voice.Inbound.Tw2Cust.Prompt";
	public static final String InboundTw2SaOptionKey = "Voice.Inbound.Tw2Sa.Prompt";
	public static final String OutboundTw2SaOptionKey = "Voice.Outbound.Tw2Sa.Prompt";
	public static final String OutboundTw2CustOptionKey = "Voice.Outbound.Tw2Cust.Prompt";
	public static final String VoiceCustToDriverPrompt = "Voice.CustToDriver.Prompt";
	public static final String VoiceCustToPDMPrompt = "Voice.CustToPDM.Prompt";
	public static final String VoiceDriverToCustPrompt = "Voice.DriverToCust.Prompt";
	public static final String VoicePDMToCustPrompt = "VoicePDMToCustPrompt";
	public static final String VoiceOptInPrompt = "CIWVoiceOptInPrompt";
	public static final String VoiceAndTextOptInPrompt = "CIWOptInPrompt";
	
	
}

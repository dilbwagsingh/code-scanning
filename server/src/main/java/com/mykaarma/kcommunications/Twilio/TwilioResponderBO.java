package com.mykaarma.kcommunications.Twilio;


import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import com.mykaarma.kcommunications.model.jpa.VoiceCall;

public interface TwilioResponderBO {

	String handleP1Connected(HttpServletRequest request) throws Exception;

	String handleP2Connected(HttpServletRequest request) throws Exception;

	String handleEndCall(HttpServletRequest request) throws Exception;

	String handleMissedCall(HttpServletRequest request) throws Exception;

	String handleFallBack(HttpServletRequest request) throws  Exception;

	String handleInboundCall(HttpServletRequest request) throws IOException, Exception;

	String handleInboundForwardCall(HttpServletRequest request)
			throws IOException, Exception;

	String handleSupportCall(HttpServletRequest request) throws Exception;
	
	void updateCallInfo(String callSid, String recordingUrl, int recordingDuration, String transcription);
	
	String handleP1ConnectedConference(HttpServletRequest request) throws Exception;

	void handleP2ConnectedConference(String friendlyCallName, String accountSID, VoiceCall vc) throws Exception;
	
	String handleEndCallConference(HttpServletRequest request) throws Exception;
	
	void handleAnnounceConferenceGreeting(String friendlyCallName, String accountSID, VoiceCall vc) throws Exception;
	
	String handleConferenceEvents(HttpServletRequest request) throws Exception;

	String handleInboundEndCall(HttpServletRequest request) throws Exception;

	String handleOutboundEndCall(HttpServletRequest request) throws Exception;

	void handleInboundBotSms(HttpServletRequest request) throws Exception;
}

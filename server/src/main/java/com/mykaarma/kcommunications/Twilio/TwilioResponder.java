package com.mykaarma.kcommunications.Twilio;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.mykaarma.kcommunications.Twilio.impl.TwilioResponderBOImpl;
import com.twilio.twiml.TwiMLException;

@Configuration
@RestController
public class TwilioResponder {
	
	@Autowired
	TwilioResponderBOImpl twilioResponderObj = new TwilioResponderBOImpl();
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TwilioResponder.class);
	private static final String TWILIO_RESPONSE_TYPE = "text/xml";
	
	@PostMapping(value = "twilio/P1Connected")
	public void handleP1Connected(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		String output = twilioResponderObj.handleP1Connected(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleP1Connected while creating TwiML", e);
		}
		
	}

	@PostMapping(value = "twilio/P2Connected")
	public void handleP2Connected(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String output = twilioResponderObj.handleP2Connected(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleP2Connected while creating TwiML", e);
		}
	}

	@PostMapping(value = "twilio/EndCall")
	public void handleEndCall(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String output = twilioResponderObj.handleEndCall(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleEndCall while creating TwiML", e);
		}
	}
	
	@PostMapping(value = "twilio/P1ConnectedSupport")
	public void handleSupportCall(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		String output = twilioResponderObj.handleSupportCall(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleSupportCall while creating TwiML", e);
		}
		
	}
	
	@PostMapping(value = "twilio/MissedCall")
	public void handleMissedCall(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String output = twilioResponderObj.handleMissedCall(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleMissedCall while creating TwiML", e);
		}
	}

	@RequestMapping(value = "twilio/FallBack", method = RequestMethod.GET)
	public void handleFallBack(HttpServletRequest request,HttpServletResponse response)
			throws Exception {

		String output = twilioResponderObj.handleFallBack(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleFallBack while creating TwiML", e);
		}
	}

	@PostMapping(value = "twilio/InboundCall")
	public void handleInboundCall(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String output = twilioResponderObj.handleInboundCall(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleInboundCall while creating TwiML", e);
		}
	}
	
	
	@PostMapping(value = "twilio/InboundCallForward")
	public void handleInboundCallForward(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String output = twilioResponderObj.handleInboundForwardCall(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleInboundCallForward while creating TwiML", e);
		}

	}
	
	
	@PostMapping(value = "twilio/P1ConnectedConference")
	public void handleP1ConnectedConference(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		String output = twilioResponderObj.handleP1ConnectedConference(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleP1ConnectedConference while creating TwiML", e);
		}
	}

	@ResponseBody
	@PostMapping(value = "twilio/EndCallConference")
	public String handleEndCallConference(HttpServletRequest request) throws Exception {
		
		return twilioResponderObj.handleEndCallConference(request);
	}

	@PostMapping(value = "twilio/conference/events")
	public void handleConferenceEvents(HttpServletRequest request, HttpServletResponse response) throws Exception{
		
		String output = twilioResponderObj.handleConferenceEvents(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleConferenceEvents while creating TwiML", e);
		}
	}
	
	@PostMapping(value = "twilio/conference/twiml/callee")
	public void handlefetchConferenceTwimlForCallee(@RequestParam("confID") String confID, HttpServletRequest request, HttpServletResponse response) throws Exception{
		String output = twilioResponderObj.handlefetchConferenceTwimlForCallee(confID);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handlefetchConferenceTwimlForCallee while creating TwiML", e);
		}
	}
	
	@PostMapping(value = "twilio/conference/twiml/warning")
	public void fetchCallRecordingWarningTwiml(@RequestParam("From") String brokerNumber, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String output = twilioResponderObj.handleFetchCallRecordingWarningTwiml(brokerNumber);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside fetchCallRecordingWarningTwiml while creating TwiML", e);
		}
				
	}
	
	@PostMapping(value = "twilio/InboundEndCall")
	public void handleInboundEndCall(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String output = twilioResponderObj.handleInboundEndCall(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleInboundEndCall while creating TwiML", e);
		}
	}
	
	@PostMapping(value = "twilio/OutboundEndCall")
	public void handleOutboundEndCall(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String output = twilioResponderObj.handleOutboundEndCall(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);		
		try {
			response.getWriter().print(output);
		} catch (TwiMLException e) {
			LOGGER.error("Error inside handleOutboundEndCall while creating TwiML", e);
		}
	}

	@PostMapping(value = "twilio/InboundBotSms")
	public void handleInboundBotSms(HttpServletRequest request, HttpServletResponse response) throws Exception {
		twilioResponderObj.handleInboundBotSms(request);
		response.setContentType(TWILIO_RESPONSE_TYPE);
		response.getWriter().print("");
	}
}

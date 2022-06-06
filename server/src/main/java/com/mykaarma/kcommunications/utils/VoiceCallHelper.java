package com.mykaarma.kcommunications.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mykaarma.kcommunications.Twilio.impl.InboundCallProcessor;
import com.mykaarma.kcommunications.Twilio.impl.TwilioConstantsBOImpl.TwilioOpsEnum;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.jpa.VoiceCall;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Conference;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Gather;
import com.twilio.twiml.voice.Play;
import com.twilio.twiml.voice.Record;
import com.twilio.twiml.voice.Redirect;
import com.twilio.twiml.voice.Say;
import com.twilio.twiml.voice.Conference.Beep;
import com.twilio.twiml.voice.Conference.Event;
import com.twilio.twiml.voice.Record.Trim;
import com.twilio.twiml.voice.Say.Voice;
import com.twilio.twiml.voice.Number;

@Component
public class VoiceCallHelper {
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Value("${base_url}")
	String BASE_URL;
	
	private static final Logger LOGGER =  LoggerFactory.getLogger(InboundCallProcessor.class);
	
	private static final String THANKYOU_FOR_USING_MYKAARMA_URL = "com/audio/recordings/Thank_you_for_using_Kaarma.mp3";
	private static final String DEALER_AUTO_REPLY_URL = "/audio/recordings/default_call_auto_reply.mp3";
	
	public VoiceCall getVoiceCall(String callIdentifier, String party1Number, String party2Number, String party2Number2, String party1prompt, String party2prompt, String brokerNumber, Boolean recordCall, char voiceGateway, Boolean isDelegate, Long voiceCallStatus, Boolean transcribe) {
		
		VoiceCall vc = new VoiceCall();
		
		vc.setCallIdentifier(callIdentifier);
		vc.setCallDateTime(new Date());
		vc.setCallingParty(party1Number);
		vc.setParty1(party1Number);
		vc.setParty1Prompt(party1prompt);
		vc.setParty2(party2Number);
		if (isDelegate) {
			vc.setParty2Delegate(party2Number2);
		}
		if(party2prompt!=null && party2prompt.isEmpty()) {
			party2prompt = null;
		}
		vc.setParty2Prompt(party2prompt);
		vc.setVoiceGateway(voiceGateway);
		vc.setRecordCall(recordCall);
		if (brokerNumber != null && !brokerNumber.isEmpty()) {
			vc.setCallBroker(brokerNumber);
		}
		vc.setTranscribeCall(transcribe);

		vc.setCallStatus(voiceCallStatus);
		
		return vc;
	}

	public String getVoiceResponseForP1Connected(String fromNumber, String toNumber, String numberURL, String actionURL, Dial.Record record_value) {
		
		com.twilio.twiml.voice.Number number = new com.twilio.twiml.voice.Number.Builder(toNumber.substring(1))
				.url(numberURL).build();
		
		Dial dial = new Dial.Builder()
				.timeout(180)
				.callerId(fromNumber)
				.record(record_value)
				.action(actionURL)
				.number(number)
				.build();
		
		return new VoiceResponse.Builder()
				.dial(dial)
				.build().toXml();
	}
	
	public String getVoiceResponseForP1ConnectedConference(String friendlyName, String from, String waitURL, String callBackConference, String actionURL, Dial.Record record_value) {
		
		List<Event> events = new ArrayList<Conference.Event>();
		events.add(Event.START);
		events.add(Event.JOIN);
		
		Conference conference = new Conference.Builder(friendlyName)
				.waitUrl(waitURL)
				.beep(Beep.FALSE)
				.statusCallbackEvents(events)
				.statusCallback(callBackConference)
				.endConferenceOnExit(true)
				.build();
				
		Dial dial = new Dial.Builder()
				.timeout(180)
				.action(actionURL)
				.callerId(from)
				.record(record_value)
				.conference(conference)
				.build();
		
		return new VoiceResponse.Builder()
				.dial(dial)
				.build()
				.toXml();		
	}
	
	public String getVoiceResponseForP1ConnectedDelegation(String fromNumber, String toNumber, String toNumberSA1, String actionURL, Dial.Record record_value) {
		
		Dial dial = new Dial.Builder()
				.record(record_value)
				.action(actionURL)
				.timeout(180)
				.callerId(fromNumber)
				.number(toNumber)
				.number(toNumberSA1)
				.build();
		
		return new VoiceResponse.Builder()
				.dial(dial)
				.build()
				.toXml();

	}
	
	public String getVoiceResponseForGreetings(String callBackURL, String greetings_url) {
				
		Play play = new Play.Builder(greetings_url) 
	            .build();
		
		Redirect redirect = new Redirect
		            .Builder(callBackURL).method(com.twilio.http.HttpMethod.POST)
		            .build();
		
		return new VoiceResponse.Builder()
				.play(play)
				.redirect(redirect)
				.build()
				.toXml();
	}
	
	public String getVoiceResponseForPrompt2Accept(String gatherURL, String missedCallURL, String prompt) {
		
		Play play = new Play.Builder(prompt).build();
		
		Gather gather = new Gather.Builder()
				.action(gatherURL)
				.play(play)
				.finishOnKey("")
				.method(com.twilio.http.HttpMethod.POST)
				.numDigits(1)
				.timeout(10)
				.build();

		Redirect redirect = new Redirect.Builder(missedCallURL)
				.method(com.twilio.http.HttpMethod.POST)
				.build();
		
		return new VoiceResponse.Builder()
				.gather(gather)
				.redirect(redirect)
				.build()
				.toXml();
	}
	
	public String getVoiceResponseForDAAutoReplyCall(String baseUrl, Long dealerAssociateID, Boolean transcribe) 
	{
		
		String twilioResponseObject = "";
			
		String actionUrl = baseUrl + TwilioOpsEnum.EndCall;
		String recordingUrl = generalRepository.getAutoReplyCallURLForCurrentOutOfOfficeForDealerAssociate(dealerAssociateID);

		Record record = new Record.Builder()
				.action(actionUrl)
				.maxLength(100)
				.trim(Trim.TRIM_SILENCE)
				.build();
		
		Play play = null;
		if(recordingUrl!=null) {
			play = new Play.Builder(recordingUrl).build();
		}
		
		Play play2 = new Play.Builder(THANKYOU_FOR_USING_MYKAARMA_URL).build();
		
		if(play==null) {
			
			twilioResponseObject = new VoiceResponse.Builder()
					.record(record)
					.play(play2)
					.build()					
					.toXml();
			
		}else {
			
			twilioResponseObject = new VoiceResponse.Builder()
					.play(play)
					.record(record)
					.play(play2)
					.build()					
					.toXml();			
		}
		
		return twilioResponseObject;
	}
	
	public String getVoiceResponseForDealerAutoReplyCall(String baseUrl, Boolean transcribe, String autoReplyRecordingUrl) 
	{
		String twilioResponseObject = "";

		String actionUrl = baseUrl + TwilioOpsEnum.EndCall;
		
		if(autoReplyRecordingUrl == null || autoReplyRecordingUrl.trim().isEmpty())
		{
			//Get default
			autoReplyRecordingUrl = BASE_URL + DEALER_AUTO_REPLY_URL;
		}
		
		Play play = new Play.Builder(autoReplyRecordingUrl).build();
		Play play2 = new Play.Builder(THANKYOU_FOR_USING_MYKAARMA_URL).build();
		
		Record record = new Record.Builder()
				.action(actionUrl)
				.maxLength(100)
				.trim(Trim.TRIM_SILENCE)
				.build();
		
		twilioResponseObject = new VoiceResponse.Builder()
				.play(play)
				.record(record)
				.play(play2)
				.build()					
				.toXml();
		
		
		return twilioResponseObject;
	}
	
	public String getVoiceResponseForGreetings(String prompt) {
		
		Play play = new Play.Builder(prompt)
	            .build(); 
		
		return new VoiceResponse.Builder()
				.play(play)
				.build().toXml();
	}	
	
	public String getVoiceResponseForWrongPrompt(String prompt, String gatherURL, String missedCallURL) {
		
		Play play = new Play.Builder(prompt).build();

		Gather gather = new Gather.Builder()
				.action(gatherURL)
				.finishOnKey("")
				.method(com.twilio.http.HttpMethod.POST)
				.numDigits(1)
				.timeout(10)
				.play(play)
				.build();
		
		Redirect redirect = new Redirect.Builder(missedCallURL)
				.method(com.twilio.http.HttpMethod.POST)
				.build();
		
		return new VoiceResponse.Builder()
				.gather(gather)
				.redirect(redirect)
				.build()
				.toXml();
	}
	
	public String getVoiceResponseForInboundCallToSA(String from, String to_Number, String sendDigits, String callBackURL, Dial.Record recordValue) {
		
		Number number = null;
		if(sendDigits==null || sendDigits=="") {
			number = new Number.Builder(to_Number).build();
		}else {
			number = new Number.Builder(to_Number).sendDigits(sendDigits).build();
		}
		
		Dial dial = new Dial.Builder()
				.timeout(180)
				.callerId(from)
				.record(recordValue)
				.action(callBackURL)
				.number(number)
				.build();
		
		Play play = new Play.Builder(THANKYOU_FOR_USING_MYKAARMA_URL)
	            .build();
		
		return new VoiceResponse.Builder()
				.dial(dial)
				.play(play)
				.build().toXml();
	}
	
	public String getVoiceResponseForfetchConferenceTwimlForCallee(String confID) {
		
		Conference conference = new Conference.Builder(confID)
				.beep(Beep.FALSE)
				.build();
				
		Dial dial = new Dial.Builder()
				.conference(conference)
				.build();
		
		return new VoiceResponse.Builder()
				.dial(dial)
				.build()
				.toXml();		
	}
	
	public String getVoiceResponseForFallback() {
		
		Say say = new Say.Builder("Sorry, call could not be completed.")
				.voice(Voice.WOMAN)
				.build();
		
		return new VoiceResponse.Builder()
				.say(say)
				.build()
				.toXml();
	}

	public List<String> getConnectingNumbersAndToNumber(ConnectingNumbers cnums, String from, String caller) {
		
		List<String> response = new ArrayList<>();
		String connectingNumber = null;
		String connectingNumberNodes = "";
		String to_number = "";
		String sendDigits = "";
		
		if(cnums.getConnectingNumbers() != null)
		{	
			for (int i = 0; i < cnums.getConnectingNumbers().size(); i++) {

				connectingNumber = cnums.getConnectingNumbers().get(i);

				if (connectingNumber != null) {
			
					from = caller;
					String[] numbers = connectingNumber.split(":");
					
					if (numbers.length == 1) {
						
						connectingNumberNodes += "<Number> " + numbers[0]+ "</Number> ";
						to_number = numbers[0];
					} else if (numbers.length == 2) {
						
						connectingNumberNodes += "<Number sendDigits=\""+ numbers[1] + "\">" + numbers[0]+ "</Number> ";
						to_number = numbers[0];
						sendDigits = numbers[1];
					}
					LOGGER.info("connecting no. nodes:\t {} FROM: {} \t TO: {}", connectingNumberNodes, from, connectingNumber);
				}
			}
		}
		
		response.add(connectingNumberNodes);
		response.add(connectingNumber);
		response.add(to_number);
		response.add(sendDigits);
		response.add(from);
		
		return response;
	}
}

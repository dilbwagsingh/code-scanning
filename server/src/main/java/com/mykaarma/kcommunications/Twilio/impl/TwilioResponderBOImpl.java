package com.mykaarma.kcommunications.Twilio.impl;

import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.MessageType;
import com.mykaarma.global.TwilioParams;
import com.mykaarma.kcommunications.Twilio.CallStatus;
import com.mykaarma.kcommunications.Twilio.TwilioResponderBO;
import com.mykaarma.kcommunications.Twilio.impl.TwilioConstantsBOImpl.TwilioOpsEnum;
import com.mykaarma.kcommunications.controller.impl.CommunicationsApiImpl;
import com.mykaarma.kcommunications.controller.impl.ForwardedAndBotMessageImpl;
import com.mykaarma.kcommunications.controller.impl.MessageSendingRules;
import com.mykaarma.kcommunications.controller.impl.PostMessageSendingHelper;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCallRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCredentialsRepository;
import com.mykaarma.kcommunications.model.jpa.VoiceCall;
import com.mykaarma.kcommunications.model.jpa.VoiceCredentials;
import com.mykaarma.kcommunications.redis.VoiceCallingRedisService;
import com.mykaarma.kcommunications.utils.Actions;
import com.mykaarma.kcommunications.utils.FilterDataRequest;
import com.mykaarma.kcommunications.utils.FilterHistory;
import com.mykaarma.kcommunications.utils.FilterName;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.InboundCallRecordingUrlResponse;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.KMessagingApiHelper;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications.utils.RulesEngineHelper;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.impl.TranscribeJobTask;
import com.mykaarma.kcommunications.utils.TwilioClientUtil;
import com.mykaarma.kcommunications.utils.VoiceCallHelper;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kmanage.model.dto.json.response.GetDepartmentResponseDTO;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Play;

@Service
public class TwilioResponderBOImpl implements TwilioResponderBO {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TwilioResponderBOImpl.class);
	
	@Autowired
	TwilioGatewayServiceBOImpl twilioGatewayServiceBOImpl;
	
	@Autowired
	TwilioConstantsBOImpl twilioConstantsBO;
	
	@Autowired
	VoiceCredentialsRepository voiceCredentialRepository;
	
	@Autowired
	VoiceCallRepository voiceCallRepository;

	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	MessageRepository messageRepository;
	
	@Autowired
	MessageSendingRules messageSendingRules;
	
	@Autowired
	KMessagingApiHelper kMessagingApiHelper;
	
	@Autowired
	CommunicationsApiImpl communicationsApiImpl;
	
	@Autowired
	@Qualifier("transcribeJobExecutor")
	private ThreadPoolTaskExecutor transcribeJobExecutor;
	
	@Autowired
	Helper helper;
	
	@Autowired
	MessagingViewControllerHelper messagingViewControllerHelper;
	
	@Autowired
	PostMessageSendingHelper postMessageSendingHelper;
	
	@Autowired
	ThreadRepository threadRepository;
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private RestTemplate restTemplate = new RestTemplate();
	
	@Autowired
	TwilioClientUtil gateway;
	
	@Autowired
	private InboundCallProcessor inboundCallProcessor;
	
	@Autowired
	VoiceCallHelper voiceCallHelper;
	
	@Autowired
	VoiceCallingRedisService voiceCallingRedisService;
	
	@Autowired
	RulesEngineHelper rulesEngineHelper;

	@Autowired
	private ForwardedAndBotMessageImpl forwardedAndBotMessageImpl;
	
	
	@Value("${krulesengine.url}")
	String KRULES_URL;
	
	@Value("${base_url}")
	String BASE_URL;
	
	@Value("${twilio_base_url}")
	String TWILIO_BASE_URL;
	
	@Value("${kmessaging_api_url}")
	String KMESSAGING_API_URL;
	
	@Value("${twilio_announce_greeting_number}")
	String TWILIO_ANNOUNCE_GREETING_NUMBER;

	
	private static final String BLANK_TRANSCRIPTION = "";
	private static final String TWILIO_RECORDING_URL = "https://api.twilio.com/2010-04-01/Accounts/%s/Calls/%s/Recordings.json";
	public static final String DEFAULTINBOUNDCALLRECORDINGURL="https://app.mykaarma.com/audio/recordings/Please_hold_you_are_now_being_connected.mp3";	
	private static final String CALLKCOMMUNICATIONSTOMIGRATERECORDINGTOS3 = "callKcommunicationsToMigrateRecordingToS3";
	private static final String PRESS1_TO_START_THE_CALL = "https://app.mykaarma.com/audio/recordings/press_1_to_start_the_call.mp3";
	private static final String YOU_HAVE_PRESSED_INCORRECTKEY = "https://app.mykaarma.com/audio/recordings/YouhavepressedanIncorrectKey.mp3";
	private static final String THANKYOU_FOR_USING_MYKAARMA = "https://app.mykaarma.com/audio/recordings/Thank_you_for_using_Kaarma.mp3";
	private static final String YOU_HAVE_REACHED_AN_INVALID_PHONE_NUMBER = "https://app.mykaarma.com/audio/recordings/You_have_reached_an_invalid_phone_new.mp3";
	private static final String YOU_HAVE_NOT_PRESSED_ANY_KEY = "https://app.mykaarma.com/audio/recordings/YouhavenotPressed_anyKey.mp3";
	private static final String LINE_IS_BUSY = "https://app.mykaarma.com/audio/recordings/TheLineisBusy.mp3";
	private static final String THERE_IS_NO_RESPONSE = "https://app.mykaarma.com/audio/recordings/SorryThere_is_No_response_from_the.mp3";
	private static final String CONFERENCE_RESPONSE_FOR_CALLEE = "conference/twiml/callee?confID=";
	private static final String WAIT_URL = "https://app.mykaarma.com/audio/recordings/phone_ringing.mp3";
	private static final String CONFERENCE_URL = "conference/events";
	private static final String DEFAULT_SALES_GREETINGS_URL = "audio/recordings/Sales_Dealership_Associate.mp3";
	private static final String PARTY1_CONNECTED_CONFERENCE = "party1-connected-conference";
	
	
	@Override
	public String handleP1Connected(HttpServletRequest request) throws Exception {
		
		return handleP1Connected(request, false, false);
	}

	@Override
	public String handleP1ConnectedConference(HttpServletRequest request) throws Exception {

		return handleP1Connected(request,true, false);
	}
	
	@Override
	public String handleSupportCall(HttpServletRequest request) throws Exception {
		
		return handleP1Connected(request, false, true);
	}
	
	public String handleP1Connected(HttpServletRequest request, Boolean isAnnounceCall, Boolean isSupportCall) throws Exception {
				
        String twilioResponseObject = "";
		try {
			String callSid = request.getParameter(TwilioParams.CALL_SID.getValue());
			String accountSID = request.getParameter(TwilioParams.ACCOUNT_SID.getValue());
			
	
			boolean retryAcceptPrompt = false;
			
			LOGGER.info("handleP1Connected call_status={} Current time={} for call_sid={} account_sid={} to_number={}",request.getParameter(TwilioParams.CALL_STATUS.getValue()), new Date(), callSid, accountSID,request.getParameter(TwilioParams.TO.getValue()));
			
			
			// Make a new function
			ExceptionClassifierRetryPolicy exceptionRetry = new ExceptionClassifierRetryPolicy();
			Map<Class<? extends Throwable>, org.springframework.retry.RetryPolicy> policyMap = new HashMap<>();
			
			//for deadlock and staleobject
			SimpleRetryPolicy policy = new SimpleRetryPolicy();
			policy.setMaxAttempts(5);
			       
			//for all other exceptions
			policyMap.put(Exception.class, policy);
		
			exceptionRetry.setPolicyMap(policyMap);
			
			ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
			backOffPolicy.setMaxInterval(500);
			backOffPolicy.setMultiplier(2);
			RetryTemplate template = new RetryTemplate();
			template.setRetryPolicy(exceptionRetry);
			template.setBackOffPolicy(backOffPolicy);
			template.setThrowLastExceptionOnExhausted(true);
			VoiceCall vc = template.execute(new RetryCallback<VoiceCall, Exception>() {

				@Override
				public VoiceCall doWithRetry(RetryContext context) throws Exception {
					LOGGER.info("trying now to fetch Voice Call in handleP1Connected call_status={} Current time={} for call_sid={} account_sid={} retry_count={} to_number={}",request.getParameter(TwilioParams.CALL_STATUS.getValue()), new Date(), callSid, accountSID,context.getRetryCount(),request.getParameter(TwilioParams.TO.getValue()));
								
					List<VoiceCall> vc = voiceCallRepository.getVoiceCall(callSid);
					VoiceCall voiceCall = null;
					if(vc!=null) {
						voiceCall = vc.get(0);
					}
					
					if(voiceCall==null){
						Exception ise=new Exception(callSid);
						LOGGER.info("not found Voice Call in handleP1Connected retrying call_status={} Current time={} for call_sid={} account_sid={} retry_count={} to_number={}",request.getParameter(TwilioParams.CALL_STATUS.getValue()), new Date(), callSid, accountSID,context.getRetryCount(),request.getParameter(TwilioParams.TO.getValue()));
						throw ise;
					}
					return voiceCall;
				}
				
			});
			
			// Checking DSO's for Outbound_Greetings and Record
						
			Boolean isGreetingEnable = false;
			Boolean recordInitialAudio = false;

			Long dealerID = voiceCredentialRepository.getDealerIDForAccountSid(accountSID);			
			String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
						
			Set<String> dsoKeyMap = new HashSet<String>();
			if(accountSID!=null) {
				
				dsoKeyMap.add(DealerSetupOption.VOICE_OUTBOUND_ISGREETING_DISABLE.getOptionKey());
				dsoKeyMap.add(DealerSetupOption.COMMUNICATIONS_RECORD_GREETING_ROLLOUT.getOptionKey());
				
				try {
					HashMap<String,String> dsoValueMap = kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(dealerUUID,dsoKeyMap);
					isGreetingEnable = "true".equalsIgnoreCase(dsoValueMap.get(DealerSetupOption.VOICE_OUTBOUND_ISGREETING_DISABLE.getOptionKey()));
					recordInitialAudio = "true".equalsIgnoreCase(dsoValueMap.get(DealerSetupOption.COMMUNICATIONS_RECORD_GREETING_ROLLOUT.getOptionKey()));						
					LOGGER.info("value of greeting enable={} record_greetings={} for account_sid={} call_sid={}" ,isGreetingEnable, recordInitialAudio, accountSID, callSid);
				}
				catch(Exception e){
					LOGGER.info("error fetching greeting enable and record_greetings dso for account_sid={} \n{}" , accountSID,e);
				}
				
			}

			boolean isDealer=false;			
			
			String greeting = vc.getParty1Prompt();
		
			LOGGER.info("handleP1Connected greeting={} for CallSID: {}", greeting, callSid);

			if(greeting != null && !greeting.isEmpty()){

				greeting = greeting.toLowerCase();
				if(greeting.startsWith(PRESS1_TO_START_THE_CALL)){
					isDealer=true; 
				}
			}

			String from = request.getParameter(TwilioParams.TO.getValue());
			from = (from == null || from.isEmpty()) ? request.getParameter(TwilioParams.CALLED.getValue()) : from;
			String brokerNumber = vc.getCallBroker();
			if (brokerNumber != null && !brokerNumber.isEmpty()) {
				from = brokerNumber;
			}
			
			if(isSupportCall) {
				from = vc.getCallingParty();
				LOGGER.info("handleP1connected support_call=true for call_sid={} from={}", vc.getCallIdentifier(), from);
			}
			
			String actionURL = "", numberURL = "", fromNumber = "", toNumberSA1 = "", toNumber = "", custPrompt = "", 
					prompt = "", gatherURL = "", missedCallURL = "", waitURL = "", friendlyName = "", callBackConference = "";
			
			Boolean recordflag = false;
			
			if (vc.getParty2Delegate() != null ) {
			
				actionURL = TWILIO_BASE_URL + TwilioOpsEnum.EndCall;
				numberURL = TWILIO_BASE_URL + TwilioOpsEnum.P2Connected;
				fromNumber = from;
				toNumberSA1 = getExtentionNumberNode(vc.getParty2Delegate());
				toNumber = getExtentionNumberNode(vc.getParty2());
				custPrompt = "";
				prompt = (retryAcceptPrompt) ? YOU_HAVE_PRESSED_INCORRECTKEY : vc.getParty1Prompt();
				recordflag = vc.isRecordCall();
				gatherURL = TWILIO_BASE_URL + TwilioOpsEnum.P1Connected;
				missedCallURL = TWILIO_BASE_URL + TwilioOpsEnum.MissedCall;
				
			} else {
				
				actionURL = TWILIO_BASE_URL + TwilioOpsEnum.EndCall;
				numberURL = TWILIO_BASE_URL + TwilioOpsEnum.P2Connected;
				fromNumber = from;
				toNumber = getExtentionNumberNode(vc.getParty2());
				custPrompt = "";
				prompt = (retryAcceptPrompt) ? YOU_HAVE_PRESSED_INCORRECTKEY : vc.getParty1Prompt();
				recordflag = vc.isRecordCall();
				gatherURL = TWILIO_BASE_URL + TwilioOpsEnum.P1Connected;
				missedCallURL = TWILIO_BASE_URL + TwilioOpsEnum.MissedCall;
				
				if(!isAnnounceCall && recordInitialAudio) {

					actionURL = TWILIO_BASE_URL + TwilioOpsEnum.OutboundEndCall;
					recordflag = false;
					
				}

				if(isAnnounceCall) {
										
//					waitURL = BASE_URL + WAIT_URL; for testing 
					waitURL = WAIT_URL;
					friendlyName = callSid;
					callBackConference = TWILIO_BASE_URL + CONFERENCE_URL;
					
				}
			}
				
			Dial.Record record_value = Dial.Record.DO_NOT_RECORD;
			if(recordflag!=null && recordflag == true) 
			{
				 record_value = Dial.Record.RECORD_FROM_RINGING;
			}
			
			LOGGER.info("Call to To_number: {} for callSid = {}",toNumber.substring(1), callSid);
			
			if (isSupportCall || (isGreetingEnable != null && isGreetingEnable)) {

				if(isAnnounceCall)
				{					
//					"twilio.p1connected.conference.xml";					
					twilioResponseObject = voiceCallHelper.getVoiceResponseForP1ConnectedConference(friendlyName, from, waitURL, callBackConference, actionURL, record_value);
					
					twilioGatewayServiceBOImpl.setCallStatus(accountSID, callSid,  CallStatus.party1_connected_conference.getID());
					
				} else {					
//					"twilio.p1connected.xml";						
					twilioResponseObject = voiceCallHelper.getVoiceResponseForP1Connected(fromNumber, toNumber, numberURL, actionURL, record_value);
					
					twilioGatewayServiceBOImpl.setCallStatus(accountSID, callSid,  CallStatus.party1_connected.getID());

				}
				if (vc.getParty2Delegate() != null) {

//					"twilio.p1connectedDelegation.xml";
					twilioResponseObject = voiceCallHelper.getVoiceResponseForP1ConnectedDelegation(fromNumber, toNumber, toNumberSA1, actionURL, record_value);
				}
			} else {

				if (request.getParameter(TwilioParams.DIGITS.getValue()) == null) {

//					"twilio.prompt2accept.xml";			
					twilioResponseObject = voiceCallHelper.getVoiceResponseForPrompt2Accept(gatherURL, missedCallURL, prompt);
					
					twilioGatewayServiceBOImpl.setCallStatus(accountSID, callSid, CallStatus.waiting_connect.getID());

				} else if (request.getParameter(TwilioParams.DIGITS.getValue()).equals("1")) {

					if(isAnnounceCall) {
//						"twilio.p1connected.conference.xml";						
						twilioResponseObject = voiceCallHelper.getVoiceResponseForP1ConnectedConference(friendlyName, from, waitURL, callBackConference, actionURL, record_value);
						
						twilioGatewayServiceBOImpl.setCallStatus(accountSID, callSid,  CallStatus.party1_connected_conference.getID());

						
					} else {
//						"twilio.p1connected.xml";
						twilioResponseObject = voiceCallHelper.getVoiceResponseForP1Connected(fromNumber, toNumber, numberURL, actionURL, record_value);

						twilioGatewayServiceBOImpl.setCallStatus(accountSID, callSid,  CallStatus.party1_connected.getID());

					}

					if (vc.getParty2Delegate() != null) {

//						"twilio.p1connectedDelegation.xml"
						LOGGER.info("\n\t Delegation-Active for CallSID: {}", callSid);
			
						twilioResponseObject = voiceCallHelper.getVoiceResponseForP1ConnectedDelegation(fromNumber, toNumber, toNumberSA1, actionURL, record_value);

					}

				} else if ((request.getParameter(TwilioParams.DIGITS.getValue()).equals("*"))
						|| (request.getParameter(TwilioParams.DIGITS.getValue()).equals("#"))) {

//					"twilio.goodbye.xml"					
					twilioResponseObject = voiceCallHelper.getVoiceResponseForGreetings(THANKYOU_FOR_USING_MYKAARMA);
					
					twilioGatewayServiceBOImpl.setCallStatus(accountSID, callSid, CallStatus.completed.getID());


				} else if ((Integer.parseInt(request.getParameter(TwilioParams.DIGITS.getValue())) > 1) || Integer.parseInt(request.getParameter(TwilioParams.DIGITS.getValue())) == 0) {

					retryAcceptPrompt = true;
//					"twilio.WrongPrompt.xml"
					
					twilioResponseObject = voiceCallHelper.getVoiceResponseForWrongPrompt(prompt, gatherURL, missedCallURL);
					
					twilioGatewayServiceBOImpl.setCallStatus(accountSID, callSid, CallStatus.wrong_prompt.getID());
					
				} else {

					retryAcceptPrompt = true;
//					"twilio.WrongPrompt.xml"
					
					twilioResponseObject = voiceCallHelper.getVoiceResponseForWrongPrompt(prompt, gatherURL, missedCallURL);
					
					twilioGatewayServiceBOImpl.setCallStatus(accountSID, callSid,  CallStatus.wrong_prompt.getID());

				}
			}
			LOGGER.info("Twilio VoiceCallResponse ={} for call_sid={}",twilioResponseObject , callSid);
			//update call status
		} catch (Exception e) {
			LOGGER.error("Error in handleP1Connected, details are: call_sid={} account_sid={} \n{}",  request.getParameter(TwilioParams.CALL_SID.getValue()), request.getParameter(TwilioParams.ACCOUNT_SID.getValue()), e);
			throw e;
		}
		
		return twilioResponseObject;
	}

	@Override
	public String handleP2Connected(HttpServletRequest request) throws Exception {
		
		String twilioResponseObject = "";
		
		try {
//			"twilio.p2connected.xml"
			String callSid = request.getParameter(TwilioParams.PARENT_CALL_SID.getValue());
			LOGGER.info("Codeflow inside handleP2Connected: for CallSID: {}, ParentCallSid = {}, CallStatus = {} DialCallStatus = {} ", 
					callSid, callSid, request.getParameter(TwilioParams.CALL_STATUS.getValue()), request.getParameter(TwilioParams.DIAL_CALL_STATUS.getValue()));
			String accountSID = request.getParameter(TwilioParams.ACCOUNT_SID.getValue());
			String childCallSid = request.getParameter(TwilioParams.CALL_SID.getValue());;
			
			if(request.getParameter(TwilioParams.CALL_STATUS.getValue()).equalsIgnoreCase("in-progress")){

				LOGGER.info("/n Party 2 picked the phone for CallSID={}",childCallSid);

				twilioGatewayServiceBOImpl.setCallStatus(accountSID, callSid, CallStatus.party2_connected.getID());

				List<VoiceCall> voiceCall = voiceCallRepository.getVoiceCall(callSid);
				VoiceCall vc = null;
				if(voiceCall!=null) {
					vc = voiceCall.get(0);
				}
				
				Long dealerID = voiceCredentialRepository.getDealerIDForAccountSid(accountSID);
				String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
				
				Long deptID = voiceCredentialRepository.getDepartmentIDForCallSid(accountSID);


				LOGGER.info("accountSId={} childCallSID={} for callSID={}",accountSID, childCallSid, callSid);
							
				String callBroker = null;
				if(vc!=null) {
					callBroker = vc.getCallBroker();
				}
				if(recordInitialAudio(accountSID,dealerUUID)) {
					LOGGER.info("Recording initial audio is true for dealer_department_id={} for call_sid={}",deptID, callSid);

					callTwilioForRecordingCall(dealerUUID, accountSID, TWILIO_BASE_URL , childCallSid, callBroker, TwilioOpsEnum.OutboundEndCall);	
					voiceCallRepository.updateVoiceCallChildSid(childCallSid, callSid);
					
				}
				
				String prompt = " ";
				if(vc==null || vc.getParty2Prompt() == null) {
					prompt = " ";
				}else {
					prompt = vc.getParty2Prompt();
				}

				twilioResponseObject = voiceCallHelper.getVoiceResponseForGreetings(prompt);
				
				LOGGER.info("dealer_department_id={} for call_sid={} \nTwilioResponseObject={}",deptID, callSid,twilioResponseObject);
			}
			else{

				LOGGER.info("\t Party 2 din't pick the call" + " for CallSID: "+ callSid);
			}
		} catch (Exception e) {
			LOGGER.error("Error in handleP2Connected, details are: CallSID: {} ParentCallSID: {} DialCallStatus: {} AccountSID: {} \n{}", request.getParameter(TwilioParams.CALL_SID.getValue()), request.getParameter(TwilioParams.PARENT_CALL_SID.getValue()), request.getParameter(TwilioParams.DIAL_CALL_STATUS.getValue()), request.getParameter(TwilioParams.ACCOUNT_SID.getValue()),e);
			throw e;
		}
		
		LOGGER.info("Twilio VoiceCallResponse = {}",twilioResponseObject);
		return twilioResponseObject;
		
	}
 
	@Override
	public String handleEndCall(HttpServletRequest request) throws Exception {

		String output="";
		try {
			
			String callSid = request.getParameter(TwilioParams.CALL_SID.getValue());	
			LOGGER.info("Codeflow inside handleEndCall:for CallSID: {}", callSid);
			output = "null";
			String status = request.getParameter(TwilioParams.DIAL_CALL_STATUS.getValue());
			LOGGER.info("CallStatus- {} \n\tCurrent Time-{} for CallSID: {}", request.getParameter(TwilioParams.CALL_STATUS.getValue()), new Date(), callSid);
			LOGGER.info("DialCallStatus: {} for CallSID: {}",status, callSid);
			String sid = request.getParameter(TwilioParams.CALL_SID.getValue());
			String recordingURL = request.getParameter(TwilioParams.RECORDING_URL.getValue());
			String recordingDuration = request.getParameter(TwilioParams.RECORDING_DURATION.getValue());
			String callDuration = request.getParameter(TwilioParams.DIAL_CALL_DURATION.getValue());
			String accountSID = request.getParameter(TwilioParams.ACCOUNT_SID.getValue());
//			"twilio.end.xml"

			recordingURL = getDecodedRecordingUrl(recordingURL);
			int durationValue = 0;
			int recordingDurationValue = 0;
			int callDurationValue = 0;
			try{
				if(callDuration!=null) {
					callDurationValue = Integer.parseInt(callDuration);
				}else {
					LOGGER.warn("Call duration is null for callSid = {}", callSid);
				}
			}
			catch(Exception e1){
				LOGGER.warn("Not able to parse call duration, setting 0, details are: AccountSID: {} CallDuration: {} \nError{}", accountSID, callDuration, e1);
			}
			
			try {
				if(recordingDuration!=null) {
					recordingDurationValue = Integer.parseInt(recordingDuration);
				}else {
					LOGGER.warn("Recording duration is null, callSid = {}", callSid);
				}
			}catch(Exception e1){
				LOGGER.warn("Error {} Not able to parse recording duration, setting 0, details are: RecordingURL {} AccountSID {} RecordingDuration {} ", e1, recordingURL, accountSID, recordingDuration);
			}
			
			durationValue = Math.max(callDurationValue, recordingDurationValue);
			LOGGER.info("updating voice_call with duration_value={} for call_sid={}", durationValue, callSid);

			generalRepository.updateCallInfo(sid, recordingURL, durationValue, BLANK_TRANSCRIPTION);
            
            if(recordingURL!=null && !"null".equals(recordingURL) && !recordingURL.isEmpty()) {
        		callKcommunicationsToMigrateRecordingToS3(sid);
            }
			
			if (transcriptionEnabled(sid)) {
				LOGGER.info("Transcription is enabled: recordingURL = {}, callSid = {}, accountSid = {}", recordingURL, callSid, accountSID);
				scheduleTranscription(sid, recordingURL);
			}
		    else {	
		    	LOGGER.info("transcription not enabled for message with call_sid={} ", sid);	
		    }			
			
			output = updateCallStatus(accountSID, sid, output, status);
	
			try {
				addToMissedCall(callDurationValue, sid, accountSID);
			} catch (Exception e) {
				LOGGER.error("Error in addToMissedCall for call_sid={} recordingDurationValue={} ", sid, recordingDurationValue, e);
			}

			
		} catch (Exception e) {
			LOGGER.error("Error in handleEndCall, details are: CallSID: {} RecordingURL: {} RecordingDuration: {} AccountSID: {} ", request.getParameter(TwilioParams.CALL_SID.getValue()), request.getParameter(TwilioParams.RECORDING_URL.getValue()), request.getParameter(TwilioParams.RECORDING_DURATION.getValue()), request.getParameter(TwilioParams.ACCOUNT_SID.getValue()));
			throw e;
		}

		return output;
	}
	
	private void addToMissedCall(int callDurationValue, String sid, String accountSID) throws Exception {
		
		LOGGER.info("Inside addToMissedCall, sid = {}, accountSid = {}",sid, accountSID);
		
		List<Object[]> result = messageRepository.getFilterDataRequest(sid);
		
		FilterDataRequest message = null;
			
		if(result != null && result.size()>0) {
			message = new FilterDataRequest();
			message.setId( ((BigInteger)result.get(0)[0]).longValue() );
			message.setCustomerID(((BigInteger)result.get(0)[1]).longValue());
			message.setDealerID(((BigInteger)result.get(0)[2]).longValue());
			message.setDealerAssociateID(((BigInteger)result.get(0)[3]).longValue());
			message.setDealerDepartmentID(((BigInteger)result.get(0)[4]).longValue());
			message.setThreadID(((BigInteger)result.get(0)[5]).longValue());
			message.setMessageType(result.get(0)[6].toString());
		}else {
			LOGGER.warn("Filterdata request result is null for sid = {}", sid);
		}
		
		if(message!=null &&  message.getMessageType().equalsIgnoreCase(MessageType.I.toString())) {
			Set<String> dsoKeys = new HashSet<String>();
			Integer dsoForMissedCall = 15;
			dsoKeys.add(DealerSetupOption.COMMUNICATIONS_WAITINGFORRESPONSE_MISSED_CALL_TIME.getOptionKey());
			
			
			Long dealerID = voiceCredentialRepository.getDealerIDForAccountSid(accountSID);
			String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);

			HashMap<String, String> map = kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(dealerUUID, dsoKeys);
			
			
			if(map.get(DealerSetupOption.COMMUNICATIONS_WAITINGFORRESPONSE_MISSED_CALL_TIME.getOptionKey()) != null)
				dsoForMissedCall = Integer.valueOf(map.get(DealerSetupOption.COMMUNICATIONS_WAITINGFORRESPONSE_MISSED_CALL_TIME.getOptionKey())); 
			if(callDurationValue <= dsoForMissedCall)
			{
				 try {
					
					 HashMap<Long,Long> customerIDAndThreadID=new HashMap<Long ,Long>(); 
					 Long customerID = message.getCustomerID();
					 Long threadID =  message.getThreadID();
					 customerIDAndThreadID.put(customerID, threadID);
					 LOGGER.info("Requesting for ADD_TO_MISSED_CALL for recording_duration_value={} for customer_id={} message_id={} call_sid={} and thread_id={}",callDurationValue,customerID,message.getId(), sid, threadID);
					 updateThreadInWaitingForResponseFilter(message.getDealerID(), message.getDealerDepartmentID(),
							null,customerIDAndThreadID,Actions.ADD_TO_MISSED_CALL.getValue());
				 }
				 catch (Exception e) {
					 LOGGER.error("Error in ADD_TO_MISSED_CALL, CallSID={}  ", sid, e);
				 }
			 }
			}	
	}
	
	public String updateCallStatus(String accountSID, String sid, String output, String status) throws Exception {
		
		String playURL = null;
		if(status == null)
		{
			LOGGER.info( "No dial status available. This is probably an OutOfOffice auto call reply. for CallSID: {}", sid);
			twilioGatewayServiceBOImpl.setCallStatus(accountSID,sid, CallStatus.completed.getID());
			return "";
		}
		else if (status.equalsIgnoreCase(CallStatus.busy.name())) {

			LOGGER.info("\n\t CallStatus-Busy for CallSID: {}", sid);
			twilioGatewayServiceBOImpl.setCallStatus(accountSID,sid, CallStatus.busy.getID());
			playURL = LINE_IS_BUSY;

		} else if (status.equalsIgnoreCase("no-answer") || status.equalsIgnoreCase(CallStatus.no_answer.name())) {

			LOGGER.info("\n\t CallStatus-No Answer for CallSID: {}", sid);
			twilioGatewayServiceBOImpl.setCallStatus(accountSID,sid, CallStatus.no_answer.getID());
			playURL = THERE_IS_NO_RESPONSE;

		} else if (status.equalsIgnoreCase(CallStatus.completed.name())){

			twilioGatewayServiceBOImpl.setCallStatus(accountSID,sid, CallStatus.completed.getID());

			LOGGER.info("CallStatus-Completed for CallSID: {}", sid);
			playURL = THANKYOU_FOR_USING_MYKAARMA;

		} else if (status.equalsIgnoreCase(CallStatus.answered.name())){
			// in case of conference
			twilioGatewayServiceBOImpl.setCallStatus(accountSID,sid, CallStatus.completed.getID());

			LOGGER.info("CallStatus-Answered for CallSID: {}", sid);
			playURL = THANKYOU_FOR_USING_MYKAARMA;

		} else {

			LOGGER.info("CallStatus-Failed for CallSID: {}", sid);
			twilioGatewayServiceBOImpl.setCallStatus(accountSID,sid,CallStatus.failed.getID());
			playURL = YOU_HAVE_REACHED_AN_INVALID_PHONE_NUMBER;
		}
		
		
		return voiceCallHelper.getVoiceResponseForGreetings(playURL);
	}

	@Override
	public String handleMissedCall(HttpServletRequest request) throws Exception {
		LOGGER.info( "In handleMissedCall method for CallSID: {}", request.getParameter(TwilioParams.CALL_SID.getValue()));
//		"twilio.missed.xml";
		
		String twilioResponseObject = "";
		String sid = request.getParameter(TwilioParams.CALL_SID.getValue());
		String accountSid = request.getParameter(TwilioParams.ACCOUNT_SID.getValue());
		try {
			twilioGatewayServiceBOImpl.setCallStatus(accountSid, sid,  CallStatus.party1_dropped.getID());
		} catch (Exception e) {
			LOGGER.error("Error {} in handleMissedCall, details are: CallSID: {} AccountSID: {} ", e, sid, accountSid);
			throw e;
		}

		twilioResponseObject = voiceCallHelper.getVoiceResponseForGreetings(YOU_HAVE_NOT_PRESSED_ANY_KEY);
		return twilioResponseObject;
	}

	@Override
	public String handleFallBack(HttpServletRequest request) throws Exception {

		LOGGER.info("Inside handleFallBack");
		
//		"twilio.fallback.xml"

		String sid = request.getParameter(TwilioParams.CALL_SID.getValue());
		String accountSid = request.getParameter(TwilioParams.ACCOUNT_SID.getValue());
		try {
			twilioGatewayServiceBOImpl.setCallStatus(accountSid, sid, CallStatus.failed.getID());
		} catch (Exception e) {
			LOGGER.error("Error = {} in handleFallBack, details are: CallSID: {} AccountSID: {} ", e, sid, accountSid);
			throw e;
		}
		
		return voiceCallHelper.getVoiceResponseForFallback();
	}

	@Override
	public String handleInboundCall(HttpServletRequest request) throws Exception {

		String callSid = request.getParameter(TwilioParams.CALL_SID.getValue());
		String callerBasic = request.getParameter(TwilioParams.CALLER.getValue());
		String called = request.getParameter(TwilioParams.CALLED.getValue());
		String accountSid = request.getParameter(TwilioParams.ACCOUNT_SID.getValue());
		String from = request.getParameter(TwilioParams.TO.getValue());
		String baseURL = TWILIO_BASE_URL;
		String appURL = BASE_URL;
		String greetings_url = ""; 
		
		Long dealerID = voiceCredentialRepository.getDealerIDForAccountSid(accountSid);
		String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
		Long departmentID = voiceCredentialRepository.getDepartmentIDForCallSid(accountSid);		
		String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(departmentID);
		
		String dsoForCountryCodeValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.COMMUNICATIONS_COUNTRYCODE_ROLLOUT.getOptionKey());

		String caller = null;
		if("true".equalsIgnoreCase(dsoForCountryCodeValue)) {
			caller = callerBasic;
		}
		else {
			caller = callerBasic.substring(2);
		}

		LOGGER.info(" Enter Inbound call at time={} for call_sid={} and account_sid={} caller_basic={} caller={} called={} from={}",new Date().getTime(),callSid,accountSid, callerBasic, caller, called, from);	

		try {
			
			LOGGER.info("dealerID = {}, dealerUUID = {}, departmentID = {}, departmentUUID = {}",dealerID, dealerUUID, departmentID, departmentUUID);
						
			if(generalRepository.checkIfCommValueCanBeUsed(caller)==false) {
				LOGGER.error("Number is not valid");
				return null;
			}
						
			GetDepartmentResponseDTO dealerDepartment = kManageApiHelper.getDealerDepartment(departmentUUID);
			String recordingInitials = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.COMMUNICATIONS_RECORD_GREETING_ROLLOUT.getOptionKey());
			
			if("true".equalsIgnoreCase(recordingInitials)) {	

				callTwilioForRecordingCall(dealerUUID, accountSid, baseURL,callSid, called, TwilioOpsEnum.InboundEndCall);	
				
			}
	
			LOGGER.info( "accountSID: {}, for CallSID: {}, dd id: {}", accountSid, callSid, dealerDepartment.getDepartmentExtendedDTO().getId());

			if(("SALES".equalsIgnoreCase(dealerDepartment.getDepartmentExtendedDTO().getName()))) {
				String defaultCallRecordingUrlForSales= appURL + DEFAULT_SALES_GREETINGS_URL;
				greetings_url = defaultCallRecordingUrlForSales;
				LOGGER.info( "TwilioResponderBOImplService: salesDefaultRecording= {}", defaultCallRecordingUrlForSales);
			} 
			
			
			try {
				
				InboundCallRecordingUrlResponse inboundCallRecordingUrlResponse = rulesEngineHelper.getInboundCallRecordingResponse(dealerDepartment);
				if(inboundCallRecordingUrlResponse!=null && inboundCallRecordingUrlResponse.getCallRecordingURL()!=	null) {
					greetings_url = inboundCallRecordingUrlResponse.getCallRecordingURL();
				}

				LOGGER.info( "TwilioResponderBOImplService: dealer_id = {},  department_id= {}, greetingsURL={}",dealerDepartment.getDepartmentExtendedDTO().getDealerMinimalDTO().getId() ,dealerDepartment.getDepartmentExtendedDTO().getId() ,greetings_url );
		
			} catch (Exception e) {
				
				LOGGER.error("TwilioResponderBOImplService: Error while fetching incomingCallRecordingURL from kRules:  url="+ KRULES_URL + "/incoming/call/recording" + " dealer_id="+dealerDepartment.getDepartmentExtendedDTO().getDealerMinimalDTO().getId() + " department_id="+dealerDepartment.getDepartmentExtendedDTO().getId());
			
			}
						
		} catch (Exception e) {
			
			LOGGER.error("Error in handleInboundCall, details are: CallSID: {} Called: {} From: {} AccountSID: {} ", callSid , called, from, accountSid, e);
		}
				
		//USING VOICE RESPONSE OBJECT
		
		String callBackURL = TWILIO_BASE_URL + TwilioOpsEnum.InboundCallForward;		
//		"twilio.greet.xml"	
		String twilioResponseObject = voiceCallHelper.getVoiceResponseForGreetings(callBackURL, greetings_url);
		
		LOGGER.info("TwilioResponseObject = {} for CallSID: {}", twilioResponseObject, callSid);
		return twilioResponseObject;
		
	}

	@Override
	public String handleInboundForwardCall(HttpServletRequest request) throws Exception {

		String callSid = request.getParameter(TwilioParams.CALL_SID.getValue());
		
		LOGGER.info( "Code in handleInboundForwardCall  for CallSID: {}", callSid);
		String callerBasic = request.getParameter(TwilioParams.CALLER.getValue());
//		String caller = callerBasic.substring(2);
		String accountSid = request.getParameter(TwilioParams.ACCOUNT_SID.getValue());
		String from = request.getParameter(TwilioParams.TO.getValue());
		
		Long dealerID = voiceCredentialRepository.getDealerIDForAccountSid(accountSid);
		String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
		
		String dsoForCountryCodeValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.COMMUNICATIONS_COUNTRYCODE_ROLLOUT.getOptionKey());

		String caller = null;
		if("true".equalsIgnoreCase(dsoForCountryCodeValue)) {
			caller = callerBasic;
		}
		else {
			caller = callerBasic.substring(2);
		}

		return inboundCallProcessor.processInboundCall(caller, callSid, from, accountSid);
	}


	@Override
	public void updateCallInfo(String callSid, String recordingUrl, int recordingDuration, String transcription) {

		try {
			generalRepository.updateCallInfo(callSid, recordingUrl, recordingDuration, transcription);
			
		} catch (Exception e) {			
			LOGGER.error("Error in UpdateCallInfo, details are: CallSID: {} RecordingURL: {} RecordingDuration: {} Transcription: {} ", callSid, recordingUrl, recordingDuration, transcription, e);
		}
		
	}

	@Override
	public void handleAnnounceConferenceGreeting(String friendlyCallName, String accountSID, VoiceCall vc)
			throws Exception {

		try {
			LOGGER.info("Codeflow inside handleAnnounceConferenceGreeting for friendy_call_name={} account_sid={}",friendlyCallName, accountSID);
			// already connected to customer, calling third party for greeting
			
			String brokerNumber = vc.getCallBroker();	
			String[] cred = getCredsDetailsForSID(accountSID, friendlyCallName, CallStatus.playing_conference_greeting.getID(), brokerNumber);

			if(cred==null) {
				return;
			}
			
			if (cred[0] == null && cred[1] == null) {
				return ;
			}

			String thirdPartyNumber = TWILIO_ANNOUNCE_GREETING_NUMBER;
			
			com.twilio.rest.api.v2010.account.Call call = gateway.conferenceCall(cred[0], cred[1], thirdPartyNumber, thirdPartyNumber,
					TWILIO_BASE_URL + CONFERENCE_RESPONSE_FOR_CALLEE + friendlyCallName, brokerNumber,
					TWILIO_BASE_URL + TwilioOpsEnum.FallBack, null);
			if(call==null) {
				LOGGER.warn("Call is null for thirdpatyNumber = {}, brokerNumber = {}", thirdPartyNumber, brokerNumber);
				return;
			}
				
			LOGGER.info("handleAnnounceConferenceGreeting for call_sid={} greeting_url={}", call.getSid(), vc.getParty2Prompt());
		} catch (Exception e) {
			LOGGER.error("Error in handleP2ConnectedConference, details are: call_sid={} account_sid={} ", friendlyCallName, accountSID, e);
		}
		
	}

	@Override
	public void handleP2ConnectedConference(String friendlyCallName, String accountSID, VoiceCall vc) throws Exception {

		try {
			
			LOGGER.info("Codeflow inside handleP2ConnectedConference for friendy_call_name={} account_sid={}",friendlyCallName, accountSID);
			
			String brokerNumber = vc.getCallBroker();			
			String[] cred = getCredsDetailsForSID(accountSID, friendlyCallName, CallStatus.party2_connected_conference.getID(), brokerNumber);

			if ((cred== null) || (cred[0] == null && cred[1] == null)) {
				return ;
			}
						
			com.twilio.rest.api.v2010.account.Call call = gateway.conferenceCall(cred[0],cred[1],vc.getParty2(), vc.getParty2(),
					TWILIO_BASE_URL+CONFERENCE_RESPONSE_FOR_CALLEE+friendlyCallName, brokerNumber,
					TWILIO_BASE_URL + TwilioOpsEnum.FallBack, null);
			
			if(call == null) {
				LOGGER.warn("Call is null for {} and {}", vc.getParty2(), brokerNumber);
				return;
			}
			
		} catch (Exception e) {
			LOGGER.error("Error in handleP2ConnectedConference, details are: call_sid={} account_sid={} ", friendlyCallName, accountSID, e);
		}

		
	}

	@Override
	public String handleEndCallConference(HttpServletRequest request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String handleConferenceEvents(HttpServletRequest request)
			throws Exception {
		
		String statusCallbackEvent = request.getParameter("StatusCallbackEvent");
		String friendlyName = request.getParameter("FriendlyName");
		String accountSid = request.getParameter(TwilioParams.ACCOUNT_SID.getValue());
		
		return handleConferenceEventsHelper(statusCallbackEvent, friendlyName, accountSid);
	}

	private String handleConferenceEventsHelper(String statusCallbackEvent, String friendlyName, String accountSid)
			throws Exception {

		LOGGER.info("handleConferenceEvents event_name={} friendly_name={} account_sid={} " ,statusCallbackEvent, friendlyName, accountSid);

		
		List<VoiceCall> voiceCall = voiceCallRepository.getVoiceCall(friendlyName);
		
		VoiceCall vc = null;
		if(voiceCall!=null && voiceCall.size()>0) {
			vc = voiceCall.get(0);
		}
		
		if(statusCallbackEvent != null) {
			if(statusCallbackEvent.contains("start")) {
				LOGGER.info("handleConferenceEvents Inside start event of conference event_name={} friendly_name={} account_sid={} " ,statusCallbackEvent, friendlyName, accountSid);
				handleAnnounceConferenceGreeting(friendlyName, accountSid, vc);	
			} else if(statusCallbackEvent.contains("join")){
				LOGGER.info("handleConferenceEvents Inside join event of conference event_name={} friendly_name={} account_sid={} " ,statusCallbackEvent, friendlyName, accountSid);
				if(vc!=null && vc.getCallStatus().intValue()==CallStatus.getByFormattedName(PARTY1_CONNECTED_CONFERENCE).getID()) {
					handleP2ConnectedConference(friendlyName, accountSid, vc);
				}
			} else {
				LOGGER.info("handleConferenceEvents Inside other event of conference event_name={} friendly_name={} account_sid={} " ,statusCallbackEvent, friendlyName, accountSid);
			}
		}
		
		return new VoiceResponse.Builder().build().toXml();
	}
	
	private String[] getCredsDetailsForSID(String accountSID, String friendlyCallName, int callStatusID, String brokerNumber) throws Exception {
		
		twilioGatewayServiceBOImpl.setCallStatus(accountSID, friendlyCallName, callStatusID);
				
		List<VoiceCredentials> voiceCredentials = voiceCredentialRepository.getVoiceCredentialsForAccountSIDAndBroker(accountSID, brokerNumber);

		if(voiceCredentials == null || voiceCredentials.size()<1) {
			// invalid case, the same broker number should be used in all the three calls
			LOGGER.error("Unable to locate Voice Credentials for given account_sid={} broker_number={} ", accountSID, brokerNumber);
			return null;
		}
		
		VoiceCredentials voiceCreds = voiceCredentials.get(0);
		
		return twilioGatewayServiceBOImpl.getSeparatedDetailsFromDealerSubaccount(voiceCreds.getDealerSubaccount());
	}
	
	@Override
	public String handleInboundEndCall(HttpServletRequest request) throws Exception {

		String twilioResponseObject = "";
		
		try {
			String callSid = request.getParameter(TwilioParams.CALL_SID.getValue());
			LOGGER.info( "Codeflow inside handleInboundEndCall: for CallSID: {}", callSid);
			String status = request.getParameter(TwilioParams.DIAL_CALL_STATUS.getValue());
			LOGGER.info( "CallStatus={}, Current Time={} for CallSID: {} DialCallStatus = {} ", request.getParameter(TwilioParams.CALL_STATUS.getValue()), new Date(), callSid, status);
			String sid = request.getParameter(TwilioParams.CALL_SID.getValue());
			String recordingURL = request.getParameter(TwilioParams.RECORDING_URL.getValue());
			String recordingDuration = request.getParameter(TwilioParams.RECORDING_DURATION.getValue());
			String callDuration = request.getParameter(TwilioParams.DIAL_CALL_DURATION.getValue());
			String accountSID = request.getParameter(TwilioParams.ACCOUNT_SID.getValue());
			String recordingSid = request.getParameter(TwilioParams.RECORDING_SID.getValue());
			
			int recordingDurationValue = 0;
			int callDurationValue = 0;
			
			recordingURL = getDecodedRecordingUrl(recordingURL);
			LOGGER.info("recording_sid={} call_duration={} recording_duration={} for call_sid={}",recordingSid, callDuration, recordingDuration, sid);

			try{
				if(callDuration!=null) {
					callDurationValue = Integer.parseInt(callDuration);
				}
				if(recordingDuration!=null) {
					recordingDurationValue = Integer.parseInt(recordingDuration);
				}
			}catch(Exception e1){
				LOGGER.warn("errorMessage = {} \n Not able to parse recording duration, setting 0, details are: RecordingURL={} AccountSID={} RecordingDuration={} CallSOD={}", e1, recordingURL, accountSID, recordingDuration, callSid);
			}
			
			if(recordingURL!=null) {
				generalRepository.updateCallInfo(sid, recordingURL, recordingDurationValue, BLANK_TRANSCRIPTION);
        		callKcommunicationsToMigrateRecordingToS3(sid);
        		
				if (transcriptionEnabled(sid)) {
					
					LOGGER.info("Transcription is enabled for callSid = {}", sid);
					scheduleTranscription(sid, recordingURL);
					
				}
			    else {	
			    	LOGGER.info("transcription not enabled for message with call_sid={} ", sid);	
	
			    }
            }
            
            if(recordingDuration==null) {
            	
            	twilioResponseObject = updateCallStatus(accountSID, sid, "", status);

            }
			try {
				if(callDuration!=null) {
					addToMissedCall(callDurationValue, sid, accountSID);
				}
			} catch (Exception e) {
				LOGGER.error("Error in addToMissedCall for call_sid={} recordingDurationValue={} ", sid, recordingDurationValue);
			}

			
		} catch (Exception e) {
			LOGGER.error("Error = {} /n in handleEndCall, details are: CallSID: {} RecordingURL: {} RecordingDuration: {} AccountSID: {} ", e, request.getParameter(TwilioParams.CALL_SID.getValue()), request.getParameter(TwilioParams.RECORDING_URL.getValue()), request.getParameter(TwilioParams.RECORDING_DURATION.getValue()), request.getParameter(TwilioParams.ACCOUNT_SID.getValue()));
			throw e;
		}

		return twilioResponseObject;
	}

	@Override
	public String handleOutboundEndCall(HttpServletRequest request) throws Exception {

		String output="";
		try {
			String callSid = request.getParameter(TwilioParams.CALL_SID.getValue());
			LOGGER.info( "Codeflow inside handleOutboundEndCall:\n\t" + " for CallSID: "+ callSid);
			String status = request.getParameter(TwilioParams.DIAL_CALL_STATUS.getValue());
			LOGGER.info( "CallStatus-" + request.getParameter(TwilioParams.CALL_STATUS.getValue()) +"\n\tCurrent Time-"+new Date() + " for CallSID: "+ callSid);
			LOGGER.info( "DialCallStatus" + status + " for CallSID: "+ callSid);
			String sid = request.getParameter(TwilioParams.CALL_SID.getValue());
			String recordingURL = request.getParameter(TwilioParams.RECORDING_URL.getValue());
			String recordingDuration = request.getParameter(TwilioParams.RECORDING_DURATION.getValue());
			String callDuration = request.getParameter(TwilioParams.DIAL_CALL_DURATION.getValue());
			String accountSID = request.getParameter(TwilioParams.ACCOUNT_SID.getValue());
			String recordingSid = request.getParameter(TwilioParams.RECORDING_SID.getValue());
			
			int recordingDurationValue = 0;
			int callDurationValue = 0;
			recordingURL = getDecodedRecordingUrl(recordingURL);
			LOGGER.info("recording_sid={} call_duration={} recording_duration={} for callsid={}",recordingSid, callDuration, recordingDuration, sid);

			String parentSid  = voiceCallRepository.getParentSidFromChildCallSid(sid);
			if(parentSid!=null && !parentSid.isEmpty()) {
				LOGGER.info("parentSid={} for callsid={}", parentSid, sid);
				sid = parentSid;
			}
			try{
				if(callDuration!=null) {
					callDurationValue = Integer.parseInt(callDuration);
				}
				if(recordingDuration!=null) {
					recordingDurationValue = Integer.parseInt(recordingDuration);
				}
			}catch(Exception e1){
				LOGGER.warn("Error in handleOutboundCall: Not able to parse recording duration, setting 0, details are: RecordingURL={} AccountSID={} RecordingDuration={} CallSID={}", recordingURL, accountSID, recordingDuration, callSid, e1);
			}
			
			if(recordingURL!=null) {
				generalRepository.updateCallInfo(sid, recordingURL, recordingDurationValue, BLANK_TRANSCRIPTION);
        		callKcommunicationsToMigrateRecordingToS3(sid);
        		
				if (transcriptionEnabled(sid)) {

					scheduleTranscription(sid, recordingURL);
				}
			    else {	
			    	LOGGER.info("transcription not enabled for message with call_sid={} ", sid);	
	
			    }
            }
            
            if(recordingDuration==null) {
            	
            	output = updateCallStatus(accountSID, sid, output, status);

            }
			try {
				if(callDuration!=null) {
					addToMissedCall(callDurationValue, sid, accountSID);
				}
			} catch (Exception e) {
				LOGGER.error("Error in addToMissedCall for call_sid={} recordingDurationValue={} ", sid, recordingDurationValue);
			}

			
		} catch (Exception e) {
			LOGGER.error("Error in handleEndCall, details are: CallSID: {} RecordingURL: {} RecordingDuration: {} AccountSID: {} ", request.getParameter(TwilioParams.CALL_SID.getValue()), request.getParameter(TwilioParams.RECORDING_URL.getValue()), request.getParameter(TwilioParams.RECORDING_DURATION.getValue()), request.getParameter(TwilioParams.ACCOUNT_SID.getValue()), e);
			throw e;
		}

		return output;
	}

	@Override
	public void handleInboundBotSms(HttpServletRequest request) throws Exception {
		try {
			String fromNumber = request.getParameter(TwilioParams.FROM.getValue());
			String body = request.getParameter(TwilioParams.BODY.getValue());
			String toNumber = request.getParameter(TwilioParams.TO.getValue());
			forwardedAndBotMessageImpl.saveIncomingBotMessageFromTwilio(fromNumber, toNumber, body);
		} catch (Exception e) {
			LOGGER.error("error in handleInboundBotSms", e);
			throw e;
		}
	}


	private boolean transcriptionEnabled(String communicationUid) {
		
		try {
			
			Long dealerID = messageRepository.getMessageDealerIDFromCommunicationSID(communicationUid);
			String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
			String optionValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.TRANSCRIPTION_ENABLE.getOptionKey());
			if (optionValue != null && optionValue.equalsIgnoreCase("true")) {
				return true;
			} else {
				return false;
			}
			
		} catch (Exception e) {
		    LOGGER.warn("error_msg=\"{}\" while determining transcription.enable status for communicationUid={} . defaulting to disabled transcription", e.getMessage(), communicationUid);
		    return false;
		}
	}

	private void scheduleTranscription(String sid, String recordingURL) {
		// setting the recording URL in the transcription task
		TranscribeJobTask transcribeJobTask = getSpringProtypeScopedTranscribeJobTask();
		
		transcribeJobTask.setTwilioRecordingUrl(recordingURL);

		// setting the message communicationUid
		transcribeJobTask.setMessageCommunicationUid(sid);			

		// Asynchronously executing transcription job
		transcribeJobExecutor.execute(transcribeJobTask);
	}
	
	public TranscribeJobTask getSpringProtypeScopedTranscribeJobTask() {
		
        return (TranscribeJobTask) context.getBean(TranscribeJobTask.class);
	}


	private void callTwilioForRecordingCall(String dealerUUID, String accountSid, String baseURL, String callSid, String called, TwilioOpsEnum callType) throws Exception {
		
		if(called==null) {
			LOGGER.error("Error inside CallTwilioForRecordingCall: called = null");
			return;
		}
		
		List<VoiceCredentials> vc = null;
		
		String dsoForCountryCodeValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.COMMUNICATIONS_COUNTRYCODE_ROLLOUT.getOptionKey());
		if("true".equalsIgnoreCase(dsoForCountryCodeValue)) {
			vc = voiceCredentialRepository.getVoiceCredentialsForAccountSIDAndBroker(accountSid, called);
		} else {
			vc = voiceCredentialRepository.getVoiceCredentialsForAccountSIDAndBroker(accountSid, called.substring(called.length() - 10));
		}
		
		
		if(vc==null || vc.size()==0) {
			LOGGER.error("VoiceCredentials are NULL for accountSid = {}, callSid = {}", accountSid, callSid);
			return;
		}
		
		VoiceCredentials voiceCreds = vc.get(0);
		
		String[] cred = twilioGatewayServiceBOImpl.getSeparatedDetailsFromDealerSubaccount(voiceCreds.getDealerSubaccount());
		HttpHeaders headers = new HttpHeaders();
		String userPass = cred[0] + ":" + cred[1];
		
	    String authHeaderValue = "Basic " + Base64.getEncoder().encodeToString(userPass.getBytes());
	    headers.set(HttpHeaders.AUTHORIZATION, authHeaderValue);
	    
		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add("RecordingStatusCallback", baseURL + callType);
		
		HttpEntity<MultiValueMap<String, String>> headersMap = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		String requestURL =String.format(TWILIO_RECORDING_URL, accountSid, callSid);
		
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		try {
			LOGGER.info("hitting twilio to record incoming voice call for callSid={} account_sid={} request_url={}",callSid, accountSid, requestURL);
			ResponseEntity<String> response = restTemplate.postForEntity(String.format(TWILIO_RECORDING_URL, accountSid, callSid), headersMap , String.class );
			LOGGER.info("response object received from twilio for account_sid={} call_sid={} is {}", accountSid, callSid, new ObjectMapper().writeValueAsString(response));
		}
		catch(Exception e){
			LOGGER.error("cant make request to twilio for call_sid={} account_sid={}",callSid, accountSid, e);
		}
		
	} 

	private Boolean recordInitialAudio(String accountSID, String dealerUUID) {	

		Boolean isEnable = false;	

		try {	

			if (dealerUUID != null) {	
				if ("true".equalsIgnoreCase(kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.COMMUNICATIONS_RECORD_GREETING_ROLLOUT.getOptionKey()))) {	
					isEnable = true;	
				}	
			}	

			LOGGER.info( "value of recording initial audio={} for AccountSID={}", isEnable ,accountSID);	

		} catch (Exception ex) {			
			LOGGER.error("Error in recording initial audio, details are: AccountSID: {} ", accountSID, ex);
		}	

		return isEnable;
	}

	private void callKcommunicationsToMigrateRecordingToS3(String callSID) {
		
		String messageUUID = null;
		String departmentUUID =null;
		Long departmentID = null;
		LOGGER.info("calling kcommunications to update recording_url for call_sid={}",callSID);
		try {

			messageUUID = messageRepository.getMessageUUIDFromCommunicationUid(callSID);
			departmentID = messageRepository.getDepartmentIDFromCommunicationUid(callSID);
				
			if(messageUUID==null || departmentID==null){
				LOGGER.warn(CALLKCOMMUNICATIONSTOMIGRATERECORDINGTOS3 + " no messageUUID found for call_sid={}",callSID);
				return;
			}
			
			departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(departmentID);
			LOGGER.info(CALLKCOMMUNICATIONSTOMIGRATERECORDINGTOS3 +" message_uuid={} department_uuid={} for call_sid={}",messageUUID,departmentUUID,callSID);
			
			
			ResponseEntity<Response> response = communicationsApiImpl.updateURLForMessage(messageUUID);
			
			if(response==null) {
				LOGGER.warn("No response while updating URL for message for callSid={}", callSID);
				return;
			}else if(response.getBody()==null) {
				LOGGER.warn("No response body while updating URL for message for callSid={}", callSID);
				return;
			}else if(response.getBody().getErrors()!=null && !response.getBody().getErrors().isEmpty()) {
				LOGGER.error(" Error in updating recording_url for message_uuid={} department_uuid={} call_sid={} errors={}",messageUUID, departmentUUID, callSID,new ObjectMapper().writeValueAsBytes(response.getBody().getErrors()));
			}else {
				LOGGER.info(CALLKCOMMUNICATIONSTOMIGRATERECORDINGTOS3 + " Message successfully queued for updating recording_url for message_uuid={} department_uuid={} call_sid={}",messageUUID, departmentUUID, callSID);
			}
		}
		catch(Exception e) {
			LOGGER.error(CALLKCOMMUNICATIONSTOMIGRATERECORDINGTOS3 + " Error in calling kcommunications for sid={}", callSID, e);
		}
		
		
	}
	
	public void updateThreadInWaitingForResponseFilter(Long dealerID,Long dealerDepartmentID,Long dealerAssociateID,HashMap<Long,Long> customerIDAndThreadID,String actionSource){
		
		LOGGER.info("updateThreadInWFRFilter for dealer_associate_id={} dealer_id={}  action_source={}" ,dealerAssociateID , dealerID , actionSource);				
		
		FilterHistory filterHistory = new FilterHistory();
		filterHistory.setActionSource(actionSource);
		filterHistory.setCustomerIDAndThreadID(customerIDAndThreadID);

		if(dealerAssociateID != null) {			
		    String dealerAssociateUUID = "";
			filterHistory.setEventRaisedByUUID(dealerAssociateUUID);
		}
		
		filterHistory.setEventRaisedByUUID(null);
		filterHistory.setDealerDepartmentID(dealerDepartmentID);
		filterHistory.setDealerID(dealerID);
		
		String filterName = FilterName.UNRESPONDED.name();
		String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(dealerDepartmentID);
		try {
			//call kmessaging api
			kMessagingApiHelper.updateFilterTable(filterHistory, filterName, departmentUUID);
			
		} catch(Exception e) {
				LOGGER.error(" Error in updating thread from filter for dealer_associate_id={} dealer_id={} \n{} " ,dealerAssociateID , dealerID,e);
		}
 }

	


	public int getWhitelistCountForDealerDepartment(Long departmentID) {

		List<VoiceCredentials> credentials = voiceCredentialRepository.findAllByDeptID(departmentID);
	
		int count = 0;
		for(VoiceCredentials creds: credentials) {
			 if(creds.getUseForOutgoingCommunication() != null && creds.getUseForOutgoingCommunication()) {
				count++;
			} 
		}
		return count;
	}
	
	public String handlefetchConferenceTwimlForCallee(String confID) {
	
		LOGGER.info("fetchConferenceTwimlForCallee for conference_id= {}", confID);
		
		String twilioResponseObject = voiceCallHelper.getVoiceResponseForfetchConferenceTwimlForCallee(confID);
		return twilioResponseObject;
				
	}
	
	public String handleFetchCallRecordingWarningTwiml(String brokerNumber) {
		
		LOGGER.info("Inside fetchCallRecordingWarningTwiml brokerNumber = {}", brokerNumber);
		String greetingUrl = null;

		greetingUrl = voiceCallingRedisService.getGreetingURLForBrokerNumber(brokerNumber);
		String responseTwiml = "";
		if(greetingUrl==null || greetingUrl.isEmpty()) {
			responseTwiml = new VoiceResponse.Builder().build().toXml();
		} else {
			LOGGER.info("fetchCallRecordingWarningTwiml greeting= {}", greetingUrl);
			responseTwiml =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
						"<Response>" +
						greetingUrl +
						"</Response>";
			Play play = new Play.Builder(greetingUrl).build();
			responseTwiml = new VoiceResponse.Builder().play(play).build().toXml();
		}
		return responseTwiml;
	}
	
	private String getExtentionNumberNode(String phoneNumber) {

		String[] result = phoneNumber.split(":");
		if (result.length > 1) {
			return (" sendDigits=\"" + result[1] + "\">" + result[0]);
		} else {
			return (">" + result[0]);
		}
	}
	
	private String getDecodedRecordingUrl(String url) {
		
		try{

            url=URLDecoder.decode( url, StandardCharsets.UTF_8.name() );  

       } 
		catch(Exception e){
			LOGGER.error(String.format("unable to decode url=%s ", url) , e);
		}	
		
		return url;
	}
	
	
}

package com.mykaarma.kcommunications.Twilio.impl;

import java.math.BigInteger; 
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.Authority;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.kcommunications.Twilio.CallStatus;
import com.mykaarma.kcommunications.Twilio.impl.TwilioConstantsBOImpl.TwilioOpsEnum;
import com.mykaarma.kcommunications.controller.impl.PostIncomingMessageSaveService;
import com.mykaarma.kcommunications.controller.impl.RateControllerImpl;
import com.mykaarma.kcommunications.controller.impl.SaveMessageHelper;
import com.mykaarma.kcommunications.controller.impl.SendMessageHelper;
import com.mykaarma.kcommunications.jpa.repository.ForwardingBrokerNumberMappingHelper;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCallRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCredentialsRepository;
import com.mykaarma.kcommunications.model.jpa.ForwardingBrokerNumberMapping;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.VoiceCall;
import com.mykaarma.kcommunications.model.kre.KaarmaRoutingResponse;
import com.mykaarma.kcommunications.model.kre.RoutingRuleResponse;
import com.mykaarma.kcommunications.model.rabbit.PostIncomingMessageSave;
import com.mykaarma.kcommunications.utils.ConnectingNumbers;
import com.mykaarma.kcommunications.utils.ConvertToJpaEntity;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.OutOfOfficeHelper;
import com.mykaarma.kcommunications.utils.RulesEngineHelper;
import com.mykaarma.kcommunications.utils.VoiceCallHelper;
import com.mykaarma.kcommunications_model.enums.CommunicationsFeature;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.twilio.twiml.voice.Dial;

@Service
public class InboundCallProcessor {
	
	@Autowired
	VoiceCredentialsRepository voiceCredentialsRepository;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	VoiceCallRepository voiceCallRepo;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	RateControllerImpl rateController;
	
	@Autowired
	SendMessageHelper sendMessageHelper;
	
	@Autowired
	Helper helper;
	
	@Autowired
	SaveMessageHelper saveMessageHelper;
	
	@Autowired
	PostIncomingMessageSaveService postIncomingMessageSaveService;
	
	@Autowired
	OutOfOfficeHelper outOfOfficeHelper;
	
	@Autowired
	ForwardingBrokerNumberMappingHelper forwardingBrokerNumberMappingHelper;
	
	@Autowired
	RulesEngineHelper rulesEngineHelper;
	
	@Autowired
	VoiceCallHelper voiceCallHelper;
	
	@Autowired
	ConvertToJpaEntity convertToJpaEntity;

	@Value("${twilio_base_url}")
	String TWILIO_BASE_URL;

	private static final Logger LOGGER =  LoggerFactory.getLogger(InboundCallProcessor.class);
	private static final String VOICE_CALL = "Voice Call";
	private static final String INVALID_PHONE_NUMBER_URL = "https://app.mykaarma.com/audio/recordings/You_have_reached_an_invalid_phone_new.mp3";
	private static String INCOMING_MESSAGE_SUBJECT_VOICE = "You have received an call from: ";	
	
	public String processInboundCall(String caller, String callSid, String from, String accountSid) {
		
		String output = "";
		String responseObject = "";
		String connectingNumber = null;
		String connectingNumberNodes = "";
		
		//call to rules engine
		Long timeStart = System.currentTimeMillis();		
		KaarmaRoutingResponse routingResponse = rulesEngineHelper.getReponseFromRulesEngineForInboundCall(accountSid, caller, callSid);
		RoutingRuleResponse routingRuleResponse =  routingResponse.getRoutingRuleResponse();
		
		//For testing only
		String temp = "";
		try {
			temp = new ObjectMapper().writeValueAsString(routingResponse);
		}catch(Exception e) {
			LOGGER.error("Error while parsing routing response to string, callSid = {}", callSid, e);
		}
		LOGGER.info("RoutingResponse = {} for callSid = {}, accoutnSid = {}", callSid, accountSid, temp);
		
		Long timeEnd = System.currentTimeMillis();
		LOGGER.info("calling krules engine for call_sid={} time_taken={}", callSid, timeEnd-timeStart);
		
		Long departmentID = routingRuleResponse.getDealerDepartmentID();
		String department_UUID = generalRepository.getDepartmentUUIDForDepartmentID(departmentID);
		Long dealerAssociateID = routingResponse.getRoutingRuleResponse().getDealerAssociateID();
		String userUUID = generalRepository.getUserUUIDForDealerAssociateID(dealerAssociateID);

		if (routingRuleResponse.getDealerID() == null) {	
			
			responseObject = voiceCallHelper.getVoiceResponseForGreetings(INVALID_PHONE_NUMBER_URL);
			return responseObject;
			
		} else {
            try {
                Boolean isValid = generalRepository.checkIfDealerIsValid(routingRuleResponse.getDealerID());
                if(isValid == null || !isValid) {
                	LOGGER.warn("Invalid dealer for dealer_id={} feature={} communications_value={} call_sid={} ",routingRuleResponse.getDealerID(), CommunicationsFeature.INCOMING_CALL.name(), caller, callSid);
                	responseObject = voiceCallHelper.getVoiceResponseForGreetings(INVALID_PHONE_NUMBER_URL);
        			return responseObject;
                }
            } catch (Exception e) {
                LOGGER.error("Exception in validating dealer, details are: caller={} call_sid={} from={}",caller, callSid, from, e);
            }
        }
		
		boolean record = false;
		boolean transcribe = false;
		
		try {	
			ResponseEntity<Response> response = rateController.rateController(department_UUID, CommunicationsFeature.INCOMING_CALL, caller);
			if(response.getBody().getErrors()!=null && !response.getBody().getErrors().isEmpty()) {
				LOGGER.warn("postUsage rate limit reached for dealer_id={} feature={} communications_value={} call_sid={}",
						routingRuleResponse.getDealerID(), CommunicationsFeature.INCOMING_CALL.name(), caller, callSid);
				output = voiceCallHelper.getVoiceResponseForGreetings(INVALID_PHONE_NUMBER_URL);
				return output;
			}
		} catch (Exception e) {
			LOGGER.error("Exception in posting usage, details are: caller={} call_sid={} from={} ",
					caller,callSid,from, e);
		}
		
		try {
			
			Set<String> listKeys = new HashSet<String>();
			String dsoforCallRecording=null ; 
			listKeys.add(DealerSetupOption.INBOUND_CALL_AUTO_REPLY.getOptionKey());
			listKeys.add(DealerSetupOption.OUT_OF_OFFICE_CALL_MASKING_ENABLE.getOptionKey());

			String dealerUUID = generalRepository.getDealerUUIDFromDealerId(routingRuleResponse.getDealerID());
			HashMap<String, String> mapDSO = kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(dealerUUID, listKeys);
			
			Boolean hasRecordAuth = kManageApiHelper.checkDealerAssociateAuthority(Authority.VOICE_RECORD.getAuthority(), userUUID, department_UUID);
			
			if((hasRecordAuth)){

				dsoforCallRecording=kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.MESSAGING_CALL_RECORD_ENABLE.getOptionKey());
				if(("true").equalsIgnoreCase(dsoforCallRecording))	
					record = true;	
				LOGGER.info("dealer_id={} dealer_associate_id={} call_recording_dso={} ",routingRuleResponse.getDealerID(),routingRuleResponse.getDealerAssociateID(),dsoforCallRecording);	
			}else {
				LOGGER.info("voice.record authority not enabled for dealer_id={} dealer_associate_id={} call_recording_dso={} call_recording_authority=false ",routingRuleResponse.getDealerID(),routingRuleResponse.getDealerAssociateID(),dsoforCallRecording);
			}
		
			LOGGER.info("dealer_id={} department_id={} callsid={} callrecord={} calltranscribe={}" , 
					routingRuleResponse.getDealerID(),routingRuleResponse.getDealerDepartmentID(), callSid, String.valueOf(record), String.valueOf(transcribe));
			
			Long customerID = routingRuleResponse.getCustomerID();
			
			//Create new customer if no customer is found
			
			if(customerID==null) {			
				customerID = helper.createNewCustomerForUnknownNumber(caller, departmentID);		
				routingRuleResponse.setCustomerID(customerID);			
			}
			
			Message callmessage = convertToJpaEntity.prepareIncomingMessageObject(customerID, routingRuleResponse.getDealerID(), routingRuleResponse.getDealerAssociateID(), 
					INCOMING_MESSAGE_SUBJECT_VOICE, VOICE_CALL, caller , MessageProtocol.VOICE_CALL.getMessageProtocol() , callSid, 0, false, null , routingRuleResponse.getDealerDepartmentID());
			callmessage.setEmailMessageId(null);
		
			try {
				callmessage = saveMessageHelper.saveMessage(callmessage);
				LOGGER.info("Message Saved, messageID = {} , messageUUID = {} callSid={}", callmessage.getId() , callmessage.getUuid(), callSid);
			}catch(Exception e) {
				LOGGER.info("Error while saving message, messageID = {} callSid:{}", callmessage.getId(), callSid, e);
			}
			
			PostIncomingMessageSave postIncomingMessageSave = helper.createPostIncomingMessageSaveObject(callmessage, dealerAssociateID);			
			postIncomingMessageSaveService.postIncomingMessageSaveProcessing(postIncomingMessageSave);				
		
			if(callmessage!=null){
				
				if(routingResponse.getOutOfOfficeResponse().getDelegateConversation())
				{
						outOfOfficeHelper.sendDelegationRequest(routingResponse);
				}
			}
						
			//populate connecting numbers obtained from rules engine.
			ConnectingNumbers cnums = new ConnectingNumbers();
			
			List<String> daIDs = new ArrayList<String>();
			if(routingRuleResponse.getConnectingDAsList() != null)
			{	
				for(Long daID: routingRuleResponse.getConnectingDAsList()) {
					daIDs.add(String.valueOf(daID));
				}
			}	
						
			cnums.setConnectingClients(daIDs);
			cnums.setConnectingNumbers(routingRuleResponse.getConnectingNumbersList());

			String brokerNumber = "";
			Boolean oooCallMaskingEnabled = false;
			
			if(routingResponse.getOutOfOfficeResponse() != null && routingResponse.getOutOfOfficeResponse().getForwardCall() && "true".equalsIgnoreCase(mapDSO.get(DealerSetupOption.OUT_OF_OFFICE_CALL_MASKING_ENABLE.getOptionKey()))) {
				brokerNumber = getForwardingBrokerNumber(routingResponse, caller, department_UUID, userUUID);
				oooCallMaskingEnabled = true;
				
				LOGGER.info("DA is OutOfOffice, call is forwarded to  {} callSid={}" ,brokerNumber, callSid);
				
			} else {				
				brokerNumber = voiceCredentialsRepository.getBrokerNumberForCaller(accountSid);
			}
		
			String to_number = "";
			String sendDigits= "";
			
			List<String> list_of_numbers = voiceCallHelper.getConnectingNumbersAndToNumber(cnums, from, caller);
			
			if(list_of_numbers!=null && list_of_numbers.size()>=5) {
				connectingNumberNodes = list_of_numbers.get(0);
				connectingNumber = list_of_numbers.get(1);
				to_number = list_of_numbers.get(2);
				sendDigits = list_of_numbers.get(3);
				from = list_of_numbers.get(4);
			}
	
			if(routingResponse.getOutOfOfficeResponse() != null && routingResponse.getOutOfOfficeResponse().getDaAutoReplyCall()) {
//				"twilio.dealerassociate.call.autoreply.xml";
				responseObject = voiceCallHelper.getVoiceResponseForDAAutoReplyCall(TWILIO_BASE_URL, routingResponse.getOutOfOfficeResponse().getAutoReplySenderDAIDList().get(0),transcribe);
		
			} else if(routingResponse.getOutOfOfficeResponse() != null && routingResponse.getOutOfOfficeResponse().getDealerCallAutoReply()) {
//				"twilio.dealer.call.autoreply.xml";
				responseObject = voiceCallHelper.getVoiceResponseForDealerAutoReplyCall(TWILIO_BASE_URL, transcribe, mapDSO.get(DealerSetupOption.INBOUND_CALL_AUTO_REPLY.getOptionKey()));
			
			} else if(routingResponse.getOutOfOfficeResponse() != null && routingResponse.getOutOfOfficeResponse().getForwardCall() && oooCallMaskingEnabled) {
			
				responseObject = handleCall(TWILIO_BASE_URL, connectingNumberNodes, to_number, sendDigits, brokerNumber, record, transcribe, routingRuleResponse.getDealerID());
				
			} else {
				
				responseObject = handleCall(TWILIO_BASE_URL, connectingNumberNodes, to_number, sendDigits, from, record, transcribe, routingRuleResponse.getDealerID());
			
			}

			try {
				createVoiceCall(callSid, caller, connectingNumber, brokerNumber,routingResponse,transcribe,record);
			} catch (Exception e) {
				LOGGER.error("Error in voice call, The Voicecall details are: CallSID: {} Caller: {} ConnectingNumber:{} BrokerNumber:{} ", callSid, caller,connectingNumber,brokerNumber, e);			
			}
		} catch (Exception ex) {
			LOGGER.error("Error In processInboundCall ", ex);
		}
		
		return responseObject;
	}

	
	private String handleCall(String baseUrl, String connectingNumberNodes, String to_Number, String sendDigits, String from, Boolean record, Boolean transcribe, Long dealerID)
	{

		String responseObject = "";
		String callBackURL = "";
		String dealerUuid = null;
		
		dealerUuid = generalRepository.getDealerUUIDFromDealerId(dealerID);

		String recordingInitials = "";	
		
		try {	
			recordingInitials = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUuid, DealerSetupOption.COMMUNICATIONS_RECORD_GREETING_ROLLOUT.getOptionKey());	
		} catch (Exception e) {	

			LOGGER.error("unable to fetch record greeting dsoo from kmanage for dealer_id={}",dealerID,e);	
			recordingInitials = "false";	
		}
				
		if("true".equalsIgnoreCase(recordingInitials)){
			callBackURL = baseUrl + TwilioOpsEnum.InboundEndCall;
			record = false;
		}
		else {
			callBackURL =  baseUrl + TwilioOpsEnum.EndCall;
		}
							
		Dial.Record recordValue = Dial.Record.DO_NOT_RECORD;
		
		if(record) {
			recordValue = Dial.Record.RECORD_FROM_RINGING;
		}
		
//		twilio.inbound.callToSA.xml	
		responseObject = voiceCallHelper.getVoiceResponseForInboundCallToSA(from, to_Number, sendDigits, callBackURL, recordValue);
				
		return responseObject;
	
	}		
	
	private VoiceCall createVoiceCall(String callSid, String caller,
			String calledTo, String brokerNumber, KaarmaRoutingResponse kaarmaRoutingResponse, Boolean transcribe
			, Boolean record) {
		
		VoiceCall voiceCall = null;
		
		try {			
			
			voiceCall = voiceCallHelper.getVoiceCall(callSid, caller, calledTo, "", "You are now being connected to Service Advisor", "You are being connected to Customer", brokerNumber, record, 'T', false, (long)CallStatus.party1_connected.getID(), false);
		
			//Party 2 prompt changes if it is an auto reply due to OutOfOffice
			//Also party2 number would be blank in auto reply case since no party2 is called in this case
			//Also record is cumpolsary for such auto reply cases
			if(kaarmaRoutingResponse != null && kaarmaRoutingResponse.getOutOfOfficeResponse() != null
					&& (kaarmaRoutingResponse.getOutOfOfficeResponse().getDaAutoReplyCall()
							||kaarmaRoutingResponse.getOutOfOfficeResponse().getDealerCallAutoReply()))
			{
				voiceCall.setParty2Prompt("");
				voiceCall.setRecordCall(true);
				voiceCall.setParty2("");
			}
		
			try {				
				voiceCallRepo.save(voiceCall);
				
			} catch (Exception e1) {
				LOGGER.warn("Error in saving voice call in createVoiceCall method, details are: ID: {} Party1: {} Party2: {}  CallingParty: {} " , voiceCall.getId(), voiceCall.getParty1(), voiceCall.getParty2() ,  voiceCall.getCallingParty(), e1);
			}

		} catch (Exception e) {
			LOGGER.warn("Exception in saving voice call in createVoiceCall method, for callSid = {}", callSid, e);
			throw e;
		} 
		
		return voiceCall;
	}
		
	private String getForwardingBrokerNumber(KaarmaRoutingResponse response, String customerCommunication, String department_UUID, String userUUID) {
		
		GetDealerAssociateResponseDTO dealerAssociate = kManageApiHelper.getDealerAssociate(department_UUID, userUUID);
		
		LOGGER.info("handleOutOfOfficeForwardCall for dealer_id={} dealer_associate_id={} customer_id={} customer_communication={} call_forwarding_number={} ",
				response.getRoutingRuleResponse().getDealerID(), dealerAssociate.getDealerAssociate().getId(), dealerAssociate.getDealerAssociate().getTextNumber(), response.getRoutingRuleResponse().getCustomerID(), customerCommunication, response.getOutOfOfficeResponse().getCallForwardingNumbers().get(0));
		
		ForwardingBrokerNumberMapping fn = outOfOfficeHelper
				.getForwardingBrokerNumberMapping(response.getRoutingRuleResponse().getDealerID(), dealerAssociate.getDealerAssociate().getId(), dealerAssociate.getDealerAssociate().getTextNumber(),
						response.getRoutingRuleResponse().getCustomerID() , customerCommunication, getPhoneNumberWithoutCountryCode(response.getOutOfOfficeResponse().getCallForwardingNumbers().get(0)));
		String brokerNumber = fn.getBrokerNumber();

		fn.setLastMessageOn(new Date());
		
		forwardingBrokerNumberMappingHelper.saveAndFlush(fn);
		LOGGER.info("ForwardingBrokerNumberMapping table updated for dealerID = {}", response.getRoutingRuleResponse().getDealerID());
		return brokerNumber;

	}

	private String getPhoneNumberWithoutCountryCode(String phoneNumber)
	{
		String output = null;
		if(phoneNumber == null)
			return output;
		phoneNumber =phoneNumber.trim();
		if(phoneNumber.length() <= 10)
			return phoneNumber;
		int length =  phoneNumber.length();
		
		output = phoneNumber.substring(length-10,length);
		return output;
		
	}
	
}
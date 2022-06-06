package com.mykaarma.kcommunications.controller.impl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


import com.google.common.base.Joiner;
import com.mykaarma.global.Authority;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.kcommunications.Twilio.impl.TwilioConstantsBOImpl;
import com.mykaarma.kcommunications.Twilio.impl.TwilioGatewayServiceBOImpl;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCredentialsRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.rabbit.PostMessageSent;
import com.mykaarma.kcommunications.utils.ConvertToJpaEntity;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.KMessagingApiHelper;
import com.mykaarma.kcommunications.utils.OutboundCallResponse;
import com.mykaarma.kcommunications.utils.OutboundMessageDetail;
import com.mykaarma.kcommunications.utils.RulesEngineHelper;
import com.mykaarma.kcommunications_model.common.VoiceCallRequest;
import com.mykaarma.kcommunications_model.enums.ErrorCode;

import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcommunications_model.response.VoiceCallResponse;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.kmanage.model.dto.json.AuthorityDTO;


@Service
public class VoiceCallingControllerImpl {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(VoiceCallingControllerImpl.class);	
	private static final String DEFAULTOUTBOUNDCALLRECORDINGURL= "https://www.kaar-ma.com/audio/recordings/Your_Service_Advisor_is_Calling_new.mp3";
	private static final String DEFAULT_SUPPORTCALL_GREETING = "audio/press1_support.wav";

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	TwilioGatewayServiceBOImpl twilioGatewayServiceBOImpl;
	
	@Autowired
	GeneralRepository generalRepo;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	MessageRepository messageRepository;
	
	@Autowired
	KMessagingApiHelper kMessagingApiHelper;
	
	@Autowired
	VoiceCredentialsRepository voiceCredentialsRepository;
	
	@Autowired
	PostMessageSendingHelper postMessageSendingHelper;
	
	@Autowired
	SaveMessageHelper saveMessageHelper;
	
	@Autowired
	Helper helper;
	
	@Autowired
	ValidateRequest validateRequest;
	
	@Autowired
	MessagePropertyImpl messagePropertyImpl;
	
	@Autowired
	RulesEngineHelper rulesEngineHelper;
	
	@Autowired
	ConvertToJpaEntity convertToJpaEntity;
	
	@Value("${krulesengine.url}")
	String URL;
	
	@Value("${base_url}")
	String BASE_URL;
	
	@Value("${kmessaging_api_url}")
	String KMESSAGING_API_URL;

	
	public ResponseEntity<VoiceCallResponse> callContact(String departmentUUID, String customerUUID, VoiceCallRequest voiceCallRequest) throws Exception{
				
		String userUUID = voiceCallRequest.getUserUUID();
		String party1Number = voiceCallRequest.getParty1Number();
		String party2Number = voiceCallRequest.getParty2Number();
		boolean isSupportCall = voiceCallRequest.isSupportCall();
		String twilioCallBackPath = voiceCallRequest.getTwilioCallBackPath();
		
		LOGGER.info("Inside callContact method, party1Number = {}, party2Number = {}, userUUID = {}, isSupportCall = {} ", party1Number, party2Number, userUUID, isSupportCall);

		String return_value = "";
		VoiceCallResponse voiceCallResponse = validateRequest.validateVoiceCallRequest(departmentUUID, customerUUID, voiceCallRequest) ;
		
		List<ApiError> errors = voiceCallResponse.getErrors();
		List<ApiWarning> warnings = voiceCallResponse.getWarnings();
		
		GetDealerAssociateResponseDTO dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUUID, userUUID);
		Long dealerAssociateID = dealerAssociate.getDealerAssociate().getId();
		String dealerAssociateUUID = dealerAssociate.getDealerAssociate().getUuid();
		
		Long dealerID = generalRepo.getDealerIDFromDepartmentUUID(departmentUUID);				
		String dealerUUID = generalRepo.getDealerUUIDFromDealerId(dealerID);		
		Long departmentID = generalRepo.getDepartmentIDForUUID(departmentUUID);
				
		LOGGER.info("dealerID = {}, dealerUUID = {}, departmentID = {}",dealerID, dealerUUID, departmentID);

		if (dealerAssociateID == null) { 
			
			ApiWarning apiWarning = new ApiWarning(ErrorCode.INVALID_USER.name(), "DealerAssociateID is null");
			warnings.add(apiWarning);
			voiceCallResponse.setStatus(Status.FAILURE);
			voiceCallResponse.setWarnings(warnings);
			return new ResponseEntity<VoiceCallResponse>(voiceCallResponse, HttpStatus.INTERNAL_SERVER_ERROR);

		}
		
		// AUTHORITY AND DSO CHECK 

		String dsoforCallRecording=null;
		Boolean canRecord = false;
		Boolean isGreetingEnabled = true;
		Boolean hasAnnouncementGreeting = false;
		Boolean hasVoiceCallAuthority = false;
		
		List<String> authorityList = getAuthoritiesForCallContact();
		List<String> daUuids = new ArrayList<>();
		daUuids.add(dealerAssociateUUID);
		List<String> deptUuids = new ArrayList<>();
		deptUuids.add(departmentUUID);
		
		Map<String, Set<AuthorityDTO>> authMap = kManageApiHelper.sortInputAndGetDealerAssociatesAuthoritiesDTO(daUuids, deptUuids, authorityList);
		if(authMap != null) {
			Set<AuthorityDTO> authorities = authMap.get(dealerAssociateUUID);
			if(authorities != null) {
				Iterator<AuthorityDTO> itr = authorities.iterator();
				while(itr.hasNext()) {
					String auth = itr.next().getAuthority();
					if(Authority.VOICE_RECORD.getAuthority().equalsIgnoreCase(auth)) {
						dsoforCallRecording = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.MESSAGING_CALL_RECORD_ENABLE.getOptionKey());	
						if(("true").equalsIgnoreCase(dsoforCallRecording)) {	
							canRecord = true;	
						}	
					} else if(Authority.COMMUNICATIONS_OUTBOUND_CALL_GREETING_DISABLE.getAuthority().equalsIgnoreCase(auth)) {
						isGreetingEnabled = false;
					} else if(Authority.VOICE_ANNOUNCEMENT_GREETING.getAuthority().equalsIgnoreCase(auth)) {
						hasAnnouncementGreeting = true;
					} else if(Authority.VOICE_CALL.getAuthority().equalsIgnoreCase(auth)) {
						hasVoiceCallAuthority = true;
					}
				}
			}
		}
		
		if(isSupportCall) {
			return callMe(dealerUUID, departmentUUID, dealerAssociateUUID, dealerID, departmentID, dealerAssociateID, canRecord, party1Number);
		}
		
		if(!isGreetingEnabled) {
			canRecord = false;
		}
		
		if(!hasVoiceCallAuthority) {
			
			LOGGER.warn("dealer_associate_id = {} User does not have voice.call authority enabled", dealerAssociateID);
			
			ApiWarning apiWarning = new ApiWarning(ErrorCode.MISSING_VOICECALL_AUTHORITY.name(), "User does not have voice.call authority enabled");
			warnings.add(apiWarning);
			voiceCallResponse.setStatus(Status.FAILURE);
			voiceCallResponse.setWarnings(warnings);
			return new ResponseEntity<VoiceCallResponse>(voiceCallResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if(hasAnnouncementGreeting) {
			String dsoValueForAnnouncementGreeting = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.VOICE_ANNOUNCEMENT_GREETING.getOptionKey());
			if(!"true".equalsIgnoreCase(dsoValueForAnnouncementGreeting)) {
				hasAnnouncementGreeting = false;
			}
		}
		
		Long customerID = generalRepo.getCustomerIDForUUID(customerUUID);

		try {
			
			String voiceOutboundTw2SaPrompt = "";
			String voiceOutboundTw2CustPrompt = "";

			String outgoingCallGreetingUrl = DEFAULTOUTBOUNDCALLRECORDINGURL;
			if(isGreetingEnabled) {
		
				OutboundCallResponse outboundCallResponse = rulesEngineHelper.getOutboundCallResponseFromRulesEngine(dealerAssociateID, dealerID, departmentID);
				
				if(outboundCallResponse!=null && outboundCallResponse.getCallRecordingURL()!=null) {
					outgoingCallGreetingUrl = outboundCallResponse.getCallRecordingURL();
				}
				
				int outgoingCallRecordingDuration = outboundCallResponse.getCallRecordingDuration();
				LOGGER.info("TwilioCallService: dealer_id={} department_id={} dealer_associate_id={} OutboundCallRecording={} OutboundCallRecordingDuration={}",
						dealerID, departmentID, dealerAssociateID, outgoingCallGreetingUrl, outgoingCallRecordingDuration);
				voiceOutboundTw2SaPrompt = preparePrompts(dealerAssociate, getPhoneNoWithSpaces(party2Number), TwilioConstantsBOImpl.OutboundTw2SaOptionKey);
				voiceOutboundTw2CustPrompt = preparePromptForPromptValue(dealerAssociate, getPhoneNoWithSpaces(party2Number), outgoingCallGreetingUrl );
				
				LOGGER.info("Party1 prompt = {}, Party2 prompt = {}", voiceOutboundTw2SaPrompt, voiceOutboundTw2CustPrompt);
				
			} else {
				voiceOutboundTw2SaPrompt = preparePrompts(dealerAssociate, getPhoneNoWithSpaces(party2Number), TwilioConstantsBOImpl.OutboundTw2SaOptionKey);
				LOGGER.info("greeting is disabled for dealer_associate_id={} dealer_id={} ",
						dealerAssociateID, dealerID);
				outgoingCallGreetingUrl = null;
			}
			
			LOGGER.info("TwilioResponder: dealer_id= {}, department_id = {}, dealer_associate_id= {}, voiceOutboundTw2CustPrompt={}", dealerID, departmentID, dealerAssociateID, voiceOutboundTw2CustPrompt);
			
			OutboundMessageDetail outboundMessageDetail = twilioGatewayServiceBOImpl.placeCall(departmentUUID, dealerAssociateUUID, dealerID, departmentID, party1Number, // party 1
					voiceOutboundTw2SaPrompt,    // party1prompt
					party2Number,                // party 2
					voiceOutboundTw2CustPrompt,  // party 2 prompt
					null,
					canRecord, isSupportCall, "SATC", customerID, hasAnnouncementGreeting); 
			
			
			if (outboundMessageDetail!=null && outboundMessageDetail.getSid() != null && !outboundMessageDetail.getSid().trim().isEmpty()) {
				
				Message message = convertToJpaEntity.createVoiceCallMessage(dealerAssociateUUID, dealerID,dealerAssociateID, customerID, outboundMessageDetail.getSid(), party2Number, true, outboundMessageDetail.getBrokerNumber(), "", departmentID);
				try {
					message = saveMessageHelper.saveMessage(message);
					LOGGER.info("Message Saved with messageID = {}, messageUUID = {}",message.getId(), message.getUuid());
				}catch(Exception e) {
					LOGGER.info("Error while saving message for messageID = {}, messageUUID = {}", message.getId(), message.getUuid(), e);
				}
				
				HashMap<String, String> dsoValues = kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(dealerUUID, getDSOListForPostMessageSending());
				Boolean postMessageProcessingToBeDone = false;
				if(dsoValues!=null && dsoValues.get(com.mykaarma.global.DealerSetupOption.COMMUNICATION_POST_MESSAGE_SENT.getOptionKey())!=null 
						&& "true".equalsIgnoreCase(dsoValues.get(com.mykaarma.global.DealerSetupOption.COMMUNICATION_POST_MESSAGE_SENT.getOptionKey()))) {
					postMessageProcessingToBeDone = true;
				}
				
				PostMessageSent postMessageSent = new PostMessageSent();
				postMessageSent.setMessage(message);
				postMessageSent.setThreadDelegatee(dealerAssociateID);
				postMessageSent.setPostMessageProcessingToBeDone(postMessageProcessingToBeDone);
				postMessageSent.setExpiration(null);
				postMessageSent.setUpdateThreadTimestamp(true);
				postMessageSent.setIsFailedMessage(false);
				
				postMessageSendingHelper.postMessageSendingHelper(postMessageSent);
				
				return_value =  outboundMessageDetail.getSid();
			}else {
				return_value = null;
			}
			
		} catch (Exception e) {
			LOGGER.error("Error inside CallContact method ",e);
			return_value = null;
		}
		
		if(return_value == null) {
			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), "Unable to initiate call");
			errors.add(apiError);
		}
		
		voiceCallResponse.setSid(return_value);
		
		voiceCallResponse.setErrors(errors);
		voiceCallResponse.setWarnings(warnings);
		
		if(return_value!=null) {
			voiceCallResponse.setStatus(Status.SUCCESS);
			return new ResponseEntity<VoiceCallResponse>(voiceCallResponse, HttpStatus.OK);
		}else {
			
			voiceCallResponse.setStatus(Status.FAILURE);
			return new ResponseEntity<VoiceCallResponse>(voiceCallResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}

public ResponseEntity<VoiceCallResponse> callMe(String dealerUUID, String departmentUUID, String dealerAssociateUUID, Long dealerID, Long departmentID, Long dealerAssociateID, Boolean canRecord, String telephoneNumber) throws Exception {
		
		LOGGER.info("SUPPORT CALL\n Inside CallMe: dealerAssociateUUID = {}, telephoneNumber = {}", dealerAssociateUUID, telephoneNumber);
	
		VoiceCallResponse voiceCallResponse = new VoiceCallResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();

		voiceCallResponse.setErrors(errors);
		voiceCallResponse.setWarnings(warnings);
		
		boolean isSupportAuthority = false;
		boolean isFallback = false;

		OutboundMessageDetail outboundMessageDetail = null;
		try {
			
			String voiceInboundTw2KaPrompt = " ";		
			
			Boolean record = canRecord;
			if (dealerAssociateID != null && dealerID != null) {
				
				LOGGER.info("using Dealership account dealerAssociateID = {}", dealerAssociateID);
						
				String supportNumber =  kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.SUPPORT_CALL.getOptionKey());
							
				if (supportNumber == null) {
							
					isSupportAuthority = false;
					LOGGER.info("Service Advisor does not have authority to make support call, dealerUUID = {}, departmentUUID = {}", dealerUUID, departmentUUID);
					
				} else {
					isSupportAuthority = true;
				}
				
				String callFallback = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.VOICECALL_FALLBACK.getOptionKey());

				if(callFallback!=null && !callFallback.isEmpty()){
					
						isFallback = true;
				}
				if (isSupportAuthority == true) {
					
					String base_url = BASE_URL;
					outboundMessageDetail = twilioGatewayServiceBOImpl.placeCall(departmentUUID,dealerAssociateUUID,
									dealerID, departmentID,
									telephoneNumber, // party1
									base_url + DEFAULT_SUPPORTCALL_GREETING, // party 1 prompt
									supportNumber, // party2
									voiceInboundTw2KaPrompt, // party2 prompt
									null,
									record, isFallback,
									null, null, false);
				}
			}
			

			voiceCallResponse.setSid(outboundMessageDetail.getSid());

			if(isSupportAuthority) {
				
				voiceCallResponse.setStatus(Status.SUCCESS);				
				return new ResponseEntity<VoiceCallResponse>(voiceCallResponse, HttpStatus.OK);

			}else {
				
				ApiWarning apiWarning = new ApiWarning(ErrorCode.MISSING_SUPPORTCALL_DSO.name(), "User does not have SupportCall enabled");
				warnings.add(apiWarning);
				voiceCallResponse.setWarnings(warnings);
				voiceCallResponse.setStatus(Status.FAILURE);
				return new ResponseEntity<VoiceCallResponse>(voiceCallResponse, HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
			
			
		} catch (Exception ex) {
			LOGGER.error((outboundMessageDetail!=null&&outboundMessageDetail.getFailureReason()!=null?outboundMessageDetail.getFailureReason().name():""), ex);
			
			voiceCallResponse.setStatus(Status.FAILURE);
			return new ResponseEntity<VoiceCallResponse>(voiceCallResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	public ResponseEntity<Response> cancelCall(String departmentUUID, String callSID){
	
		Long dealerID = generalRepo.getDealerIDFromDepartmentUUID(departmentUUID);
		Long departmentID = generalRepo.getDepartmentIDForUUID(departmentUUID);
		Response response = validateRequest.validateCancelCallRequest(dealerID, callSID, departmentID);
		
		List<String> listCreds = voiceCredentialsRepository.getTwilioCredentialsForDealerDept(dealerID, departmentID);

		List<String> cred = new ArrayList<>();
		if(listCreds!=null && listCreds.size()>0 && listCreds.get(0)!=null) {
			cred = Arrays.asList(listCreds.get(0).split("~"));
		}
		
		try {
			twilioGatewayServiceBOImpl.cancelCall(cred.get(0), cred.get(1), callSID);
			LOGGER.info("Cancelled call for callSid = {}", callSID);
		}catch (Exception e) {
			LOGGER.error("Error while cancelling call for callSid = {}", callSID);
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
	
	private String preparePrompts(GetDealerAssociateResponseDTO da, String customerPhone, String promptName)
			throws Exception {
		
		String promptValue = "";
		
		String dealerUUID = da.getDealerAssociate().getDepartmentExtendedDTO().getDealerMinimalDTO().getUuid();
		promptValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID,promptName);
		
		if(promptValue == null)
			promptValue = "";
		
		String dealerName = da.getDealerAssociate().getDepartmentExtendedDTO().getDealerMinimalDTO().getName();
		String saName = da.getDealerAssociate().getFirstName();
		
		promptValue = promptValue
				.replaceAll("_customerPhone", customerPhone)
				.replaceAll("_dealerName", dealerName)
				.replaceAll("_saName", saName)
				.replaceAll("<Play>", "")
				.replaceAll("</Play>", "");
		
		return promptValue;
	}
	
	private String preparePromptForPromptValue(GetDealerAssociateResponseDTO da, String customerPhone, String promptValue)
			throws Exception {
				
		if(promptValue == null)
			promptValue = "";
		
		String dealerName = da.getDealerAssociate().getDepartmentExtendedDTO().getDealerMinimalDTO().getName();
		String saName = da.getDealerAssociate().getFirstName();
		
		promptValue = promptValue
				.replaceAll("_customerPhone", customerPhone)
				.replaceAll("_dealerName", dealerName)
				.replaceAll("_saName", saName)
				.replaceAll("<Play>", "")
				.replaceAll("</Play>", "");

		return promptValue;
		
	}
	
	public static String getPhoneNoWithSpaces(String phone) throws Exception
	{
		return Joiner.on(" ").join(phone.replaceAll("[^0-9]", "").split(""));
	}

	public static List<String> getAuthoritiesForCallContact() {
		List<String> authorities = new ArrayList<>();
		authorities.add(Authority.VOICE_RECORD.getAuthority());
		authorities.add(Authority.COMMUNICATIONS_OUTBOUND_CALL_GREETING_DISABLE.getAuthority());
		authorities.add(Authority.VOICE_ANNOUNCEMENT_GREETING.getAuthority());
		authorities.add(Authority.VOICE_CALL.getAuthority());
		return authorities;
	}
	
	
	private Set<String> getDSOListForPostMessageSending() {
		Set<String> dsoList = new HashSet<String>();
		dsoList.add(com.mykaarma.global.DealerSetupOption.COMMUNICATION_POST_MESSAGE_SENT.getOptionKey());
		dsoList.add(com.mykaarma.global.DealerSetupOption.COMMUNICATION_POST_MESSAGE_SENT_AUTOMATIC.getOptionKey());
		return dsoList;
	}
}

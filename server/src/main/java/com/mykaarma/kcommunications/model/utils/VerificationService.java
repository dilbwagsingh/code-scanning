package com.mykaarma.kcommunications.model.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.controller.impl.SaveMessageHelper;
import com.mykaarma.kcommunications.controller.impl.SendMessageHelper;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCredentialsRepository;
import com.mykaarma.kcommunications.model.api.FailedMessagesRequest;
import com.mykaarma.kcommunications.model.jpa.VoiceCredentials;
import com.mykaarma.kcommunications.utils.TwilioClientUtil;
import com.mykaarma.kcommunications_model.enums.VerificationFailureReason;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Call;

import antlr.StringUtils;

@Service
public class VerificationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationService.class);
	
	public static final String CALLING_VERIFICAITION_PAREMETER = "Calling";
	public static final String TEXTING_VERIFICAITION_PAREMETER = "Texting";
	
	@Autowired
	TwilioClientUtil twilioClientUtil;
	
	@Autowired
	VoiceCredentialsRepository voiceCredentialsRepository;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	SendMessageHelper sendMessageHelper;
	
	public void verifyCommunicationsBillingTwilio(String messagePayLoad) throws Exception{
		
		ObjectMapper objectMapper = new ObjectMapper();
		LocalDateTime startDate = null;
		LocalDateTime endDate = null;
		Boolean registerFailedMessage = false;
		CommunicationsVerification communicationsVerification = objectMapper.readValue(messagePayLoad, CommunicationsVerification.class); 
		try {
			LOGGER.info("communicationsVerification request received for dealer_departmet_id={} start_date={} end_date={} verification_type={}", communicationsVerification.getDepartmentId(),
					communicationsVerification.getStartDate(), communicationsVerification.getEndDate(), communicationsVerification.getVerificationType());
			startDate = communicationsVerification.getStartDate().toInstant()
					.atZone(ZoneId.of("America/Los_Angeles")).toLocalDateTime();
			endDate = communicationsVerification.getEndDate().toInstant()
					.atZone(ZoneId.of("America/Los_Angeles")).toLocalDateTime();
			registerFailedMessage = communicationsVerification.getRegisterFailedMessage();
			
			if(CALLING_VERIFICAITION_PAREMETER.equalsIgnoreCase(communicationsVerification.getVerificationType())){
				verifyCallsInGivenTimeFrame(communicationsVerification.getDepartmentId(), startDate, endDate);
			}
			else if(TEXTING_VERIFICAITION_PAREMETER.equalsIgnoreCase(communicationsVerification.getVerificationType())){
				verifyTextsInGivenTimeFrame(communicationsVerification.getDepartmentId(), startDate, endDate, registerFailedMessage);			
			}
		}
		catch(Exception e) {
			LOGGER.error("communicationsVerification request failed for dealer_departmet_id={} start_date={} end_date={} verification_type={}", communicationsVerification.getDepartmentId(),
										communicationsVerification.getStartDate(), communicationsVerification.getEndDate(), communicationsVerification.getVerificationType());
			throw e;
		}
	}

	private void verifyCallsInGivenTimeFrame(Long departmentId, LocalDateTime startDate, LocalDateTime endDate) throws Exception{
		
		String callSid = null;
		String duration = null;
		List<VoiceCredentials> voiceCredentials = voiceCredentialsRepository.findAllByDeptID(departmentId);
		if(voiceCredentials!=null && !voiceCredentials.isEmpty()) {
			LOGGER.info("VoiceCredentials_Size={} for dealer_departmet_id={} start_date={} end_date={} verification_type=calling", voiceCredentials.size(), departmentId,
					startDate, endDate);
			for(VoiceCredentials vc : voiceCredentials) {
				ResourceSet<Call> calls = twilioClientUtil.fetchCallsForGivenTimeFrame(vc, startDate, endDate, departmentId);
				if(calls!=null) {
					for(Call call : calls) {
						callSid = call.getSid();
						duration = call.getDuration();
						verifyCallDataInDataBase(departmentId, callSid, duration);
					}
				}
				else {
					LOGGER.info("no calls exist for department_id={} start_date={} end_date={}", departmentId, startDate, endDate);
				}
			}
		}
		else {
			LOGGER.info("no voice creds found for department_id={}", departmentId);
		}
		return;
	}
	
	private void verifyCallDataInDataBase(Long departmentId, String callSid, String duration) {
		
		Integer durationMKDB = null;
		Object[] voiceCallData = null;
		
		try {
			voiceCallData = generalRepository.getDataForCall(callSid);
			if(voiceCallData!=null) {
				durationMKDB = (Integer)voiceCallData[1];
			}
			else {
				LOGGER.info("verification_failure_reason={} rules failed for department_id={} call_identifier={} duration={} ", VerificationFailureReason.CALL_SID_DOES_NOT_EXIST.name()
						, departmentId, callSid, duration);
				return;
			}
			if(Integer.valueOf(duration).equals(durationMKDB)) {
				LOGGER.info("rules passed for department_id={} call_identifier={} duration={}", departmentId, callSid, duration);
			}
			else {
				LOGGER.info("verification_failure_reason={} rules failed for department_id={} call_identifier={} duration={} ", VerificationFailureReason.DURATION_MISMATCH.name()
						, departmentId, callSid, duration);
				return;
			}
		}
		catch(Exception e) {
			LOGGER.error("error applyiing rules for calling department_id={} call_identifier={} duration={}", departmentId, callSid, duration);
		}
	}

	private void verifyTextsInGivenTimeFrame(Long departmentId, LocalDateTime startDate, LocalDateTime endDate, Boolean logInDB) {
		
		String messageSid = null;
		int messageBodySize = 0;;
		String verificatonValue = null;
		List<VoiceCredentials> voiceCredentials = voiceCredentialsRepository.findAllByDeptID(departmentId);
		FailedMessagesRequest failedMessageRequest = null;
		List<String> messageSidList = new ArrayList<String>();
		if(voiceCredentials!=null && !voiceCredentials.isEmpty()) {
			LOGGER.info("VoiceCredentials_Size={} for dealer_departmet_id={} start_date={} end_date={} verification_type=texting", voiceCredentials.size(), departmentId,
					startDate, endDate);
			for(VoiceCredentials vc : voiceCredentials) {
				
				try{
					failedMessageRequest = new FailedMessagesRequest();
					messageSidList = new ArrayList<String>();
					failedMessageRequest.setAccountSid(vc.getDealerSubaccount().split("~")[0]);
					ResourceSet<com.twilio.rest.api.v2010.account.Message> texts = twilioClientUtil.fetchMessagesForGivenTimeFrame(vc, startDate, endDate, departmentId);
					if(texts!=null) {
						LOGGER.info("texts={} for startDate={} endDate={}", texts, startDate,endDate);
						for(com.twilio.rest.api.v2010.account.Message text : texts) {
							LOGGER.info("message_sid={} for account_sid={}", texts, vc.getDealerSubaccount().split("~")[0]);
							messageSid = text.getSid();
							messageBodySize = text.getBody().length();
							verificatonValue = verifyTextDataInDataBase(departmentId, messageSid, messageBodySize);
							if(VerificationFailureReason.TEXT_SID_DOES_NOT_EXIST.name().equals(verificatonValue)) {
								messageSidList.add(messageSid);
							}
						}
						if(messageSidList!=null && !messageSidList.isEmpty() && logInDB!=null && logInDB) {
							
							failedMessageRequest.setMessageSidList(messageSidList);
							try {
								LOGGER.info("inserting missing messsages in mk db for message_sid_size={} acccount_sid={} department_id={}",
										messageSidList.size(), vc.getDealerSubaccount().split("~")[0], departmentId);
								sendMessageHelper.saveFailedMessages(failedMessageRequest);
							}
							catch(Exception e) {
								LOGGER.error("unable to save failed messages for message_sid_size={} department_id={}", messageSidList.size(),
										departmentId, e);
							}
							
						}
					}
					else {
						LOGGER.info("no texts exist for department_id={} start_date={} end_date={}", departmentId, startDate, endDate);
					}
				}
				catch(Exception e) {
					LOGGER.error("unable to process verification request for department_id={}", departmentId, e);
				}
			}
		}
		else {
			LOGGER.info("no voice creds found for department_id={}", departmentId);
		}
		return;
	}

	private String verifyTextDataInDataBase(Long departmentId, String messageSid, int messageBodySize) {
		
		Integer messageBodySizeMKDB = null;
		Object[] messageData = null;
		
		try {
			messageData = generalRepository.getMessageDataForCommunicationUid(messageSid);
			if(messageData!=null) {
				messageBodySizeMKDB = (Integer)messageData[1];
			}
			else {
				LOGGER.info("verification_failure_reason={} rules failed for department_id={} message_sid={} messageBodySize={} ", VerificationFailureReason.TEXT_SID_DOES_NOT_EXIST.name()
						, departmentId, messageSid, messageBodySize);
				
				return VerificationFailureReason.TEXT_SID_DOES_NOT_EXIST.name();
			}
			if(Integer.valueOf(messageBodySize).equals(messageBodySizeMKDB)) {
				LOGGER.info("rules passed for department_id={} message_sid={} messageBodySize={}", departmentId, messageSid, messageBodySize);
			}
			else {
				LOGGER.info("verification_failure_reason={} rules failed for department_id={} message_sid={} messageBodySize={} ", VerificationFailureReason.MESSAGE_BODY_SIZE_MISMATCH.name()
						, departmentId, messageSid, messageBodySize);
				return VerificationFailureReason.MESSAGE_BODY_SIZE_MISMATCH.name();
			}
		}
		catch(Exception e) {
			LOGGER.error("error applyiing rules for texting department_id={} message_sid={} messageBodySize={}", departmentId, messageSid, messageBodySize);
		}
		
		return "";
		
	}
}

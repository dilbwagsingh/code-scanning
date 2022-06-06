package com.mykaarma.kcommunications.Twilio.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.kcommunications.Twilio.CallStatus;
import com.mykaarma.kcommunications.Twilio.TwilioGatewayServiceBO;
import com.mykaarma.kcommunications.Twilio.impl.TwilioConstantsBOImpl.TwilioOpsEnum;
import com.mykaarma.kcommunications.controller.impl.VoiceCredentialsImpl;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCallRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCredentialsRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.VoiceCall;
import com.mykaarma.kcommunications.redis.RedisService;
import com.mykaarma.kcommunications.redis.VoiceCallingRedisService;
import com.mykaarma.kcommunications.redis.VoiceCredentialsService;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.KNotificationApiHelper;
import com.mykaarma.kcommunications.utils.OutboundMessageDetail;
import com.mykaarma.kcommunications.utils.TwilioClientUtil;
import com.mykaarma.kcommunications.utils.VoiceCallHelper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Call.UpdateStatus;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;

@Service
public class TwilioGatewayServiceBOImpl implements TwilioGatewayServiceBO {

	private static final Logger LOGGER=LoggerFactory.getLogger(TwilioGatewayServiceBOImpl.class);
	
	@Autowired
	TwilioClientUtil twilioClientUtil;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	VoiceCredentialsRepository voiceCredentialsRepository;
	
	@Autowired
	MessageRepository messageRepository;
	
	@Autowired
	VoiceCallRepository voiceCallRepository;
	
	@Autowired
	VoiceCredentialsService voiceCredentialsService;
	
	@Autowired
	KNotificationApiHelper kNotificationApiHelper;
	
	@Autowired
	RedisService kaarmaRedisService;
	
	@Autowired
	OutboundMessageDetail messageDetail;
	
	@Autowired
	VoiceCredentialsImpl voiceCredentialsImpl;
	
	@Autowired
	VoiceCallHelper voiceCallHelper;
	
	@Autowired
	VoiceCallingRedisService voiceCallingRedisService;
	
	@Value("${krulesengine.url}")
	String URL;
	
	@Value("${twilio_base_url}")
	String TWILIO_BASE_URL;
	
	@Value("${notification-engine-url}")
	private String notificationEngineUrl;

	
	@Override
	public OutboundMessageDetail placeCall(String departmentUUID, String concernedDAUUID, Long dealerID, Long departmentID, String party1Number,
			String party1prompt, String party2Number, String party2prompt, String party2Number2,
			boolean recordCall, boolean isSupport,
			String mode, Long customerID, Boolean announceCall) {
		

		LOGGER.info("Inside PlaceCall function details: DepartmentUUID: {}, concernedDAUUID = {}, party1Number = {}, party2Number = {}, isSupportCall = {}, mode = {}, AnnounceCall = {}",departmentUUID,concernedDAUUID, party1Number, party2Number, isSupport, mode, announceCall);

		String brokerNumber = null;
		boolean isDelegate = false;		
		String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);	
		String callIdentifier = "";
		
		LOGGER.info("dealerID = {}, dealerUUID = {}, departmentID = {} ",dealerID, dealerUUID, departmentID);
		
		Message message = new Message();
		message.setDealerDepartmentId(departmentID);
		message.setCustomerID(customerID);
		message.setDealerID(dealerID);
		
		try {

			if(mode != null && mode.equalsIgnoreCase("Delegation")&&party2Number2 != null)
			{
				isDelegate= true;
			}

			List<String> response = voiceCredentialsImpl.getVoiceCredentialsAndBrokerNumber(message, dealerUUID, departmentID, customerID, isSupport);
			if(response==null) {
				LOGGER.error("No broker number and voice credentials found for dealerID = {}", dealerID);
				return null;
			}
			
			String accountSID = response.get(0);
			String authToken = response.get(1);
			brokerNumber = response.get(2);
			
			LOGGER.info("ACCOUNT_SID = {}, AUTH_TOKEN = {}",accountSID, authToken);			

			try {

				if(announceCall && !isSupport) {
					LOGGER.info("placeCall announce_call=true dealer_id={} party1_number={} party2_number={} party1_prompt={} party2_prompt={} customer_id={}"
							,dealerID,party1Number,party2Number,party1prompt, party2prompt,customerID);
					
					com.twilio.rest.api.v2010.account.Call announcedGreetingCall = twilioClientUtil.conferenceCall(accountSID, authToken, party2Number, party1Number,
							TWILIO_BASE_URL + TwilioOpsEnum.P1ConnectedConference, brokerNumber,
							TWILIO_BASE_URL + TwilioOpsEnum.FallBack, null);

					if (announcedGreetingCall == null) {
						LOGGER.info("placeCall with announced greeting call=null announce_call=true dealer_id={} party1_number={} party2_number={} party1_prompt={} party2_prompt={} customer_id={}"
							,dealerID,party1Number,party2Number,party1prompt, party2prompt,customerID);
						return null;
					}
					
					callIdentifier = announcedGreetingCall.getSid();
					messageDetail.setSid(announcedGreetingCall.getSid()); 
					
					String countryCode = "+1";					
					String dsoForCountryCodeValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.COMMUNICATIONS_COUNTRYCODE_ROLLOUT.getOptionKey());

					if("true".equalsIgnoreCase(dsoForCountryCodeValue)) {
						voiceCallingRedisService.pushGreetingURLForBrokerNumber(brokerNumber, party2prompt);
						LOGGER.info("placeCall broker_number={} customer_greeting={}",brokerNumber,party2prompt);
					} else {
						voiceCallingRedisService.pushGreetingURLForBrokerNumber(countryCode+brokerNumber, party2prompt);
						LOGGER.info("placeCall broker_number={} customer_greeting={} ",countryCode + brokerNumber,party2prompt);
					}
					
					
				} else if(isSupport){
					
					Call call = twilioClientUtil.call(accountSID, authToken, party2Number, party1Number,
							TWILIO_BASE_URL + TwilioOpsEnum.P1ConnectedSupport, party2Number,
							TWILIO_BASE_URL + TwilioOpsEnum.FallBack, null);
					
					if (call == null) {
						return null;
					}
					
					callIdentifier = call.getSid();
					messageDetail.setSid(call.getSid());
					
				} else {

					Call call = twilioClientUtil.call(accountSID, authToken, party2Number, party1Number,
							TWILIO_BASE_URL + TwilioOpsEnum.P1Connected, brokerNumber,
							TWILIO_BASE_URL + TwilioOpsEnum.FallBack, null);
					
					if (call == null) {
						LOGGER.warn("Call is NULL for accountSid = {}", accountSID);
						return null;
					}
					
					callIdentifier = call.getSid();
					messageDetail.setSid(call.getSid());
					
				}
				
				VoiceCall vc = voiceCallHelper.getVoiceCall(callIdentifier, party1Number, party2Number, party2Number2, party1prompt, party2prompt, brokerNumber, recordCall, 'T', isDelegate, (long)CallStatus.in_progress.getID(), false);
				voiceCallRepository.save(vc);

				messageDetail.setBrokerNumber(brokerNumber);
				return messageDetail;

			} catch (Exception e) {
				LOGGER.error("Error inside place call, callIdentifier = {}, party1Numner = {}, party2Number = {} ",callIdentifier, party1Number, party2Number, e);
				return null;
			}
		}catch (Exception th) {
			LOGGER.error("Error in place Call, callIdentifier = {}", callIdentifier, th);
			return null;
		}

	}
	
	
	@Override
	public void setCallStatus(String accountSid,String callSid, int callStatusId) throws Exception {
		 
		LOGGER.info("Inside TwilioGatewayServiceImpl setCallStatus callsid: {}, accountsid: {}", callSid, accountSid);
		Long dealerID = voiceCredentialsRepository.getDealerIDForAccountSid(accountSid);
		 
		try {
			if(dealerID!=null) {
			  kNotificationApiHelper.broadcastCallUpdateEvent(dealerID, callSid, callStatusId);
			}
			else {
			  LOGGER.warn("unable to broadcast call update event for account_sid={} sid={}",accountSid,callSid);
			}
		} catch (Exception e) {
			LOGGER.error("Error inside setCallStatus, acoountSid = {}, callSid = {}", accountSid, callSid);
		}
		
		//save status to DB
		voiceCallRepository.setCallStatus(callSid, callStatusId);
	
	}

	public void cancelCall(String AccountSid, String AuthToken, String callSid) {
		Twilio.init(AccountSid, AuthToken);
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("Status", "completed");
		
		Call call = Call.updater(callSid)
				.setStatus(UpdateStatus.COMPLETED).update();
		return;
	}
	
	@Override
	public String[] getSeparatedDetailsFromDealerSubaccount(String dealerSubaccount) {
		return dealerSubaccount.split("~");
	}

	@Override
	public com.twilio.rest.api.v2010.account.Message sendText(String accountSid, String authToken, String messageBody, String brokerNumber, String toNumber, List<URI> mediaUrls, String callbackUrl) {
		Twilio.init(accountSid, authToken);
		LOGGER.info("in for message_body={} broker_number={} to_number={} ", messageBody, brokerNumber, toNumber);
		MessageCreator messageCreator = com.twilio.rest.api.v2010.account.Message.creator(
			new PhoneNumber(KCommunicationsUtils.getNumberInInternationalFormat(toNumber)),
			new PhoneNumber(KCommunicationsUtils.getNumberInInternationalFormat(brokerNumber)),
			messageBody);
		if(mediaUrls != null && !mediaUrls.isEmpty()) {
			messageCreator.setMediaUrl(mediaUrls);
		}
		if(StringUtils.hasText(callbackUrl)) {
			messageCreator.setStatusCallback(callbackUrl);
		}
		return messageCreator.create();
	}

}

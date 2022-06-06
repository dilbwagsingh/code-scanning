package com.mykaarma.kcommunications.controller.impl;

import javax.persistence.NoResultException;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.FailedDraftEnum;
import com.mykaarma.global.MessageType;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessage;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessageMetaData;
import com.mykaarma.kcommunications.jpa.repository.DelegationHistoryRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageExtnRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageThreadRepository;
import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.model.api.FailedMessagesRequest;
import com.mykaarma.kcommunications.model.jpa.DraftMessageMetaData;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.model.jpa.MessageThread;
import com.mykaarma.kcommunications.model.jpa.VoiceCredentials;
import com.mykaarma.kcommunications.model.kre.InboundTextRequest;
import com.mykaarma.kcommunications.model.kre.KaarmaRoutingResponse;
import com.mykaarma.kcommunications.model.rabbit.OptInAwaitingMessageExpire;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.redis.OptOutRedisService;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.AppConfigHelper;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KCustomerApiHelperV2;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.MessageMetaDataConstants;
import com.mykaarma.kcommunications.utils.RulesEngineHelper;
import com.mykaarma.kcommunications.utils.TwilioClientUtil;
import com.mykaarma.kcommunications_model.enums.CommunicationsFeature;
import com.mykaarma.kcommunications_model.enums.DraftStatus;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.FailedMessageResponse;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;
import com.mykaarma.kcommunications_model.response.SendMessageWithoutCustomerResponse;
import com.mykaarma.kcustomer_model.dto.CustomerUpdateRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message.Direction;
import com.twilio.type.PhoneNumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class SendMessageHelper {
	
	@Value("${twilio.callback.url}")
	private String statusCallback;
	
	@Value("${twilioMasterAccountSid}")
	private String twilioMasterAccountSid;
	
	@Value("${twilioMasterAccountAuthToken}")
	private String twilioMasterAccountAuthToken;
	
	private Logger LOGGER = LoggerFactory.getLogger(SendMessageHelper.class);
	
	@Autowired
	private VoiceCredentialsImpl voiceCredentialsImpl;
	
	@Autowired
	private AppConfigHelper appConfigHelper;
	
	@Autowired
	private RabbitHelper rabbitHelper;
	
	@Autowired
	private CustomerLockService customerLockService;
	
	@Autowired
	private SendCallback sendCallback;
	
	@Autowired
	private MessageSendingRules messageSendingRules;
	
	@Autowired
	private SaveMessageHelper saveMessageHelper;
	
	@Autowired
	private RateControllerImpl rateControllerImpl;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	@Autowired
	private KCommunicationsUtils kCommunicationsUtils;
	
	@Autowired
	private RulesEngineHelper rulesEngineHelper;
	
	@Autowired
	private DelegationHistoryRepository delegationHistoryRepository;
	
	@Autowired
	MessageExtnRepository messageExtnRepository;
	
	@Autowired
	private TwilioClientUtil twilioClientUtil;
	
	@Autowired
	MessageRepository messageRepository;
	
	@Autowired
	KCustomerApiHelperV2 kCustomerApiHelperV2;
	
	@Autowired
	ThreadRepository threadRepository;
	
	@Autowired
	MessageThreadRepository messageThreadRepository;
	
	@Autowired
	private KManageApiHelper kManageApiHelper;

	@Autowired
	private OptOutRedisService optOutRedisService;

	@Autowired
	private Helper helper;

	public static final String DEFAULT_MESSAGE_SUBJECT = "You have received a text from: %s";
	private static final Long DEFAULT_OPTIN_AWAITING_QUEUE_LENGTH = 1L;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	public SendMessageResponse sendTextMessage(String departmentUUID, Message message, Boolean sendSynchronously) throws Exception {
		SendMessageResponse response = new SendMessageResponse();
		List<ApiError> errors = new ArrayList<>();
		List<ApiWarning> warnings = new ArrayList<>();
		String customerUUID =generalRepository.getCustomerUUIDFromCustomerID( message.getCustomerID());
		response.setCustomerUUID(customerUUID);
		response.setMessageUUID(message.getUuid());
		response.setStatus(Status.SUCCESS);
		response.setWarnings(warnings);
		response.setErrors(errors);
		HashMap<String, String> metaDataMap = messageSendingRules.getMetaData(message);
		MessageExtn messageExtn = message.getMessageExtn();

        String dealerUUID = kCommunicationsUtils.getDealerUUIDFromDepartmentUUID(departmentUUID);
        HashMap<String, String> dsoMap = kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(dealerUUID, getDSOList());
        
		String callbackURL = metaDataMap.get(APIConstants.CALLBACK_URL);
		
		try {
	
			VoiceCredentials voiceCredentials = voiceCredentialsImpl.getVoiceCredentialsForMessage(message);
			if(voiceCredentials==null || voiceCredentials.getBrokerNumber()==null || voiceCredentials.getBrokerNumber().isEmpty()) {
				LOGGER.warn(String.format("Voice credentials not available for message_uuid=%s customer_id=%s dealer_department_id=%s ", 
						message.getUuid(), message.getCustomerID(), message.getDealerDepartmentId()));
				ApiError apiError = new ApiError(ErrorCode.MISSING_VOICE_CREDENTIALS.name(), String.format("Voice credentials missing "));
				errors.add(apiError);
				response.setErrors(errors);
				response.setStatus(Status.FAILURE);
				sendCallback.sendCallback(callbackURL, response);
				handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.DEALERSHIP_NOT_AUTHORIZED_TO_SEND_MESSAGE, dsoMap, false);
				LOGGER.warn("Can not send message since voice credentials not available for uuid={} for dealer_id={} customer_id={} dealer_department_id={}", message.getUuid(), message.getDealerID(),
						message.getCustomerID(), message.getDealerDepartmentId());
				return response;
			}
		
			String[] credentials = voiceCredentials.getDealerSubaccount().split("~");
			
			LOGGER.info(" in sendTextMessage checking optout status for uuid={} for dealer_id={} customer_id={} dealer_department_id={} communication_value={} ", message.getUuid(), message.getDealerID(),
					message.getCustomerID(), message.getDealerDepartmentId(), message.getToNumber());
			boolean textingAllowed = processMessageBasedOnOptOutStatus(message, metaDataMap, dsoMap, response, callbackURL, sendSynchronously);
			if(!textingAllowed) {
				return response;
			}

			message.setSentOn(new Date());
			message.setReceivedOn(new Date());
			message.setRoutedOn(new Date());
			
			Boolean overrideHolidays = messageSendingRules.overrideHolidays(metaDataMap, message, dsoMap);
			if(!overrideHolidays) {
				Date nextDate = KManageApiHelper.getNextAvailableSlotForDealer(dealerUUID, message.getSentOn());
				if(nextDate.after(message.getSentOn())) {
					ApiError apiError = new ApiError(ErrorCode.DEALERSHIP_HOLIDAY.name(), String.format("Dealership is not working on this date %s", message.getSentOn()));
					errors.add(apiError);
					response.setErrors(errors);
					response.setStatus(Status.FAILURE);
					sendCallback.sendCallback(callbackURL, response);
					handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.TIME_NOT_CORRECT_FOR_SENDING_REASON, dsoMap, false);
					LOGGER.warn("Can not send message since Dealership is not working on this date for uuid={} for dealer_id={} customer_id={} dealer_department_id={} current_date={} next_date={} ", message.getUuid(), message.getDealerID(),
							message.getCustomerID(), message.getDealerDepartmentId(), message.getSentOn(), nextDate);
					return response;
				}
			}
			
			if(message.getIsManual()!=null && dsoMap!=null && message.getIsManual() && "true".equalsIgnoreCase(dsoMap.get(DealerSetupOption.MESSAGING_CUSTOMER_LOCK_ENABLE.getOptionKey()))) {
				try {
					customerLockService.obtainCustomerLock(message);
				} catch (Exception e) {
					LOGGER.error("Error in claiming customer lock for message_uuid={} ", message.getUuid(), e);
					ApiError apiError = new ApiError(ErrorCode.CUSTOMER_LOCKED.name(), String.format("Customer locked by another advisor. Please try later."));
					errors.add(apiError);
					response.setErrors(errors);
					response.setStatus(Status.FAILURE);
					sendCallback.sendCallback(callbackURL, response);
					LOGGER.warn("Can not send message since lock not available for uuid={} for dealer_id={} customer_id={} dealer_department_id={}", message.getUuid(), message.getDealerID(),
							message.getCustomerID(), message.getDealerDepartmentId());
					return response;
				}
			}
			
			Boolean rateLimitReached = rateControllerImpl.rateLimitReached(departmentUUID, CommunicationsFeature.OUTGOING_TEXT, message.getToNumber());
			if(rateLimitReached) {
				ApiError apiError = new ApiError(ErrorCode.RATE_LIMIT_REACHED.name(), String.format("Message sending limit reached for this number."));
				errors.add(apiError);
				response.setErrors(errors);
				response.setStatus(Status.FAILURE);
				sendCallback.sendCallback(callbackURL, response);
				handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.GENERIC_FAILURE_REASON, dsoMap, false);
				LOGGER.warn("Can not send message since rate limit reached for uuid={} for dealer_id={} customer_id={} dealer_department_id={} communication_value={} ",
						message.getUuid(), message.getDealerID(), message.getCustomerID(), message.getDealerDepartmentId(), message.getToNumber());
				return response;
			}
			String messageBody = messageSendingRules.prepareMessageBody(message, messageExtn,metaDataMap, dsoMap);
			messageExtn.setMessageBody(messageBody);
			List<URI> mediaURLs = null;
			try {
				mediaURLs = messageSendingRules.getMediaURLs(message, voiceCredentials.getBrokerNumber());
			} catch (Exception e) {
				LOGGER.error("Error in getting media URL, sending message without it for message_uuid={} dealer_id={} ",
						message.getUuid(), message.getDealerID(), e);
			}
			
			String dsoForCountryCode = appConfigHelper.getDealerSetupOptionValueFromConfigService(message.getDealerID(), DealerSetupOption.COMMUNICATIONS_COUNTRYCODE_ROLLOUT.getOptionKey());
			
			com.twilio.rest.api.v2010.account.Message twilioMessage = null;
			try{
				if(mediaURLs!=null && !mediaURLs.isEmpty()) {
					twilioMessage = sendMessage(credentials, messageBody, 
							voiceCredentials.getBrokerNumber(), message.getToNumber(), mediaURLs, dsoForCountryCode);
				} else {
					twilioMessage = sendMessage(credentials, messageBody, 
							voiceCredentials.getBrokerNumber(), message.getToNumber(), dsoForCountryCode);
				}
			} catch (TwilioException e) {
				ApiError apiError = new ApiError(ErrorCode.TWILIO_SENDING_FAILURE.name(), String.format("Twilio sending failure."
						+ " error_description=%s",e.getMessage()));
				LOGGER.warn(String.format("Twilio exception received for dealer_id=%s number=%s "
						+ "customer_id=%s exception=%s",  message.getDealerID(),  message.getToNumber(), message.getCustomerID(),e.getMessage()), e);
				
				errors.add(apiError);
				response.setErrors(errors);
				response.setStatus(Status.FAILURE);
				sendCallback.sendCallback(callbackURL, response);
				handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.GENERIC_FAILURE_REASON, dsoMap, false);
				return response;
			}
			message.setFromNumber(voiceCredentials.getBrokerNumber());
			message.setCommunicationUid(twilioMessage.getSid());
			message.setDeliveryStatus("1");
			
			if(twilioMessage.getErrorCode()!=null) {
				LOGGER.error(String.format("Error in sending message from twilio for message_uuid=%s customer_id=%s dealer_department_id=%s ", 
						message.getUuid(), message.getCustomerID(), message.getDealerDepartmentId()));
				message.setTwilioDeliveryFailureMessage("0");
				ApiError apiError = new ApiError(ErrorCode.TWILIO_SENDING_FAILURE.name(), String.format("Twilio sending failure. error_code=%s", twilioMessage.getErrorCode()));
				errors.add(apiError);
				response.setErrors(errors);
				response.setStatus(Status.FAILURE);
				sendCallback.sendCallback(callbackURL, response);
				handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.FAILED_TO_SEND_TEXT, dsoMap, false);
				LOGGER.warn("Can not send message since error in sending message by twilio for uuid={} for dealer_id={} customer_id={} dealer_department_id={} error_code={}", message.getUuid(), message.getDealerID(),
						message.getCustomerID(), message.getDealerDepartmentId(), twilioMessage.getErrorCode());
				return response;
			}
			if(message.getMessageType().equalsIgnoreCase(MessageType.F.name())){
				message.setMessageType(MessageType.S.name());
				if(message.getDraftMessageMetaData()!= null){
					message.getDraftMessageMetaData().setStatus(DraftStatus.SENT.name());
				}
			}
		} catch (Exception e) {
			throw e;
		}
		
		message = saveMessageHelper.saveMessage(message);
		rabbitHelper.pushToMessagePostSendingQueue(message, messageSendingRules.delegationRules(message, dsoMap), messageSendingRules.postMessageProcessingToBeDone(message, dsoMap), false, false, null);
		
		sendCallback.sendCallback(callbackURL, response);
		return response;
	}
	
	public SendMessageWithoutCustomerResponse sendTextMessageWithoutCustomer(ExternalMessage message) {
		LOGGER.info("Sending text message without customer with messageUuid={}", message.getUuid());
		SendMessageWithoutCustomerResponse response = new SendMessageResponse();
		List<ApiError> errors = new ArrayList<>();
		List<ApiWarning> warnings = new ArrayList<>();
		response.setMessageUUID(message.getUuid());
		response.setStatus(Status.SUCCESS);
		response.setWarnings(warnings);
		response.setErrors(errors);
		
		if(!ObjectUtils.isEmpty(message)) {
			ExternalMessageMetaData metaData = message.getMessageMetaData();
			String metaDataString = metaData.getMetaData();
			HashMap<String, String> metaDataMap;
			try {
				metaDataMap = helper.getMessageMetaDatMap(metaDataString);
			} catch (Exception e) {
				LOGGER.info("Some exception occured while converting message meta data to map for message with uuid={} and error={}", message.getUuid(), e);
				metaDataMap = new HashMap<>();
			}
			
			try {
			
				String[] credentials = new String[2];
				credentials[0] = twilioMasterAccountSid;
				credentials[1] = twilioMasterAccountAuthToken;
				
				message.setSentOn(new Date());
				message.setDeliveryStatus("1");
				

				com.twilio.rest.api.v2010.account.Message twilioMessage = null;
				try{
					twilioMessage = sendMessage(credentials, message.getMessageExtn().getMessageBody(), 
							message.getFromValue(), message.getToValue(), Boolean.TRUE.toString());
				} catch (TwilioException e) {
					ApiError apiError = new ApiError(ErrorCode.TWILIO_SENDING_FAILURE.name(), String.format("Twilio sending failure."
							+ " error_description=%s",e.getMessage()));
					errors.add(apiError);
					response.setErrors(errors);
					response.setStatus(Status.FAILURE);
					message.setDeliveryStatus(APIConstants.MESSAGE_DELIVERY_FAILED);
				}
				
				if(twilioMessage != null) {
					metaDataMap.put(APIConstants.COMMUNICATION_ID, twilioMessage.getSid());
					
					if(twilioMessage.getErrorCode()!=null) {
						metaDataMap.put(APIConstants.DELIVERY_FAILURE_REASON, twilioMessage.getErrorCode().toString());
						message.setDeliveryStatus(APIConstants.MESSAGE_DELIVERY_FAILED);
						ApiError apiError = new ApiError(ErrorCode.TWILIO_SENDING_FAILURE.name(), String.format("Twilio sending failure. error_code=%s", twilioMessage.getErrorCode()));
						errors.add(apiError);
						response.setErrors(errors);
						response.setStatus(Status.FAILURE);
						LOGGER.warn("Can not send message since error in sending message by twilio for messageUuid={} error_code={}", message.getUuid(), twilioMessage.getErrorCode());
					}
				} else {
					message.setDeliveryStatus(APIConstants.MESSAGE_DELIVERY_FAILED);
					ApiError apiError = new ApiError(ErrorCode.TWILIO_SENDING_FAILURE.name(), "Twilio sending failure");
					errors.add(apiError);
					response.setErrors(errors);
					response.setStatus(Status.FAILURE);
					LOGGER.warn("Can not send message since error in sending message by twilio for messageUuid={}", message.getUuid());
				}
			} catch (Exception e) {
				LOGGER.info("Some exception occured while sending message with uuid={} and error={}", message.getUuid(), e);
				ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), String.format(e.getMessage()));
				errors.add(apiError);
				response.setErrors(errors);
				response.setStatus(Status.FAILURE);
			}
			
			try {
				saveMessageHelper.saveMessageSentWithoutCustomer(message);
			} catch (Exception e) {
				LOGGER.error("Error while storing message data in DB ", e);
			}
		} else {
			LOGGER.info("empty message body received");
			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), "Empty message body received");
			errors.add(apiError);
			response.setErrors(errors);
			response.setStatus(Status.FAILURE);
		}
		
		return response;
	}
	
	
	private com.twilio.rest.api.v2010.account.Message sendMessage(String[] credentials, String messageBody, String brokerNumber,
			String toNumber, List<URI> mediaURLs, String dsoForCountryCode) {
		com.twilio.rest.api.v2010.account.Message twilioMessage = null;
		Twilio.init(credentials[0], credentials[1]);
		LOGGER.info("in sendMessage callback={}", statusCallback);
		if("true".equalsIgnoreCase(dsoForCountryCode)) {
			twilioMessage = com.twilio.rest.api.v2010.account.Message.creator(new PhoneNumber(toNumber), 
					new PhoneNumber(brokerNumber), 
					messageBody).setStatusCallback(statusCallback).setMediaUrl(mediaURLs).create();
		} else {
			twilioMessage = com.twilio.rest.api.v2010.account.Message.creator(new PhoneNumber(APIConstants.COUNTRY_CODE+toNumber), 
					new PhoneNumber(brokerNumber), 
					messageBody).setStatusCallback(statusCallback).setMediaUrl(mediaURLs).create();
		}
		return twilioMessage;
	}
	
	private com.twilio.rest.api.v2010.account.Message sendMessage(String[] credentials, String messageBody, String brokerNumber,
			String toNumber, String dsoForCountryCode) {
		com.twilio.rest.api.v2010.account.Message twilioMessage = null;
		Twilio.init(credentials[0], credentials[1]);
		LOGGER.info("in sendMessage callback={}", statusCallback);
		if("true".equalsIgnoreCase(dsoForCountryCode)) {
			twilioMessage = com.twilio.rest.api.v2010.account.Message.creator(new PhoneNumber(toNumber), 
					new PhoneNumber(brokerNumber), 
					messageBody).setStatusCallback(statusCallback).create();
			
		} else {
			twilioMessage = com.twilio.rest.api.v2010.account.Message.creator(new PhoneNumber(APIConstants.COUNTRY_CODE+toNumber), 
					new PhoneNumber(brokerNumber), 
					messageBody).setStatusCallback(statusCallback).create();
		}
		return twilioMessage;
		
	}
	
	public ResponseEntity<FailedMessageResponse> saveFailedMessages(
			FailedMessagesRequest failedMessagesRequest) {
		
		FailedMessageResponse  failedMessageResponse = new FailedMessageResponse();
		List<String> failedMessageSids = new ArrayList<String>();
		List<String> messageSidsWithNoCustomer = new ArrayList<String>();
		
		String accountSid = failedMessagesRequest.getAccountSid();
		String authToken = null;
		String dealerSubAccount = null;
		InboundTextRequest inboundTextRequest = null; 
		String messageSid = null;
		Long threadID = null;
		String customerName = null;
		String customerNumber = null;
		String daNumber = null;
		Boolean isOutbound = null;
		
		try {
			dealerSubAccount = kCommunicationsUtils.getDealerSubAccountBySid(accountSid);
		    authToken = dealerSubAccount.split("~")[1];
		}
		catch(Exception e) {
			LOGGER.error("credentials for account_sid={} account does not exist in mykaarma", accountSid, e);
			throw e;
		}
		Twilio.init(accountSid, authToken);
		for(int i=0; i< failedMessagesRequest.getMessageSidList().size(); i++) {
			
			try {
				
				messageSid = failedMessagesRequest.getMessageSidList().get(i);
				com.twilio.rest.api.v2010.account.Message message = twilioClientUtil.fetchMessageForMessageSid(accountSid, authToken, messageSid);
				LOGGER.info("message_recieved from twilio for message_sid={} is {}", messageSid, new ObjectMapper().writeValueAsString(message));
				isOutbound = Direction.OUTBOUND_API.equals(message.getDirection());
				inboundTextRequest = kCommunicationsUtils.createInboundTextRequest(message);
				customerNumber = inboundTextRequest.getFrom();
				daNumber = inboundTextRequest.getTo();
				LOGGER.info("inboundTextRequest for message_sid={} is {}", messageSid, new ObjectMapper().writeValueAsString(inboundTextRequest));
				KaarmaRoutingResponse response = rulesEngineHelper.getReponseFromRulesEngine(inboundTextRequest);
				LOGGER.info("response received from krules for message_sid={} is {}", messageSid,new ObjectMapper().writeValueAsString(response));
				
				if(response.getRoutingRuleResponse().getCustomerID()==null) {
					
					LOGGER.info("customer does not exist for contact number={} message_sid={}", customerNumber, messageSid);
					messageSidsWithNoCustomer.add(messageSid);
					
					String customerUUID = prepareObjectAndCallKcustomerV2ToSaveNewCustomer(customerNumber,
							"Unknown Number", customerNumber, response.getRoutingRuleResponse().getDealerDepartmentID(), messageSid);
					Object[] customerInfo = generalRepository.getCustomerIDandNameFromUUID(customerUUID);
					Long customerID = ((BigInteger) customerInfo[0]).longValue();
					customerName = (String) customerInfo[1];
					response.getRoutingRuleResponse().setCustomerID(customerID);
					
					threadID = prepareAndSaveThreadObject(customerID, response.getRoutingRuleResponse().getDealerAssociateID(),
							response.getRoutingRuleResponse().getDealerDepartmentID(), response.getRoutingRuleResponse().getDealerID(), 
							false, false, inboundTextRequest.getReceivedDate());
					
				}
				else {
					threadID = response.getRoutingRuleResponse().getThreadID();
					
					try {
						Long delegatedFrom = delegationHistoryRepository.checkLatestDelgationAfterReceivedDate(threadID, inboundTextRequest.getReceivedDate());
						if(delegatedFrom!=null) {
							response.getRoutingRuleResponse().setDealerAssociateID(delegatedFrom);
						}
						customerName = generalRepository.getCustomerNameFromId(response.getRoutingRuleResponse().getCustomerID());
						LOGGER.info("delegateFrom={} for customer_id={} department_id={} thread_id={}  message_sid={}", delegatedFrom, response.getRoutingRuleResponse().getCustomerID(), 
								response.getRoutingRuleResponse().getDealerDepartmentID(), response.getRoutingRuleResponse().getThreadID(), messageSid);
					}
					catch(NoResultException nre) {
						LOGGER.info("no  delegation after the received date for message_sid={} dealerID={}",messageSid, response.getRoutingRuleResponse().getDealerID(), nre);
					}
					
				}
				
				String dealerAssociateName = generalRepository.getDealerAssociateName(response.getRoutingRuleResponse().getDealerAssociateID());
				
				
				Message messageObject = kCommunicationsUtils.createMessageObject(response, inboundTextRequest.getBody(), inboundTextRequest.getReceivedDate(), MessageProtocol.TEXT, 
						isOutbound ? com.mykaarma.kcommunications_model.enums.MessageType.OUTGOING : com.mykaarma.kcommunications_model.enums.MessageType.INCOMING, 
								isOutbound ? daNumber : customerNumber , isOutbound ? customerNumber : daNumber, isOutbound ? dealerAssociateName: customerName, 
										isOutbound ? customerName : dealerAssociateName, messageSid);
				
				String subject = String.format(DEFAULT_MESSAGE_SUBJECT,customerName);
				
				MessageExtn messageExtn =  kCommunicationsUtils.createMessageExtnObject(inboundTextRequest.getBody(), subject);
				
				messageObject = saveMessageAndMessageExtn(messageObject, messageExtn, messageSid);
				
				prepareAndSaveMessageThread(messageObject.getId(), threadID);
				
				LOGGER.info("message inserted successfully for message_sid={}", messageSid);
				
			}
			catch(Exception e) {
				LOGGER.error("unable to process request for message_sid={}", messageSid,e);
				failedMessageSids.add(messageSid);
			}
		}
		
		if(!failedMessageSids.isEmpty()) {
			failedMessageResponse.setFailedMessageSIDsList(failedMessageSids);
			failedMessageResponse.setMessageSidsWithNoCustomer(messageSidsWithNoCustomer);
			return new ResponseEntity<FailedMessageResponse>(failedMessageResponse, HttpStatus.BAD_REQUEST);
		}
		
		failedMessageResponse.setMessageSidsWithNoCustomer(messageSidsWithNoCustomer);
		return new ResponseEntity<FailedMessageResponse>(failedMessageResponse, HttpStatus.OK);
	}


	private Set<String> getDSOList() {
		Set<String> dsoList = new HashSet<>();
		dsoList.add(DealerSetupOption.COMMUNICATIONS_MANUAL_MESSAGE_OPT_OUT_FOOTER_DISABLE.getOptionKey());
		dsoList.add(DealerSetupOption.COMMUNICATIONS_OPT_OUT_FOOTER_TEXT.getOptionKey());
		dsoList.add(DealerSetupOption.COMMUNICATIONS_TEXT_FOOTER.getOptionKey());
		dsoList.add(DealerSetupOption.MESSAGING_DRAFT_BLACKOUTDATE_ENABLE.getOptionKey());
		dsoList.add(DealerSetupOption.MESSAGING_CUSTOMER_LOCK_ENABLE.getOptionKey());
		dsoList.add(DealerSetupOption.COMMUNICATION_POST_MESSAGE_SENT.getOptionKey());
		dsoList.add(DealerSetupOption.COMMUNICATION_POST_MESSAGE_SENT_AUTOMATIC.getOptionKey());
		dsoList.add(DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_ENABLE.getOptionKey());
		dsoList.add(DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_OPTIN_AWAITING_QUEUE_LENGTH.getOptionKey());
		return dsoList;
	}
	
	private String prepareObjectAndCallKcustomerV2ToSaveNewCustomer(String firstName, String lastName, String phoneNumber, Long departmentID, String messageSid) {
		
		CustomerUpdateRequest customerUpdateRequest = kCommunicationsUtils.createCustomerRequestObject(firstName,
				lastName, phoneNumber);
		String departmentUUID = appConfigHelper.getDealerDepartmentUUIDForID(departmentID);
		String customerUUID = KCustomerApiHelperV2.saveCustomer(departmentUUID, customerUpdateRequest);
		LOGGER.info("customerUUID={} received after saving customer for message_sid={}", customerUUID, messageSid);
		
		return customerUUID;
		
	}
	
	private Message saveMessageAndMessageExtn(Message messageObject, MessageExtn messageExtn, String messageSid) {
		
		messageObject = messageRepository.save(messageObject);
		LOGGER.info("message_id received for customer_id={} dealer_id={} department_id={} message_sid={}", messageObject.getId(), messageObject.getCustomerID(),
				messageObject.getDealerID(), messageObject.getDealerDepartmentId(), messageSid);
		messageExtn.setMessageID(messageObject.getId());
		messageExtnRepository.save(messageExtn);
		return messageObject;
	}
	
	private Long prepareAndSaveThreadObject(Long customerID, Long dealerAssociateID, Long departmentID, Long dealerID, Boolean isArchived, Boolean isClosed, Date receivedDate) {
		
		com.mykaarma.kcommunications.model.jpa.Thread thread = kCommunicationsUtils.createThreadObject(customerID, dealerAssociateID, departmentID, dealerID, isArchived, isClosed, receivedDate);
		thread = threadRepository.save(thread);
		return thread.getId();
	}
	
	private void prepareAndSaveMessageThread(Long messageId, Long threadId) {
		
		MessageThread messageThread = kCommunicationsUtils.createMessageThreadObject(messageId, threadId);
		messageThreadRepository.save(messageThread);
	}

	public void handleDraftSendFailureAndPushToPostMessageSentQueue(Message message, FailedDraftEnum failureReason, HashMap<String, String> dsoMap, Boolean showAsFailedDraft) throws Exception {
		if ((MessageType.F.name().equalsIgnoreCase(message.getMessageType()) && message.getDraftMessageMetaData() != null)
				|| MessageType.S.name().equalsIgnoreCase(message.getMessageType())) {
			Boolean isFailedMessage = MessageType.S.name().equalsIgnoreCase(message.getMessageType());
			if(isFailedMessage || showAsFailedDraft == null || !showAsFailedDraft) {
				message.setDeliveryStatus("0");
				message.setMessageType(MessageType.S.name());
				message.setSentOn(new Date());
				message.setReceivedOn(new Date());
				message.setRoutedOn(new Date());
				message.setTwilioDeliveryFailureMessage(failureReason.getFailureReason());
			}
			if(!isFailedMessage) {
				DraftMessageMetaData draftMessageMetaData = null;
				if (message.getDraftMessageMetaData() != null) {
					draftMessageMetaData = message.getDraftMessageMetaData();
				} else {
					draftMessageMetaData = new DraftMessageMetaData();
					message.setDraftMessageMetaData(draftMessageMetaData);
				}
				if (draftMessageMetaData.getReasonForLastFailure() == null
						|| draftMessageMetaData.getReasonForLastFailure().isEmpty()) {
					message.getDraftMessageMetaData().setStatus(DraftStatus.FAILED.name());
					message.getDraftMessageMetaData().setReasonForLastFailure(failureReason.getFailureReason());
				}
			}
			message = saveMessageHelper.saveMessage(message);
			rabbitHelper.pushToMessagePostSendingQueue(message, null, false, false, isFailedMessage, null);
		}
	}

	private boolean processMessageBasedOnOptOutStatus(Message message, HashMap<String, String> metaDataMap, HashMap<String, String> dsoMap, SendMessageResponse response, String callbackURL, Boolean sendSynchronously) throws Exception {
		Boolean textingAllowed = messageSendingRules.isTextingAllowed(message, metaDataMap, "true".equalsIgnoreCase(dsoMap.get(DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_ENABLE.getOptionKey())));
		if(!textingAllowed) {
			LOGGER.warn(String.format("Texting not allowed for message_uuid=%s customer_id=%s dealer_department_id=%s ",
				message.getUuid(), message.getCustomerID(), message.getDealerDepartmentId()));
			if("true".equalsIgnoreCase(metaDataMap.get(MessageMetaDataConstants.QUEUE_IF_OPTED_OUT)) &&
				"true".equalsIgnoreCase(dsoMap.get(DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_ENABLE.getOptionKey()))) {
				Lock lock = null;
				try {
					ApiWarning apiWarning = new ApiWarning(ErrorCode.OPTED_OUT_COMMUNICATION_VALUE.name(), String.format("Phone Number/Email %s for the customer is opted-out.", message.getToNumber()));
					response.getWarnings().add(apiWarning);
					lock = optOutRedisService.obtainLockOnOptInAwaitingMessageQueue(message.getDealerID(), message.getUuid());
					List<String> optInAwaitingMessageQueue = optOutRedisService.getOptInAwaitingMessageQueue(message.getDealerID(), message.getToNumber());
					Long optinAwaitingQueueMaxSize = DEFAULT_OPTIN_AWAITING_QUEUE_LENGTH;
					try {
						optinAwaitingQueueMaxSize = Long.parseLong(dsoMap.get(DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_OPTIN_AWAITING_QUEUE_LENGTH.getOptionKey()));
					} catch (Exception e) {
						LOGGER.warn("Error while parsing dso={} with value={}", DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_OPTIN_AWAITING_QUEUE_LENGTH.getOptionKey(),
							dsoMap.get(DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_OPTIN_AWAITING_QUEUE_LENGTH.getOptionKey()), e);
					}
					if(optInAwaitingMessageQueue != null && optInAwaitingMessageQueue.size() >= optinAwaitingQueueMaxSize) {
						LOGGER.warn("Can not send message since texting not allowed for uuid={} for dealer_id={} customer_id={} dealer_department_id={} communication_value={} and optin awaiting message queue is full.", message.getUuid(), message.getDealerID(),
							message.getCustomerID(), message.getDealerDepartmentId(), message.getToNumber());
						ApiError apiError = new ApiError(ErrorCode.OPTED_OUT_COMMUNICATION_VALUE.name(), String.format("Phone Number/Email %s for the customer is opted-out.", message.getToNumber()));
						response.getErrors().add(apiError);
						response.setStatus(Status.FAILURE);
						if(!sendSynchronously) {
							sendCallback.sendCallback(callbackURL, response);
							handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.CUSTOMER_OPTED_OUT, dsoMap, false);
						}
					}
					optInAwaitingMessageQueue = optInAwaitingMessageQueue == null ? new ArrayList<>() : optInAwaitingMessageQueue;
					LOGGER.info("Queueing message for uuid={} for dealer_id={} customer_id={} dealer_department_id={} communication_value={} due to opt-out", message.getUuid(), message.getDealerID(),
						message.getCustomerID(), message.getDealerDepartmentId(), message.getToNumber());
					convertMessageToQueuedDraftAndPushToPostMessageSentQueue(message);
					optInAwaitingMessageQueue.add(message.getUuid());
					optOutRedisService.setOptInAwaitingMessageQueue(message.getDealerID(), message.getToNumber(), optInAwaitingMessageQueue);
					response.setStatus(Status.SUCCESS);
					sendCallback.sendCallback(callbackURL, response);
				} finally {
					if(lock != null) {
						lock.unlock();
					}
				}
			} else {
				ApiError apiError = new ApiError(ErrorCode.OPTED_OUT_COMMUNICATION_VALUE.name(), String.format("Phone Number/Email %s for the customer is opted-out.", message.getToNumber()));
				response.getErrors().add(apiError);
				response.setStatus(Status.FAILURE);
				LOGGER.warn("Can not send message since texting not allowed for uuid={} for dealer_id={} customer_id={} dealer_department_id={} communication_value={} ", message.getUuid(), message.getDealerID(),
					message.getCustomerID(), message.getDealerDepartmentId(), message.getToNumber());
				if(!sendSynchronously) {
					sendCallback.sendCallback(callbackURL, response);
					handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.CUSTOMER_OPTED_OUT, dsoMap, false);
				}
			}
		}
		return textingAllowed;
	}

	private void convertMessageToQueuedDraftAndPushToPostMessageSentQueue(Message message) throws Exception {
		message.setMessageType(MessageType.F.name());
		message.setSentOn(null);
		message.setReceivedOn(null);
		message.setRoutedOn(null);
		DraftMessageMetaData draftMessageMetaData;
		if (message.getDraftMessageMetaData() != null) {
			draftMessageMetaData = message.getDraftMessageMetaData();
		} else {
			draftMessageMetaData = new DraftMessageMetaData();
			message.setDraftMessageMetaData(draftMessageMetaData);
		}
		draftMessageMetaData.setStatus(DraftStatus.QUEUED.name());
		message = saveMessageHelper.saveMessage(message);
		rabbitHelper.pushToMessagePostSendingQueue(message, null, false, false, false, null);
	}

	public void expireOptInAwaitingMessage(OptInAwaitingMessageExpire optInAwaitingMessageExpire) throws Exception {
		LOGGER.info("in expireOptInAwaitingMessage for request={}", OBJECT_MAPPER.writeValueAsString(optInAwaitingMessageExpire));
		Message message = helper.getMessageObject(optInAwaitingMessageExpire.getMessageUUID());
		if(DraftStatus.QUEUED.name().equalsIgnoreCase(message.getDraftMessageMetaData().getStatus())) {
			HashMap<String, String> dsoMap = kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(generalRepository.getDealerUUIDFromDealerId(message.getDealerID()), getDSOList());
			LOGGER.info("in expireOptInAwaitingMessage queued message_uuid={} still not sent for communication_value={}. Converting to failed draft", message.getUuid(), message.getToNumber());
			if(message.getMessageMetaData() != null && message.getMessageMetaData().getMetaData() != null) {
				HashMap<String, String> metaDataMap = helper.getMessageMetaDatMap(message.getMessageMetaData().getMetaData());
				metaDataMap.put(MessageMetaDataConstants.QUEUE_IF_OPTED_OUT, "false");
				message.getMessageMetaData().setMetaData(helper.getMessageMetaData(metaDataMap));
			}
			handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.CUSTOMER_OPTED_OUT, dsoMap, true);
		} else {
			LOGGER.info("in expireOptInAwaitingMessage queued message_uuid={} for communication_value={} has been updated. Discarding request to expire", message.getUuid(), message.getToNumber());
		}
		optOutRedisService.removeMessageUUIDListFromOptinAwaitingMessageQueue(message.getDealerID(), message.getToNumber(), Collections.singletonList(message.getUuid()));
	}
}


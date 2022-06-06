package com.mykaarma.kcommunications.mq.impl;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.MessageType;
import com.mykaarma.global.OrderStatus;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessage;
import com.mykaarma.kcommunications.communications.repository.UIElementTranslationRepository;
import com.mykaarma.kcommunications.controller.impl.CommunicationsApiImpl;
import com.mykaarma.kcommunications.controller.impl.ForwardedAndBotMessageImpl;
import com.mykaarma.kcommunications.controller.impl.OptOutImpl;
import com.mykaarma.kcommunications.controller.impl.PostIncomingMessageSaveService;
import com.mykaarma.kcommunications.controller.impl.PostMessageReceivedImpl;
import com.mykaarma.kcommunications.controller.impl.PostMessageSendingHelper;
import com.mykaarma.kcommunications.controller.impl.PostOptOutImpl;
import com.mykaarma.kcommunications.controller.impl.PostUniversalMessageSendService;
import com.mykaarma.kcommunications.controller.impl.PreferredCommunicationModeImpl;
import com.mykaarma.kcommunications.controller.impl.SaveMessageHelper;
import com.mykaarma.kcommunications.controller.impl.SendEmailHelper;
import com.mykaarma.kcommunications.controller.impl.SendMessageHelper;
import com.mykaarma.kcommunications.controller.impl.TemplateImpl;
import com.mykaarma.kcommunications.event.handler.FollowupEventHandler;
import com.mykaarma.kcommunications.jpa.repository.DraftMessageMetaDataRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageExtnRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCredentialsRepository;
import com.mykaarma.kcommunications.model.api.DelayedFilterRemovalRequest;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.mvc.FilterDataRemovalRequest;
import com.mykaarma.kcommunications.model.rabbit.CustomerSubscriptionsUpdate;
import com.mykaarma.kcommunications.model.rabbit.DoubleOptInDeployment;
import com.mykaarma.kcommunications.model.rabbit.FetchCustomersDealer;
import com.mykaarma.kcommunications.model.rabbit.MessageSavingQueueData;
import com.mykaarma.kcommunications.model.rabbit.MessageUpdateOnEvent;
import com.mykaarma.kcommunications.model.rabbit.MultipleMessageSending;
import com.mykaarma.kcommunications.model.rabbit.OptInAwaitingMessageExpire;
import com.mykaarma.kcommunications.model.rabbit.OptOutStatusUpdate;
import com.mykaarma.kcommunications.model.rabbit.PostIncomingBotMessageSave;
import com.mykaarma.kcommunications.model.rabbit.PostIncomingMessageSave;
import com.mykaarma.kcommunications.model.rabbit.PostMessageReceived;
import com.mykaarma.kcommunications.model.rabbit.PostMessageSent;
import com.mykaarma.kcommunications.model.rabbit.PostOptOutStatusUpdate;
import com.mykaarma.kcommunications.model.rabbit.PostUniversalMessageSendPayload;
import com.mykaarma.kcommunications.model.rabbit.PreferredCommunicationModePrediction;
import com.mykaarma.kcommunications.model.rabbit.SaveHistoricalMessageRequest;
import com.mykaarma.kcommunications.model.rabbit.TemplateIndexingRequest;
import com.mykaarma.kcommunications.model.utils.VerificationService;
import com.mykaarma.kcommunications.redis.SaveMessageRedisService;
import com.mykaarma.kcommunications.utils.AWSClientUtil;
import com.mykaarma.kcommunications.utils.ConvertToJpaEntity;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications.utils.RabbitQueueInfo;
import com.mykaarma.kcommunications.utils.ReportingApiUtils;
import com.mykaarma.kcommunications.utils.SystemNotificationHelper;
import com.mykaarma.kcommunications.utils.ThreadPrintingHelper;
import com.mykaarma.kcommunications.utils.TwilioClientUtil;
import com.mykaarma.kcommunications_model.common.DealerMessagesFetchRequest;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.RecordingURLMessageUpdateRequest;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessagePurpose;
import com.mykaarma.kcommunications_model.enums.VerificationFailureReason;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryMailRequest;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryRequest;
import com.mykaarma.kcommunications_model.request.SaveMessageRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.CommunicationHistoryResponse;
import com.mykaarma.kcommunications_model.response.SaveMessageResponse;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;
import com.mykaarma.kcustomer_model.dto.Customer;
import com.mykaarma.kcustomer_model.lombokresponse.CustomerWithVehiclesResponse;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.korder_model.v3.common.GlobalOrderTransitionDTO;
import com.mykaarma.templateengine.TemplateEngine;

@Service
public class KCommunicationsRequestProcessor {

	private final Logger LOGGER = LoggerFactory.getLogger(KCommunicationsRequestProcessor.class);

	@Autowired
	SendMessageHelper sendMessageHelper;

	@Autowired
	SendEmailHelper sendEmailHelper;

	@Autowired
	CommunicationsApiImpl communicationsApiImpl;

	@Autowired
	private MessagingViewControllerHelper messagingViewControllerHelper;

	@Autowired
	PostMessageSendingHelper postMessageSendingHelper;

	@Autowired
	SaveMessageHelper saveMessageHelper;

	@Autowired
	FollowupEventHandler followupEventHandler;

	@Autowired
	MessageRepository messageRepository;

	@Autowired
	PostMessageReceivedImpl postMessageReceivedImpl;

	@Autowired
	MessageExtnRepository messageExtnRepository;

	@Autowired
	DraftMessageMetaDataRepository draftMessageMetaDataRepository;

	@Autowired
	KCommunicationsUtils kCommunicationsUtils;

	@Autowired
	ReportingApiUtils reportingApiUtils;
	
	@Autowired
	AWSClientUtil awsClientUtil;

	@Autowired
	GeneralRepository generalRepository;

	@Autowired
	TwilioClientUtil twilioClientUtil;

	@Autowired
	RabbitHelper rabbitHelper;

	@Autowired
	ThreadPrintingHelper threadPrintingHelper;

	@Autowired
	ThreadRepository threadRepository;

	@Autowired
    private PreferredCommunicationModeImpl preferredCommunicationModeImpl;

    @Autowired
	VoiceCredentialsRepository voiceCredentialsRepository;

	@Autowired
	VerificationService verificationService;

	@Autowired
	OptOutImpl optOutImpl;

	@Autowired
	PostOptOutImpl postOptOutImpl;

	@Autowired
	PostIncomingMessageSaveService postIncomingMessageSaveService;

	@Autowired
	Helper helper;

	@Autowired
	ConvertToJpaEntity convertToJpaEntity;

	@Autowired
	SaveMessageRedisService saveMessageRedisService;

	@Autowired
	KManageApiHelper kManageApiHelper;

	@Autowired
	TemplateImpl templateImpl;

	@Autowired
	PostUniversalMessageSendService postUniversalMessageSendService;

    @Autowired
    private SystemNotificationHelper systemNotificationHelper;

    @Autowired
    private UIElementTranslationRepository uiElementTranslationRepository;

	@Autowired
	private ForwardedAndBotMessageImpl forwardedAndBotMessageImpl;

	@Value("${cfURLPrefix}")
	private String cfURLPrefix;

	@Value("${multiplier:5}")
	private int multiplier;

	@Value("${maximumretries:6}")
	private int maximumretries;

	private ObjectMapper objectMapper = new ObjectMapper();

	public static final String TWILIO_RECORDING_BASE_URL = "api.twilio.com";
	public static final String RECORDING_CONTENT_TYPE = "audio/mpeg";
	public static final String RECORDING_EXTENSION = ".mp3";
	public static final String RULES_PASSED = "passed";
	public static final String HOURLY_TMP_DIR = "/consumer/h/";
	public static final String VOICE_CALL = "Voice Call";
	public static final Long completedCall = 100l;
	public static final String COMMUNICATIONS_RECORDING_FOLDER_PREFIX="/recordings/";
	public static final String DELAYED_COLLECTION="DELAYED_COLLECTION";
	public static final String CALLING_VERIFICAITION_PAREMETER = "Calling";
	public static final String TEXTING_VERIFICAITION_PAREMETER = "Texting";

	private static final String THREAD_OWNERSHIP_CHANGED_EVENT_NOTE_TEMPLATE = "Thread.Ownership.Change.On.RO.Creation.SystemNote";
	private static final String DEALER_ASSOCIATE_NAME = "dealer_associate_name";
	private static final String ORDER_NUMBER = "order_number";

	public void sendMessage(String messagePayload) throws Exception {
		try {
			Message message = objectMapper.readValue(messagePayload, Message.class);
			if(message!=null && message.getIsManual()==null){
				message.setIsManual(false);
			}
			
			Boolean isAutoCsiMessage = reportingApiUtils.isAutoCsiMessage(message);
				
	        String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(message.getDealerDepartmentId());
	        
	        SendMessageResponse response = null;
	        
			if(message.getProtocol().equalsIgnoreCase(MessageProtocol.TEXT.getMessageProtocol())) {
				response = sendMessageHelper.sendTextMessage(departmentUUID, message,false);			
			} else if(message.getProtocol().equalsIgnoreCase(MessageProtocol.EMAIL.getMessageProtocol())) {
				response = sendEmailHelper.sendEmail(departmentUUID, message);
			}

			if(isAutoCsiMessage) {
				reportingApiUtils.sendAutoCsiMessageErrorsToReporting(response, message);
			}

		} catch (Exception e) {
			LOGGER.error("Error in sendMessage for messagePayLoad={} ", messagePayload, e);
			throw e;
		}
	}

	public void saveMessage(MessageSavingQueueData data) throws Exception {
		try {
			Boolean isEditedDraft = false;
            if(data.getMessage().getId() != null && MessageType.F.name().equals(data.getMessage().getMessageType())) {
                isEditedDraft = true;
            }
			saveMessageHelper.saveMessage(data.getMessage());

			if(com.mykaarma.kcommunications_model.enums.MessageType.NOTE.getMessageType().equalsIgnoreCase(data.getMessage().getMessageType()) && data.getMessage().getIsManual()) {
				PostUniversalMessageSendPayload postUniversalMessageSendPayload = new PostUniversalMessageSendPayload();
				postUniversalMessageSendPayload.setMessage(data.getMessage());
				postUniversalMessageSendPayload.setUsersToNotify(data.getUsersToNotify());
				postUniversalMessageSendPayload.setUpdateThreadTimestamp(data.getUpdateThreadTimestamp());
				postUniversalMessageSendService.postUniversalMessageSendProcessing(postUniversalMessageSendPayload);
			} else {
				PostMessageSent postMessageSent = new PostMessageSent();
				postMessageSent.setMessage(data.getMessage());
				postMessageSent.setPostMessageProcessingToBeDone(false);
				postMessageSent.setUpdateThreadTimestamp(data.getUpdateThreadTimestamp());
				postMessageSent.setIsEditedDraft(isEditedDraft);
				LOGGER.info("calling postmessagesending helper synchronously for message={} is_edited_draft={}", objectMapper.writeValueAsString(data), isEditedDraft);
				postMessageSendingHelper.postMessageSendingHelper(postMessageSent);
			}

		} catch (Exception e) {
			LOGGER.error("Error in saveMessage for messagePayLoad={} ", objectMapper.writeValueAsString(data), e);
			throw e;
		}
	}

	public void postMessageSent(String messagePayload) throws Exception {
		try {
			LOGGER.info("mykaarma.communications.api.post.message.send consumer {}", messagePayload);
			PostMessageSent postMessageSent = objectMapper.readValue(messagePayload, PostMessageSent.class);
			postMessageSendingHelper.postMessageSendingHelper(postMessageSent);
		} catch (Throwable e) {
			LOGGER.error("Error in post message sent for messagePayLoad={} ", messagePayload, e);
			throw e;
		}
	}

	public void postIncomingMessageSave(String messagePayload) throws Exception {
		try {
			PostIncomingMessageSave postIncomingMessageSave = objectMapper.readValue(messagePayload, PostIncomingMessageSave.class);
			postIncomingMessageSaveService.postIncomingMessageSaveProcessing(postIncomingMessageSave);
		} catch (Exception e) {
			LOGGER.error("Error in post incoming message save for messagePayLoad={} ", messagePayload, e);
			throw e;
		}
	}

	public void postUniversalMessageSend(String messagePayload) throws Exception {
		try {
			PostUniversalMessageSendPayload postUniversalMessageSendPayload = objectMapper.readValue(messagePayload, PostUniversalMessageSendPayload.class);
			postUniversalMessageSendService.postUniversalMessageSendProcessing(postUniversalMessageSendPayload);
		} catch (Exception e) {
			LOGGER.error("Error in post universal message send for messagePayLoad={} ", messagePayload, e);
			throw e;
		}
	}

	public void eventProcessor(MessageUpdateOnEvent messageUpdateOnEvent) throws Exception {
		try {
			switch(messageUpdateOnEvent.getEvent()) {

				case MANUAL_FOLLOWUP:
					followupEventHandler.followUpEventHandler(messageUpdateOnEvent);
					break;

				default:
					throw new Exception(String.format("Unknown event={} for message_uuid={}  ", messageUpdateOnEvent.getEvent(), messageUpdateOnEvent.getMessageUUID()));

			}
		} catch (Exception e) {
			LOGGER.error("Error in eventProcessor for message_uuid={} ", messageUpdateOnEvent.getMessageUUID(), e);
			throw e;
		}
	}

	public void eventProcessor(String messagePayload) throws Exception {
		try {
			MessageUpdateOnEvent messageUpdateOnEvent = objectMapper.readValue(messagePayload, MessageUpdateOnEvent.class);
			switch(messageUpdateOnEvent.getEvent()) {

				case MANUAL_FOLLOWUP:
					followupEventHandler.followUpEventHandler(messageUpdateOnEvent);
					break;

				default:
					throw new Exception(String.format("Unknown event={} for message_payload={} ", messageUpdateOnEvent.getEvent(), messagePayload));

			}
		} catch (Exception e) {
			LOGGER.error("Error in eventProcessor for messagePayLoad={} ", messagePayload, e);
			throw e;
		}
	}

	public void fetchMessagesForDealer(String messagePayload) throws Exception {
		List<BigInteger> messageIDs = new ArrayList<BigInteger>();
		Date fromDate = null;
		Date toDate = null;
		Long dealerID = null;
		try {

			DealerMessagesFetchRequest message = objectMapper.readValue(messagePayload, DealerMessagesFetchRequest.class);
			LOGGER.info("request received to fetch messages for dealer_id={} fromDate={} endDate={}",message.getDealerID(), message.getStartDate(), message.getEndDate());
			dealerID = message.getDealerID();
			String startDate = message.getStartDate();
			String endDate =  message.getEndDate();
			Boolean deleteRecording = message.getDeleteRecordings();
			Boolean verifyRecordings = message.getVerifyRecordings();
			if(startDate==null || endDate==null) {
				messageIDs = messageRepository.fetchAllMessagesForDealerByProtocol(dealerID, MessageProtocol.VOICE_CALL.getMessageProtocol());
			} else{
				fromDate = kCommunicationsUtils.getPstDateFromIsoDate(startDate);
				toDate = kCommunicationsUtils.getPstDateFromIsoDate(endDate);
				if(fromDate!=null && toDate!=null) {
					messageIDs = messageRepository.fetchAllMessagesForDealerByStartAndEndDateAndProtocol(dealerID, fromDate, toDate, MessageProtocol.VOICE_CALL.getMessageProtocol());
				}

			}
			if(messageIDs!=null) {
				LOGGER.info("message_count={} received for dealerID={} fromDate={} to endDate={}",messageIDs.size(), dealerID, fromDate, toDate);
				for(BigInteger messageID : messageIDs) {
					LOGGER.info(String.format("pushing message to queue=%s for message_id=%s",RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE.getQueueName(), messageID));
					pushMessageToUpdateRecordingQueue(messageID.longValue(), dealerID, deleteRecording, verifyRecordings);
				}
			}
			else {
				LOGGER.warn(String.format("messageIDs are null for messagePayload=%s dealer_id=%s",  messagePayload, dealerID));
			}
		} catch (Exception e) {
			LOGGER.error(String.format("Error in fetching messageIDs for messagePayload=%s dealer_id=%s",  messagePayload, dealerID),e);
			throw e;
		}
	}

	public void pushMessageToUpdateRecordingQueue(Long messageID, Long dealerID, Boolean deleteRecordings, Boolean verifyRecording) {

		RecordingURLMessageUpdateRequest updateRecordingURLForMessage = new RecordingURLMessageUpdateRequest();
		updateRecordingURLForMessage.setMessageID(messageID);
		updateRecordingURLForMessage.setDealerID(dealerID);
		updateRecordingURLForMessage.setDeleteRecordings(deleteRecordings);
		updateRecordingURLForMessage.setVerifyRecording(verifyRecording);
		try {
			rabbitHelper.pushDataToUpdateURLForMessage(updateRecordingURLForMessage);
		} catch (Exception e) {
			LOGGER.error(String.format("can't push data to queue=%s for message_id=%s",RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE.getQueueName(), updateRecordingURLForMessage.getMessageID()),e);
		}

	}

	public void takeActionsPostMessageReceived(String messagePayload) throws Exception {

		try {
			LOGGER.info("in takeActionsPostMessageReceived message_payload={}",messagePayload);

			PostMessageReceived postMessageReceived=objectMapper.readValue(messagePayload, PostMessageReceived.class);
			if (postMessageReceived != null && postMessageReceived.getDealerID() != null
				&& postMessageReceived.getMessageUUID() != null && !postMessageReceived.getMessageUUID().isEmpty()) {
				LOGGER.info("in takeActionsPostMessageReceived for dealerID={} message_uuid={}", postMessageReceived.getDealerID(), postMessageReceived.getMessageUUID());

				postMessageReceivedImpl.takePostMessageReceivedActions(postMessageReceived.getMessageUUID(), postMessageReceived.getDealerID(), postMessageReceived.getDepartmentUUID());
			}

		} catch(Exception e) {
    		LOGGER.error("error in takeActionsPostMessageReceived for message_payload={}", messagePayload, e);
    		throw e;
    	}
	}

	public void updateRecordingURLForMessage(String messagePayload) throws Exception {

		InputStream audioInputStream = null;
		String recordingSID = null;
		String uploadUrl = null;
		String updatedMessageBody = null;
		String tempTwilioUrl=null;
		Date receivedOn = new Date();
		try {

			RecordingURLMessageUpdateRequest updateRecordingURLForMessage = objectMapper.readValue(messagePayload, RecordingURLMessageUpdateRequest.class);
			Long messageID = updateRecordingURLForMessage.getMessageID();
			Long dealerID = updateRecordingURLForMessage.getDealerID();
			Boolean verifyRecording = updateRecordingURLForMessage.getVerifyRecording();
			Boolean deleteRecording = updateRecordingURLForMessage.getDeleteRecordings();
			LOGGER.info("delete_recordings={} verifyRecording={} for message_id={} ", deleteRecording, verifyRecording, messageID);
			if(verifyRecording!=null && verifyRecording) {
				verifyRecordingMigratedSuccesfullyToS3(messageID);
			}
			else {

				List<Object[]> messagePropertyResult = messageRepository.fetchMessageBodyAndReceivedOnForGivenMessageID(messageID);
				String messageBody = (String) messagePropertyResult.get(0)[1];
			    try {
					receivedOn = (Date) messagePropertyResult.get(0)[0];
			    }
			    catch(Exception e) {
			    	LOGGER.warn("unable to parse received on date for message_id={} dealer_id={} setting date to current date",messageID, dealerID);
			    }
			    if(receivedOn==null) {
			    	LOGGER.warn("receivedOn date does not exist for message_id={} dealer_id={}, setting it to current date",messageID, dealerID);
			    	receivedOn = new Date();
			    }
			    LOGGER.info("received date={} for message_id={} dealer_id={}",receivedOn, messageID, dealerID);
			    String recordingUrl = kCommunicationsUtils.getRecordingURLForMessageBody(messageBody);
			    if(recordingUrl!=null && !recordingUrl.equals("null") && !recordingUrl.isEmpty() && recordingUrl.contains(TWILIO_RECORDING_BASE_URL)) {

					try {
			    		tempTwilioUrl = generalRepository.getRecordingUrlFromTempTable(messageID);
			    		if(tempTwilioUrl==null) {
			    			generalRepository.insertInRecordingTemp(messageID, recordingUrl, dealerID);
			    			LOGGER.info("succesfully inserted in recordingTemp recording_url={} for message_id={} and dealer_id={}",recordingUrl, messageID, dealerID);
			    		}
			    		else {
			    			LOGGER.info("twilio_url={} already present in RecordingTemp for message_id={} dealer_id={}", tempTwilioUrl, messageID, dealerID);
			    		}
			    	}
			    	catch(Exception e){
			    		LOGGER.error("unable to insert in Recording temp for message_id={} dealer_id={}",messageID, dealerID);
			    		throw e;
			    	}
			    	long t1 = System.currentTimeMillis();
			    	try {

						audioInputStream = kCommunicationsUtils.getInputSreamOfAudio(recordingUrl);
			    		 long t2 = System.currentTimeMillis();
			    		 LOGGER.info("time_taken={} to fetch audio strem for message_id={} dealer_id={} recording_url={}",t2-t1,messageID, dealerID, recordingUrl);
			    	}
			    	catch(java.io.FileNotFoundException e) {
			    		LOGGER.warn("can not get audio stream for recording_url={} as audio does not exist on twilio for message_id={} dealer_id={}", recordingUrl, messageID, dealerID, e);
			    		return;
			    	}
			    	catch(Exception e) {
			    		LOGGER.error("can not get audio stream for recording_url={} message_id={} dealer_id={}", recordingUrl, messageID, dealerID, e);
			    	}

					try {
			    		recordingSID = kCommunicationsUtils.getRecordingSID(recordingUrl);
			    	}
			    	catch (Exception e) {
			    		LOGGER.error("can not get recording sid for recording_url={} message_id={} dealer_id={}", recordingUrl, messageID, dealerID, e);
			    		throw e;
			    	}

					LOGGER.info("uploading recording to s3 for recording_url={} and message_id={} dealer_id={} and recording_sid={}",recordingUrl, messageID, dealerID, recordingSID);
			    	long t3 = System.currentTimeMillis();
			    	try {
			    		uploadUrl = awsClientUtil.uploadMediaToAWSS3(audioInputStream, recordingSID+RECORDING_EXTENSION, RECORDING_CONTENT_TYPE, receivedOn.getTime(), messageID, dealerID, false, COMMUNICATIONS_RECORDING_FOLDER_PREFIX);
			    		long t4 = System.currentTimeMillis();
			    		LOGGER.info("successfully uploadede recording to s3 for recording_url={} and message_id={} dealer_id={} and recording_sid={} aws_url={} time_taken={}",recordingUrl,messageID, dealerID, recordingSID, uploadUrl, t4-t3);
			    	}
			    	catch(Exception e) {
			    		long t4 = System.currentTimeMillis();
			    		LOGGER.error("error uploading recording to s3 for recording_url={} and message_id={} and recording_sid={} time_taken={}",recordingUrl,messageID,recordingSID,t4-t3,e);
			    		throw e;
			    	}

					try {
			    		updatedMessageBody = kCommunicationsUtils.getUpdatedRecordingurlinMessageBody(messageBody, uploadUrl);
			    	}
			    	catch(Exception e) {
			    		LOGGER.error("Unable to parse and update recording_url={} for message_body={} message_id={}",recordingUrl, messageBody, messageID,e);
			    		throw e;
			    	}

					LOGGER.info("updating MessageExtn and Voice Call table with s3_url={} for message_id={} dealer_id={} and old recording_url={}",uploadUrl, messageID, dealerID, recordingUrl);

					try {

						generalRepository.updateVoiceCall(messageID, updatedMessageBody, uploadUrl);
			    		generalRepository.updateMessageExtn(messageID, updatedMessageBody, uploadUrl);
			    		generalRepository.updateRecordingTemp(messageID, uploadUrl);

					}
			    	catch(Exception e) {
			    		LOGGER.error("unable to update MessageExtn and Voice Call table with s3_url={} for message_id={} dealer_id={} and old recording_url={}",uploadUrl, messageID, dealerID, recordingUrl,e);
			    		throw e;
			    	}
			    	LOGGER.info("successfully updated MessageExtn and Voice Call table with s3_url={} for message_id={} dealer_id={} and old recording_url={}",uploadUrl, messageID, dealerID, recordingUrl);

					try {
			    		verifyRecordingForMessage(messageID, dealerID);
			    	}
			    	catch(Exception e) {
			    		LOGGER.error("verification failed for message_id={} dealer_id={} twilio_url={} aws_url={}" ,messageID, dealerID, uploadUrl, recordingUrl);
			    	}
			    	if(deleteRecording) {
				    	try {
				    		twilioClientUtil.deleteRecordingsFromTwilio(recordingUrl, messageID);
				    	}
				    	catch(Exception e) {
				    		LOGGER.error("unable to process delete request for recording_url={} and message_id={} recordingSID={}", recordingUrl, messageID, recordingSID, e);
				    	}
			    	}
			    }
			    else if(recordingUrl!=null && !recordingUrl.equals("null") && !recordingUrl.isEmpty() && recordingUrl.contains(cfURLPrefix) && deleteRecording) {
			    	LOGGER.info("delete recordings for dealer_id={} message_id={} aws_url={}", dealerID, messageID, recordingUrl);
			    	tempTwilioUrl = generalRepository.getRecordingUrlFromTempTable(messageID);
			    	if(tempTwilioUrl!=null && !tempTwilioUrl.isEmpty() ) {
				    	try {
				    		Boolean audioExistsOnTwilio = kCommunicationsUtils.checkIfAudioExistsOrNot(tempTwilioUrl);
				    		if(audioExistsOnTwilio) {
				    			LOGGER.info("valid twilio_url={} trying to delete from twilio for message_id={} dealer_id={}", tempTwilioUrl, messageID, dealerID);
					    		twilioClientUtil.deleteRecordingsFromTwilio(tempTwilioUrl, messageID);
					    		LOGGER.info("deleted recording for dealer_id={} message_id={} aws_url={} twilio_url={}", dealerID, messageID, recordingUrl, tempTwilioUrl);
				    		}
				    		else {
				    			LOGGER.info("url present in db but recording does not exist on twilio twilio_url={} message_id={} dealer_id={}", tempTwilioUrl, messageID, dealerID);
				    		}
				    	}
				    	catch(org.springframework.web.client.HttpClientErrorException e) {
				    		LOGGER.error("unable to delete recording for dealer_id={} message_id={} aws_url={} twilio_url={} as recording has already been deleted", dealerID, messageID, recordingUrl, tempTwilioUrl,e);

						}
				    	catch(Exception e) {
				    		LOGGER.error("unable to delete recording for dealer_id={} message_id={} aws_url={} twilio_url={}", dealerID, messageID, recordingUrl, tempTwilioUrl,e);
							throw e;
						}
					} else {
						LOGGER.info("twiliourl not present for message_id={} dealer_id={} in Recording temp table", messageID, dealerID);
					}
				} else {
					LOGGER.info("recording does not exist on twilio for message_id={} dealer_id={}", messageID, dealerID);
				}

			}
		}
		catch (Exception e) {
			LOGGER.error("Error in processing update recording url for messagePayLoad={} ", messagePayload, e);
			throw e;
		}

	}

	private void verifyRecordingForMessage(Long messageID, Long dealerID) {

		Object[] recordingTempAttr = generalRepository.getRecordingTempAttributesForAMessage(messageID);
		String messageBody = messageExtnRepository.findByMessageId(messageID);
	    String recordingUrlFromMessageExtn = kCommunicationsUtils.getRecordingURLForMessageBody(messageBody);
	    String twilioURL = null;
	    String awsURL =null;
	    String failureReason = "";
	    if(recordingTempAttr==null && (recordingUrlFromMessageExtn==null || recordingUrlFromMessageExtn.equals("null") || recordingUrlFromMessageExtn.isEmpty())){
	    	LOGGER.info("This is not a recorded call, no need for verification for message_id={}",messageID);
	    	return;
	    }

		if(recordingTempAttr!=null) {
		    twilioURL = (String) recordingTempAttr[1];
		    awsURL = (String) recordingTempAttr[2];
	    }
	    else {
	    	LOGGER.info("recordingTempArr not present for message_id={}",messageID);
	    }
	    failureReason = applyIfUrlsAreSame(twilioURL, awsURL, recordingUrlFromMessageExtn, messageID);
	    if(RULES_PASSED.equals(failureReason)) {
	    	failureReason = applyBothUrlsAreValidAndSameBytes(twilioURL, awsURL, recordingUrlFromMessageExtn, messageID);
	    }
		if(RULES_PASSED.equals(failureReason))
		{
			LOGGER.info("Rules passed for message_id={} msg_extn_recording_url={} aws_recording_url={} and twilio_url={}",messageID, recordingUrlFromMessageExtn, awsURL, twilioURL );
			return;
		}
		else {
			LOGGER.info("Rules failed for message_id={} msg_extn_recording_url={} aws_recording_url={} and twilio_url={} with failure_reason={}",messageID, recordingUrlFromMessageExtn, awsURL, twilioURL, failureReason );
			return;
		}

	}

	private String applyBothUrlsAreValidAndSameBytes(String twilioURL, String awsURL,
			String recordingUrlFromMessageExtn, Long messageID) {

		Long twilioBytes = 0l;
		Long awsBytes = 0l;

		try {
			twilioBytes = kCommunicationsUtils.getBytesFromAudio(twilioURL);
		}
		catch(Exception e) {
			LOGGER.error("applyBothUrlsAreValidAndSameBytes failed unable to get bytes for twilio_url={} message_id={}", twilioURL, messageID,e);
			return VerificationFailureReason.INVALID_TWILIO_BYTES.name();
		}

		try {
			awsBytes = kCommunicationsUtils.getBytesFromAudio(awsURL);
		}
		catch(Exception e) {
			LOGGER.error("applyBothUrlsAreValidAndSameBytes failed unable to get bytes for aws_url={} message_id={}", awsURL, messageID,e);
			return VerificationFailureReason.INVALID_AWS_BYTES.name();
		}
		LOGGER.info("aws_bytes={} for aws_url={} and twilio_bytes={} for twilio_url={}",awsBytes, awsURL, twilioBytes, twilioURL);
		if(!twilioBytes.equals(awsBytes)) {
			LOGGER.warn("applyBothUrlsAreValidAndSameBytes failed aws_bytes={} for aws_url={} and twilio_bytes={} for twilio_url={}",awsBytes, awsURL, twilioBytes, twilioURL);
			return VerificationFailureReason.BYTES_MISMATCH.name();
		}
		LOGGER.info("applyBothUrlsAreValidAndSameBytes passed for aws_bytes={} for aws_url={} and twilio_bytes={} for twilio_url={}",awsBytes, awsURL, twilioBytes, twilioURL);
		return RULES_PASSED;
	}


	public void sendMultipleMessages(String messagePayload) throws Exception {
		try {
			MultipleMessageSending multipleMessageSending = objectMapper.readValue(messagePayload, MultipleMessageSending.class);
			LOGGER.info("sendMultipleMessages message_payload={}", messagePayload);
			communicationsApiImpl.processMultiplemessageRequest(multipleMessageSending);
		} catch (Exception e) {
			LOGGER.error("Error in sendMultipleMessages for messagePayLoad={} ", messagePayload, e);
			throw e;
		}
	}

	private String applyIfUrlsAreSame(String twilioURL, String awsURL,
			String recordingUrlFromMessageExtn, Long messageID) {

		if(!checkEmptyString(recordingUrlFromMessageExtn) && !checkEmptyString(awsURL) && recordingUrlFromMessageExtn.equals(awsURL)) {

			LOGGER.info("applyIfUrlsAreSame passed for message_id={} and aws_recording_url={} and twilio_url={}",messageID, awsURL, twilioURL);
			return RULES_PASSED;
		}
		LOGGER.info("applyIfUrlsAreSame rule failed for message_id={} msg_extn_recording_url aws_recording_url={} and twilio_url={}",messageID, recordingUrlFromMessageExtn, awsURL, twilioURL);

		return VerificationFailureReason.AUDIO_URL_MISMATCH.name();

	}

	private Boolean checkEmptyString(String msg) {

		return (msg==null || msg.equals("null") || msg.isEmpty());
	}

	private void verifyRecordingMigratedSuccesfullyToS3(Long messageID) throws Exception {

		String recordingUrl = null;
		Integer duration = null;
		BigInteger callStatus = null;
		String callIdentifier = null;
		BigInteger dealerID = null;

		try {
			Object[] callData = generalRepository.getDataForCall(messageID);
			if(callData == null) {
				LOGGER.warn("call data did not save in VoiceCall table for message_id={} ", messageID);
				return;
			}
			recordingUrl = (String) callData[0];
			duration = (Integer) callData[1];
			callStatus = (BigInteger) callData[2];
			callIdentifier = (String) callData[3];
			dealerID = (BigInteger) callData[4];
		}
		catch(Exception e) {
			LOGGER.error("exception in processing for message_id={}", messageID, e);
			throw e;
		}

		applyRulesForMessage(recordingUrl, duration, callStatus, callIdentifier, dealerID, messageID);
		return;
	}

	private void applyRulesForMessage(String recordingUrl, Integer duration, BigInteger callStatus,
			String callIdentifier, BigInteger dealerID, Long messageID) throws Exception{

		String twilioUrl = null;
		try {
			LOGGER.info("request received to verify call recording for message_id={} dealer_id={} call_identifier={} recording_url={}", messageID, dealerID, callIdentifier, recordingUrl);
			if(checkEmptyString(recordingUrl) || VOICE_CALL.equalsIgnoreCase(recordingUrl)) {

				if(completedCall.equals(callStatus.longValue())) {

					addFailureLogForVerificationRules(VerificationFailureReason.NO_RECORDING_URL, duration, callStatus, recordingUrl, callIdentifier, messageID, dealerID.longValue());

				}
				else {
					LOGGER.info("Call not recorded because call was never completed for call_identifier={} message_id={} dealer_id={} call_status={}", callIdentifier
							, messageID, dealerID, callStatus);
				}
			}
			else if(recordingUrl.contains(TWILIO_RECORDING_BASE_URL)) {
				try {
					if(kCommunicationsUtils.checkIfAudioExistsOrNot(recordingUrl)) {
						addFailureLogForVerificationRules(VerificationFailureReason.AUDIO_NOT_MIGRATED_TO_S3, duration, callStatus, recordingUrl, callIdentifier, messageID, dealerID.longValue());
						pushMessageToUpdateRecordingQueue(messageID, dealerID.longValue(), true, false);
					}
					else {
						if(duration>=10) {
							addFailureLogForVerificationRules(VerificationFailureReason.RECORDING_DELETED, duration, callStatus, recordingUrl, callIdentifier, messageID, dealerID.longValue());
						}
						else {
							LOGGER.info("short call not recorded for call_identifier={} message_id={} dealer_id={} call_status={} duration={} recording_url={}", callIdentifier,
								messageID, dealerID, callStatus, duration, recordingUrl);
						}
					}
				}
				catch(Exception e) {
					LOGGER.error("Error checking if twilio_url is valid for message_id={} dealer_id={} call_identifier={} recording_url={} ", messageID, dealerID, callIdentifier, recordingUrl, e);
					throw e;
				}
			}
			else if(recordingUrl.contains(cfURLPrefix)) {
				try {
					if(!kCommunicationsUtils.checkIfAudioExistsOrNot(recordingUrl)) {
						addFailureLogForVerificationRules(VerificationFailureReason.INVALID_AWS_URL, duration, callStatus, recordingUrl, callIdentifier, messageID, dealerID.longValue());
					}
					else {
						LOGGER.info("audio successfuly migrated to s3 for message_id={} dealer_id={} call_identifier={} recording_url={} ", messageID, dealerID, callIdentifier, recordingUrl);
						twilioUrl = kCommunicationsUtils.fetchTwilioUrlForMessage(messageID);
						if(kCommunicationsUtils.checkIfAudioExistsOrNot(twilioUrl)) {
							addFailureLogForVerificationRules(VerificationFailureReason.AUDIO_EXISTS_ON_TWILIO, duration, callStatus, recordingUrl, callIdentifier, messageID, dealerID.longValue());
							LOGGER.info("since audio exists on twilio trying to delete it for message_id={} dealer_id={} call_identifier={} recording_url={} ", messageID, dealerID, callIdentifier, twilioUrl);
							deleteRecordingFromTwilio(twilioUrl, messageID);
						}
						else {
							LOGGER.info("audio deleted successfuly for message_id={} dealer_id={} call_identifier={} recording_url={}", messageID, dealerID, callIdentifier, recordingUrl);
						}
					}
				}
				catch(Exception e) {
					LOGGER.error("Error checking if aws_url is valid for message_id={} dealer_id={} call_identifier={} recording_url={} ", messageID, dealerID, callIdentifier, recordingUrl, e);
					throw e;
				}
			}
			else {
				LOGGER.info("unknown message_id={} dealer_id={} call_identifier={} recording_url={} ", messageID, dealerID, callIdentifier, recordingUrl);
			}
		}
		catch(Exception e) {
			LOGGER.error("error processing verifiction script for message_id={} dealer_id={} call_identifier={} recording_url={}", messageID, dealerID, callIdentifier, recordingUrl, e);
			throw e;
		}
		return;
	}

	public void removeMessagesFromDelayedFilter(String messagePayload) throws Exception{

		try{
			ObjectMapper objectMapper=new ObjectMapper();
			DelayedFilterRemovalRequest delayedFilterRemovalRequest = objectMapper.readValue(messagePayload, DelayedFilterRemovalRequest.class);
			List<Object[]> draftInfo = draftMessageMetaDataRepository.fetchMessageForGivenDraftStatusAndDealers(delayedFilterRemovalRequest.getDraftStatus(), delayedFilterRemovalRequest.getFromDealerID(),
					delayedFilterRemovalRequest.getToDealerID(), delayedFilterRemovalRequest.getBatchSize(), delayedFilterRemovalRequest.getOffset());
			if(draftInfo==null || draftInfo.isEmpty()){
				return;
			}
			HashMap<Long,Long> messageIDDealerIDMap=new HashMap<Long,Long> ();
			for(Object[] draftTemp:draftInfo){
				Long messageID =null;
				try {
					messageID = ((BigInteger)draftTemp[0]).longValue();
					Long dealerID = ((BigInteger)draftTemp[1]).longValue();
					messageIDDealerIDMap.put(messageID, dealerID);
				} catch(Exception e) {
					LOGGER.error("error in removeMessagesFromDelayedFilter for message_id={} from_dealer_id={} to_dealer_id={} offset={}", messageID,delayedFilterRemovalRequest.getFromDealerID(),
							delayedFilterRemovalRequest.getToDealerID(),delayedFilterRemovalRequest.getOffset(), e);
				}

			}
			if(messageIDDealerIDMap!=null){
				FilterDataRemovalRequest filterDataRemovalRequest=new FilterDataRemovalRequest();
				filterDataRemovalRequest.setMessageIdDealerIdMap(messageIDDealerIDMap);
				filterDataRemovalRequest.setCollectionName(DELAYED_COLLECTION);
				messagingViewControllerHelper.updateFilterData(filterDataRemovalRequest);
			}
			if(draftInfo!=null && delayedFilterRemovalRequest.getBatchSize().equals(draftInfo.size())){
				LOGGER.info("in removeMessagesFromDelayedFilter request_object={} draft_message_list_size={} ",objectMapper.writeValueAsString(delayedFilterRemovalRequest),
						draftInfo.size());

				Long offset=delayedFilterRemovalRequest.getOffset();
				offset+=delayedFilterRemovalRequest.getBatchSize();
				delayedFilterRemovalRequest.setOffset(offset);
				rabbitHelper.pushDataForDelayedFilterRemoval(delayedFilterRemovalRequest);
			}
		}catch(Exception e){
			LOGGER.error("error in removeMessagesFromDelayedFilter for message_payload={}", messagePayload, e);
			throw e;
		}

	}

	private void deleteRecordingFromTwilio(String twilioUrl, Long messageID) {

		try {
			twilioClientUtil.deleteRecordingsFromTwilio(twilioUrl, messageID);
			LOGGER.info("successfully deleted recording for message_id={} dealer_id={} call_identifier={} recording_url={} ", messageID,twilioUrl);
		}
		catch(Exception e) {
			LOGGER.error("could not delete audio for message_id={} dealer_id={} call_identifier={} recording_url={} ", messageID, twilioUrl);
		}

	}

	private void addFailureLogForVerificationRules(VerificationFailureReason noRecordingUrl, Integer duration, BigInteger callStatus, String recordingUrl, String callIdentifier, Long messageID, Long dealerId) {

		LOGGER.info("rules to verify call recorded and migrated failed for message_id={} dealer_id={} call_identifier={} recording_url={} call_status={} duration={} due to  failure_reason={}",
			messageID, dealerId, callIdentifier, recordingUrl, callStatus, duration, noRecordingUrl.name());

	}

	public void mailCustomerThread(String messagePayload) throws Exception {
		String customerUUID = null;
		String departmentUUID = null;
		CommunicationHistoryRequest commHistoryRequest = null;
		CommunicationHistoryMailRequest communicationHistoryMailRequest = objectMapper.readValue(messagePayload, CommunicationHistoryMailRequest.class);

		try {
			customerUUID = communicationHistoryMailRequest.getCustomerUUID();
			departmentUUID = communicationHistoryMailRequest.getDepartmentUUID();
			commHistoryRequest = communicationHistoryMailRequest.getCommHistoryRequest();
			ResponseEntity<CommunicationHistoryResponse> commHistoryResponse  =  threadPrintingHelper.getCommunicationHistory(departmentUUID, customerUUID, commHistoryRequest);
			if(commHistoryResponse!=null && commHistoryResponse.getBody()!=null && commHistoryResponse.getBody().getErrors()!=null && commHistoryResponse.getBody().getErrors().size() > 0){
				if(!com.mykaarma.kcommunications_model.enums.ErrorCode.FILE_DELETION_FAILED.name().equalsIgnoreCase(commHistoryResponse.getBody().getErrors().get(0).getErrorDescription())){
					throw new Exception(commHistoryResponse.getBody().getErrors().get(0).getErrorDescription());
				}
				else {
					LOGGER.info("file deletion failed after mailing not rtrying for customer_uuid={}, department_uuid={} ", customerUUID, departmentUUID);
				}
			}
		}
		catch(Exception e) {
			LOGGER.error("unable to mail CustomerThread for customer_uuid={} department_uuid={} communicationHistoryRequest={} ", customerUUID
					, departmentUUID, objectMapper.writeValueAsString(commHistoryRequest), e);
			throw e;
		}
	}

	public void fetchCustomersForDealer(String messagePayLoad) throws Exception{

		ObjectMapper objectMapper = new ObjectMapper();
		FetchCustomersDealer fetchCustomersDealer = objectMapper.readValue(messagePayLoad, FetchCustomersDealer.class);
		List<BigInteger> customerIds = null;
		Long dealerId = fetchCustomersDealer.getDealerId();
		Long offSet = fetchCustomersDealer.getOffSet();
		Long batchSize = fetchCustomersDealer.getBatchSize();
		LOGGER.info("received request to fetch customers for dealer_id={} off_Set={} batch_Size={}", dealerId, offSet, batchSize);
		try {

			customerIds = generalRepository.fetchCustomersForDealer(dealerId, offSet, batchSize);
			if(customerIds!=null && customerIds.size()>0) {
				LOGGER.info("pushing Data To Update Customer Subscriptions queue customer_ids={} for dealer_id={} ",customerIds.size(),dealerId);
				pushDataToUpdateCustomerSubscriptions(customerIds, dealerId);
				if(customerIds.size() == batchSize) {

					LOGGER.info("pushing to fetch Customers queue for dealer_id={} off_Set={} batch_Size={}", dealerId, offSet, batchSize);
					pushDataToFetchCustomersForDealer(offSet, batchSize, dealerId);

				}
				else {
					LOGGER.info("all customers found for dealer_id={} off_Set={}", dealerId, offSet);
				}
			}
			else {
				LOGGER.info("no customers found for dealer_id={}", dealerId);
			}

		}
		catch(Exception e) {

			LOGGER.error("error while fetching customers for dealer_id={} and pushing to queue ", dealerId);
			throw e;
		}
	}

	private void pushDataToFetchCustomersForDealer(Long offSet, Long batchSize, Long dealerId) throws Exception{

		Long newOffSet = null;
		newOffSet = offSet + batchSize;
		FetchCustomersDealer fetchCustomersDealer = new FetchCustomersDealer();
		fetchCustomersDealer.setDealerId(dealerId);
		fetchCustomersDealer.setBatchSize(batchSize);
		fetchCustomersDealer.setOffSet(newOffSet);
		rabbitHelper.pushDatatoSubscriptionUpdateForDealer(fetchCustomersDealer);


	}

	private void pushDataToUpdateCustomerSubscriptions(List<BigInteger> customerIds, Long dealerId) throws Exception{

		for(BigInteger customer: customerIds) {
			CustomerSubscriptionsUpdate customerSubscriptionsDelete = new CustomerSubscriptionsUpdate();
			customerSubscriptionsDelete.setCustomerId(customer.longValue());
			rabbitHelper.pushDataToDeleteCustomerSubscriptionsQueue(customerSubscriptionsDelete);
			LOGGER.info("pushing customer_id={} dealer_id={} for further processing", customer, dealerId);
		}

	}

	public void updateCustomerSubscribers(String messagePayLoad) throws Exception{

		ObjectMapper objectMapper = new ObjectMapper();
		CustomerSubscriptionsUpdate customerSubscriptionsDelete = objectMapper.readValue(messagePayLoad, CustomerSubscriptionsUpdate.class);
		Long customerId = customerSubscriptionsDelete.getCustomerId();
		List<Object[]> threadOwnerList = null;
		BigInteger threadOwnerId = null;
		BigInteger dealerDepartmentId = null;

		try {
			threadOwnerList = threadRepository.findDealerAssociateIdAndDealerDepartmentIdByCustomerId(customerId);
			LOGGER.info("thread_owner_id={} for customer_id={}", threadOwnerId, customerId);
			if(threadOwnerList != null && threadOwnerList.size()>0) {

				for(Object[] thread: threadOwnerList) {
					threadOwnerId = (BigInteger) thread[0];
					dealerDepartmentId = (BigInteger) thread[1];
					LOGGER.info("calling update Subscriptions for customer_id={} dealer_department_id={} thread_owner_id={}", customerId, dealerDepartmentId, threadOwnerId);
					messagingViewControllerHelper.updateSubscriptionsForCustomer(customerId, threadOwnerId.longValue(), dealerDepartmentId.longValue());
					LOGGER.info("subscriptions removed for customer_id={} thread_owner_id={}", customerId, threadOwnerId);
				}
			}
			else {
				LOGGER.info("no thread for customer_id={}", customerId);
			}
		}
		catch(Exception e) {
			LOGGER.error("error removing customer subscriptions for customer_id={} thread_owner_id={}", customerId, threadOwnerId, e);
			throw e;
		}
	}
	
	public void indexTemplate(String messagePayload) throws Exception {
		try {
            LOGGER.info("in indexTemplate received request={}", messagePayload);
            ObjectMapper om = new ObjectMapper();
            TemplateIndexingRequest templateIndexingRequest = om.readValue(messagePayload, TemplateIndexingRequest.class);
            templateImpl.indexTemplate(templateIndexingRequest.getTemplateType(), templateIndexingRequest.getTemplateUuid());
            LOGGER.info("in indexTemplate successfully indexed for request={}", messagePayload);
        } catch(Exception e) {
            LOGGER.error("error in indexTemplate for request={}", messagePayload, e);
            throw e;
        }
	}

    public void predictPreferredCommunicationModeReceiver(String messagePayload) throws Exception {
        try {
            LOGGER.info("in predictPrefferedCommunicationModeReceiver received request={}", messagePayload);
            ObjectMapper om = new ObjectMapper();
            PreferredCommunicationModePrediction preferredCommunicationModePrediction = om.readValue(messagePayload, PreferredCommunicationModePrediction.class);
            String customerUUID = preferredCommunicationModePrediction.getCustomerUUID();
            String departmentUUID = preferredCommunicationModePrediction.getDepartmentUUID();
            Message message = preferredCommunicationModePrediction.getMessage();
			preferredCommunicationModeImpl.predictPreferredCommunicationMode(departmentUUID, customerUUID, message);
        } catch(Exception e) {
            LOGGER.error("error in predictPrefferedCommunicationModeReceiver for request={}", messagePayload, e);
            throw e;
        }
    }

	public void verifyCommunicationsBillingTwilio(String messagePayLoad) throws Exception{

		verificationService.verifyCommunicationsBillingTwilio(messagePayLoad);

	}

	public void updateOptOutStatusReceiver(String messagePayload) throws Exception {
        try {
            LOGGER.info("in updateOptOutStatusReceiver received request={}", messagePayload);
            ObjectMapper om = new ObjectMapper();
            OptOutStatusUpdate optOutStatusUpdate = om.readValue(messagePayload, OptOutStatusUpdate.class);
            optOutImpl.updateOptOutStatus(optOutStatusUpdate);
        } catch(Exception e) {
            LOGGER.error("error in updateOptOutStatusReceiver for request={}", messagePayload, e);
            throw e;
        }
    }

	public void postOptOutStatusUpdateReceiver(String messagePayload) throws Exception {
        try {
            LOGGER.info("in postOptOutStatusUpdateReceiver received request={}", messagePayload);
            ObjectMapper om = new ObjectMapper();
            PostOptOutStatusUpdate postOptOutStatusUpdate = om.readValue(messagePayload, PostOptOutStatusUpdate.class);
            postOptOutImpl.postOptOutStatusUpdate(postOptOutStatusUpdate);
        } catch(Exception e) {
            LOGGER.error("error in postOptOutStatusUpdateReceiver for request={}", messagePayload, e);
            throw e;
        }
    }

	public void deployDoubleOptinReceiver(String messagePayload) throws Exception {
        try {
            LOGGER.info("in deployDoubleOptinReceiver received request={}", messagePayload);
            ObjectMapper om = new ObjectMapper();
            DoubleOptInDeployment doubleOptInDeployment = om.readValue(messagePayload, DoubleOptInDeployment.class);
            optOutImpl.deployDoubleOptIn(doubleOptInDeployment);
        } catch(Exception e) {
            LOGGER.error("error in deployDoubleOptinReceiver for request={}", messagePayload, e);
            throw e;
        }
    }

	public void optinAwaitingMessageExpireReceiver(String messagePayload) throws Exception {
		try {
			LOGGER.info("in optinAwaitingMessageExpireReceiver received request={}", messagePayload);
			ObjectMapper om = new ObjectMapper();
			OptInAwaitingMessageExpire optInAwaitingMessageExpire = om.readValue(messagePayload, OptInAwaitingMessageExpire.class);
			sendMessageHelper.expireOptInAwaitingMessage(optInAwaitingMessageExpire);
		} catch (Exception e) {
			LOGGER.error("error in optinAwaitingMessageExpireReceiver for request={}", messagePayload, e);
			throw e;
		}
	}

	public void saveHistoricalCommunications(String messagePayLoad) throws Exception {

		LOGGER.info("request received to save historical communications={}", messagePayLoad);
		SaveHistoricalMessageRequest saveHistoricalMessageRequest = objectMapper.readValue(messagePayLoad, SaveHistoricalMessageRequest.class);
		String customerUuid = saveHistoricalMessageRequest.getCustomerUuid();
		String departmentUuid = saveHistoricalMessageRequest.getDepartmentUuid();
		SaveMessageRequest saveMessageRequest = saveHistoricalMessageRequest.getSaveMessageRequest();
		String callBackPathUrl = saveHistoricalMessageRequest.getSaveMessageRequest().getCallBackPathUrl();
		CustomerWithVehiclesResponse customerWithVehicles = null;
		Customer customer = saveHistoricalMessageRequest.getCustomer();
		List<ApiError> apiErrors = new ArrayList<ApiError>();
		String prefferedTextValue = null;
		String prefferedEmailValue = null;
		Long departmentId = null;
		Long dealerId = null;
		String dealerUuid = null;
		Long threadId = saveHistoricalMessageRequest.getThreadID();
		Boolean logInMongo = saveHistoricalMessageRequest.getLogInMongo();
		GetDealerAssociateResponseDTO dealerAssociate = null;
		Message message = null;
		MessageExtn messageExtn = null;
		MessageAttributes messageAttributes = saveMessageRequest.getMessageAttributes();
		String prefferedCommValue = null;
		String userUuid = saveMessageRequest.getUserUuid();
		String dealerAssociateUuid = null;
		com.mykaarma.kcommunications_model.enums.MessageType messageType = messageAttributes.getType();
		MessageProtocol messageProtocol = messageAttributes.getProtocol();
		String sourceUuid = saveMessageRequest.getSourceUuid();
		SaveMessageResponse saveMessageResponse = new SaveMessageResponse();
		Boolean messageAlreadySaved = false;
		String savedMessageUuid = null;

		try {

			saveMessageResponse.setSourceUuid(sourceUuid);
			saveMessageResponse.setMessageUuid(saveMessageRequest.getMessageUuid());
			dealerId = generalRepository.getDealerIDFromDepartmentUUID(departmentUuid);
			departmentId = generalRepository.getDepartmentIDForUUID(departmentUuid);
			dealerUuid = generalRepository.getDealerUUIDFromDealerId(dealerId);

			prefferedTextValue = helper.getPreferredCommunicationValueForProtocolNoOk(customer, MessageProtocol.TEXT);
			prefferedEmailValue = helper.getPreferredCommunicationValueForProtocolNoOk(customer, MessageProtocol.EMAIL);

			LOGGER.info("prefferedTextValue={} prefferedEmailValue={} for customer_uuid={} source_uuid={}", prefferedTextValue, prefferedEmailValue,
				customerUuid, saveMessageRequest.getSourceUuid());

			savedMessageUuid = saveMessageRedisService.getHistoricalMessage(sourceUuid);
			LOGGER.info("savedMessageUuid={} for source_uuid={}", savedMessageUuid, sourceUuid);
			if (savedMessageUuid != null) {
				try {
					message = messageRepository.findByuuid(savedMessageUuid);
					if (message != null) {
						messageExtn = messageExtnRepository.findByMessageID(message.getId());
						message.setMessageExtn(messageExtn);
					}
					messageAlreadySaved = true;
				} catch (Exception e) {
					LOGGER.error("error fetching message for uuid={}", saveMessageRequest.getMessageUuid());
				}
			}
			if (message == null) {
				try {

					dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUuid, userUuid);
					dealerAssociateUuid = dealerAssociate.getDealerAssociate().getUuid();
					LOGGER.info("dealer_associate_id={} for source_uuid={} user_uuid={} department_uuid={}", dealerAssociate.getDealerAssociate().getId(), sourceUuid, userUuid,
							departmentUuid);
				}
				catch(Exception e) {
					
					checkExpirationAndSendCallBack(saveHistoricalMessageRequest.getExpiration(), saveMessageResponse, callBackPathUrl,
							ErrorCode.DEALER_ASSOCIATE_NOT_FOUND.name(), "dealer associate not found");
					LOGGER.error("unable to fetch dealerAssociate for source_uuid={} customer_uuid={} dealer_associate_uuid={} user_uuid={}"
							+ " department_uuid={} skipping further processing", 
							saveMessageRequest.getSourceUuid(), customerUuid, dealerAssociateUuid, userUuid, departmentUuid, e);
					throw e;
				}

				if (MessageProtocol.TEXT.equals(messageProtocol) || MessageProtocol.VOICE_CALL.equals(messageProtocol)) {

					prefferedCommValue = prefferedTextValue;

					if (prefferedTextValue == null || prefferedTextValue.isEmpty()) {
						saveMessageResponse.setErrors(Arrays.asList(new ApiError(ErrorCode.INVALID_COMMUNICATION_VALUE.name(), "No text value found for text message")));
						helper.sendResponseToCallBack(callBackPathUrl, saveMessageResponse);
						LOGGER.error("prefferedTextValue is null for source_uuid={} customer_uuid={} but messageProtocol={} skipping further processing",
							saveMessageRequest.getSourceUuid(), customerUuid, messageProtocol);
						return;
					}

				} else if (MessageProtocol.EMAIL.equals(messageProtocol)) {
					prefferedCommValue = prefferedEmailValue;
					if (prefferedEmailValue == null || prefferedEmailValue.isEmpty()) {
						saveMessageResponse.setErrors(Arrays.asList(new ApiError(ErrorCode.INVALID_COMMUNICATION_VALUE.name(), "No email value found for customer")));
						helper.sendResponseToCallBack(callBackPathUrl, saveMessageResponse);
						LOGGER.error("prefferedEmailValue is null for source_uuid={} customer_uuid={} but messageProtocol={} skipping further processing",
							saveMessageRequest.getSourceUuid(), customerUuid, messageProtocol);
						return;
					}
				}

				try {

					message = convertToJpaEntity.getMessageJpaEntityForSaveMessageRequest(saveMessageRequest, customer,
						dealerAssociate.getDealerAssociate(), prefferedCommValue);

					LOGGER.info("message created={}", objectMapper.writeValueAsString(message));
					message = saveMessageHelper.saveMessage(message);
					saveMessageRequest.setMessageUuid(message.getUuid());
					saveMessageResponse.setMessageUuid(message.getUuid());
					saveMessageRedisService.pudhHistoricalMessage(sourceUuid, message.getUuid());
				} catch (Exception e) {

					checkExpirationAndSendCallBack(saveHistoricalMessageRequest.getExpiration(), saveMessageResponse, callBackPathUrl,
						ErrorCode.MESSAGE_CREATE_FAILED.name(), "error saving message details in database");
					LOGGER.error("exception converting and saving message for source_uuid={} customer_uuid={}", saveMessageRequest.getSourceUuid(),
						customerUuid, e);
					throw e;
				}
			} else {
				saveMessageResponse.setMessageUuid(savedMessageUuid);
				LOGGER.info("message already exists for this request, just need to hanlde postmessage for message_uuid={}"
					+ " source_uuid={} customer_uuid={}", message.getUuid(), sourceUuid, customerUuid);
			}
			try {
				threadId = postMessageSendingHelper.postMessageSaveHandler(message, threadId, logInMongo);
				LOGGER.info("processing successful for source_uuid={} message_uuid={}", sourceUuid, message.getUuid());
				helper.sendResponseToCallBack(callBackPathUrl, saveMessageResponse);
			} catch (Exception e) {

				checkExpirationAndSendCallBack(saveHistoricalMessageRequest.getExpiration(), saveMessageResponse, callBackPathUrl,
					ErrorCode.THREAD_UPDATE_FAILED.name(), "Error occured post saving message");
				LOGGER.error("thread save event failed for source_uuid={} customer_uuid={}", saveMessageRequest.getSourceUuid(), customerUuid, e);
				throw e;
			}

		}
		catch(Exception e) {
			
			LOGGER.error("processing failed for message_uuid={} customer_uuid={} source_uuid={}", message!=null ? message.getUuid(): "", customerUuid, sourceUuid, e);
			throw e;
		}

	}

	private void checkExpirationAndSendCallBack(Integer expiration, SaveMessageResponse saveMessageResponse, String callBackPathUrl,
												String errorCode, String errorDescription) throws Exception {

		try {
			if (expiration != null && expiration >= Math.pow(multiplier, maximumretries - 2) * 1000) {
				LOGGER.info("sending callback as message processing done successfully");
				saveMessageResponse.setErrors(Arrays.asList(new ApiError(errorCode, errorDescription)));
				helper.sendResponseToCallBack(callBackPathUrl, saveMessageResponse);
			}
		} catch (Exception e) {
			LOGGER.error("error while comparing expiration and sending callback", e);
		}
	}
	
	
	public void receiveMessageFromGlobalDealerOrderUpdatesQueue(String payload) throws Exception {
		try {
			LOGGER.info(" payload={} received in receiveMessageFromGlobalDealerOrderUpdatesQueue ", new ObjectMapper().writeValueAsString(payload));
			ObjectMapper mapper = new ObjectMapper();
			GlobalOrderTransitionDTO orderEvent = mapper.readValue(payload, GlobalOrderTransitionDTO.class);

			processDealerOrderUpdate(orderEvent);
		} catch (Exception e) {
			LOGGER.error(" payload={} Exception while processing global dealer order updates in receiveMessageFromGlobalDealerOrderUpdatesQueue ", new ObjectMapper().writeValueAsString(payload), e);
			throw e;
		}
	}

	private void processDealerOrderUpdate(GlobalOrderTransitionDTO orderEvent) throws Exception{
		if(!toProcessGlobalEvent(orderEvent)){
			return;
		}
		try{
			
			String dealerAssociateUUID = orderEvent.getOrder().getHeader().getDealerAssociateUuid();
			Long dealerAssociateID = generalRepository.getDealerAssociateIDForUUID(dealerAssociateUUID);
			String dealerAssociateName = generalRepository.getDealerAssociateName(dealerAssociateID);
			
			String customerUUID=orderEvent.getOrder().getCustomer().getUuid();

			String template = THREAD_OWNERSHIP_CHANGED_EVENT_NOTE_TEMPLATE;
			Long dealerID = generalRepository.getDealerIDFromDepartmentUUID(orderEvent.getDepartmentUuid());
            String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
            String locale = helper.getDealerPreferredLocale(dealerUUID);
            String messageBody = uiElementTranslationRepository.getTranslatedText(template, locale);
            
            Map<String, Object> templateParams = new HashMap<String, Object>();
            templateParams.put(DEALER_ASSOCIATE_NAME, dealerAssociateName);
            templateParams.put(ORDER_NUMBER, orderEvent.getOrder().getHeader().getOrderNumber());
            
            messageBody = TemplateEngine.getCompiledTemplate(templateParams, messageBody);
			
			systemNotificationHelper.saveThreadOwnershipChangedNote(orderEvent.getDepartmentUuid(), messageBody, customerUUID, dealerAssociateID, EventName.THREAD_DELEGATED.name(), MessagePurpose.THREAD_OWNERSHIP_CHANGED_NOTIFICATION);
			
		} catch(Exception e){
			LOGGER.error("error in processDealerOrderUpdate processing order status"
					+ " udpate for thread ownership changed event order_data={}",new ObjectMapper().writeValueAsString(orderEvent),e);
		} 
	}
	
	private boolean toProcessGlobalEvent(GlobalOrderTransitionDTO orderEvent) throws Exception {	
		if((orderEvent!=null && orderEvent.getOldOrder()==null && orderEvent.getOrder()!=null
				&& OrderStatus.OPEN.getOrderStatus().equalsIgnoreCase(orderEvent.getOrder().getHeader().getStatus()))){
			return true;
		} else {
			return false;	
		}
		
	}	
	
	public void sendMessageWithoutCustomer(String messagePayload) {
		try {
			ExternalMessage message = objectMapper.readValue(messagePayload, ExternalMessage.class);
			LOGGER.info("message consumed from the queue for sendMessageWithoutCustomer with payload = {} ", messagePayload);
			sendMessageHelper.sendTextMessageWithoutCustomer(message);
		} catch (Exception e) {
			LOGGER.error("Error in sendMessageWithoutCustomer for messagePayLoad={} ", messagePayload, e);
		}
	}

	public void postIncomingBotMessageSave(String messagePayload) throws Exception {
		try {
			PostIncomingBotMessageSave message = objectMapper.readValue(messagePayload, PostIncomingBotMessageSave.class);
			LOGGER.info("message consumed from the queue for postIncomingBotMessageSave with payload = {} ", messagePayload);
			forwardedAndBotMessageImpl.postIncomingBotMessageSave(message);
		} catch (Exception e) {
			LOGGER.error("Error in postIncomingBotMessageSave for messagePayLoad={} ", messagePayload, e);
			throw e;
		}
	}
}

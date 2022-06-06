package com.mykaarma.kcommunications.controller.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.CustomerSentiment;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.MessageKeyword;
import com.mykaarma.global.MessagePredictionFeedbackTypes;
import com.mykaarma.global.MessagePurpose;
import com.mykaarma.global.MessageType;
import com.mykaarma.global.NeedsResponsePredictionTypes;
import com.mykaarma.global.SentimentAnalysisPredictionTypes;
import com.mykaarma.kcommunications.exception.JPAException;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageExtnRepository;
import com.mykaarma.kcommunications.jpa.repository.MessagePredictionFeedbackRepository;
import com.mykaarma.kcommunications.jpa.repository.MessagePredictionRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.jpa.repository.PredictionFeatureRepository;
import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.model.api.DelayedFilterRemovalRequest;
import com.mykaarma.kcommunications.model.api.ErrorCodes;
import com.mykaarma.kcommunications.model.api.FailedMessagesRequest;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessagePrediction;
import com.mykaarma.kcommunications.model.jpa.MessagePredictionFeedback;
import com.mykaarma.kcommunications.model.jpa.PredictionFeature;
import com.mykaarma.kcommunications.model.jpa.Thread;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.rabbit.FetchCustomersDealer;
import com.mykaarma.kcommunications.model.rabbit.MultipleMessageSending;
import com.mykaarma.kcommunications.model.rabbit.PostMessageReceived;
import com.mykaarma.kcommunications.model.rabbit.SaveHistoricalMessageRequest;
import com.mykaarma.kcommunications.model.utils.CommunicationsVerification;
import com.mykaarma.kcommunications.model.utils.MessageComparatorByReceivedOn;
import com.mykaarma.kcommunications.mq.impl.KCommunicationsRequestProcessor;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.AWSClientUtil;
import com.mykaarma.kcommunications.utils.Actions;
import com.mykaarma.kcommunications.utils.ConvertToJpaEntity;
import com.mykaarma.kcommunications.utils.FilterHistory;
import com.mykaarma.kcommunications.utils.FilterName;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsException;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KCustomerApiHelperV2;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.KMessagingApiHelper;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications.utils.RabbitQueueInfo;
import com.mykaarma.kcommunications.utils.ReportingApiUtils;
import com.mykaarma.kcommunications.utils.ThreadPrintingHelper;
import com.mykaarma.kcommunications.utils.TwilioClientUtil;
import com.mykaarma.kcommunications_model.common.DealerMessagesFetchRequest;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.MultipleMessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.RecordingURLMessageUpdateRequest;
import com.mykaarma.kcommunications_model.dto.DocFileDTO;
import com.mykaarma.kcommunications_model.dto.MessageDTO;
import com.mykaarma.kcommunications_model.dto.MessagePredictionDTO;
import com.mykaarma.kcommunications_model.dto.MessagePredictionFeedbackDTO;
import com.mykaarma.kcommunications_model.enums.DraftStatus;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.enums.WarningCode;
import com.mykaarma.kcommunications_model.request.AutoCsiLogEventRequest;
import com.mykaarma.kcommunications_model.request.CommunicationCountRequest;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryMailRequest;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryRequest;
import com.mykaarma.kcommunications_model.request.CommunicationsBillingRequest;
import com.mykaarma.kcommunications_model.request.DeleteAttachmentFromS3Request;
import com.mykaarma.kcommunications_model.request.DeleteSubscriptionsRequest;
import com.mykaarma.kcommunications_model.request.MultipleMessageRequest;
import com.mykaarma.kcommunications_model.request.NotifierDeleteRequest;
import com.mykaarma.kcommunications_model.request.RecordingRequest;
import com.mykaarma.kcommunications_model.request.RecordingUpdateRequest;
import com.mykaarma.kcommunications_model.request.RecordingVerifyRequest;
import com.mykaarma.kcommunications_model.request.SaveMessageRequest;
import com.mykaarma.kcommunications_model.request.SaveMessageRequestList;
import com.mykaarma.kcommunications_model.request.SendDraftRequest;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcommunications_model.request.SubscriptionUpdateRequest;
import com.mykaarma.kcommunications_model.request.ThreadInWaitingForResponseQueueRequest;
import com.mykaarma.kcommunications_model.request.TwilioDealerIDRequest;
import com.mykaarma.kcommunications_model.request.UpdateCustomerSentimentStatusRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionFeedbackRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionFeedbackRequestNew;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionRequest;
import com.mykaarma.kcommunications_model.request.UploadAttachmentsToS3Request;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.CommunicationCountResponse;
import com.mykaarma.kcommunications_model.response.CommunicationHistoryResponse;
import com.mykaarma.kcommunications_model.response.DeleteAttachmentFromS3Response;
import com.mykaarma.kcommunications_model.response.GetDepartmentsUsingKaarmaTwilioURLResponse;
import com.mykaarma.kcommunications_model.response.MediaPreviewURLFetchResponse;
import com.mykaarma.kcommunications_model.response.FailedMessageResponse;
import com.mykaarma.kcommunications_model.response.NotifierDeleteResponse;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcommunications_model.response.SaveMessageListResponse;
import com.mykaarma.kcommunications_model.response.SaveMessageResponse;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;
import com.mykaarma.kcommunications_model.response.SendMultipleMessageResponse;
import com.mykaarma.kcommunications_model.response.SubscriptionsUpdateResponse;
import com.mykaarma.kcommunications_model.response.UpdateMessagePredictionFeedbackResponse;
import com.mykaarma.kcommunications_model.response.UpdateVoiceUrlResponse;
import com.mykaarma.kcommunications_model.response.UploadAttachmentResponse;
import com.mykaarma.kcustomer_model.lombokresponse.CustomerWithVehiclesResponse;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDepartmentResponseDTO;

@Service
public class CommunicationsApiImpl {	
	
	@Value("${rate-limit-bulk-outgoing-text-per-day:200}")
    private int bulkTextSendingLimitPerDay;
	
	@Value("${rate-limit-bulk-outgoing-text-per-minute:1}")
    private int bulkTextSendingLimitPerMinute;
	
	@Autowired 
	private ValidateRequest validateRequest;
	
	@Autowired
	private ConvertToJpaEntity convertToJpaEntity;
	
	@Autowired
	private Helper helper;
	
	@Autowired
	private RabbitHelper rabbitHelper;
	
	@Autowired
	MessageSignalingEngineHelper messageSignalingEngineHelper;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	@Autowired
	private TwilioClientUtil twilioClientUtil;
	
	@Autowired
	MessageRepository messageRepository;
	
	@Autowired
	private SendMessageHelper sendMessageHelper;
	
	@Autowired
	private SendEmailHelper sendEmailHelper;
	
	@Autowired
	private SaveMessageHelper saveMessageHelper;

	@Autowired
	private SendCallback sendCallback;
	
	@Autowired
	private KCommunicationsUtils kCommunicationsUtils;
	
	@Autowired
	private ReportingApiUtils reportingApiUtils;
	
	@Autowired
	MessageExtnRepository messageExtnRepository;
	
	@Autowired
	MessagePredictionFeedbackRepository messagePredictionFeedbackRepository;

	@Autowired
	KCommunicationsRequestProcessor kCommunicationsRequestProcessor;

	@Autowired
	PredictionFeatureRepository predictionFeatureRepo;

	@Autowired
	MessagePredictionRepository messagePredictionRepo;
	
	@Autowired
	ThreadPrintingHelper threadPrintingHelper;
	
	@Autowired
	AWSClientUtil awsClientUtil;
	
	@Autowired
	KManageApiHelper kManageApiHelper;

	@Autowired
	private IncomingMessageService incomingMessageService;

	@Autowired
	private AttachmentService attachmentService;
	
	@Autowired
	MessageImpl messageImpl;
	
	@Autowired
	MessagingViewControllerHelper messagingViewControllerHelper;
	
	@Autowired
	ThreadRepository threadRepository;
	
	@Autowired
	KMessagingApiHelper kMessagingApiHelper;
	
	@Autowired
	KCustomerApiHelperV2 kCustomerAPIHelperV2;
	
	@Autowired
	CustomerSentimentImpl customerSentimentImpl;
	
	@Value("${message-view-controller-url}")
	String viewApiControllerUrl;
	
	@Value("${awsVideoOutputBucketUrl}")
	private String awsVideoOutputBucketUrl;

	@Autowired
	private MessageSendingRules messageSendingRules;
	
	private ObjectMapper objectMapper = new ObjectMapper();
	
	private static final String LOG_MESSAGE_API_SUPPORT = "Internal error - %s while processing request! Please contact Communications API support";
	public static final String COUNTRY_CODE = "+1";
	private final static Logger LOGGER = LoggerFactory.getLogger(CommunicationsApiImpl.class);
	public final static String HOURS_IN_A_DAY = "24";

	public static final String COMMUNICATIONS_ATTACHMENT_FOLDER_PREFIX="/attachments/";
	private static final Long DEFAULT_BATCH_SIZE = 500l;
	private static final String TEMPLATE_VCF_TAG = "_c_mms_vcard";

	public ResponseEntity<Response> logAutoCsiStatus(String departmentUUID, String dealerOrderUUID,
			AutoCsiLogEventRequest logAutoCsiRequest) throws Exception {
		LOGGER.info("in logAutoCsiStatus received request for creating message for department_uuid={} dealer__order={}, logAutoCsiRequest= {}}",
				departmentUUID,dealerOrderUUID,objectMapper.writeValueAsString(logAutoCsiRequest));
		try {
			reportingApiUtils.sendAutoCsiRequestToReporting(logAutoCsiRequest);
			return new ResponseEntity<Response>(new Response(), HttpStatus.OK);
		}catch(Exception e) {
			LOGGER.error(" dealer_order={} Exception while sending a autocsi-log-event to awsEventBridge,  exception = {}", dealerOrderUUID, e);
			throw e;		
		}
	}
	
	public ResponseEntity<SendMessageResponse> createMessage(String customerUUID, String departmentUUID, String userUUID,
			SendMessageRequest createMessageRequest, String serviceSubscriberName) throws Exception {
		LOGGER.info("in createMessage received request for creating message for department_uuid={} customer_uuid={} user_uuid={} send_message_request={}",
				departmentUUID, customerUUID, userUUID, objectMapper.writeValueAsString(createMessageRequest));

		try {
			SendMessageResponse createMessageResponse = new SendMessageResponse();
			createMessageResponse = validateRequest.validateSendMessageRequest(createMessageRequest);
			if(createMessageResponse.getErrors()!=null && !createMessageResponse.getErrors().isEmpty()) {
				createMessageResponse.setStatus(Status.FAILURE);
				return new ResponseEntity<SendMessageResponse>(createMessageResponse, HttpStatus.BAD_REQUEST);
			}

			if(com.mykaarma.kcommunications_model.enums.MessageType.INCOMING.getMessageType().equalsIgnoreCase(createMessageRequest.getMessageAttributes().getType().getMessageType())) {
				return incomingMessageService.processIncomingMessage(customerUUID, departmentUUID, userUUID, createMessageRequest, serviceSubscriberName, createMessageResponse);
			} else {
				return sendMessage(customerUUID, departmentUUID, userUUID, createMessageRequest, serviceSubscriberName, createMessageResponse);
			}
		} catch (Exception e) {
			LOGGER.error("Exception in creating message", e);
			throw new KCommunicationsException(com.mykaarma.kcommunications_model.enums.ErrorCode.INTERNAL_SERVER_ERROR,
					String.format(LOG_MESSAGE_API_SUPPORT, e.getMessage()));
		}
	}

	public ResponseEntity<SendMessageResponse> sendMessage(String customerUUID, String departmentUUID, String userUUID,
			SendMessageRequest sendMessageRequest, String serviceSubscriberName, SendMessageResponse sendMessageResponse) throws Exception {
        LOGGER.info("in sendMessage received request for creating message for department_uuid={} customer_uuid={} user_uuid={} send_message_request={}", 
            departmentUUID, customerUUID, userUUID, objectMapper.writeValueAsString(sendMessageRequest));
		sendMessageResponse.setCustomerUUID(customerUUID);

		try{
			CustomerWithVehiclesResponse customer = null;
			
			try {
				customer = KCustomerApiHelperV2.getCustomerWithoutVehicle(departmentUUID, customerUUID);
			} catch (Exception e) {
				LOGGER.error(String.format("Error in fetching customer for customer_uuid=%s dealer_department_uuid=%s ", customerUUID, departmentUUID), e);
			}
			validateRequest.validateCustomer(sendMessageRequest, sendMessageResponse, customer, customerUUID);
			customer.getCustomerWithVehicles().getCustomer().setId(generalRepository.getCustomerIDForUUID(customerUUID));
			if(sendMessageResponse.getErrors()!=null && !sendMessageResponse.getErrors().isEmpty()) {
				sendMessageResponse.setStatus(Status.FAILURE);
				LOGGER.info("Error in sending message request error={}", objectMapper.writeValueAsString(sendMessageResponse.getErrors()));
				return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
			}
			
			GetDealerAssociateResponseDTO dealerAssociate=null;
			if(APIConstants.DEFAULT.equalsIgnoreCase(userUUID)) {
				dealerAssociate = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);
			} else {
				dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUUID, userUUID);
			}
			validateRequest.validateDealerAssociate(userUUID, sendMessageRequest, sendMessageResponse, dealerAssociate);
			if(sendMessageResponse.getErrors()!=null && !sendMessageResponse.getErrors().isEmpty()) {
				sendMessageResponse.setStatus(Status.FAILURE);
				LOGGER.info("Error in sending message request error={}", objectMapper.writeValueAsString(sendMessageResponse.getErrors()));
				return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
			}
			
			String communicationValue = null;
			if(sendMessageRequest.getMessageSendingAttributes()!=null &&
					sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer()!=null
					&& !sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer().isEmpty()) {
				communicationValue = sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer();
				LOGGER.info("found communication_value={}", communicationValue);
			} else {
				 communicationValue = helper.getPreferredCommunicationValueForProtocol(customer.getCustomerWithVehicles().getCustomer(),
							sendMessageRequest.getMessageAttributes().getProtocol());
				LOGGER.info("not found communication_value={}", communicationValue);
			}

			if(sendMessageRequest.getMessageAttributes()!=null && sendMessageRequest.getMessageAttributes().getBody()!=null
					&& sendMessageRequest.getMessageAttributes().getBody().contains(TEMPLATE_VCF_TAG)) {
				String messageBody = sendMessageRequest.getMessageAttributes().getBody();
				messageBody = messageBody.replace(TEMPLATE_VCF_TAG, "");
				sendMessageRequest.getMessageAttributes().setBody(messageBody);
				String dsoValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO().getDealerMinimalDTO().getUuid(), DealerSetupOption.WELCOME_TEXT_VCF_ENABLE.getOptionKey());
				if(dsoValue!=null && "true".equalsIgnoreCase(dsoValue)) {
					if(sendMessageRequest.getMessageSendingAttributes()==null) {
						MessageSendingAttributes msa = new MessageSendingAttributes();
						msa.setSendVCard(true);
						sendMessageRequest.setMessageSendingAttributes(msa);
					}
					else {
						sendMessageRequest.getMessageSendingAttributes().setSendVCard(true);
					}
				}
			}

			validateRequest.applyMessageSendingRules(sendMessageRequest, sendMessageResponse, dealerAssociate.getDealerAssociate(),
					customer.getCustomerWithVehicles().getCustomer(), communicationValue, userUUID, serviceSubscriberName);
			if(sendMessageResponse.getErrors()!=null && !sendMessageResponse.getErrors().isEmpty()) {
				sendMessageResponse.setStatus(Status.FAILURE);
				LOGGER.info("Error in sending message request error={}", objectMapper.writeValueAsString(sendMessageResponse.getErrors()));
				return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
			}

			MDC.put(APIConstants.DEALER_ASSOCIATE_ID, dealerAssociate.getDealerAssociate().getId());
			MDC.put(APIConstants.CUSTOMER_ID, customer.getCustomerWithVehicles().getCustomer().getId());

			Message message = getMessageObject(sendMessageRequest, customer, dealerAssociate);
			sendMessageResponse.setMessageUUID(message.getUuid());
            
            Integer delay = 0;
            if(sendMessageRequest.getMessageSendingAttributes()!=null &&
                    sendMessageRequest.getMessageSendingAttributes().getDelay() != null &&
                    sendMessageRequest.getMessageSendingAttributes().getDelay() > 0) {
                delay = sendMessageRequest.getMessageSendingAttributes().getDelay();
            }
            Boolean sendSynchronously = false;
            if(sendMessageRequest.getMessageSendingAttributes() != null &&
                    sendMessageRequest.getMessageSendingAttributes().getSendSynchronously() != null
                    && sendMessageRequest.getMessageSendingAttributes().getSendSynchronously()) {
                sendSynchronously = true;
            }
            
            sendMessageResponse = sendMessageObject(departmentUUID, message, sendSynchronously, delay, false, false, 
            		sendMessageRequest.getMessageAttributes().getUpdateThreadTimestamp());
            MDC.put(APIConstants.MESSAGE_ID, message.getId());
			return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.OK);
		} catch(Exception e){

			LOGGER.error("Exception in sending message", e);
			throw new KCommunicationsException(com.mykaarma.kcommunications_model.enums.ErrorCode.INTERNAL_SERVER_ERROR, String.format(
					LOG_MESSAGE_API_SUPPORT
					, e.getMessage()));
		}
	}

	public ResponseEntity<SendMessageResponse> sendMessage(String departmentUUID, String messageUUID, SendDraftRequest sendDraftRequest, Boolean updateThreadTimestamp) throws Exception {

		SendMessageResponse sendMessageResponse = new SendMessageResponse();
		sendMessageResponse.setErrors(new ArrayList<>());
		sendMessageResponse.setMessageUUID(messageUUID);
		List<ApiError> errors = sendMessageResponse.getErrors();

		try{
			Message message = helper.getMessageObject(messageUUID);
			if(message==null) {
				ApiError apiError = new ApiError(ErrorCode.INVALID_MESSAGE_UUID.name(), String.format("Message_UUID=%s is invalid ", messageUUID));
				errors.add(apiError);
				sendMessageResponse.setErrors(errors);
				sendMessageResponse.setStatus(Status.FAILURE);
				return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
			}
			validateRequest.validateIfMessageAlreadySent(message, sendMessageResponse);
			if(sendMessageResponse.getErrors()!=null && !sendMessageResponse.getErrors().isEmpty()) {
				return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
			}
			Long departmentID = generalRepository.getDepartmentIDForUUID(departmentUUID);
			if(!message.getDealerDepartmentId().equals(departmentID)) {
				ApiError apiError = new ApiError(ErrorCode.MISMATCH_DEPARTMENT_MESSAGE.name(), String.format("Message_UUID=%s is not matching with department_uuid=%s ",
						messageUUID, departmentUUID));
				errors.add(apiError);
				sendMessageResponse.setErrors(errors);
				sendMessageResponse.setStatus(Status.FAILURE);
				return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
			}
            Boolean sendSynchronously = false;
            if(sendDraftRequest != null && sendDraftRequest.getSendSynchronously() != null && sendDraftRequest.getSendSynchronously()) {
                sendSynchronously = true;
            }
            sendMessageResponse = sendMessageObject(departmentUUID, message, sendSynchronously, 0, false, true, updateThreadTimestamp);
            return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.OK);
		} catch(Exception e){

			LOGGER.error("Exception in sending message", e);
			throw new KCommunicationsException(com.mykaarma.kcommunications_model.enums.ErrorCode.INTERNAL_SERVER_ERROR, String.format(
					LOG_MESSAGE_API_SUPPORT
					, e.getMessage()));
		}
	}

    public ResponseEntity<SendMessageResponse> editDraft(String departmentUUID, String userUUID,
        String customerUUID, String messageUUID, SendMessageRequest sendMessageRequest)  throws Exception {
        LOGGER.info("in editDraft received request with send_message_request={} message_uuid={}", objectMapper.writeValueAsString(sendMessageRequest), messageUUID);    
        SendMessageResponse sendMessageResponse = new SendMessageResponse();
        sendMessageResponse.setCustomerUUID(customerUUID);

        try{
            sendMessageResponse = validateRequest.validateEditDraftRequest(sendMessageRequest, messageUUID);
            if(sendMessageResponse.getErrors()!=null && !sendMessageResponse.getErrors().isEmpty()) {
                sendMessageResponse.setStatus(Status.FAILURE);
                return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
            }
            CustomerWithVehiclesResponse customer = null;
            
            try {
                customer = KCustomerApiHelperV2.getCustomerWithoutVehicle(departmentUUID, customerUUID);
            } catch (Exception e) {
                LOGGER.error(String.format("Error in fetching customer for customer_uuid=%s dealer_department_uuid=%s ", customerUUID, departmentUUID), e);
            }
            
            if((sendMessageRequest==null || sendMessageRequest.getMessageAttributes()== null
            		|| sendMessageRequest.getMessageAttributes().getDraftAttributes()==null 
            		|| sendMessageRequest.getMessageAttributes().getDraftAttributes().getDraftStatus()==null
            		|| !DraftStatus.DISCARDED.equals(sendMessageRequest.getMessageAttributes().getDraftAttributes().getDraftStatus()))) {
                validateRequest.validateCustomer(sendMessageRequest, sendMessageResponse, customer, customerUUID);	
            }
            customer.getCustomerWithVehicles().getCustomer().setId(generalRepository.getCustomerIDForUUID(customerUUID));
            if(sendMessageResponse.getErrors()!=null && !sendMessageResponse.getErrors().isEmpty()) {
                sendMessageResponse.setStatus(Status.FAILURE);
                LOGGER.info("Error in sending message request error={}", objectMapper.writeValueAsString(sendMessageResponse.getErrors()));
                return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
            }
            
            GetDealerAssociateResponseDTO dealerAssociate=null;
            if(APIConstants.DEFAULT.equalsIgnoreCase(userUUID)) {
                dealerAssociate = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);
            } else {
                dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUUID, userUUID);
            }
            validateRequest.validateDealerAssociate(userUUID, sendMessageRequest, sendMessageResponse, dealerAssociate);
            if(sendMessageResponse.getErrors()!=null && !sendMessageResponse.getErrors().isEmpty()) {
                sendMessageResponse.setStatus(Status.FAILURE);
                LOGGER.info("Error in sending message request error={}", objectMapper.writeValueAsString(sendMessageResponse.getErrors()));
                return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
            }
            String communicationValue = null;
            if(sendMessageRequest.getMessageSendingAttributes()!=null &&
                    sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer()!=null
                    && !sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer().isEmpty()) {
                communicationValue = sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer();
                LOGGER.info("found communication_value={}", communicationValue);
            } else {
                    communicationValue = helper.getPreferredCommunicationValueForProtocol(customer.getCustomerWithVehicles().getCustomer(),
                            sendMessageRequest.getMessageAttributes().getProtocol());
                LOGGER.info("not found communication_value={}", communicationValue);
            }
            
            if((sendMessageRequest==null || sendMessageRequest.getMessageAttributes()== null
            		|| sendMessageRequest.getMessageAttributes().getDraftAttributes()==null 
            		|| sendMessageRequest.getMessageAttributes().getDraftAttributes().getDraftStatus()==null
            		|| !DraftStatus.DISCARDED.equals(sendMessageRequest.getMessageAttributes().getDraftAttributes().getDraftStatus()))) {
                validateRequest.applyMessageSendingRules(sendMessageRequest, sendMessageResponse, dealerAssociate.getDealerAssociate(),
                        customer.getCustomerWithVehicles().getCustomer(), communicationValue, userUUID, null);
            }
            if(sendMessageResponse.getErrors()!=null && !sendMessageResponse.getErrors().isEmpty()) {
                sendMessageResponse.setStatus(Status.FAILURE);
                LOGGER.info("Error in sending message request error={}", objectMapper.writeValueAsString(sendMessageResponse.getErrors()));
                return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
            }


            MDC.put(APIConstants.DEALER_ASSOCIATE_ID, dealerAssociate.getDealerAssociate().getId());
            MDC.put(APIConstants.CUSTOMER_ID, customer.getCustomerWithVehicles().getCustomer().getId());

            Message oldMessage = helper.getMessageObject(messageUUID);
            if(oldMessage == null) {
                List<ApiError> errors = new ArrayList<ApiError>();
                ApiError apiError = new ApiError(ErrorCode.INVALID_MESSAGE_UUID.name(), String.format("message_uuid=%s is invalid ", messageUUID));
                errors.add(apiError);
				sendMessageResponse.setErrors(errors);
				sendMessageResponse.setStatus(Status.FAILURE);
                return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.BAD_REQUEST);
            }
            Message message = getMessageObject(sendMessageRequest, customer, dealerAssociate);
            message = updateMessageObject(oldMessage, message); 
            Integer delay = 0;
            if(sendMessageRequest.getMessageSendingAttributes()!=null &&
                    sendMessageRequest.getMessageSendingAttributes().getDelay() != null &&
                    sendMessageRequest.getMessageSendingAttributes().getDelay() > 0) {
                delay = sendMessageRequest.getMessageSendingAttributes().getDelay();
            }
            Boolean sendSynchronously = false;
            if(sendMessageRequest.getMessageSendingAttributes() != null &&
                    sendMessageRequest.getMessageSendingAttributes().getSendSynchronously() != null
                    && sendMessageRequest.getMessageSendingAttributes().getSendSynchronously()) {
                sendSynchronously = true;
            }
            Boolean isEditedDraft = true;
            if(MessageType.D.name().equals(message.getMessageType())) {
                isEditedDraft = false;
            }
            sendMessageResponse = sendMessageObject(departmentUUID, message, sendSynchronously, delay, isEditedDraft, false, false);
            MDC.put(APIConstants.MESSAGE_ID, message.getId());
            return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.OK);
        } catch(Exception e){

            LOGGER.error("Exception in sending message", e);
            throw new KCommunicationsException(com.mykaarma.kcommunications_model.enums.ErrorCode.INTERNAL_SERVER_ERROR, String.format(
                    LOG_MESSAGE_API_SUPPORT
                    , e.getMessage()));
        }
    }

    private Message updateMessageObject(Message oldMessage, Message message) {
        message.setId(oldMessage.getId());
        message.setVersion(oldMessage.getVersion());
        message.setUuid(oldMessage.getUuid());
        return message;
	}

    private SendMessageResponse sendMessageObject(String departmentUUID, Message message, Boolean sendSynchronously, Integer delay, Boolean isEditedDraft,
    		Boolean sendDraft, Boolean updateThreadTimestamp) throws Exception {
        SendMessageResponse sendMessageResponse = new SendMessageResponse();
        sendMessageResponse.setStatus(Status.SUCCESS);
        if(message == null) {
            LOGGER.error("in sendMessageObject trying to send a null message object");
            sendMessageResponse.setStatus(Status.FAILURE);
            return sendMessageResponse;
        }
        String customerUUID = generalRepository.getCustomerUUIDFromCustomerID( message.getCustomerID());
		sendMessageResponse.setCustomerUUID(customerUUID);
        sendMessageResponse.setMessageUUID(message.getUuid());
        LOGGER.info("in sendMessageObject trying to send message for message_object={} send_synchronously={} delay={} is_edited_draft={}",
        		objectMapper.writeValueAsString(message), sendSynchronously, delay, isEditedDraft);
        if(sendSynchronously == null) {
            sendSynchronously = false;
        }
        if(delay == null) {
            delay = 0;
        }
        if(sendDraft == null) {
            sendDraft = false;
        }
        if(sendSynchronously) {
            if(message.getMessageType().equalsIgnoreCase(MessageType.S.name()) || sendDraft) {
                if(message.getProtocol().equalsIgnoreCase(MessageProtocol.TEXT.getMessageProtocol())) {
                    sendMessageResponse = sendMessageHelper.sendTextMessage(departmentUUID, message, true);
                } else if(message.getProtocol().equalsIgnoreCase(MessageProtocol.EMAIL.getMessageProtocol())) {
                    sendMessageResponse = sendEmailHelper.sendEmail(departmentUUID, message);
                }
            } else {
                saveMessageHelper.saveMessage(message);
                rabbitHelper.pushToMessagePostSendingQueue(message, null, false, isEditedDraft, false, updateThreadTimestamp);
            }
        } else {
            pushToQueue(message, delay, sendDraft, updateThreadTimestamp);
        }
        return sendMessageResponse;
    }

	public ResponseEntity<NotifierDeleteResponse> deleteNotifierEntries(String userUUID, String departmentUUID) throws Exception{


		NotifierDeleteResponse notifierDeleteResponse = new NotifierDeleteResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		notifierDeleteResponse = validateRequest.validateNotifierDeleteRequest(userUUID, departmentUUID);
		if(notifierDeleteResponse.getErrors()!=null && !notifierDeleteResponse.getErrors().isEmpty()) {
			return new ResponseEntity<NotifierDeleteResponse>(notifierDeleteResponse, HttpStatus.BAD_REQUEST);
		}
		GetDealerAssociateResponseDTO dealerAssociate=null;
		
		dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUUID, userUUID);
		
		if(dealerAssociate == null || dealerAssociate.getDealerAssociate()==null) {
			LOGGER.error("dealerAssociate dose not exist for department_uuid={} and user_uuid={}",departmentUUID, userUUID);
			ApiError apiError = new ApiError(ErrorCode.MISSING_NOTIFIER_ATTRIBUTES.name(), "Dealer Associate does not exist for this user");
			errors.add(apiError);
			notifierDeleteResponse.setErrors(errors);
			return new ResponseEntity<NotifierDeleteResponse>(notifierDeleteResponse, HttpStatus.BAD_REQUEST);
		}
		if(dealerAssociate!=null && dealerAssociate.getDealerAssociate()!=null && dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO()!=null && dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO().getDealerMinimalDTO()!=null)
			LOGGER.info("delete Notifier entries for dealer_associate_id={} and dealer_id={}", dealerAssociate.getDealerAssociate().getId(),dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO().getDealerMinimalDTO().getId());
		else {
			
			ApiError apiError = new ApiError(ErrorCode.MISSING_NOTIFIER_ATTRIBUTES.name(), "Dealer does not exist for this user");
			errors.add(apiError);
			notifierDeleteResponse.setErrors(errors);
			return new ResponseEntity<NotifierDeleteResponse>(notifierDeleteResponse, HttpStatus.BAD_REQUEST);
		}
		
		String url = viewApiControllerUrl+"/deleteNotifierEntries";
		NotifierDeleteRequest notifierDeleteRequest = new NotifierDeleteRequest();
		notifierDeleteRequest.setDealerAssociateID(dealerAssociate.getDealerAssociate().getId());
		notifierDeleteRequest.setDealerID(dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO().getDealerMinimalDTO().getId());
		notifierDeleteRequest.setDeleteAllEntriesForUser(true);
		RestTemplate restTemplate = new RestTemplate();
		try {
			restTemplate.postForObject(url, notifierDeleteRequest, Boolean.class);
			LOGGER.info("success deleting entries for department_uuid={} and user_uuid= {}",departmentUUID, userUUID);
			return new ResponseEntity<NotifierDeleteResponse>(notifierDeleteResponse, HttpStatus.OK);
		}
		catch(Exception e) {
			LOGGER.error("unable to delete Notifier entries for user_uuid={} and department_uuid={}", userUUID,departmentUUID,e);
			throw new KCommunicationsException(com.mykaarma.kcommunications_model.enums.ErrorCode.INTERNAL_SERVER_ERROR, String.format(
					LOG_MESSAGE_API_SUPPORT
					, e.getMessage()));	
		}
	}
	
	private Message getMessageObject(SendMessageRequest sendMessageRequest, CustomerWithVehiclesResponse customerWithVehiclesResponse, 
			GetDealerAssociateResponseDTO dealerAssociateResponse) throws Exception {
		String communicationValue = null;
		if(sendMessageRequest.getMessageSendingAttributes()!=null && sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer()!=null && 
				!sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer().isEmpty()) {
			communicationValue = sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer();
		} else {
			communicationValue = helper.getPreferredCommunicationValueForProtocol(customerWithVehiclesResponse.getCustomerWithVehicles().getCustomer(), 
					sendMessageRequest.getMessageAttributes().getProtocol());
		}
		Message message = convertToJpaEntity.getMessageJpaEntity(sendMessageRequest, customerWithVehiclesResponse.getCustomerWithVehicles().getCustomer(),
				dealerAssociateResponse.getDealerAssociate(), communicationValue);
		LOGGER.info("in getMessageObject communicationValue={} sendmessagerequest={} message_object={}",
				communicationValue, objectMapper.writeValueAsString(sendMessageRequest)
				,objectMapper.writeValueAsString(message));
		return  message;
	}
	
	private void pushToQueue(Message message, Integer delayInSeconds, Boolean sendDraft, Boolean updateThreadTimestamp) throws Exception {
		if(sendDraft == null) {
            sendDraft = false;
        }
        LOGGER.info("in pushToQueue message_object={} delay_in_seconds={} send_draft={}", objectMapper.writeValueAsString(message), delayInSeconds, sendDraft);
		if(message.getMessageType().equalsIgnoreCase(MessageType.S.name()) || sendDraft) {
			rabbitHelper.pushToMessageSendingQueue(message, delayInSeconds * 1000);
		} else {
			rabbitHelper.pushToMessageSavingQueue(message, updateThreadTimestamp, null);
		}
	}
	
	public ResponseEntity<UpdateVoiceUrlResponse> updateVoiceUrlForDealers(TwilioDealerIDRequest twilioDealerIDRequest)
	{
		return twilioClientUtil.updateVoiceUrlForDealers(twilioDealerIDRequest);
	}

	public ResponseEntity<Response> updateRecordingForDealers(RecordingRequest recordingRequest) throws Exception {
		
		Response response = validateRequest.validateRecordingUrlUpdateRequestForDealers(recordingRequest.getDealerIDs());
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		
		if(response.getErrors()!=null && !response.getErrors().isEmpty()) {
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		}
		
		try {
			 pushDataToFetchMessageForEachDealerInQueue(recordingRequest);

		}
		catch (Exception e) {
			LOGGER.error("unable to push data to queue for dealer_id_list={} fromDate={} endDate={}", recordingRequest.getDealerIDs(), recordingRequest.getStartDate(), recordingRequest.getEndDate(), e);
			
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
		
	}

	private void pushDataToFetchMessageForEachDealerInQueue(RecordingRequest recordingRequest) {
		
		List<Long> dealerIDs = recordingRequest.getDealerIDs();
		for(Long dealerID : dealerIDs) {
			
			DealerMessagesFetchRequest fetchMessagesForDealer = new DealerMessagesFetchRequest();
			fetchMessagesForDealer.setDealerID(dealerID);
			fetchMessagesForDealer.setStartDate(recordingRequest.getStartDate());
			fetchMessagesForDealer.setEndDate(recordingRequest.getEndDate());
			
			if(recordingRequest instanceof RecordingUpdateRequest) {
				fetchMessagesForDealer.setDeleteRecordings(((RecordingUpdateRequest)recordingRequest).getRecordingDelete());
				fetchMessagesForDealer.setVerifyRecordings(false);
			}
			else if(recordingRequest instanceof RecordingVerifyRequest){
				fetchMessagesForDealer.setVerifyRecordings(true);
			}
			try {
				rabbitHelper.pushDataToFetchMessageForDealerQueue(fetchMessagesForDealer);
				LOGGER.info(String.format("successfully pushed data to queue=%s for dealer_id=%s",RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_DEALER.getQueueName(),dealerID));
			} catch (Exception e) {
				LOGGER.error(String.format("can't push data to queue=%s for dealer_id=%s",RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_DEALER.getQueueName(),dealerID),e);
			}
		}
	}
	
	public ResponseEntity<Response> processIncomingMessagePostReceived(String messageUUID,Long dealerID,String departmentUUID){
		try {
			PostMessageReceived postMessageReceived=new PostMessageReceived();
			postMessageReceived.setDealerID(dealerID);
			postMessageReceived.setDepartmentUUID(departmentUUID);
			postMessageReceived.setMessageUUID(messageUUID);
			rabbitHelper.pushDataToTakePostMessageReceivedActions(postMessageReceived);
			return new ResponseEntity<Response>(new Response(), HttpStatus.OK);
		}
		catch(Exception e) {
			LOGGER.error(String.format("in processIncomingMessagePostReceived can't push data to queue=%s for message_uuid=%s dealer_id=%s",RabbitQueueInfo.POST_MESSAGE_RECEIVED_QUEUE.getQueueName(), messageUUID,dealerID),e);
			Response response=new Response();
			List<ApiError> errors = new ArrayList<ApiError>();
			response = getErrorResponse(ErrorCode.QUEUE_PUSH_FAILUE.name(), ErrorCodes.QUEUE_PUSH_FAILURE.getErrorDescription(), errors, response);
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<Response> updateURLForMessage(String messageUUID) throws Exception{
		
		
		Response response = validateRequest.validateRecordingUrlUpdateRequest(messageUUID);
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		Long messageID = null;
		Long dealerID = null;
		
		if(response.getErrors()!=null && !response.getErrors().isEmpty()) {
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		}
		
		List<Object[]> message = messageRepository.findDealerIDAndMessageIDByuuid(messageUUID);
		
		
		try {
			messageID = ((BigInteger)message.get(0)[0]).longValue();
		}
		catch(Exception e) {
			response = getErrorResponse(ErrorCode.INVALID_MESSAGE_ID.name(), ErrorCodes.INVALID_MESSAGE_ID.getErrorDescription(), errors, response);
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		}
		
		try {
			 dealerID = ((BigInteger)message.get(0)[1]).longValue();
		}
		catch (Exception e){
			response = getErrorResponse(ErrorCode.INVALID_DEALER_ID.name(), ErrorCodes.INVALID_DEALER_ID.getErrorDescription(), errors, response);
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		}
		
		RecordingURLMessageUpdateRequest updateRecordingURLForMessage = new RecordingURLMessageUpdateRequest();
		updateRecordingURLForMessage.setMessageID(messageID);
		updateRecordingURLForMessage.setDealerID(dealerID);
		updateRecordingURLForMessage.setDeleteRecordings(true);
		try {
			rabbitHelper.pushDataToUpdateURLForMessageDelayedQueue(updateRecordingURLForMessage);
			LOGGER.info(String.format("successfully pushed data to queue=%s for dealer_id=%s message_id=%s",RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE_DELAYED.getQueueName(),dealerID, messageID));
		} catch (Exception e) {
			LOGGER.error(String.format("can't push data to queue=%s for message_id=%s",RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE_DELAYED.getQueueName(), updateRecordingURLForMessage.getMessageID()),e);
			response = getErrorResponse(ErrorCode.QUEUE_PUSH_FAILUE.name(), ErrorCodes.QUEUE_PUSH_FAILURE.getErrorDescription(), errors, response);
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	private Response getErrorResponse(String errorName, String errorDescription, List<ApiError> errors, Response response) {
		
		ApiError apiError = new ApiError(errorName, errorDescription);
		errors.add(apiError);
		response.setErrors(errors);
		
		return response;
	}

	public ResponseEntity<Response> postMessageReceived(String messageUUID, String departmentUUID) throws Exception {
		
		Response response = new Response();
		
		response=validateRequest.validatePostMessageReceived(messageUUID, departmentUUID);
		if(response.getErrors()!=null && !response.getErrors().isEmpty()) {
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		}

    	GetDepartmentResponseDTO getDepartmentResponseDTO = kManageApiHelper.getDealerDepartment(departmentUUID);
        String dealerUUID = getDepartmentResponseDTO.getDepartmentExtendedDTO().getDealerMinimalDTO().getUuid();
		Long dealerId = getDepartmentResponseDTO.getDepartmentExtendedDTO().getDealerMinimalDTO().getId();
		
		response=kCommunicationsUtils.checkIfFeatureEnabledForDealership(dealerUUID,DealerSetupOption.COMMUNICATIONS_MESSAGING_SIGNALING_ENGINE_ENABLE.getOptionKey(), response);
		
		if(response.getWarnings()!=null && !response.getWarnings().isEmpty()) {
			return new ResponseEntity<Response>(response, HttpStatus.OK);
		}
		
		return processIncomingMessagePostReceived(messageUUID,dealerId,departmentUUID);
	}
	
	
	
	public ResponseEntity<SendMultipleMessageResponse> sendMultipleMessages(MultipleMessageRequest multipleMessageRequest, 
			String dealerDepartmentUUID, String userUUID) throws Exception {
        LOGGER.info("in sendMultipleMessages received request with multiple_message_request={}", objectMapper.writeValueAsString(multipleMessageRequest));
		Integer dailyLimit = null;
		Integer minuteLimit = null;
        MultipleMessageSendingAttributes multipleMessageSendingAttributes = multipleMessageRequest.getMultipleMessageSendingAttributes();
		if(multipleMessageSendingAttributes != null && (multipleMessageSendingAttributes.getBulkText() == null || multipleMessageSendingAttributes.getBulkText())) {
            dailyLimit = bulkTextSendingLimitPerDay;
            minuteLimit = bulkTextSendingLimitPerMinute;
            try {
                String dealerUUID = kCommunicationsUtils.getDealerUUIDFromDepartmentUUID(dealerDepartmentUUID);
                HashMap<String, String> dsoMap = kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(dealerUUID, getDSOSetForMultipleMessageSending());
                
                if(dsoMap!=null && dsoMap.get(DealerSetupOption.COMMUNICATIONS_MULTIPLE_MESSAGE_SENDING_DAY_LIMIT.getOptionKey())!=null) {
                    dailyLimit = Integer.parseInt(dsoMap.get(DealerSetupOption.COMMUNICATIONS_MULTIPLE_MESSAGE_SENDING_DAY_LIMIT.getOptionKey()));
                }
                if(dsoMap!=null && dsoMap.get(DealerSetupOption.COMMUNICATIONS_MULTIPLE_MESSAGE_SENDING_MINUTE_LIMIT.getOptionKey())!=null) {
                    minuteLimit = Integer.parseInt(dsoMap.get(DealerSetupOption.COMMUNICATIONS_MULTIPLE_MESSAGE_SENDING_MINUTE_LIMIT.getOptionKey()));
                }
            } catch (Exception e) {
                LOGGER.warn("Error in fetching DSOs for department_uuid={} ", dealerDepartmentUUID, e);
            }          
		}         
        SendMultipleMessageResponse response = validateRequest.validateMultipleMessageRequest(multipleMessageRequest, dailyLimit, minuteLimit);
        if(response.getErrors()!=null && !response.getErrors().isEmpty()) {
			return new ResponseEntity<SendMultipleMessageResponse>(response, HttpStatus.BAD_REQUEST);
        }
        if(multipleMessageSendingAttributes.getCustomerUUIDList() == null || multipleMessageSendingAttributes.getCustomerUUIDList().isEmpty()) {
            List<String> customerUUIDList = new ArrayList<String>();
            customerUUIDList.addAll(multipleMessageSendingAttributes.getCustomerUUIDToCommunicationValues().keySet());
            multipleMessageSendingAttributes.setCustomerUUIDList(customerUUIDList);
        }
		MultipleMessageSending multipleMessageSending = new MultipleMessageSending();
		SendMessageRequest sendMessageRequest = new SendMessageRequest();
		sendMessageRequest.setMessageAttributes(multipleMessageRequest.getMessageAttributes());
        MessageSendingAttributes messageSendingAttributes = new MessageSendingAttributes();
        messageSendingAttributes.setSendSynchronously(multipleMessageSendingAttributes.getSendSynchronously());
		messageSendingAttributes.setAddFooter(multipleMessageSendingAttributes.getAddFooter());
		messageSendingAttributes.setAddSignature(multipleMessageSendingAttributes.getAddSignature());
		messageSendingAttributes.setAddTCPAFooter(multipleMessageSendingAttributes.getAddTCPAFooter());
		messageSendingAttributes.setOverrideHolidays(multipleMessageSendingAttributes.getOverrideHolidays());
        messageSendingAttributes.setListOfEmailsToBeCCed(multipleMessageSendingAttributes.getListOfEmailsToBeCCed());
        messageSendingAttributes.setListOfEmailsToBeBCCed(multipleMessageSendingAttributes.getListOfEmailsToBeBCCed());
        String requestUUID = helper.getBase64EncodedSHA256UUID();
        Long delay = 0l;
        Long gapInSeconds = 0l;
        if(multipleMessageSendingAttributes.getBulkText() == null || multipleMessageSendingAttributes.getBulkText()) {
            String url = String.format("communications/department/%s/request/%s/message/response", dealerDepartmentUUID,
				requestUUID);
            messageSendingAttributes.setCallbackURL(url);
            Date pstStartDate = kCommunicationsUtils.getPstDateFromIsoDate(multipleMessageSendingAttributes.getStartTimeOfSendingMessages());
            Date pstEndDate = kCommunicationsUtils.getPstDateFromIsoDate(multipleMessageSendingAttributes.getEndTimeOfSendingMessages());
            Date currentDate = new Date();
            delay = pstStartDate.getTime() - currentDate.getTime();
            int customerListSize = multipleMessageSendingAttributes.getCustomerUUIDList().size();
            Long durationInSeconds = (pstEndDate.getTime() - pstStartDate.getTime());
            gapInSeconds = durationInSeconds/customerListSize/1000;
            if(delay < 0) 
                delay=0l;
            LOGGER.info("attr delay={} durSeconds={} gapInSeconds={} cust_size={}", delay, durationInSeconds, gapInSeconds, customerListSize);
        }
		multipleMessageSending.setCustomerUUIDList(multipleMessageSendingAttributes.getCustomerUUIDList());
		multipleMessageSending.setDealerDepartmentUUID(dealerDepartmentUUID);
		multipleMessageSending.setRequestUUID(requestUUID);
		multipleMessageSending.setUserUUID(userUUID);
        multipleMessageSending.setCustomerUUIDToCommunicationValues(multipleMessageSendingAttributes.getCustomerUUIDToCommunicationValues());
        sendMessageRequest.setMessageSendingAttributes(messageSendingAttributes);
        multipleMessageSending.setSendMessageRequest(sendMessageRequest);
		multipleMessageSending.setGapInSendingMessagesInSeconds(gapInSeconds.intValue());
		response = new SendMultipleMessageResponse();
        if(multipleMessageSendingAttributes.getSendSynchronously() != null && multipleMessageSendingAttributes.getSendSynchronously()) {
            List<SendMessageResponse> sendMessageResponses = processMultiplemessageRequest(multipleMessageSending);
            List<ApiError> errors = new ArrayList<ApiError>();
            List<ApiWarning> warnings = new ArrayList<ApiWarning>();
            for(SendMessageResponse sendMessageResponse : sendMessageResponses) {
                if(sendMessageResponse.getErrors() != null) {
                    errors.addAll(sendMessageResponse.getErrors());
                }
                if(sendMessageResponse.getWarnings() != null) {
                    warnings.addAll(sendMessageResponse.getWarnings());
                }
            }
            response.setSendMessageResponses(sendMessageResponses);
            response.setErrors(errors);
            response.setWarnings(warnings);
        }
        else {
            rabbitHelper.pushDataToMultipleMessageSendingRequestDelayedQueue(multipleMessageSending, delay.intValue());
        }
		return new ResponseEntity<SendMultipleMessageResponse>(response, HttpStatus.OK);
	}
	
	
	public ResponseEntity<Response> saveMessageResponse(String requestUUID, String departmentUUID, SendMessageResponse sendMessageResponse) {
		Response response = new Response();
		String failureReason = null;
		try {
			if(Status.FAILURE.equals(sendMessageResponse.getStatus()) && sendMessageResponse.getErrors()!=null 
					&& !sendMessageResponse.getErrors().isEmpty()) {
				failureReason = sendMessageResponse.getErrors().get(0).getErrorCode();
			}
			generalRepository.updateBulkMessageResponse(requestUUID,  sendMessageResponse.getCustomerUUID(), 
					sendMessageResponse.getMessageUUID(), failureReason, departmentUUID, sendMessageResponse.getStatus().name());
		} catch (Exception e) {
			LOGGER.info("Exception in saveMessageResponse for message_uuid={} customer_uuid={} request_uuid={} status={} ", 
					sendMessageResponse.getMessageUUID(), sendMessageResponse.getCustomerUUID(), requestUUID,
					sendMessageResponse.getStatus(), e);
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
	
	public List<SendMessageResponse> processMultiplemessageRequest(MultipleMessageSending multipleMessageSending) throws Exception {
        LOGGER.info("in processMultiplemessageRequest received multiple_message_sending_request={}", objectMapper.writeValueAsString(multipleMessageSending));
        List<SendMessageResponse> sendMessageResponses = new ArrayList<SendMessageResponse>();
        int delay = 0;
        SendMessageRequest sendMessageRequest = multipleMessageSending.getSendMessageRequest();
		for(String customerUUID: multipleMessageSending.getCustomerUUIDList()) {
            SendMessageResponse sendMessageResponse = null;
			try {
                sendMessageRequest.getMessageSendingAttributes().setDelay(delay);
                if(multipleMessageSending.getCustomerUUIDToCommunicationValues() != null && multipleMessageSending.getCustomerUUIDToCommunicationValues().containsKey(customerUUID)) {
                    sendMessageRequest.getMessageSendingAttributes().setCommunicationValueOfCustomer(multipleMessageSending.getCustomerUUIDToCommunicationValues().get(customerUUID));
                }
				ResponseEntity<SendMessageResponse> response = createMessage(customerUUID, multipleMessageSending.getDealerDepartmentUUID(),
                        multipleMessageSending.getUserUUID(), sendMessageRequest, null);
                delay += multipleMessageSending.getGapInSendingMessagesInSeconds();
                sendMessageResponse = response.getBody();
				if(response!=null && response.getBody().getErrors()!=null && !response.getBody().getErrors().isEmpty()
						&& multipleMessageSending.getSendMessageRequest()!=null && multipleMessageSending.getSendMessageRequest().getMessageSendingAttributes().getCallbackURL()!=null) {
					sendCallback.sendCallback(multipleMessageSending.getSendMessageRequest().getMessageSendingAttributes().getCallbackURL(),
							response.getBody());
				}
			} catch (Exception e) {
				LOGGER.error("Error in sendMultipleMessages for request_uuid={} customer_uuid={} user_uuid={} ",
                        multipleMessageSending.getRequestUUID(), customerUUID, multipleMessageSending.getUserUUID(), e);
            }
            finally {
                if(sendMessageResponse == null) {
                    sendMessageResponse = new SendMessageResponse();
                    sendMessageResponse.setCustomerUUID(customerUUID);
                    sendMessageResponse.setStatus(Status.FAILURE);
                    List<ApiError> errors = new ArrayList<ApiError>();
                    errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), "Something wrong happened while sending message"));
                    sendMessageResponse.setErrors(errors);
                }
                sendMessageResponses.add(sendMessageResponse);
            }
		}
        return sendMessageResponses;
	}
	
	private Set<String> getDSOSetForMultipleMessageSending() {
		Set<String> dsoSet = new HashSet<>();
		dsoSet.add(DealerSetupOption.COMMUNICATIONS_MULTIPLE_MESSAGE_SENDING_DAY_LIMIT.getOptionKey());
		dsoSet.add(DealerSetupOption.COMMUNICATIONS_MULTIPLE_MESSAGE_SENDING_MINUTE_LIMIT.getOptionKey());
		return dsoSet;
	}

	public ResponseEntity<FailedMessageResponse> saveFailedMessages(
			FailedMessagesRequest failedMessagesRequest) {
		
		return sendMessageHelper.saveFailedMessages(failedMessagesRequest);
		
	}
	
	public ResponseEntity<UpdateMessagePredictionFeedbackResponse> updateMessagePredictionFeedbackNew(String departmentUUID, String userUUID, UpdateMessagePredictionFeedbackRequestNew updateMessagePredictionFeedbackRequest) throws JPAException{
		
		UpdateMessagePredictionFeedbackResponse response = new UpdateMessagePredictionFeedbackResponse();
		response.setIsThreadStatusUpdated(Boolean.FALSE);
	
		try {
			PredictionFeature predictionFeature =  new PredictionFeature();
			predictionFeature = predictionFeatureRepo.findByPredictionFeature(updateMessagePredictionFeedbackRequest.getPredictionFeature());
			
			Message message = new Message();
			message = messageRepository.findByuuid(updateMessagePredictionFeedbackRequest.getMessageUUID());
			
			Long messagePredictionId = messagePredictionRepo.getMessagePredictionIDForMessageIDAndPredictionFeatureID(message.getId(), predictionFeature.getId());
			
			MessagePredictionFeedback messagePredictionFeedback = new MessagePredictionFeedback();
			messagePredictionFeedback.setDepartmentUUID(departmentUUID);
			messagePredictionFeedback.setUserUUID(userUUID);
			messagePredictionFeedback.setMessagePredictionID(messagePredictionId);
			messagePredictionFeedback.setUserFeedback(updateMessagePredictionFeedbackRequest.getUserFeedback());
			
			messagePredictionFeedbackRepository.upsertMessagePredictionFeedback(messagePredictionFeedback.getMessagePredictionID(), messagePredictionFeedback.getUserFeedback(),
					null , userUUID, departmentUUID);

		} catch (Exception e) {
			LOGGER.error("Error while updating message prediction feedback for department_uuid={} and user_uuid={} and message_uuid={} prediction_feature={}"
					,departmentUUID, userUUID, updateMessagePredictionFeedbackRequest.getMessageUUID(), updateMessagePredictionFeedbackRequest.getPredictionFeature(), e);
			throw new JPAException();
		}
		
		if(com.mykaarma.global.PredictionFeature.NEEDS_RESPONSE.getFeatureKey().equalsIgnoreCase(updateMessagePredictionFeedbackRequest.getPredictionFeature())) {
			try {
				Long customerId=generalRepository.getCustomerIDForUUID(updateMessagePredictionFeedbackRequest.getCustomerUUID());
				Long dealerId=generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
				Long departmentId=generalRepository.getDepartmentIDForUUID(updateMessagePredictionFeedbackRequest.getMessageDepartmentUUID());
				
				Thread thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(customerId, departmentId, false);
				Long threadId = thread.getId();					
						
				List<MessageDTO> messages = getMessagesForCustomer(customerId, dealerId, userUUID);
				
				ThreadInWaitingForResponseQueueRequest threadInWaitingForResponseQueueRequest = new ThreadInWaitingForResponseQueueRequest();
				threadInWaitingForResponseQueueRequest.setCustomerID(customerId);
				threadInWaitingForResponseQueueRequest.setDealerDepartmentID(departmentId);
				threadInWaitingForResponseQueueRequest.setDealerID(dealerId);
				threadInWaitingForResponseQueueRequest.setThreadID(threadId);
				
				boolean isThreadInUnrespondedHelper = messagingViewControllerHelper.checkIfThreadIsInWaitingForResponseQueue(threadInWaitingForResponseQueueRequest);
				boolean isThreadAlreadyInWFR = thread.getIsWaitingForResponse();

				boolean isThreadInWFRAfterFeedback = checkIfThreadInWaitingForResponseAfterFeedback(messages);
				
				if(isThreadInUnrespondedHelper || isThreadAlreadyInWFR ) {
					if(!isThreadInWFRAfterFeedback) {
	
						HashMap<Long,Long> customerIDAndThreadID=new HashMap<Long ,Long>();
						customerIDAndThreadID.put(customerId, threadId);
						updateThreadInWaitingForResponseFilter(dealerId, departmentId, updateMessagePredictionFeedbackRequest.getDealerAssociateUUID(), customerIDAndThreadID, Actions.CLICK_DISMISS_FROM_WAITING_FOR_RESPONSE_AS_NOT_WFR.getValue());
						response.setIsThreadStatusUpdated(Boolean.TRUE);
					}
				} else {
					if(isThreadInWFRAfterFeedback) {
	
						HashMap<Long,Long> customerIDAndThreadID=new HashMap<Long ,Long>();
						customerIDAndThreadID.put(customerId, threadId);
						updateThreadInWaitingForResponseFilter(dealerId, departmentId, updateMessagePredictionFeedbackRequest.getDealerAssociateUUID(), customerIDAndThreadID, Actions.CLICK_ADD_TO_WAITING_FOR_RESPONSE.getValue());
						response.setIsThreadStatusUpdated(Boolean.TRUE);
						
					}
				}
				
			} catch (Exception e) {
				LOGGER.error(" Error in updating thread from filter for dealer_associate_id={} messageUUID={} \n{} " ,updateMessagePredictionFeedbackRequest.getDealerAssociateUUID() , updateMessagePredictionFeedbackRequest.getMessageUUID(),e);
			}
			
		} else if(com.mykaarma.global.PredictionFeature.SENTIMENT_ANALYSIS.getFeatureKey().equalsIgnoreCase(updateMessagePredictionFeedbackRequest.getPredictionFeature())) {
			
			try {
				Long customerId=generalRepository.getCustomerIDForUUID(updateMessagePredictionFeedbackRequest.getCustomerUUID());
				Long dealerId=generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
						
				List<MessageDTO> messages = getMessagesForCustomer(customerId, dealerId, userUUID);
				
				boolean isUpset = kCustomerAPIHelperV2.checkCustomerSentimentStatus(updateMessagePredictionFeedbackRequest.getCustomerUUID(), updateMessagePredictionFeedbackRequest.getMessageDepartmentUUID());
				boolean isCustomerUpsetAfterFeedback = false;
				if(MessagePredictionFeedbackTypes.POSITIVE.getType().equalsIgnoreCase(updateMessagePredictionFeedbackRequest.getUserFeedback())) {
					isCustomerUpsetAfterFeedback = true;
				} else {
					isCustomerUpsetAfterFeedback = checkIfThreadInUpsetCustomerAfterFeedback(messages);
				}

				if(isUpset) {
					if(!isCustomerUpsetAfterFeedback) {

						Long departmentId = generalRepository.getDepartmentIDForUUID(updateMessagePredictionFeedbackRequest.getMessageDepartmentUUID());
						Thread thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(customerId, departmentId, false);
						Long threadId = thread.getId();	
						Long dealerAssociateID = generalRepository.getDealerAssociateIDForUUID(updateMessagePredictionFeedbackRequest.getDealerAssociateUUID());
						
						UpdateCustomerSentimentStatusRequest updateCustomerSentimentStatusRequest = new UpdateCustomerSentimentStatusRequest();
						HashMap<Long,Long> customerIDAndThreadID=new HashMap<Long ,Long>();
						customerIDAndThreadID.put(customerId, threadId);
						updateCustomerSentimentStatusRequest.setCustomerIDAndThreadID(customerIDAndThreadID);
						updateCustomerSentimentStatusRequest.setDealerAssociateID(dealerAssociateID);
						updateCustomerSentimentStatusRequest.setDealerID(dealerId);
						updateCustomerSentimentStatusRequest.setCustomerSentiment(CustomerSentiment.NORMAL.name());
						
						customerSentimentImpl.updateCustomerSentimentStatus(updateCustomerSentimentStatusRequest, updateMessagePredictionFeedbackRequest.getMessageDepartmentUUID());
						response.setIsThreadStatusUpdated(Boolean.TRUE);
					}
				} else {
					if(isCustomerUpsetAfterFeedback) {

						Long departmentId = generalRepository.getDepartmentIDForUUID(updateMessagePredictionFeedbackRequest.getMessageDepartmentUUID());
						Thread thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(customerId, departmentId, false);
						Long threadId = thread.getId();	
						Long dealerAssociateID = generalRepository.getDealerAssociateIDForUUID(updateMessagePredictionFeedbackRequest.getDealerAssociateUUID());
						
						UpdateCustomerSentimentStatusRequest updateCustomerSentimentStatusRequest = new UpdateCustomerSentimentStatusRequest();
						HashMap<Long,Long> customerIDAndThreadID=new HashMap<Long ,Long>();
						customerIDAndThreadID.put(customerId, threadId);
						updateCustomerSentimentStatusRequest.setCustomerIDAndThreadID(customerIDAndThreadID);
						updateCustomerSentimentStatusRequest.setDealerAssociateID(dealerAssociateID);
						updateCustomerSentimentStatusRequest.setDealerID(dealerId);
						updateCustomerSentimentStatusRequest.setCustomerSentiment(CustomerSentiment.UPSET.name());
						
						customerSentimentImpl.updateCustomerSentimentStatus(updateCustomerSentimentStatusRequest, updateMessagePredictionFeedbackRequest.getMessageDepartmentUUID());
						response.setIsThreadStatusUpdated(Boolean.TRUE);
					}
				}
				
				
			} catch (Exception e) {
				LOGGER.error(" Error in updating thread from filter for dealer_associate_id={} messageUUID={} \n{} " ,updateMessagePredictionFeedbackRequest.getDealerAssociateUUID() , updateMessagePredictionFeedbackRequest.getMessageUUID(),e);
			}
			
		}
		return new ResponseEntity<UpdateMessagePredictionFeedbackResponse>(response, HttpStatus.OK);
	}
	
	public void updateThreadInWaitingForResponseFilter(Long dealerID,Long dealerDepartmentID, String dealerAssociateUUID,HashMap<Long,Long> customerIDAndThreadID,String actionSource){
		
		LOGGER.info("updateThreadInWFRFilter for dealer_associate_uuid={} dealer_id={}  action_source={}" ,dealerAssociateUUID , dealerID , actionSource);				
		
		FilterHistory filterHistory = new FilterHistory();
		filterHistory.setActionSource(actionSource);
		filterHistory.setCustomerIDAndThreadID(customerIDAndThreadID);
		
		filterHistory.setEventRaisedByUUID(dealerAssociateUUID);
		filterHistory.setDealerDepartmentID(dealerDepartmentID);
		filterHistory.setDealerID(dealerID);
		
		String filterName = FilterName.UNRESPONDED.name();
		String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(dealerDepartmentID);
		try {
			//call kmessaging api
			kMessagingApiHelper.updateFilterTable(filterHistory, filterName, departmentUUID);
			
		} catch(Exception e) {
				LOGGER.error(" Error in updating thread from filter for dealer_associate_id={} dealer_id={} \n{} " ,dealerAssociateUUID , dealerID,e);
				throw e;
		}
	}
	
	private List<MessageDTO> getMessagesForCustomer(Long customerId, Long dealerId, String userUuid) throws Exception{
		List<String> messageUuids = messageRepository.getMessageUuidsForCustomer(customerId, 20,  null);
		
		if(messageUuids!=null && !messageUuids.isEmpty()) {
			List<MessageDTO> messagesList= messageImpl.getMessagesForUuids(messageUuids, userUuid);
			LOGGER.info("in getMessagesForCustomer messages list size={} for customer_id={} dealer_id={}",messagesList.size(),customerId,dealerId);
			return messagesList;
		} else {
			LOGGER.info("in getMessagesForCustomer no messages found for customer_id={} dealer_id={}",customerId,dealerId);
			return null;
		}
	}
	
	private boolean checkIfThreadInWaitingForResponseAfterFeedback(List<MessageDTO> messages) {
		
		boolean isThreadInWFR = false;
		boolean sentMessageFlag = false;
		for(MessageDTO message: messages) {
			
			if(!(MessageType.S.name().equalsIgnoreCase(message.getMessageType()) 
					|| (MessagePurpose.WFR.name().equalsIgnoreCase(message.getMessagePurpose()) && EventName.DISMISS_AS_ALREADY_RESPONDED.name().equalsIgnoreCase(message.getMessageExtnDTO().getSubject())) ) 
					&& !sentMessageFlag) {
				if(MessageType.I.name().equalsIgnoreCase(message.getMessageType()) && (com.mykaarma.global.MessageProtocol.V.name().equalsIgnoreCase(message.getProtocol()) || 
						com.mykaarma.global.MessageProtocol.X.name().equalsIgnoreCase(message.getProtocol()) || com.mykaarma.global.MessageProtocol.E.name().equalsIgnoreCase(message.getProtocol()))) {
					if(checkIfMessageNeedsResponse(message)) {
						isThreadInWFR = true;
					}
				}
			} else {
				sentMessageFlag = true;
			}
		}
		
		return isThreadInWFR;
	}
	
	private boolean checkIfMessageNeedsResponse(MessageDTO message) {
		
		boolean isThreadInWFRTemp = false;
		
		if(message.getMessagePredictionDTOSet()!=null && !message.getMessagePredictionDTOSet().isEmpty()) {
			for(MessagePredictionDTO messagePrediction : message.getMessagePredictionDTOSet()) {
				if(NeedsResponsePredictionTypes.NEEDS_RESPONSE.getType().equalsIgnoreCase(messagePrediction.getPrediction())) {
					if(messagePrediction.getMessagePredictionFeedback()!=null && !messagePrediction.getMessagePredictionFeedback().isEmpty()) {
						for(MessagePredictionFeedbackDTO predictionFeedback: messagePrediction.getMessagePredictionFeedback()) {
							if(MessagePredictionFeedbackTypes.POSITIVE.getType().equalsIgnoreCase(predictionFeedback.getUserFeedback())) {
								isThreadInWFRTemp = true;
							}
						}
					} else {
						isThreadInWFRTemp = true;
					}
				}
			}
		}
		
		return isThreadInWFRTemp;
	}
	
	private boolean checkIfThreadInUpsetCustomerAfterFeedback(List<MessageDTO> messages) {
		
		boolean isCustomerUpset = false;
		boolean notUpsetCustomerMessageFlag = false;
		for(MessageDTO message: messages) {
			
			if(!(MessagePurpose.CUSTOMER_SENTIMENT_STATUS.name().equalsIgnoreCase(message.getMessagePurpose()) 
					&& EventName.MARK_CUSTOMER_NOT_UPSET.name().equalsIgnoreCase(message.getMessageExtnDTO().getSubject())) 
					&& !notUpsetCustomerMessageFlag) {
				if(MessageType.I.name().equalsIgnoreCase(message.getMessageType()) && (com.mykaarma.global.MessageProtocol.V.name().equalsIgnoreCase(message.getProtocol()) || 
						com.mykaarma.global.MessageProtocol.X.name().equalsIgnoreCase(message.getProtocol()) || com.mykaarma.global.MessageProtocol.E.name().equalsIgnoreCase(message.getProtocol()))) {
					if(checkIfMessageIsUpsetCustomerType(message)) {
						isCustomerUpset = true;
					}
				}
			} else {
				notUpsetCustomerMessageFlag = true;
			}
		}
		
		return isCustomerUpset;
	}
	
	private boolean checkIfMessageIsUpsetCustomerType(MessageDTO message) {
		
		boolean isCustomerUpsetTemp = false;
		
		if(message.getMessagePredictionDTOSet()!=null && !message.getMessagePredictionDTOSet().isEmpty()) {
			for(MessagePredictionDTO messagePrediction : message.getMessagePredictionDTOSet()) {
				if(com.mykaarma.global.PredictionFeature.SENTIMENT_ANALYSIS.getFeatureKey().equalsIgnoreCase(messagePrediction.getPredictionFeature().getPredictionFeature())) {
					if(SentimentAnalysisPredictionTypes.NEGATIVE.getType().equalsIgnoreCase(messagePrediction.getPrediction())) {
						if(messagePrediction.getMessagePredictionFeedback()!=null && !messagePrediction.getMessagePredictionFeedback().isEmpty()) {
							for(MessagePredictionFeedbackDTO predictionFeedback: messagePrediction.getMessagePredictionFeedback()) {
								if(MessagePredictionFeedbackTypes.POSITIVE.getType().equalsIgnoreCase(predictionFeedback.getUserFeedback())) {
									isCustomerUpsetTemp = true;
								}
							}
						} else {
							isCustomerUpsetTemp = true;
						}
					}
				}
			}
		}
		
		return isCustomerUpsetTemp;
	}
	
	public ResponseEntity<Response> updateMessagePredictionFeedback(String departmentUUID, String userUUID, UpdateMessagePredictionFeedbackRequest updateMessagePredictionFeedbackRequest) throws JPAException{
		
		Response response = new Response();
		
		MessagePredictionFeedback messagePredictionFeedback = new MessagePredictionFeedback();
		messagePredictionFeedback.setDepartmentUUID(departmentUUID);
		messagePredictionFeedback.setUserUUID(userUUID);
		messagePredictionFeedback.setMessagePredictionID(updateMessagePredictionFeedbackRequest.getMessagePredictionID());
		messagePredictionFeedback.setUserFeedback(updateMessagePredictionFeedbackRequest.getUserFeedback());
		messagePredictionFeedback.setFeedbackReason(updateMessagePredictionFeedbackRequest.getReason());
	    
		try {
			messagePredictionFeedbackRepository.upsertMessagePredictionFeedback(messagePredictionFeedback.getMessagePredictionID(), messagePredictionFeedback.getUserFeedback(),
					updateMessagePredictionFeedbackRequest.getReason(), userUUID, departmentUUID);

		} catch (Exception e) {
			LOGGER.error("Error while updating message prediction feedback for department_uuid={} and user_uuid={} and message_prediction_id={} "
					,departmentUUID, userUUID, updateMessagePredictionFeedbackRequest.getMessagePredictionID(), e);
			throw new JPAException();
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	public ResponseEntity<Response> verifyRecordingsForDealer(RecordingRequest verificationRequest) {
		
		Response response = new Response();
		List<Long> dealerIDList = verificationRequest.getDealerIDs();
		String startDate = verificationRequest.getStartDate();
		String endDate = verificationRequest.getEndDate();
		Date fromDate = null;
		Date toDate = null;
		List<BigInteger> messagesList = null;
		
		try {
			if(startDate==null || endDate==null) {
				fromDate = kCommunicationsUtils.getXDaysBackDateForGivenHours(HOURS_IN_A_DAY);
				toDate = new Date();
			}
			else {
				fromDate = kCommunicationsUtils.getPstDateFromIsoDate(startDate);
				toDate = kCommunicationsUtils.getPstDateFromIsoDate(endDate);
			}

			if(dealerIDList==null || dealerIDList.isEmpty()) {
				messagesList = generalRepository.getVoiceCallsForGivenTimeFrame(fromDate, toDate);
				LOGGER.info("message_count={} fromDate={} toDate={}", messagesList.size(), fromDate, toDate);
			}
			else {
				messagesList = generalRepository.getVoiceCallsForGivenTimeFrameForGivenDealerIDs(fromDate, toDate, dealerIDList);
				LOGGER.info("message_count={} fromDate={} toDate={} dealer_id_list_size={}",messagesList.size(), fromDate, toDate, dealerIDList.size());
			}
			
			if(messagesList!=null && !messagesList.isEmpty()) {
				for(BigInteger messageID : messagesList) {
					LOGGER.info(String.format("pushing message to queue=%s for message_id=%s",RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE.getQueueName(), messageID));
					kCommunicationsRequestProcessor.pushMessageToUpdateRecordingQueue(messageID.longValue(), null, false, true);
				}
			}
			else {
				LOGGER.info("no messages for in fromDate={} toDate={}",fromDate, toDate);
			}
		}
		catch(Exception e) {
			LOGGER.error("failed processing for from_date={} to_Date={}", fromDate, toDate, e);
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
	
	public void updateMessagePrediction(Long messageID, String prediction, String metadata, String predictionFeature) throws Exception {
		
		MessagePrediction messagePrediction = new MessagePrediction();
		messagePrediction.setMessageID(messageID);
		PredictionFeature predictionFeatureObj;
		
		try{
			predictionFeatureObj = predictionFeatureRepo.findByPredictionFeature(predictionFeature);
		} catch (Exception e) {
			LOGGER.error(String.format("Exception in getting prediction feature id for prediction_feature=NeedsResponse "), e);
			throw e;
		}
		
		if(predictionFeature!=null) {
			messagePrediction.setPredictionFeature(predictionFeatureObj);
			
			if(com.mykaarma.global.PredictionFeature.SENTIMENT_ANALYSIS.getFeatureKey().equalsIgnoreCase(predictionFeature)) {
				
				if(SentimentAnalysisPredictionTypes.NEGATIVE.getType().equalsIgnoreCase(prediction)) {
					messagePrediction.setPrediction(SentimentAnalysisPredictionTypes.NEGATIVE.getType());
				} else {
					messagePrediction.setPrediction(SentimentAnalysisPredictionTypes.NEUTRAL.getType());
				}
			}
			
			else if(com.mykaarma.global.PredictionFeature.NEEDS_RESPONSE.getFeatureKey().equalsIgnoreCase(predictionFeature)) {

				if(NeedsResponsePredictionTypes.NEEDS_RESPONSE.getType().equalsIgnoreCase(prediction)) {
					messagePrediction.setPrediction(NeedsResponsePredictionTypes.NEEDS_RESPONSE.getType());
				} else {
					messagePrediction.setPrediction(NeedsResponsePredictionTypes.DOES_NOT_NEED_RESPONSE.getType());
				}
			}
			
			else if(com.mykaarma.global.PredictionFeature.OPT_OUT.getFeatureKey().equalsIgnoreCase(predictionFeature)) {

				if(MessageKeyword.STOP.name().equalsIgnoreCase(prediction)) {
					messagePrediction.setPrediction(MessageKeyword.STOP.name());
				} else if(MessageKeyword.STOP_SUSPECTED.name().equalsIgnoreCase(prediction)){
					messagePrediction.setPrediction(MessageKeyword.STOP_SUSPECTED.name());
				}
			}
			
            else if(com.mykaarma.global.PredictionFeature.PREFERRED_COMMUNICATION_MODE.getFeatureKey().equalsIgnoreCase(predictionFeature)) {
                
                if(MessageProtocol.TEXT.getMessageProtocol().equalsIgnoreCase(prediction)) {
                    messagePrediction.setPrediction(MessageProtocol.TEXT.getMessageProtocol());
                } else if(MessageProtocol.EMAIL.getMessageProtocol().equalsIgnoreCase(prediction)) {
                    messagePrediction.setPrediction(MessageProtocol.EMAIL.getMessageProtocol());
                } else if(MessageProtocol.VOICE_CALL.getMessageProtocol().equalsIgnoreCase(prediction)) {
                    messagePrediction.setPrediction(MessageProtocol.VOICE_CALL.getMessageProtocol());
                }
            }

			try {
				messagePredictionRepo.insertMessagePrediction(messageID, messagePrediction.getPredictionFeature().getId(), messagePrediction.getPrediction(), metadata); 
			} catch (Exception e) {
				LOGGER.error(String.format("Exception in updating message_prediction table for message_id=%s , prediction_feature_id=%s",
						messageID, messagePrediction.getPredictionFeature().getId()), e);
				throw e;
			}
		} else {
			LOGGER.error(String.format("Could not find prediction feature id for prediction_feature=%s", predictionFeature));
            throw new Exception();
		}
	}

	public ResponseEntity<Response> updateMessagePrediction(UpdateMessagePredictionRequest updateMessagePredictionRequest, String departmentUUID) throws Exception {
		
		Response response = new Response();
		try {	
			
			response = validateRequest.validateUpdateMessagePredictionRequest(updateMessagePredictionRequest);
			if(response.getErrors() != null && !response.getErrors().isEmpty()) {
				return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
			}
			
			updateMessagePrediction(updateMessagePredictionRequest.getMessageID(), updateMessagePredictionRequest.getPrediction(), 
					updateMessagePredictionRequest.getMetadata(), updateMessagePredictionRequest.getPredictionFeature());
			
			return new ResponseEntity<Response>(response, HttpStatus.OK);
		}
		catch(Exception e) {
			LOGGER.error("Error in updating message prediction status for department_uuid={} message_id={} prediction_feature={} ", 
				departmentUUID, updateMessagePredictionRequest.getMessageID(), updateMessagePredictionRequest.getPredictionFeature(), e);
			throw e;
		}
	}

	public ResponseEntity<CommunicationHistoryResponse> printThreadForCustomer(String departmentUUID,String customerUUID, CommunicationHistoryRequest commHistoryRequest) throws Exception {
		
		CommunicationHistoryResponse commHistoryResponse = new CommunicationHistoryResponse();
		if(commHistoryRequest.getSendPdf()!=null && !commHistoryRequest.getSendPdf()) {
			return threadPrintingHelper.getCommunicationHistory(departmentUUID, customerUUID, commHistoryRequest);
		}
		else {
			try {
				pushDataToMailCustomerThreadQueue(customerUUID, departmentUUID, commHistoryRequest);
				commHistoryResponse.setRequestSubmitted("true");
				new ResponseEntity<CommunicationHistoryResponse>(commHistoryResponse, HttpStatus.OK);
			}
			catch(Exception e) {
				LOGGER.error("Unable to push data to mail Thread History To customer for customer_uuid={} department_uuid={}",customerUUID, departmentUUID );
				commHistoryResponse.setRequestSubmitted("false");
				commHistoryResponse.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.QUEUE_PUSH_FAILUE.name())));
				return new ResponseEntity<CommunicationHistoryResponse>(commHistoryResponse, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<CommunicationHistoryResponse>(commHistoryResponse, HttpStatus.OK);
	}
	
	

	private void pushDataToMailCustomerThreadQueue(String customerUUID, String departmentUUID,
			CommunicationHistoryRequest commHistoryRequest) throws Exception{
		
		CommunicationHistoryMailRequest communicationHistoryMailRequest = new CommunicationHistoryMailRequest();
		communicationHistoryMailRequest.setCustomerUUID(customerUUID);
		communicationHistoryMailRequest.setDepartmentUUID(departmentUUID);
		communicationHistoryMailRequest.setCommHistoryRequest(commHistoryRequest);
		rabbitHelper.pushDataToMailCustomerThreadQueue(communicationHistoryMailRequest);
		
	}

	public ResponseEntity<CommunicationCountResponse> fetchMessageCountForCustomerInGivenRange(String departmentUUID,
			String customerUUID, CommunicationCountRequest communicationCountRequest){
		
		CommunicationCountResponse commCountResponse = new CommunicationCountResponse();
		try {
			Long dealerID = generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
			Long customerID = generalRepository.getCustomerIDForUUID(customerUUID);
			List<BigInteger> messageCount = messageRepository.getMessageCountForLastGivenDates(dealerID, customerID, null, null, communicationCountRequest.getFromDate(), 
					communicationCountRequest.getToDate());
			LOGGER.info("fetchMessageCountForCustomerInGivenRange message_count={} processing for from_date={} to_date={}", communicationCountRequest.getFromDate(), communicationCountRequest.getToDate(), Long.valueOf(messageCount.size()));
			commCountResponse.setMessageCount(Long.valueOf(messageCount.size()));
			return new ResponseEntity<CommunicationCountResponse>(commCountResponse, HttpStatus.OK);
		}
		catch(Exception e) {
			LOGGER.error("fetchMessageCountForCustomerInGivenRange failed processing for from_date={} to_date={}", communicationCountRequest.getFromDate(), communicationCountRequest.getToDate(), e);
			return new ResponseEntity<CommunicationCountResponse>(commCountResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
		
	public ResponseEntity<Response> deleteSubscriptionsForUsers(DeleteSubscriptionsRequest deleteSubscriptionsRequest) throws KCommunicationsException {
		
		String url = viewApiControllerUrl+"/deleteSubscriptionsForDealerAssociates";
		RestTemplate restTemplate = new RestTemplate();
		try {
			Boolean output = restTemplate.postForObject(url, deleteSubscriptionsRequest, Boolean.class);
			if(output) {
				LOGGER.info("success deleting subscriptions for dealer_associates={}", deleteSubscriptionsRequest.getDealerAssociates());
			}
			else{
				LOGGER.info("unable to delete subscriptions for dealert_associates={}", deleteSubscriptionsRequest.getDealerAssociates());
			}
			return new ResponseEntity<Response>(new Response(), HttpStatus.OK);
		}
		catch(Exception e) {
			LOGGER.error("unable to delete subscriptions for dealert_associates={}", deleteSubscriptionsRequest.getDealerAssociates(), e);
			throw new KCommunicationsException(com.mykaarma.kcommunications_model.enums.ErrorCode.INTERNAL_SERVER_ERROR, String.format("Internal error - %s while processing "
					+ "request! Please contact Communications API support"
					, e.getMessage()));	
		}
	}

	public ResponseEntity<UploadAttachmentResponse> uploadAttachmentsToS3(UploadAttachmentsToS3Request uploadAttachmentsToS3Request, String departmentUUID) throws Exception {
		String folderPrefix = COMMUNICATIONS_ATTACHMENT_FOLDER_PREFIX;
		if(uploadAttachmentsToS3Request.getFolderPrefix()!=null && !uploadAttachmentsToS3Request.getFolderPrefix().isEmpty()) {
			folderPrefix = uploadAttachmentsToS3Request.getFolderPrefix();
		}
		
		Long dealerID = generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
		
		if(uploadAttachmentsToS3Request.getUploadFromUrl() != null && uploadAttachmentsToS3Request.getUploadFromUrl()) {
			LOGGER.info("Uploading attachment using Url : {}", uploadAttachmentsToS3Request.getMediaUrl());
			return attachmentService.uploadAttachmentUsingUrl(uploadAttachmentsToS3Request, dealerID, folderPrefix);
		} else {
			LOGGER.info("Uploading using byteFile");
			return attachmentService.uploadAttachmentUsingByteFile(uploadAttachmentsToS3Request, dealerID, folderPrefix);
		}

	}

	public ResponseEntity<MediaPreviewURLFetchResponse> uploadMediaToAWSAndFetchMediaURL(DocFileDTO docFileDTO) throws Exception {
		return attachmentService.uploadMediaToAWSAndFetchMediaURL(docFileDTO);
	}
	
	public ResponseEntity<Response> removeMessagesFromDelayedFilter(DelayedFilterRemovalRequest delayedFilterRemovalRequest) throws JsonProcessingException, KCommunicationsException{
		try {
			rabbitHelper.pushDataForDelayedFilterRemoval(delayedFilterRemovalRequest);
			return new ResponseEntity<Response>(new Response(), HttpStatus.OK); 
		} catch (Exception e) {
			LOGGER.error("unable to remove messages from delayed_filter for request={}", objectMapper.writeValueAsString(delayedFilterRemovalRequest), e);
			throw new KCommunicationsException(com.mykaarma.kcommunications_model.enums.ErrorCode.INTERNAL_SERVER_ERROR, String.format("Internal error - %s while processing "
					+ "request! Please contact Communications API support"
					, e.getMessage()));	
		}
		
	}

	public ResponseEntity<DeleteAttachmentFromS3Response> deleteAttachmentFromS3(DeleteAttachmentFromS3Request deleteAttachmentFromS3Request, 
			String departmentUUID) throws Exception {
		
		DeleteAttachmentFromS3Response response = new DeleteAttachmentFromS3Response();
		Long dealerID = generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
		
		List<String> attachmentUrlList = deleteAttachmentFromS3Request.getAttachmentUrlList();
		LOGGER.info(String.format("Attachments Url Received : %s for dealerId : %s", attachmentUrlList.toString(), dealerID));
	
		try {
			List<String> attachmentsDeletedList = awsClientUtil.deleteMediaFromAWSS3(attachmentUrlList, dealerID);
			
			List<String> attachmentsFailedToDeleteList = new ArrayList<>();
			for(String attachment: attachmentUrlList) {
				if(!attachmentsDeletedList.contains(attachment)) {
					attachmentsFailedToDeleteList.add(attachment);
				}
			}
			
			if(attachmentsDeletedList != null && !attachmentsDeletedList.isEmpty()) {
				response.setAttachmentsDeletedList(attachmentsDeletedList);
			}
			
			if(attachmentsFailedToDeleteList != null && attachmentsFailedToDeleteList.size() > 0) {			
				LOGGER.warn(String.format("Attachments failed to delete: %s for dealerId : %s", attachmentsFailedToDeleteList.toString(), dealerID));
				
				List<ApiWarning> warnings = new ArrayList<>();
				warnings.add(new ApiWarning(WarningCode.ATTACHMENTS_FAILED_TO_DELETE.name(), attachmentsFailedToDeleteList.toString()));
				response.setWarnings(warnings);
			}
			
			return new ResponseEntity<DeleteAttachmentFromS3Response>(response, HttpStatus.OK);
			
		} catch (Exception e) {
			LOGGER.error(String.format("Unable to delete Attachment from S3 for attachment url list: %s for dealerId: %s", 
					attachmentUrlList, dealerID), e);
			response.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.DELETE_ATTACHMENT_FAILED.name())));
			return new ResponseEntity<DeleteAttachmentFromS3Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}

	public ResponseEntity<SubscriptionsUpdateResponse> updateSubscriptionsForDealers(
			SubscriptionUpdateRequest subscriptionUpdateRequest) {
		
		SubscriptionsUpdateResponse response = validateRequest.validateSubscriptionUpdateRequestForDealers(subscriptionUpdateRequest);
		Long fromDealerId = null;
		Long toDealerId = null;
		Long dealerIdProcessing = null;
		Long batchSize = null;
		try {
			LOGGER.info(objectMapper.writeValueAsString(response));
			if(response!=null && response.getErrors()!=null && response.getErrors().isEmpty()) {
				
				fromDealerId = subscriptionUpdateRequest.getFromDealerId();
				toDealerId = subscriptionUpdateRequest.getToDealerId(); 
				dealerIdProcessing = fromDealerId;
				batchSize = (subscriptionUpdateRequest.getBatchSize() != null && subscriptionUpdateRequest.getBatchSize() != 0)? subscriptionUpdateRequest.getBatchSize(): DEFAULT_BATCH_SIZE;
				while(dealerIdProcessing.compareTo(toDealerId) <= 0) {
					
					FetchCustomersDealer fetchCustomersDealer = new FetchCustomersDealer();
					fetchCustomersDealer.setDealerId(dealerIdProcessing);
					fetchCustomersDealer.setBatchSize(batchSize);
					fetchCustomersDealer.setOffSet(0l);
					rabbitHelper.pushDatatoSubscriptionUpdateForDealer(fetchCustomersDealer);
					dealerIdProcessing = dealerIdProcessing + 1;
				}
				return new ResponseEntity<SubscriptionsUpdateResponse>(response, HttpStatus.OK);
			}
			else {
				
				return new ResponseEntity<SubscriptionsUpdateResponse>(response, HttpStatus.BAD_REQUEST);
			}
			
		}
		catch(Exception e) {
			LOGGER.error(String.format("Unable to update subscriptions for from_dealerId: %s to_dealerId: %s", 
					subscriptionUpdateRequest.getFromDealerId(), subscriptionUpdateRequest.getToDealerId()), e);
			response.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.DELETE_SUBCRIPTION_FAILED.name())));
		}
		return null;
	}

	public ResponseEntity<Response> verifyCommunicationsBilling(CommunicationsBillingRequest communicationsBillingRequest) {
		
		Response response = new Response();
		CommunicationsVerification communicationsVerification = new CommunicationsVerification();
		List<BigInteger> departmentIds = null;
		
		for(Long dealerId: communicationsBillingRequest.getDealerIds()) {
			
			try {
				departmentIds = generalRepository.getAllDepartemntIDsForDealerId(dealerId);
			}
			catch(Exception e) {
				LOGGER.error("unable to fetch departments for dealer_id={} hence skipping", dealerId, e);
				continue;
			}
			if(departmentIds!=null && !departmentIds.isEmpty()) {
				for(BigInteger deptId: departmentIds) {
					try {
						communicationsVerification.setDepartmentId(deptId.longValue());
						communicationsVerification.setVerificationType(communicationsBillingRequest.getVerificationType());
							communicationsVerification.setStartDate(kCommunicationsUtils.getPstDateFromIsoDate(communicationsBillingRequest.getStartDate()));
							communicationsVerification.setEndDate(kCommunicationsUtils.getPstDateFromIsoDate(communicationsBillingRequest.getEndDate()));
							communicationsVerification.setRegisterFailedMessage(communicationsBillingRequest.getRegisterFailedMessage());
							LOGGER.info("processing request to run verification request for verification_type={} dealer_department_id={}", communicationsBillingRequest.getVerificationType(), deptId.longValue());
							rabbitHelper.pushDataForVerifyCommunicationsBilling(communicationsVerification);
						}
					catch(Exception e) {
						LOGGER.error("error processing verification request for verification_type={} dealer_department_id={} ", communicationsBillingRequest.getVerificationType(), deptId.longValue());
						return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}
		    }
			else {
				LOGGER.info("no departments found for dealer_id={} hence skipping", dealerId);
			}
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

    public ResponseEntity<GetDepartmentsUsingKaarmaTwilioURLResponse> getDepartmentsUsingKaarmaTwilioURL(Boolean getOnlyValidDealers, Long minDealerID, Long maxDealerID) throws Exception {
		if(getOnlyValidDealers == null) {
			getOnlyValidDealers = true;
		}
		if(minDealerID == null) {
			minDealerID = 0L;
		}
		if(maxDealerID == null) {
			maxDealerID = 2000L;
		}
		GetDepartmentsUsingKaarmaTwilioURLResponse response = new GetDepartmentsUsingKaarmaTwilioURLResponse();
		List<BigInteger> dealerIDList = generalRepository.getAllDealers(getOnlyValidDealers, minDealerID, maxDealerID);
		LOGGER.info("in getDepartmentsUsingKaarmaTwilioURL found dealer_id_count={} min_found_dealer_id={} max_found_dealer_id={}", dealerIDList.size(), dealerIDList.get(0), dealerIDList.get(dealerIDList.size() - 1));
		response.setDealerIDdepartmentIDListMap(new HashMap<>());
		response.setErrors(new ArrayList<>());
		response.setWarnings(new ArrayList<>());
		for(BigInteger dealerId: dealerIDList) {
			try {
				twilioClientUtil.getDepartmentsUsingKaarmaTwilioURL(dealerId.longValue(), response);
			} catch (Exception e) {
				LOGGER.error("error while processing departments for dealer_id={} hence skipping", dealerId, e);
			}
		}
		LOGGER.info("in getDepartmentsUsingKaarmaTwilioURL completed with response={}", objectMapper.writeValueAsString(response));
		return new ResponseEntity<>(response, HttpStatus.OK);
    }

	public ResponseEntity<SaveMessageListResponse> saveMessages(String departmentUUID, String customerUUID,
			SaveMessageRequestList saveMessageRequestList) {
		
		SaveMessageListResponse saveMessageListResponse = new SaveMessageListResponse();
		SaveHistoricalMessageRequest saveHistoricalMessageRequest = new SaveHistoricalMessageRequest();
		List<SaveMessageRequest> finalSaveMessageRequestList = new ArrayList<SaveMessageRequest>();
		List<SaveMessageResponse> saveMessageResponseList = new ArrayList<SaveMessageResponse>();
		List<SaveMessageRequest> listSaveMessageRequest = saveMessageRequestList.getSaveMessageRequestList();
		List<String> succesfullySubmittedSourceUuids = new ArrayList<String>();
		List<String> failedSourceUuids = new ArrayList<String>();
		List<ApiError> apiError = new ArrayList<ApiError>();
		List<ApiWarning> apiWarning = new ArrayList<ApiWarning>();
		CustomerWithVehiclesResponse customerWithVehicles = null;
		Long threadID = null;
		Long customerID = null;
		Long dealerID = null;
		Long dealerDepartmentID = null;
		
 		try {
 			customerWithVehicles = KCustomerApiHelperV2.getCustomerWithoutVehicle(departmentUUID, customerUUID);
 		}
 		catch(Exception e) {
 			LOGGER.error("unable to fetch customer for customer_uuid={} department_uuid={}", customerUUID, departmentUUID, e);
 			saveMessageListResponse.setErrors(Arrays.asList(new ApiError(ErrorCode.CUSTOMER_NOT_FOUND.name(), "customer fetching request failed")));
 			return new ResponseEntity<SaveMessageListResponse>(saveMessageListResponse, HttpStatus.INTERNAL_SERVER_ERROR);
 		}
 		
 		if(customerWithVehicles==null || customerWithVehicles.getCustomerWithVehicles()==null || customerWithVehicles.getCustomerWithVehicles().getCustomer()==null) {
 			saveMessageListResponse.setErrors(Arrays.asList(new ApiError(ErrorCode.CUSTOMER_NOT_FOUND.name(), "customer not found")));
 			return new ResponseEntity<SaveMessageListResponse>(saveMessageListResponse, HttpStatus.BAD_REQUEST);
 		}
 		saveMessageListResponse = validateRequest.validateBulkMessageSaveRequest(listSaveMessageRequest);
		if(saveMessageListResponse!=null && saveMessageListResponse.getErrors()!=null && saveMessageListResponse.getErrors().size() > 0) {
			return new ResponseEntity<SaveMessageListResponse>(saveMessageListResponse, HttpStatus.BAD_REQUEST);
		}
		
		LOGGER.info("validation passed for customer_uuid={} department_uuid={} messageRequestSize={} ", 
				customerUUID, departmentUUID, listSaveMessageRequest.size());
		
		customerID = generalRepository.getCustomerIDForUUID(customerUUID);
		dealerID = generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
		dealerDepartmentID = generalRepository.getDepartmentIDForUUID(departmentUUID);
		
		saveHistoricalMessageRequest.setCustomerUuid(customerUUID);
		saveHistoricalMessageRequest.setDepartmentUuid(departmentUUID);
		saveHistoricalMessageRequest.setCustomer(customerWithVehicles.getCustomerWithVehicles().getCustomer());
		
		try {
			for(SaveMessageRequest saveMessageRequest: listSaveMessageRequest) {
				
				try {
					
					LOGGER.info("starting to process request for source_uuid={} reuest_json={} ", saveMessageRequest.getSourceUuid(), 
							objectMapper.writeValueAsString(saveMessageRequest));
					SaveMessageResponse saveMessageResponse = new SaveMessageResponse();
					
					saveMessageResponse = validateRequest.validateSaveMessageRequest(saveMessageRequest);
					apiError = saveMessageResponse.getErrors();
					apiWarning = saveMessageResponse.getWarnings();
					
					if(saveMessageResponse!=null && ((apiError!=null  && !apiError.isEmpty()) 
							|| (apiWarning!=null && !apiWarning.isEmpty()))) {
						
						saveMessageResponseList.add(saveMessageResponse);
						if(apiError!=null  && !apiError.isEmpty()) {
							failedSourceUuids.add(saveMessageRequest.getSourceUuid());
							LOGGER.info("removing source_uuid={} due to failure_reasone={}", saveMessageRequest.getSourceUuid(), objectMapper.writeValueAsString(apiError));
						}
						
					}
		
					if(saveMessageResponse==null || (saveMessageResponse.getErrors()==null || saveMessageResponse.getErrors().isEmpty())) {
						finalSaveMessageRequestList.add(saveMessageRequest);
						succesfullySubmittedSourceUuids.add(saveMessageRequest.getSourceUuid());
					}
					
				}
				catch(Exception e) {
					LOGGER.error("error validating saveMessageRequest for source_uuid={} customer_uuid={} department_uuid={}"
							+ " moving to next saveMessageRequest",
							saveMessageRequest.getSourceUuid(), customerUUID, departmentUUID, e);
					failedSourceUuids.add(saveMessageRequest.getSourceUuid());
					SaveMessageResponse saveMessageResponse = new SaveMessageResponse();
					saveMessageResponse.setErrors(Arrays.asList(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), "some error occured while validating")));
					saveMessageResponse.setSourceUuid(saveMessageRequest.getSourceUuid());
					saveMessageResponseList.add(saveMessageResponse);
				}
			}
					
			if(!helper.isListEmpty(succesfullySubmittedSourceUuids)) {
				try {
					Collections.sort(finalSaveMessageRequestList, new MessageComparatorByReceivedOn());
					threadID = createThreadForCustomer(customerID, dealerDepartmentID, dealerID, finalSaveMessageRequestList.get(finalSaveMessageRequestList.size()-1));
					if(threadID==null) {
						saveMessageListResponse.setErrors(Arrays.asList(new ApiError(ErrorCode.THREAD_CREATION_FAILED.name(), "thread creation failed")));
			 			return new ResponseEntity<SaveMessageListResponse>(saveMessageListResponse, HttpStatus.INTERNAL_SERVER_ERROR);
					}
					for(int i=0; i<finalSaveMessageRequestList.size(); i++) {
						try {
							if(i==finalSaveMessageRequestList.size()-1) {
								saveHistoricalMessageRequest.setLogInMongo(true);
							}
							saveHistoricalMessageRequest.setThreadID(threadID);
							saveHistoricalMessageRequest.setSaveMessageRequest(finalSaveMessageRequestList.get(i));
							rabbitHelper.pushDataForHistoricalMessageProcessingQueue(saveHistoricalMessageRequest);
						}
						catch(Exception e) {
							LOGGER.error("unable to push data to queue, abandoning request please try again for source_uuid={} "
									+ "customer_uuid={}", finalSaveMessageRequestList.get(i).getSourceUuid(), customerUUID, e);
							failedSourceUuids.add(finalSaveMessageRequestList.get(i).getSourceUuid());
							SaveMessageResponse saveMessageResponse = new SaveMessageResponse();
							saveMessageResponse.setErrors(Arrays.asList(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), "some error occured while validating")));
							saveMessageResponse.setSourceUuid(finalSaveMessageRequestList.get(i).getSourceUuid());
							saveMessageResponseList.add(saveMessageResponse);
						}
					}
					
				}
				catch(Exception e) {
					LOGGER.error("unable to push data to queue, abandoning complete request please try again for customer_uuid={}",
							customerUUID, e);
				}
				
			}
			else {
				LOGGER.info("no successful request present, stoppinf further execution");
			}
			
			saveMessageListResponse.setSuccesfullySubmittedSourceUuids(succesfullySubmittedSourceUuids);
			saveMessageListResponse.setFailedSourceUuids(failedSourceUuids);
			saveMessageListResponse.setSaveMessageResponse(saveMessageResponseList);
			LOGGER.info("failed_source_uuids={} successful_source_uuids={} failed_list_size={} successful_list_size={}", failedSourceUuids, succesfullySubmittedSourceUuids,
					failedSourceUuids.size(), succesfullySubmittedSourceUuids.size());
		}
		catch(Exception e) {
			LOGGER.error("unable to process save bullk message request for customer_uuid={}", customerUUID, e);
		}
		return new ResponseEntity<SaveMessageListResponse>(saveMessageListResponse, HttpStatus.OK);
	}
	
	private Long createThreadForCustomer(Long customerID, Long dealerDepartmentID, Long dealerID, SaveMessageRequest saveMessageRequest) {
		
		Long threadID = null;
		Date messageTimeStamp = saveMessageRequest.getReceivedOn();
		String userUUID = saveMessageRequest.getUserUuid();
		Long threadDelegatee = generalRepository.getDealerAssociateIDForUserUUID(userUUID, dealerDepartmentID);
		try {
			threadID = helper.getRecentThreadIDForCustomer(customerID, dealerDepartmentID, dealerID, messageTimeStamp, threadDelegatee, userUUID);
		}
		catch(Exception e) {
			LOGGER.error("unable to create thread for customer_id={} dealer_id={}", customerID, dealerID, e);
		}
		return threadID;
		
	}

}

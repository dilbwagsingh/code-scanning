package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mykaarma.followup.model.enums.FollowUpActionType;
import com.mykaarma.followup.model.enums.FollowUpState;
import com.mykaarma.followup.model.request.MultipleFollowupUpdateRequest;
import com.mykaarma.global.Authority;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.Delegator;
import com.mykaarma.global.MessagePurpose;
import com.mykaarma.global.MessageType;
import com.mykaarma.global.TemplateType;
import com.mykaarma.kcommunications.jpa.repository.DelegationHistoryRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageThreadRepository;
import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.model.jpa.DelegationHistory;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageThread;
import com.mykaarma.kcommunications.model.kne.KNotificationMessage;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.rabbit.OptInAwaitingMessageExpire;
import com.mykaarma.kcommunications.model.rabbit.PostMessageSent;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.redis.OptOutRedisService;
import com.mykaarma.kcommunications.utils.AppConfigHelper;
import com.mykaarma.kcommunications.utils.FollowUpApiHelper;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.KMessagingApiHelper;
import com.mykaarma.kcommunications.utils.KNotificationApiHelper;
import com.mykaarma.kcommunications.utils.MessageMetaDataConstants;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.NotificationAttributes;
import com.mykaarma.kcommunications_model.common.NotificationButton;
import com.mykaarma.kcommunications_model.enums.DraftStatus;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.NotificationButtonTheme;
import com.mykaarma.kcommunications_model.request.PredictPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.mykaarma.leadsmodel.mongo.DealerAssociate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class PostMessageSendingHelper {
	
	@Autowired
	private ThreadRepository threadRepository;
	
	@Autowired
	private Helper helper;
	
	@Autowired
	private MessageThreadRepository messageThreadRepository;
	
	@Autowired
	private DelegationHistoryRepository delegationHistoryRepository;
	
	@Autowired
	private MessagingViewControllerHelper messagingViewControllerHelper;
	
	@Autowired
	private KMessagingApiHelper kMessagingApiHelper;
	
	@Autowired
	private AppConfigHelper appConfigHelper;
	
	@Autowired
	private MessagePropertyImpl messagePropertyImpl;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	@Autowired
	private CommunicationsApiImpl communicationsApiImpl;
	
	@Autowired
	private KCommunicationsUtils kCommunicationsUtils;
	
	@Autowired
	private KNotificationApiHelper kNotificationApiHelper;
	
	@Autowired
	private KManageApiHelper kManageApiHelper;

	@Autowired
	private FollowUpApiHelper followUpApiHelper;
	
	@Autowired
	SaveMessageHelper saveMessageHelper;

	@Autowired
	private RabbitHelper rabbitHelper;

    @Autowired
    private PreferredCommunicationModeImpl preferredCommunicationModeImpl;
	
    @Autowired
    SubscriptionsApiImpl subscriptionsApiImpl;

    @Autowired
	OptOutRedisService optOutRedisService;
    
	private final static ObjectMapper objectMapper=new ObjectMapper();
	
	private final static Logger LOGGER = LoggerFactory.getLogger(PostMessageSendingHelper.class);

	private static final String MESSAGE_FAILURE_REASON_PARAM = "_messagesending_failure_reason";
	private static final String FAILED_MESSAGEID_PARAM = "FAILED_MESSAGE_ID";
	private static final String THREAD_ID_PARAM = "THREAD_ID";
	private static final String MINIMIZE_NOTIFIER_PARAM = "MinimizeNotifier";
	private static final String VIEW_BUTTON_TEXT = "VIEW";
	private static final String VIEW_BUTTON_TEXT_KEY = "failedReasonViewBtn";
	private static final String TEXT_WIDGET_KEY = "messagedisplay_widget";
	private static final String FOLLOW_UP_UUID_LIST="followUpUUIDList";
	private static final Integer DEFAULT_OPTIN_AWAITING_MESSAGE_EXPIRY = 86400;
	
	public void postMessageSendingHelper(PostMessageSent postMessageSent) {
		
		try {
			postMessageSendingHelperUtil(postMessageSent);
		} catch (Exception e) {
			
			if(e instanceof OptimisticLockingFailureException || e instanceof StaleStateException) {
				try {
					LOGGER.info(String.format("OptimisticLockingFailureException / StaleStateException occured in postMessageSendingHelperUtil "
							+ "retrying the process for message_uuid=%s , customerId=%s , dealerId=%s", postMessageSent.getMessage().getUuid()), 
							postMessageSent.getMessage().getCustomerID(), postMessageSent.getMessage().getDealerID());
					postMessageSendingHelperUtil(postMessageSent);
				} catch (Exception e2) {
					LOGGER.error("Error in processing followup message for message_uuid={} ", postMessageSent.getMessage().getUuid(), e);
				}
			} else {
				LOGGER.error("Error in processing followup message for message_uuid={} ", postMessageSent.getMessage().getUuid(), e);
			}
			
		}
	}
	
	private void postMessageSendingHelperUtil(PostMessageSent postMessageSent) throws Exception {
		Message message = postMessageSent.getMessage();

		Long customerDealerId = generalRepository.getDealerIdForCustomerId(message.getCustomerID());
		com.mykaarma.kcommunications.model.jpa.Thread thread = null;

		if(message.getDealerID().equals(customerDealerId)) {
			thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(), message.getDealerDepartmentId(), false);
		} else {
			thread = threadRepository.findFirstByCustomerIDAndClosedOrderByLastMessageOnDesc(message.getCustomerID(), false);
		}

		Boolean newThread=false;

		if (thread == null) {
			Long threadDelegatee = postMessageSent.getThreadDelegatee();
			if (MessageProtocol.NONE.getMessageProtocol().equalsIgnoreCase(message.getProtocol()) && message.getIsManual() != null && !message.getIsManual()) {
				threadDelegatee = message.getDealerAssociateID();
			}
			thread = helper.getNewThread(message, threadDelegatee, new Date());
			newThread=true;
		}
		Boolean updateThreadTimestamp = (MessageType.S.name().equals(message.getMessageType()) || (postMessageSent.getUpdateThreadTimestamp() != null && postMessageSent.getUpdateThreadTimestamp()));
		updateThreadTimestamp = updateThreadTimestamp && !isFailedDraft(message) && !postMessageSent.getIsFailedMessage();
        if (updateThreadTimestamp) {
        	LOGGER.info("Updating thread timestamp for message_id={} thread_id={} isFailedDraft={} isFailedMessage={} updateThreadTimestamp={}",
        			message.getId(), thread.getId(), isFailedDraft(message), postMessageSent.getIsFailedMessage(), postMessageSent.getUpdateThreadTimestamp());
            thread.setLastMessageOn(new Date());
        } else {
        	LOGGER.info("Not updating thread timestamp for message_id={} thread_id={} isFailedDraft={} isFailedMessage={} updateThreadTimestamp={}",
        			message.getId(), thread.getId(), isFailedDraft(message), postMessageSent.getIsFailedMessage(), postMessageSent.getUpdateThreadTimestamp());
        }
		Long previousOwner = thread.getDealerAssociateID();
		if(postMessageSent.getThreadDelegatee()!=null) {
			thread.setDealerAssociateID(postMessageSent.getThreadDelegatee());
		}
		thread.setArchived(false);
		
		try {
			thread = threadRepository.saveAndFlush(thread);
		} catch (Exception e) {
			
			if(e instanceof ObjectOptimisticLockingFailureException || e instanceof StaleObjectStateException) {
				try {
					LOGGER.info(String.format("ObjectOptimisticLockingFailureException / StaleObjectStateException occured in delegate method retrying the process for "
							+ "message_uuid=%s , customerId=%s , dealerId=%s" , message.getUuid(), message.getCustomerID(), message.getDealerID()));
					if(message.getDealerID().equals(customerDealerId)) {
						thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(), message.getDealerDepartmentId(), false);
					} else {
						thread = threadRepository.findFirstByCustomerIDAndClosedOrderByLastMessageOnDesc(message.getCustomerID(), false);
					}

					if(MessageType.S.name().equals(message.getMessageType()) && !isFailedDraft(message)) {
			            thread.setLastMessageOn(new Date());
			        }
					if(postMessageSent.getThreadDelegatee()!=null) {
						thread.setDealerAssociateID(postMessageSent.getThreadDelegatee());
					}
					thread.setArchived(false);
					thread = threadRepository.saveAndFlush(thread);
				} catch (Exception e2) {
					LOGGER.error("Error in processing followup message for message_uuid={} ", message.getUuid(), e);
				}
			} else {
				LOGGER.error("Error in processing followup message for message_uuid={} ", message.getUuid(), e);
			}
			
		}
		
		if(newThread) {
			LOGGER.info("publishing new thread creation event for thread_id={} dealer_id={} message_id={}",thread.getId(),message.getDealerID(),message.getId());
        	messagingViewControllerHelper.publishThreadCreatedEvent(thread, message.getDealerID());
        }

		saveMessageThread(thread, postMessageSent, updateThreadTimestamp);
		
		String departmentUUID = appConfigHelper.getDealerDepartmentUUIDForID(message.getDealerDepartmentId());
		
		if (postMessageSent.getThreadDelegatee()!=null) {
			if (!previousOwner.equals(postMessageSent.getThreadDelegatee())) {
				delegate(message, thread, previousOwner, customerDealerId);
			}
		}
		
		String dealerUUID = null;
        dealerUUID = generalRepository.getDealerUUIDFromDealerId(message.getDealerID());
		if (postMessageSent.getPostMessageProcessingToBeDone()) {
			try {
				kMessagingApiHelper.onMessageSending(message, departmentUUID, dealerUUID);
			} catch (Exception e) {
				LOGGER.error("Error in post message sending message_id={}", message.getId(), e);
			}
		}
		try {
			if (message.getIsManual()!=null && message.getIsManual() && MessagePurpose.FOLLOWUP.name().equalsIgnoreCase(message.getMessagePurpose())) {
				messagePropertyImpl.processFollowupMessage(message, departmentUUID, message.getUuid());
			}
		} catch (Exception e) {
			LOGGER.error("Error in processing followup message for message_uuid={} ", message.getUuid(), e);
		}
		
		if (isFailedDraft(message) || (postMessageSent.getIsFailedMessage() != null && postMessageSent.getIsFailedMessage())) {
			LOGGER.info("Failed draft found. Trying to send notification for message_id={} dealer_associate_id={}", message.getId(), message.getDealerAssociateID());
			sendMessageFailedNotification(message, departmentUUID, dealerUUID, thread.getId());
		}
        predictPreferredCommunicationModeForCustomer(dealerUUID, departmentUUID, message);
		handleForFollowUp(message,postMessageSent.getIsFailedMessage());
		handleForOptInAwaitingMessage(message);
	}

	private void handleForOptInAwaitingMessage(Message message) {
		try {
			if(message.getMessageMetaData() != null && message.getMessageMetaData().getMetaData() != null) {
				Map<String, String> metaDatMap = helper.getMessageMetaDatMap(message.getMessageMetaData().getMetaData());
				if("true".equalsIgnoreCase(metaDatMap.get(MessageMetaDataConstants.QUEUE_IF_OPTED_OUT))) {
					if(message.getDraftMessageMetaData() != null && DraftStatus.QUEUED.name().equals(message.getDraftMessageMetaData().getStatus())) {
						LOGGER.info("in handleForOptInAwaitingMessage message_uuid={} is awaiting for optin. Queueing for expiry.", message.getUuid());
						OptInAwaitingMessageExpire optInAwaitingMessageExpire = new OptInAwaitingMessageExpire();
						optInAwaitingMessageExpire.setMessageUUID(message.getUuid());
						Integer delay = DEFAULT_OPTIN_AWAITING_MESSAGE_EXPIRY;
						String dsoValue = null;
						try {
							dsoValue = kManageApiHelper.getDealerSetupOptionValueForADealer(
								generalRepository.getDealerUUIDFromDealerId(message.getDealerID()),
								DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_OPTIN_AWAITING_QUEUE_TTL.getOptionKey()
							);
							delay = Integer.parseInt(dsoValue);
						} catch (Exception e) {
							LOGGER.error("error in parsing dso={} with value={}",
								DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_OPTIN_AWAITING_QUEUE_TTL.getOptionKey(), dsoValue);
						}
						rabbitHelper.pushToOptInAwaitingMessageExpireQueue(optInAwaitingMessageExpire, delay * 1000);
					} else if(message.getDraftMessageMetaData() != null && DraftStatus.DISCARDED.name().equals(message.getDraftMessageMetaData().getStatus())) {
						LOGGER.info("in handleForOptInAwaitingMessage message_uuid={} was discarded. Removing from optin awaiting message queue.", message.getUuid());
						optOutRedisService.removeMessageUUIDListFromOptinAwaitingMessageQueue(message.getDealerID(), message.getToNumber(), Collections.singletonList(message.getUuid()));
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("error in handleForOptInAwaitingMessage for message_uuid={}", message.getUuid(), e);
		}
	}

	private void handleForFollowUp(Message message,Boolean isFailedMessage){
		
		try {
			if(message==null || !MessagePurpose.FOLLOWUP.name().equalsIgnoreCase(message.getMessagePurpose())
					|| !(MessageProtocol.TEXT.getMessageProtocol().equalsIgnoreCase(message.getProtocol())
							|| MessageProtocol.EMAIL.getMessageProtocol().equalsIgnoreCase(message.getProtocol()))
					||message.getMessageMetaData()==null || message.getMessageMetaData().getMetaData()==null
					|| message.getMessageMetaData().getMetaData().isEmpty()){
				LOGGER.info("in handleForFollowUp not processing for message={} is_failed_message={}",objectMapper.writeValueAsString(message),isFailedMessage);
				return;
			}
			
			HashMap<String, String> metaDataMap = helper.getMessageMetaDatMap(message.getMessageMetaData().getMetaData());
			String followUpUUIDListStr=metaDataMap.get(FOLLOW_UP_UUID_LIST);
			
			if(followUpUUIDListStr==null || followUpUUIDListStr.isEmpty()){
				return ;
			}
			List<String> followUpUUIDList = new ObjectMapper().readValue(followUpUUIDListStr, new TypeReference<List<String>>(){});
			
			if(followUpUUIDListStr==null || followUpUUIDListStr.isEmpty()){
				return ;
			}
			
			String dealerDepartmentUUID=appConfigHelper.getDealerDepartmentUUIDForID(message.getDealerDepartmentId());
			String dealerAssociateUUID=appConfigHelper.getDealerAssociateUUIDForID(message.getDealerAssociateID());
			
			DealerAssociateExtendedDTO dealerAssocaite = kManageApiHelper.getDealerAssociateForDealerAssociateUUID(dealerDepartmentUUID, dealerAssociateUUID);
			
			String userUUID=dealerAssocaite.getUserUuid();
			
			DealerAssociate actionTakenBy=new DealerAssociate();
			actionTakenBy.setFName(dealerAssocaite.getFirstName());
			actionTakenBy.setLName(dealerAssocaite.getLastName());
			actionTakenBy.setUuid(dealerAssocaite.getUuid());
			
			MultipleFollowupUpdateRequest multipleFollowupUpdateRequest=new MultipleFollowupUpdateRequest();
			multipleFollowupUpdateRequest.setFollowUpActionType(FollowUpActionType.FOLLOW_UP_STATUS_UPDATE);
			multipleFollowupUpdateRequest.setFollowUpUUIDs(followUpUUIDList);
			if(MessageProtocol.EMAIL.getMessageProtocol().equalsIgnoreCase(message.getProtocol())){
				multipleFollowupUpdateRequest.setUpdateEmailMessageFollowupStatus(true);
				if(isFailedDraft(message) || (isFailedMessage != null && isFailedMessage)){
					multipleFollowupUpdateRequest.setEmailMessageFollowupStatus(FollowUpState.NOT_STARTED);
					multipleFollowupUpdateRequest.setEmailMessageUUID(null);
					multipleFollowupUpdateRequest.setFollowedUpStatus(FollowUpState.NOT_STARTED);
					multipleFollowupUpdateRequest.setFollowedUpBy(new DealerAssociate());
					multipleFollowupUpdateRequest.setActionTakenBy(actionTakenBy);
				} else if ( MessageType.F.name().equalsIgnoreCase(message.getMessageType())){
					multipleFollowupUpdateRequest.setEmailMessageFollowupStatus(FollowUpState.SCHEDULED);
					multipleFollowupUpdateRequest.setEmailMessageUUID(message.getUuid());
					multipleFollowupUpdateRequest.setFollowedUpStatus(FollowUpState.SCHEDULED);
					multipleFollowupUpdateRequest.setActionTakenBy(actionTakenBy);
					multipleFollowupUpdateRequest.setFollowedUpBy(actionTakenBy);
				} else {
					multipleFollowupUpdateRequest.setEmailMessageFollowupStatus(FollowUpState.COMPLETE);
					multipleFollowupUpdateRequest.setEmailMessageUUID(message.getUuid());
					multipleFollowupUpdateRequest.setFollowedUpStatus(FollowUpState.COMPLETE);
					multipleFollowupUpdateRequest.setActionTakenBy(actionTakenBy);
					multipleFollowupUpdateRequest.setFollowedUpBy(actionTakenBy);
					multipleFollowupUpdateRequest.setUpdateFollowedUpBy(true);
				}
			} else if(MessageProtocol.TEXT.getMessageProtocol().equalsIgnoreCase(message.getProtocol())){
				multipleFollowupUpdateRequest.setUpdateTextMessageFollowupStatus(true);
				if(isFailedDraft(message) || (isFailedMessage != null && isFailedMessage)){
					multipleFollowupUpdateRequest.setTextMessageFollowupStatus(FollowUpState.NOT_STARTED);
					multipleFollowupUpdateRequest.setTextMessageUUID(null);
					multipleFollowupUpdateRequest.setFollowedUpStatus(FollowUpState.NOT_STARTED);
					multipleFollowupUpdateRequest.setFollowedUpBy(new DealerAssociate());
					multipleFollowupUpdateRequest.setActionTakenBy(actionTakenBy);
				} else if ( MessageType.F.name().equalsIgnoreCase(message.getMessageType())){
					multipleFollowupUpdateRequest.setTextMessageFollowupStatus(FollowUpState.SCHEDULED);
					multipleFollowupUpdateRequest.setTextMessageUUID(message.getUuid());
					multipleFollowupUpdateRequest.setFollowedUpStatus(FollowUpState.SCHEDULED);
					multipleFollowupUpdateRequest.setActionTakenBy(actionTakenBy);
					multipleFollowupUpdateRequest.setFollowedUpBy(actionTakenBy);
				} else {
					multipleFollowupUpdateRequest.setTextMessageFollowupStatus(FollowUpState.COMPLETE);
					multipleFollowupUpdateRequest.setTextMessageUUID(message.getUuid());
					multipleFollowupUpdateRequest.setFollowedUpStatus(FollowUpState.COMPLETE);
					multipleFollowupUpdateRequest.setActionTakenBy(actionTakenBy);
					multipleFollowupUpdateRequest.setFollowedUpBy(actionTakenBy);
					multipleFollowupUpdateRequest.setUpdateFollowedUpBy(true);
				}
			}
			
			
			followUpApiHelper.updateFollowUps(dealerDepartmentUUID, userUUID, multipleFollowupUpdateRequest);
			KNotificationMessage kNotificationMessage = new KNotificationMessage();
			Set<Long> daidSet = new HashSet<>();
			daidSet.add(message.getDealerAssociateID());
			
			kNotificationMessage = kNotificationApiHelper.getKNotificationMessage(null, message.getCustomerID(), message.getDealerAssociateID(), message.getDealerID(), null, 
					daidSet, daidSet, null, null, null, EventName.FOLLOWED_UP.name(), false, true);
			LOGGER.info("update notification message followed up={} ", new ObjectMapper().writeValueAsString(kNotificationMessage));
			kNotificationApiHelper.pushToPubnub(kNotificationMessage);
	
		} catch (Exception e) {
			LOGGER.info(String.format("error in handleForFollowUp for message_uuid=%s is_failed=%s", message.getUuid(),isFailedMessage));
			e.printStackTrace();
		}

	}
	
    private void predictPreferredCommunicationModeForCustomer(String dealerUUID, String departmentUUID, Message message) {
        try {
            ObjectMapper om = new ObjectMapper();
            LOGGER.info("in predictPreferredCommunicationModeForCustomer for dealer_uuid={} department_uuid={} message={}", dealerUUID, departmentUUID, om.writeValueAsString(message));
            String customerUUID = generalRepository.getCustomerUUIDFromCustomerID(message.getCustomerID());
            LOGGER.info("in predictPreferredCommunicationModeForCustomer for dealer_uuid={} department_uuid={} customerUUID={}", dealerUUID, departmentUUID, customerUUID);
            String preferredCommunicationModePredictionEnabledDSO = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.COMMUNICATIONS_CUSTOMER_COMMUNICATIONMODE_PREDICTION_ENABLE_ALPHA.getOptionKey());
            LOGGER.info("in predictPreferredCommunicationModeForCustomer for department_uuid={} customerUUID={} customer_comm_mode_prediction_dso={}", departmentUUID, customerUUID, preferredCommunicationModePredictionEnabledDSO);
            if("true".equalsIgnoreCase(preferredCommunicationModePredictionEnabledDSO)) {
                PredictPreferredCommunicationModeRequest request = new PredictPreferredCommunicationModeRequest();
                request.setMessageUUID(message.getUuid());
                preferredCommunicationModeImpl.predictPreferredCommunicationMode(departmentUUID, customerUUID, request);
            }
        } catch (Exception e) {
            LOGGER.error("error in predictPreferredCommunicationModeForCustomer for department_uuid={} message_uuid={}", departmentUUID, message.getUuid(), e);
        }
    }
	private void sendMessageFailedNotification(Message message, String departmentUUID, String dealerUUID, Long threadID) {
		if (message == null) {
			return;
		}
		if (dealerUUID == null) {
			dealerUUID = generalRepository.getDealerUUIDFromDealerId(message.getDealerID());
		}
		String userUUID = generalRepository.getUserUUIDForDealerAssociateID(message.getDealerAssociateID());
		if (KManageApiHelper.checkDealerAssociateAuthority(Authority.NOTIFY_FOR_FAILED_MESSAGE.getAuthority(), userUUID, departmentUUID)) {
			String messageBody;
			String locale = null;
			LOGGER.info("User has authority to receive notification for message sending failure. Proceeding. "
					+ "failed_message_id={} dealer_id={} dealer_associate_id={} ", message.getId(), message.getDealerID(), message.getDealerAssociateID());
			try {
				locale = helper.getDealerPreferredLocale(dealerUUID);
				messageBody = helper.getEmailTemplateTypeAndDealerID(message.getDealerID(), TemplateType.MESSAGE_FAILURE_NOTIFICATION.getTitle(), locale);
			} catch (Exception e) {
				LOGGER.error("Error while getting template for posting notification of message sending failure. message_id={} dealer_id={}", message.getId(), message.getDealerID());
				return;
			}
			
			try {
				String messageSendingFailureReason = message.getDraftMessageMetaData() != null ? message.getDraftMessageMetaData().getReasonForLastFailure() : message.getTwilioDeliveryFailureMessage();
				if (messageSendingFailureReason == null) {
					LOGGER.warn("No reason available for message failure. message_id={} Not sending the notification", message.getId());
					return;
				} else {
					String messageSendingFailureI18nReason = appConfigHelper.getTranslatedText("messagedisplay_widget", messageSendingFailureReason, locale);
					if (messageSendingFailureI18nReason != null && !messageSendingFailureI18nReason.trim().isEmpty()) {
						messageSendingFailureReason = messageSendingFailureI18nReason;
					}
				}
				messageBody = messageBody.replace(MESSAGE_FAILURE_REASON_PARAM, messageSendingFailureReason);
				
				MessageAttributes messageAttributes = kCommunicationsUtils.getMessageAttributes(messageBody, false, MessageProtocol.NONE,
						com.mykaarma.kcommunications_model.enums.MessagePurpose.MESSAGE_SEND_FAILURE_NOTIFICATION, null, null,
						com.mykaarma.kcommunications_model.enums.MessageType.NOTE, false, false, false, null, null);
				MessageSendingAttributes messageSendingAttributes = kCommunicationsUtils.getMessageSendingAttributes(null, null, false, false,
						false, null, null, null, true, false, false, false);
				List<NotificationButton> notificationButtons = new ArrayList<NotificationButton>();
				HashMap<String, String> buttonActionEventData = new HashMap<String, String>();
				buttonActionEventData.put(FAILED_MESSAGEID_PARAM, message.getId()+"");
				buttonActionEventData.put(THREAD_ID_PARAM, threadID+"");
				buttonActionEventData.put(MINIMIZE_NOTIFIER_PARAM, "true");
				NotificationButton notificationButton = kCommunicationsUtils.getNotificationButton(TEXT_WIDGET_KEY, VIEW_BUTTON_TEXT_KEY, VIEW_BUTTON_TEXT, NotificationButtonTheme.PRIMARY, buttonActionEventData);
				notificationButtons.add(notificationButton);
				List<String> notificationDAUUIDs = new ArrayList<String>();
				notificationDAUUIDs.add(appConfigHelper.getDealerAssociateUUIDForID(message.getDealerAssociateID()));
				NotificationAttributes notificationAttributes = kCommunicationsUtils.getNotificationAttributes(false, false, false, notificationDAUUIDs, notificationButtons);
				SendMessageRequest sendMessageRequest = kCommunicationsUtils.getSendMessageRequest(messageAttributes, messageSendingAttributes, notificationAttributes);
				
				communicationsApiImpl.createMessage(generalRepository.getCustomerUUIDFromCustomerID(message.getCustomerID()), departmentUUID,
						userUUID, sendMessageRequest, null);
				LOGGER.info("Request submitted for message failure notification for message_id={} ", message.getId());
			} catch (Exception e) {
				LOGGER.warn("Error while submitting request for message failure notification for message_id={} ", message.getId());
			}
		} else {
			LOGGER.info("User doesn't have authority to receive notification for message sending failure. Skipping. "
					+ "failed_message_id={} dealer_id={} dealer_associate_id={} ", message.getId(), message.getDealerID(), message.getDealerAssociateID());
		}
	}

	private void saveMessageThread(com.mykaarma.kcommunications.model.jpa.Thread thread, PostMessageSent postMessageSent, Boolean updateThreadTimestamp) {
	
		Message message = postMessageSent.getMessage();
		MessageThread messageThread = new MessageThread();
		messageThread.setMessageID(message.getId());
		messageThread.setThreadID(thread.getId());
		
		try {
			messageThreadRepository.saveAndFlush(messageThread);
			LOGGER.info("MessageThread saved for threadID = {}, messageID = {}", thread.getId(), message.getId());
		}catch(Exception e) {
			LOGGER.info("Error while saving messageThread",e);
		}
		
		if (postMessageSent.getIsFailedMessage() != null && postMessageSent.getIsFailedMessage()) {
			return; //Do not publish MVC event for non-draft failed message
		}
        EventName eventName;
        if(postMessageSent.getIsEditedDraft() != null && postMessageSent.getIsEditedDraft() && MessageType.F.name().equals(message.getMessageType())) {
            eventName = EventName.DRAFT_MESSAGE_EDITED;
        } else if(isFailedDraft(message)) {
            eventName = EventName.DRAFT_MESSAGE_FAILED;
        } else {
            eventName = messagingViewControllerHelper.getEventName(message);
        }
		if(eventName!=null) {
			LOGGER.info("event_name={} for message_id={} protocol={} message_type={}", eventName.name(), message.getId(), message.getProtocol(), message.getMessageType());
			messagingViewControllerHelper.publishMessageSaveEvent(message, eventName, thread, updateThreadTimestamp);
		} else {
			LOGGER.info("event name is null for message_id={} protocol={} message_type={}", message.getId(), message.getProtocol(), message.getMessageType());
		}
	}
	
	private void delegate(Message message, com.mykaarma.kcommunications.model.jpa.Thread thread, Long previousOwner, Long customerDealerId) {
		Delegator delegator = helper.getDelegatorForThreadDelegation(previousOwner);
		DelegationHistory delegationHistory = helper.getDelegationHistory(message, thread, previousOwner,delegator);
		
		delegationHistoryRepository.saveAndFlush(delegationHistory);
		thread.setLastDelegationOn(new Date().getTime());
		
		try {
			thread = threadRepository.saveAndFlush(thread);
		} catch (Exception e) {
			
			if(e instanceof OptimisticLockingFailureException || e instanceof StaleStateException) {
				try {
					LOGGER.info(String.format("OptimisticLockingFailureException / StaleStateException occured in delegate method retrying the process for "
							+ "message_uuid=%s , customerId=%s , dealerId=%s" , message.getUuid(), message.getCustomerID(), message.getDealerID()));

					if(message.getDealerID().equals(customerDealerId)) {
						thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(), message.getDealerDepartmentId(), false);
					} else {
						thread = threadRepository.findFirstByCustomerIDAndClosedOrderByLastMessageOnDesc(message.getCustomerID(), false);
					}

					thread.setLastDelegationOn(new Date().getTime());
					thread = threadRepository.saveAndFlush(thread);
				} catch (Exception e2) {
					LOGGER.error("Error in processing followup message for message_uuid={} ", message.getUuid(), e);
				}
			} else {
				LOGGER.error("Error in processing followup message for message_uuid={} ", message.getUuid(), e);
			}
			
		}
		
		messagingViewControllerHelper.publishDelegationEvent(thread, previousOwner, message);
	}
	
	private Boolean isFailedDraft(Message message) {
        if(MessageType.S.name().equalsIgnoreCase(message.getMessageType()) || MessageType.F.name().equalsIgnoreCase(message.getMessageType())) {
			if (message.getDraftMessageMetaData() != null && DraftStatus.FAILED.name().equalsIgnoreCase(message.getDraftMessageMetaData().getStatus())) {
				return true;
			}
        }
        return false;
    }
	
	public void saveMessageThread(Long messageId, Long threadId) {
		
		MessageThread messageThread = new MessageThread();
		messageThread.setMessageID(messageId);
		messageThread.setThreadID(threadId);
		messageThreadRepository.saveAndFlush(messageThread);
		
	}
	public Long postMessageSaveHandler(Message message, Long threadId, Boolean logInMongo) {
		
		com.mykaarma.kcommunications.model.jpa.Thread thread = null;
		EventName eventName = messagingViewControllerHelper.getEventName(message);
		
		LOGGER.info("event_name={} thread_id={} for message_uuid={} logInMongo={}", eventName, 
				threadId, message.getUuid(), logInMongo);
		
		saveMessageThread(message.getId(), threadId);
		
		if((logInMongo!=null &&  logInMongo) || EventName.DRAFT_MESSAGE_SAVED.equals(eventName)) {
			
			com.mykaarma.kcommunications.model.jpa.Thread latestThread = threadRepository.findById(threadId).get();
			if(message.getSentOn().after(latestThread.getLastMessageOn())
					|| message.getSentOn().equals(latestThread.getLastMessageOn())) {
				LOGGER.info("sending event info to message view controller");
				latestThread.setLastMessageOn(message.getSentOn());
				threadRepository.saveAndFlush(latestThread);
				messagingViewControllerHelper.publishHistoricalData(message, eventName, latestThread, true);
			}
			else {
				if(EventName.DRAFT_MESSAGE_SAVED.equals(eventName))
				{
					messagingViewControllerHelper.publishHistoricalData(message, eventName, latestThread, true);
				}
			}
		}
		
		return threadId;
	}
}

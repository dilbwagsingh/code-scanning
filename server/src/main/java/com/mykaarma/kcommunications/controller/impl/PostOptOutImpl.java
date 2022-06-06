package com.mykaarma.kcommunications.controller.impl;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mykaarma.global.PredictionFeature;
import com.mykaarma.kcommunications.communications.repository.UIElementTranslationRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageMetaDataRepository;
import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageMetaData;
import com.mykaarma.kcommunications.model.kne.KNotificationMessage;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.rabbit.OptOutStatusUpdate;
import com.mykaarma.kcommunications.model.rabbit.PostOptOutStatusUpdate;
import com.mykaarma.kcommunications.model.rabbit.PostOptOutStatusUpdate.PostOptOutStatusUpdateEvent;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.redis.OptOutRedisService;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KNotificationApiHelper;
import com.mykaarma.kcommunications.utils.MessageMetaDataConstants;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.enums.MessageKeyword;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.OptOutState;
import com.mykaarma.kcommunications_model.request.SendDraftRequest;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.templateengine.TemplateEngine;

@Service
public class PostOptOutImpl {

    @Autowired
    private GeneralRepository generalRepository;

    @Autowired
    private UIElementTranslationRepository uiElementTranslationRepository;

    @Autowired
    private Helper helper;

    @Autowired
    private CommunicationsApiImpl communicationsApiImpl;

    @Autowired
    private OptOutRedisService optOutRedisService;

    @Autowired
    private KNotificationApiHelper kNotificationApiHelper;

    @Autowired
    private MessageMetaDataRepository messageMetaDataRepository;

    @Autowired
    private RabbitHelper rabbitHelper;

    @Autowired
    private MessagingViewControllerHelper messagingViewControllerHelper;

    @Autowired
    private ThreadRepository threadRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(PostOptOutImpl.class);
    private static final Long MAX_AUTORESPONDER_COUNT_IN_TIME_FRAME = 3L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public void postOptOutStatusUpdate(PostOptOutStatusUpdate postOptOutStatusUpdate) throws Exception {

        LOGGER.info("in postOptOutStatusUpdate for request={}", OBJECT_MAPPER.writeValueAsString(postOptOutStatusUpdate));

        OptOutStatusUpdate optOutStatusUpdate = postOptOutStatusUpdate.getOptOutStatusUpdate();
        String communicationValue = optOutStatusUpdate.getCommunicationValue();
        OptOutState currentOptOutState = postOptOutStatusUpdate.getCurrentOptOutState();
        Long customerID = optOutStatusUpdate.getCustomerID();
        Long departmentID = optOutStatusUpdate.getDealerDepartmentID();
        Long dealerAssociateID = optOutStatusUpdate.getDealerAssociateID();
        Long dealerID = optOutStatusUpdate.getDealerID();
        PostOptOutStatusUpdateEvent event = postOptOutStatusUpdate.getEvent();
        OptOutState newOptOutState = postOptOutStatusUpdate.getNewOptOutState();
        String template = postOptOutStatusUpdate.getTemplate();
        Map<String, Object> templateParams = postOptOutStatusUpdate.getTemplateParams();

        
        switch (event) {
            case SEND_AUTORESPONDER:
                if(!currentOptOutState.equals(newOptOutState)) {
                    optOutRedisService.deleteAutoreponderCount(dealerID, departmentID, communicationValue, template);
                }
                Long autoReponderCount = optOutRedisService.getAutoreponderCount(dealerID, departmentID, communicationValue, template);
                if(autoReponderCount == null) {
                    throw new Exception(String.format("Error in postOptOutStatusUpdate while fetching autoreponder count for dealer_id=%s department_id=%s communication_value=%s template=%s", 
                        dealerID, departmentID, communicationValue, template));
                } else if(MAX_AUTORESPONDER_COUNT_IN_TIME_FRAME.equals(autoReponderCount)) {
                    LOGGER.warn("in postOptOutStatusUpdate max_autoresponder_count={} reached for dealer_id={} department_id={} communication_value={} template={}. Discarding request",
                        MAX_AUTORESPONDER_COUNT_IN_TIME_FRAME, dealerID, departmentID, communicationValue, template);
                } else {
                    optOutRedisService.incrementAutoreponderCount(dealerID, departmentID, communicationValue, template);
                    sendMessage(dealerID, customerID, departmentID, dealerAssociateID, communicationValue, template, false, templateParams);
                }

                // Process Opt-In awaiting messages (Sending Synchronously to not create race-condition)
                try {
                    if(optOutStatusUpdate.getDoubleOptInEnabled() &&
                            OptOutState.OPTED_OUT.equals(currentOptOutState) && OptOutState.OPTED_IN.equals(newOptOutState)) {
                        postOptOutStatusUpdate.setEvent(PostOptOutStatusUpdateEvent.OPTIN_AWAITING_MESSAGE_QUEUE_PROCESSING);
                        rabbitHelper.pushToPostOptOutStatusUpdateQueue(postOptOutStatusUpdate);
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception in postOptOutStatusUpdate while processing opt-in awaiting messages for post_optout_status_update={}", OBJECT_MAPPER.writeValueAsString(postOptOutStatusUpdate), e);
                }
                break;
            case SEND_SYSTEM_NOTIFICATION:
                sendMessage(dealerID, customerID, departmentID, dealerAssociateID, communicationValue, template, true, templateParams);
                break;
            case SEND_OPTOUT_STATUS_UPDATE_KNOTIFICATION_MESSAGE:
                Set<Long> daIDSet = Collections.singleton(dealerAssociateID);
                KNotificationMessage kNotificationMessage = kNotificationApiHelper.getKNotificationMessage(null, customerID, dealerAssociateID, dealerID, departmentID, daIDSet, daIDSet, null, null, null,
                    EventName.OPTOUT_STATUS_UPDATE.name(), true, false);
                kNotificationMessage.setMessageProtocol(optOutStatusUpdate.getMessageProtocol().getMessageProtocol());
                kNotificationMessage.setCommunicationValue(optOutStatusUpdate.getCommunicationValue());
                kNotificationApiHelper.pushToPubnub(kNotificationMessage);
                break;
            case UPDATE_MESSAGE_META_DATA_AND_MESSAGE_PREDICTION:
                String messageMetaData = saveMessageMetaData(optOutStatusUpdate.getMessageID(), optOutStatusUpdate.getOptOutV2Score(), optOutStatusUpdate.getMessageKeyword(), newOptOutState);
                communicationsApiImpl.updateMessagePrediction(optOutStatusUpdate.getMessageID(), optOutStatusUpdate.getMessageKeyword().name(), messageMetaData, PredictionFeature.OPT_OUT.getFeatureKey());
                break;
            case OPTIN_AWAITING_MESSAGE_QUEUE_PROCESSING:
                processOptinAwaitingMessageQueue(dealerID, communicationValue, postOptOutStatusUpdate.getRequestedDepartmentGroupDepartments());
                break;
            case SEND_MVC_UPDATE:
                Message message = helper.getMessageObjectById(optOutStatusUpdate.getMessageID());
                com.mykaarma.kcommunications.model.jpa.Thread thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(), message.getDealerDepartmentId(), false);
                messagingViewControllerHelper.publishMessageSaveEvent(message, EventName.STOP_MESSAGE_NEW, thread, false);
            default:
                LOGGER.warn("in postOptOutStatusUpdate unsupported_event={} for dealer_id={} department_id={} communication_value={}. Discarding request",
                    event.name(), dealerID, departmentID, communicationValue);
                break;
        }
    }

    private void processOptinAwaitingMessageQueue(Long dealerID, String communicationValue, List<Long> optedInDepartmentIDList) throws Exception {
        Lock lock = null;
        try {
            lock = optOutRedisService.obtainLockOnOptInAwaitingMessageQueue(dealerID, communicationValue);
            List<String> optInAwaitingMessageUUIDList = optOutRedisService.getOptInAwaitingMessageQueue(dealerID, communicationValue);
            if(optInAwaitingMessageUUIDList == null || optInAwaitingMessageUUIDList.isEmpty()) {
                LOGGER.info("in processOptinAwaitingMessageQueue no messages awaiting optin for dealer_id={} communication_value={}",
                    dealerID, communicationValue);
                return;
            }
            List<Object[]> sendDraftList = generalRepository.filterMessagesForDepartments(optedInDepartmentIDList, optInAwaitingMessageUUIDList);
            if(sendDraftList != null && !sendDraftList.isEmpty()) {
                for(Object[] object : sendDraftList) {
                    String messageUUID = (String) object[0];
                    String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(((BigInteger)object[1]).longValue());
                    SendDraftRequest sendDraftRequest = new SendDraftRequest();
                    sendDraftRequest.setSendSynchronously(true);
                    communicationsApiImpl.sendMessage(departmentUUID, messageUUID, sendDraftRequest, false);
                    optInAwaitingMessageUUIDList.remove(messageUUID);
                }
            }
            optOutRedisService.setOptInAwaitingMessageQueue(dealerID, communicationValue, optInAwaitingMessageUUIDList);
        } finally {
            if(lock != null) {
                lock.unlock();
            }
        }
    }

    private void sendMessage(Long dealerID, Long customerID, Long departmentID, Long dealerAssociateID, String communicationValue, String template, Boolean isSystemNotification, Map<String, Object> templateParams) throws Exception {
        LOGGER.info("in postOptOutImpl sending message for dealer_id={} dealer_associate_id={} customer_id={} communication_value={} template={} system_notification={}", dealerID, dealerAssociateID, customerID, communicationValue, template, isSystemNotification);
        SendMessageRequest sendMessageRequest = createSendMessageRequestForTemplate(dealerID, customerID, communicationValue, template, isSystemNotification, templateParams);
        String customerUUID = generalRepository.getCustomerUUIDFromCustomerID(customerID);
        String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(departmentID);
        String userUUID = generalRepository.getUserUUIDForDealerAssociateID(dealerAssociateID);
        communicationsApiImpl.createMessage(customerUUID, departmentUUID, userUUID, sendMessageRequest, APIConstants.COMMUNICATIONS_API_SUBSRIBER_NAME);
    }

    public String saveMessageMetaData(Long messageID, Double optOutV2Score, MessageKeyword messageKeyword, OptOutState optOutState) throws Exception {
        MessageMetaData mmd = messageMetaDataRepository.findByMessageID(messageID);
        HashMap<String, String> metaData = new HashMap<>();
        if(mmd != null && mmd.getMetaData() != null && !mmd.getMetaData().isEmpty()) {
            metaData = helper.getMessageMetaDatMap(mmd.getMetaData());
            LOGGER.info("in saveMessageMetaData orig_meta_date={} message_id={}", OBJECT_MAPPER.writeValueAsString(metaData), messageID);
        }
        if(optOutV2Score != null) {
            metaData.put(MessageMetaDataConstants.OPT_OUT_V2_SCORE, optOutV2Score.toString());
        }
        metaData.put(MessageMetaDataConstants.MESSAGE_TYPE, messageKeyword.name());
        metaData.put(MessageMetaDataConstants.OPT_OUT_STATUS, optOutState.name());
        String messageMetaData = helper.getMessageMetaData(metaData);
        LOGGER.info("in saveMessageMetaData new_meta_data={} message_id={}", messageMetaData, messageID);
        messageMetaDataRepository.upsertMessageMetaData(messageID, messageMetaData);
        return messageMetaData;
    }

    private SendMessageRequest createSendMessageRequestForTemplate(Long dealerID, Long customerID, String communicationValue, String template, Boolean isSystemNotification, Map<String, Object> templateParams) throws Exception {
        String locale;
        String messageBody;
        if(isSystemNotification) {
            String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
            locale = helper.getDealerPreferredLocale(dealerUUID);
            messageBody = uiElementTranslationRepository.getTranslatedText(template, locale);
        } else {
            locale = helper.getCustomerPreferredLocale(customerID);
            messageBody = helper.getEmailTemplateTypeAndDealerID(dealerID, template, locale);
        }
        if(messageBody == null || messageBody.isEmpty()) {
            throw new Exception(String.format("Template not configured for template_type=%s dealer_id=%s locale=%s is_system_notification=%s", template, dealerID, locale, isSystemNotification));
		}
        if(templateParams != null) {
            messageBody = TemplateEngine.getCompiledTemplate(templateParams, messageBody);
        }
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        MessageAttributes messageAttributes = new MessageAttributes();
        MessageSendingAttributes messageSendingAttributes = new MessageSendingAttributes();
		messageAttributes.setIsManual(Boolean.FALSE);
        messageAttributes.setBody(messageBody);
        messageSendingAttributes.setSendSynchronously(true);
		if(isSystemNotification) {
            messageAttributes.setProtocol(MessageProtocol.NONE);
            messageAttributes.setType(MessageType.NOTE);
        } else {
            messageAttributes.setProtocol(MessageProtocol.TEXT);
            messageAttributes.setType(MessageType.OUTGOING);
            messageSendingAttributes.setOverrideOptoutRules(true);
            messageSendingAttributes.setCommunicationValueOfCustomer(KCommunicationsUtils.getNumberInInternationalFormat(communicationValue));
            messageSendingAttributes.setAddTCPAFooter(false);
        }
		sendMessageRequest.setMessageAttributes(messageAttributes);
        sendMessageRequest.setMessageSendingAttributes(messageSendingAttributes);
        return sendMessageRequest;
    }
    
}

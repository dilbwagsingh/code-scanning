package com.mykaarma.kcommunications.controller.impl;

import java.util.Date;

import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.Delegator;
import com.mykaarma.kcommunications.jpa.repository.DelegationHistoryRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageThreadRepository;
import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.model.jpa.DelegationHistory;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageThread;
import com.mykaarma.kcommunications.model.jpa.Thread;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.rabbit.PostIncomingMessageSave;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications.utils.SystemNotificationHelper;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.request.PredictPreferredCommunicationModeRequest;

@Service
public class PostIncomingMessageSaveService {

    private final static Logger LOGGER = LoggerFactory.getLogger(PostIncomingMessageSaveService.class);

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    Helper helper;

    @Autowired
    private MessagingViewControllerHelper messagingViewControllerHelper;

    @Autowired
    private MessageThreadRepository messageThreadRepository;

    @Autowired
    private GeneralRepository generalRepository;

    @Autowired
    private KManageApiHelper kManageApiHelper;

    @Autowired
    private DelegationHistoryRepository delegationHistoryRepository;

    @Autowired
    private SystemNotificationHelper systemNotificationHelper;

    @Autowired
    private PreferredCommunicationModeImpl preferredCommunicationModeImpl;

    public void postIncomingMessageSaveProcessing(PostIncomingMessageSave postIncomingMessageSave) {
        try {
            postIncomingMessageSaveProcessingUtil(postIncomingMessageSave);
        } catch (Exception e) {
            if(e instanceof OptimisticLockingFailureException || e instanceof StaleStateException) {
                try {
                    LOGGER.info(String.format("OptimisticLockingFailureException / StaleStateException occurred in postIncomingMessageSaveProcessingUtil "
                                    + "retrying the process for message_uuid=%s , customerId=%s , dealerId=%s", postIncomingMessageSave.getMessage().getUuid(),
                            postIncomingMessageSave.getMessage().getCustomerID(), postIncomingMessageSave.getMessage().getDealerID()));
                    postIncomingMessageSaveProcessingUtil(postIncomingMessageSave);
                } catch (Exception e2) {
                    LOGGER.error("Error in postIncomingMessageSaveProcessing for message_uuid={} ", postIncomingMessageSave.getMessage().getUuid(), e);
                }
            } else {
                LOGGER.error("Error in postIncomingMessageSaveProcessing for message_uuid={} ", postIncomingMessageSave.getMessage().getUuid(), e);
            }
        }
    }

    private void postIncomingMessageSaveProcessingUtil(PostIncomingMessageSave postIncomingMessageSave) throws Exception {
        Message message = postIncomingMessageSave.getMessage();
        Thread thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(), message.getDealerDepartmentId(), false);

        Boolean newThread=false;
        if(thread == null) {
            Long threadDelegatee = postIncomingMessageSave.getThreadDelegatee();
            if (MessageProtocol.NONE.getMessageProtocol().equalsIgnoreCase(message.getProtocol()) && message.getIsManual() != null && !message.getIsManual()) {
                threadDelegatee = message.getDealerAssociateID();
            }

            thread = helper.getNewThread(message, threadDelegatee, new Date());
            newThread=true;
        }

        boolean updateThreadTimestamp = (MessageType.INCOMING.getMessageType().equalsIgnoreCase(message.getMessageType()) ||
                (postIncomingMessageSave.getUpdateThreadTimestamp() != null && postIncomingMessageSave.getUpdateThreadTimestamp()));

        if (updateThreadTimestamp) {
            LOGGER.info("Updating thread timestamp for message_id={} thread_id={} updateThreadTimestamp={}", message.getId(), thread.getId(),
                    postIncomingMessageSave.getUpdateThreadTimestamp());
            thread.setLastMessageOn(new Date());
        } else {
            LOGGER.info("Not updating thread timestamp for message_id={} thread_id={} updateThreadTimestamp={}", message.getId(), thread.getId(),
                    postIncomingMessageSave.getUpdateThreadTimestamp());
        }
        thread.setArchived(false);

        Long previousOwner = thread.getDealerAssociateID();
        if(postIncomingMessageSave.getThreadDelegatee() != null) {
            thread.setDealerAssociateID(postIncomingMessageSave.getThreadDelegatee());
        }

        saveThread(thread, postIncomingMessageSave, updateThreadTimestamp);
        if(newThread) {
        	messagingViewControllerHelper.publishThreadCreatedEvent(thread, message.getDealerID());
        }

        saveMessageThread(thread, postIncomingMessageSave, updateThreadTimestamp);
        
        String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(message.getDealerDepartmentId());
        String dealerUUID = generalRepository.getDealerUUIDFromDealerId(message.getDealerID());
        if (postIncomingMessageSave.getThreadDelegatee() != null && !previousOwner.equals(postIncomingMessageSave.getThreadDelegatee())) {
                delegate(message, thread, previousOwner);
        }

        systemNotificationHelper.sendUnassignedNotification(message);
        predictPreferredCommunicationModeForCustomer(dealerUUID, departmentUUID, message);
    }

    private void saveThread(Thread thread, PostIncomingMessageSave postIncomingMessageSave, Boolean updateThreadTimestamp) {
        Message message = postIncomingMessageSave.getMessage();

        try {
            thread = threadRepository.saveAndFlush(thread);
        } catch (Exception e) {
            if(e instanceof ObjectOptimisticLockingFailureException || e instanceof StaleObjectStateException) {
                LOGGER.info(String.format("ObjectOptimisticLockingFailureException / StaleObjectStateException occurred in postIncomingMessageSaveProcessingUtil retrying the process for "
                        + "message_uuid=%s , customerId=%s , dealerId=%s", message.getUuid(), message.getCustomerID(), message.getDealerID()));

                thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(),message.getDealerDepartmentId(), false);

                if(updateThreadTimestamp) {
                    thread.setLastMessageOn(new Date());
                }
                if(postIncomingMessageSave.getThreadDelegatee()!=null) {
                    thread.setDealerAssociateID(postIncomingMessageSave.getThreadDelegatee());
                }
                thread.setArchived(false);

                thread = threadRepository.saveAndFlush(thread);
            } else {
                LOGGER.error("Error in saving thread for post incoming message save for message_uuid={} ", message.getUuid(), e);
            }
        }
    }

    private void saveMessageThread(Thread thread, PostIncomingMessageSave postIncomingMessageSave, Boolean updateThreadTimestamp) {
        Message message = postIncomingMessageSave.getMessage();
        MessageThread messageThread = new MessageThread();
        messageThread.setMessageID(message.getId());
        messageThread.setThreadID(thread.getId());
        messageThreadRepository.saveAndFlush(messageThread);

        EventName eventName = messagingViewControllerHelper.getEventName(message);
        if(eventName != null) {
            LOGGER.info("event_name={} for message_id={} protocol={} message_type={}", eventName.name(), message.getId(), message.getProtocol(), message.getMessageType());
            messagingViewControllerHelper.publishMessageSaveEvent(message, eventName, thread, updateThreadTimestamp);
        } else {
            LOGGER.info("event name is null for message_id={} protocol={} message_type={}", message.getId(), message.getProtocol(), message.getMessageType());
        }
    }

    private void delegate(Message message, Thread thread, Long previousOwner) {
        Delegator delegator = helper.getDelegatorForThreadDelegation(previousOwner);
        DelegationHistory delegationHistory = helper.getDelegationHistory(message, thread, previousOwner,delegator);
        delegationHistoryRepository.saveAndFlush(delegationHistory);

        thread.setLastDelegationOn(new Date().getTime());
        try {
            thread = threadRepository.saveAndFlush(thread);
        } catch (Exception e) {
            if(e instanceof OptimisticLockingFailureException || e instanceof StaleStateException) {
                try {
                    LOGGER.info(String.format("OptimisticLockingFailureException / StaleStateException occurred in delegate method retrying the process for "
                            + "message_uuid=%s , customerId=%s , dealerId=%s", message.getUuid(), message.getCustomerID(), message.getDealerID()));
                    thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(),
                            message.getDealerDepartmentId(), false);
                    thread.setLastDelegationOn(new Date().getTime());
                    thread = threadRepository.saveAndFlush(thread);
                } catch (Exception e2) {
                    LOGGER.error("Error in saving thread in method delegate for message_uuid={} ", message.getUuid(), e);
                }
            } else {
                LOGGER.error("Error in saving thread in method delegate for message_uuid={} ", message.getUuid(), e);
            }
        }

        messagingViewControllerHelper.publishDelegationEvent(thread, previousOwner, message);
        systemNotificationHelper.addNoteForOoODelegation(message, delegator, previousOwner, thread.getDealerAssociateID());
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

}

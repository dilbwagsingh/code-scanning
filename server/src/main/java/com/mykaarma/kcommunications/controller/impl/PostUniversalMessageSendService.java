package com.mykaarma.kcommunications.controller.impl;

import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageThreadRepository;
import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageThread;
import com.mykaarma.kcommunications.model.jpa.Thread;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.mvc.ManualNoteSaveEventData;
import com.mykaarma.kcommunications.model.rabbit.PostUniversalMessageSendPayload;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications_model.enums.MessageType;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class PostUniversalMessageSendService {

    private final static Logger LOGGER = LoggerFactory.getLogger(PostUniversalMessageSendService.class);

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

    public void postUniversalMessageSendProcessing(PostUniversalMessageSendPayload postUniversalMessageSendPayload) {
        try {
            postUniversalMessageSendProcessingUtil(postUniversalMessageSendPayload);
        } catch (Exception e) {
            if(e instanceof OptimisticLockingFailureException || e instanceof StaleStateException) {
                try {
                    LOGGER.info(String.format("OptimisticLockingFailureException / StaleStateException occurred in postUniversalMessageSendProcessingUtil "
                                    + "retrying the process for message_uuid=%s , customerId=%s , dealerId=%s", postUniversalMessageSendPayload.getMessage().getUuid(),
                            postUniversalMessageSendPayload.getMessage().getCustomerID(), postUniversalMessageSendPayload.getMessage().getDealerID()));
                    postUniversalMessageSendProcessingUtil(postUniversalMessageSendPayload);
                } catch (Exception e2) {
                    LOGGER.error("Error in postUniversalMessageSendProcessing for message_uuid={} ", postUniversalMessageSendPayload.getMessage().getUuid(), e);
                }
            } else {
                LOGGER.error("Error in postUniversalMessageSendProcessing for message_uuid={} ", postUniversalMessageSendPayload.getMessage().getUuid(), e);
            }
        }
    }

    private void postUniversalMessageSendProcessingUtil(PostUniversalMessageSendPayload postUniversalMessageSendPayload) throws Exception {
        Message message = postUniversalMessageSendPayload.getMessage();
        Thread thread = null;

        Long customerDealerId = generalRepository.getDealerIdForCustomerId(message.getCustomerID());
        if(message.getDealerID().equals(customerDealerId)) {
            thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(), message.getDealerDepartmentId(), false);
        } else {
            thread = threadRepository.findFirstByCustomerIDAndClosedOrderByLastMessageOnDesc(message.getCustomerID(), false);
        }

        Boolean newThread=false;

        if(thread == null) {
            Long threadAssignee = null;
            if(MessageType.NOTE.getMessageType().equalsIgnoreCase(message.getMessageType())) {
                threadAssignee = message.getDealerAssociateID();
            }

            thread = helper.getNewThread(message, threadAssignee, new Date());
            newThread=true;
        }

        boolean updateThreadTimestamp = (postUniversalMessageSendPayload.getUpdateThreadTimestamp() != null && postUniversalMessageSendPayload.getUpdateThreadTimestamp());
        if (updateThreadTimestamp) {
            LOGGER.info("Updating thread timestamp for message_id={} thread_id={} updateThreadTimestamp={}", message.getId(), thread.getId(),
                    postUniversalMessageSendPayload.getUpdateThreadTimestamp());
            thread.setLastMessageOn(new Date());
        } else {
            LOGGER.info("Not updating thread timestamp for message_id={} thread_id={} updateThreadTimestamp={}", message.getId(), thread.getId(),
                    postUniversalMessageSendPayload.getUpdateThreadTimestamp());
        }
        thread.setArchived(false);

        saveThread(thread, message, updateThreadTimestamp, customerDealerId);
        if(newThread) {
            LOGGER.info("publishing new thread creation event for thread_id={} dealer_id={} message_id={}", thread.getId(), message.getDealerID(), message.getId());
            messagingViewControllerHelper.publishThreadCreatedEvent(thread, message.getDealerID());
        }

        saveMessageThread(thread, postUniversalMessageSendPayload);
    }

    private void saveThread(Thread thread, Message message, Boolean updateThreadTimestamp, Long customerDealerId) {
        try {
            thread = threadRepository.saveAndFlush(thread);
        } catch (Exception e) {
            if(e instanceof ObjectOptimisticLockingFailureException || e instanceof StaleObjectStateException) {
                LOGGER.info(String.format("ObjectOptimisticLockingFailureException / StaleObjectStateException occurred in postUniversalMessageSendProcessingUtil retrying the process for "
                        + "message_uuid=%s , customerId=%s , dealerId=%s", message.getUuid(), message.getCustomerID(), message.getDealerID()));

                if(message.getDealerID().equals(customerDealerId)) {
                    thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(), message.getDealerDepartmentId(), false);
                }else {
                    thread = threadRepository.findFirstByCustomerIDAndClosedOrderByLastMessageOnDesc(message.getCustomerID(), false);
                }

                thread.setArchived(false);
                if(updateThreadTimestamp) {
                    thread.setLastMessageOn(new Date());
                    thread = threadRepository.saveAndFlush(thread);
                }
            } else {
                LOGGER.error("Error in saving thread for post universal message send for message_uuid={} ", message.getUuid(), e);
            }
        }
    }

    private void saveMessageThread(Thread thread, PostUniversalMessageSendPayload postUniversalMessageSendPayload) {
        Message message = postUniversalMessageSendPayload.getMessage();

        MessageThread messageThread = new MessageThread();
        messageThread.setMessageID(message.getId());
        messageThread.setThreadID(thread.getId());
        messageThreadRepository.saveAndFlush(messageThread);

        EventName eventName = messagingViewControllerHelper.getEventName(message);
        if(eventName != null) {
            LOGGER.info("event_name={} for message_id={} protocol={} message_type={}", eventName.name(), message.getId(), message.getProtocol(), message.getMessageType());
            ManualNoteSaveEventData internalNoteSaveEventData = messagingViewControllerHelper.prepareInternalCommentSaveEvent(
                    message, thread, postUniversalMessageSendPayload.getUsersToNotify(), eventName);
            messagingViewControllerHelper.publishInternalCommentSaveEvent(internalNoteSaveEventData);
        } else {
            LOGGER.info("event name is null for message_id={} protocol={} message_type={}", message.getId(), message.getProtocol(), message.getMessageType());
        }
    }

}

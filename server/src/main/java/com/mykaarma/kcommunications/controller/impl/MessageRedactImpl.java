package com.mykaarma.kcommunications.controller.impl;

import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.redis.MessageRedactRedisService;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.MessageRedactApiHelper;
import com.mykaarma.kcommunications_model.response.MessageRedactResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MessageRedactImpl {
    
    @Autowired
    private ValidateRequest validateRequest;
    
    @Autowired
    private Helper helper;
    
    @Autowired
    private KCommunicationsUtils kCommunicationsUtils;

    @Autowired
    private MessageRedactRedisService messageRedactRedisService;
    
    @Autowired
    private MessageRedactApiHelper redactMessageApiHelper;

    private Logger LOGGER = LoggerFactory.getLogger(MessageRedactImpl.class);

    public ResponseEntity<MessageRedactResponse> redactMessage(String messageUUID) throws Exception {
        
        MessageRedactResponse messageRedactResponse;
        messageRedactResponse = validateRequest.validateMessageRedactRequest(messageUUID);
		if(messageRedactResponse.getErrors() != null && !messageRedactResponse.getErrors().isEmpty()) {
			return new ResponseEntity<MessageRedactResponse>(messageRedactResponse, HttpStatus.BAD_REQUEST);
		}
        
        messageRedactResponse = new MessageRedactResponse();
        
        Message message = helper.getMessageObject(messageUUID);
        if(message == null) {
            return new ResponseEntity<MessageRedactResponse>(messageRedactResponse, HttpStatus.BAD_REQUEST);
        }
        String messageBody = null;
        messageBody = messageRedactRedisService.getRedactedMessage(messageUUID);
        if(messageBody == null) {
            messageBody = message.getMessageExtn().getMessageBody();
            Boolean isReaction = kCommunicationsUtils.isMessageReactionType(messageBody);
            if(messageBody == null) {
                messageBody = "";
            }
            if(!messageBody.isEmpty()) {
                messageBody = redactMessageApiHelper.getRedactedMessage(messageUUID, messageBody, messageRedactResponse);
                if(messageRedactResponse.getErrors() != null && !messageRedactResponse.getErrors().isEmpty()) {
                    return new ResponseEntity<MessageRedactResponse>(messageRedactResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                }        
                if(!isReaction) {
                    messageRedactRedisService.pushRedactedMessage(messageUUID, messageBody);
                } else {
                    LOGGER.info("message is reaction-type, not pushing into redis cache for message_uuid={} and message_body={}", messageUUID, messageBody);
                }
            }
        }
        messageRedactResponse.setRedactedMessageBody(messageBody);
        LOGGER.info("redact message completed for message_uuid={} with redacted_message_body={}", messageUUID, messageBody);

        return new ResponseEntity<MessageRedactResponse>(messageRedactResponse, HttpStatus.OK);
    }
}
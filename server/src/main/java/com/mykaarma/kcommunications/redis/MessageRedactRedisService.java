package com.mykaarma.kcommunications.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MessageRedactRedisService {

    private final static Logger LOGGER = LoggerFactory.getLogger(MessageRedactRedisService.class);	

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private final static String MESSAGE_REDACT = "MESSAGE_REDACT~";
    private final static Long KEY_EXPIRATION_DAYS = 7l;
    
    String createRedisKey(String messageUUID) {
        return MESSAGE_REDACT + messageUUID;
    }

    public void pushRedactedMessage(String messageUUID, String messageBody) {
        String key = createRedisKey(messageUUID);
        if(messageBody == null) {
            messageBody = "";           
        }
        try {
            stringRedisTemplate.opsForValue().set(key, messageBody, KEY_EXPIRATION_DAYS, TimeUnit.DAYS);
            LOGGER.info(String.format("pushRedactedMessage for message_uuid=%s option_key=%s option_value=%s", 
            messageUUID, key, messageBody));
        } catch(Exception e) {
            LOGGER.error("error in pushRedactedMessage for message_uuid=%s option_key=%s option_value=%s", 
            messageUUID, key, messageBody, e);
        }
    }

    public String getRedactedMessage(String messageUUID) {
        String key = createRedisKey(messageUUID);
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch(Exception e) {
            LOGGER.error("error in redactedMessageExists for message_uuid=%s option_key=%s", 
            messageUUID, key, e);
            return null;
        }
    }
}
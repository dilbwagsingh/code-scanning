package com.mykaarma.kcommunications.redis;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SaveMessageRedisService {

	 private final static Logger LOGGER = LoggerFactory.getLogger(SaveMessageRedisService.class);	

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private final static String HISTORICAL_MESSAGE = "HISTORICAL_MESSAGE~";
    private final static Long KEY_EXPIRATION_DAYS = 7l;
    
    String createRedisKey(String messageUUID) {
        return HISTORICAL_MESSAGE + messageUUID;
    }

    public void pudhHistoricalMessage(String sourceUuid, String messageUuid) {
        String key = createRedisKey(sourceUuid);
        if(messageUuid == null) {
        	messageUuid = "";           
        }
        try {
            stringRedisTemplate.opsForValue().set(key, messageUuid, KEY_EXPIRATION_DAYS, TimeUnit.DAYS);
            LOGGER.info(String.format("pudhHistoricalMessage for source_uuid=%s option_key=%s messageUuid=%s", 
            		sourceUuid, key, messageUuid));
        } catch(Exception e) {
            LOGGER.error("error in pudhHistoricalMessage for source_uuid=%s option_key=%s messageUuid=%s", 
            		sourceUuid, key, messageUuid, e);
        }
    }

    public String getHistoricalMessage(String sourceUuid) {
        String key = createRedisKey(sourceUuid);
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch(Exception e) {
            LOGGER.error("error in getHistoricalMessage for message_uuid=%s option_key=%s", 
            		sourceUuid, key, e);
            return null;
        }
    }
}

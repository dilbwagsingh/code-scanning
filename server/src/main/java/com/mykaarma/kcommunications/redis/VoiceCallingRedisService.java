package com.mykaarma.kcommunications.redis;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class VoiceCallingRedisService {

	
	@Autowired
    StringRedisTemplate stringRedisTemplate;
	 
	
	private static Logger LOGGER = LoggerFactory.getLogger(VoiceCallingRedisService.class);
    private final static String BROKERNUMBER_REDACT = "BROKERNUMBER_REDACT~";
    private final static Long KEY_EXPIRATION_DAYS = 7l;
    
    String createRedisKey(String brokerNumber) {
        return BROKERNUMBER_REDACT + brokerNumber;
    }
	    
	public void pushGreetingURLForBrokerNumber(String brokerNumber, String prompt) {
        String key = createRedisKey(brokerNumber);
        if(prompt == null) {
        	prompt = "";           
        }
        try {
            stringRedisTemplate.opsForValue().set(key, prompt, KEY_EXPIRATION_DAYS, TimeUnit.DAYS);
            LOGGER.info("pushRedactedBrokerNumber for brokerNumber={} option_key={} option_value={}", 
            		brokerNumber, key, prompt);
        } catch(Exception e) {
            LOGGER.error("error in pushRedactedbrokerNumber for brokerNumber={} option_key={} option_value={}", 
            		brokerNumber, key, prompt, e);
        }
    }

    public String getGreetingURLForBrokerNumber(String brokerNumber) {
        String key = createRedisKey(brokerNumber);
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch(Exception e) {
            LOGGER.error("error in redactedBrokerNumberExists for brokerNumber={} option_key={}", 
            		brokerNumber, key, e);
            return null;
        }
    }
}

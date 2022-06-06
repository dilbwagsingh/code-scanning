package com.mykaarma.kcommunications.redis;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.mykaarma.kcommunications_model.enums.CommunicationsFeature;

@Service
public class RateControllerRedisService {

	@Value("${cache-expiry-minutes:3}")
    private int expiryMinutes;
	
	@Value("${cache-expiry-days:1}")
    private int expiryDays;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(RateControllerRedisService.class);	
	
	
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	
	public static final String DEPARTMENT_UUID="DEPARTMENT_UUID";
	public static final String RATE_CONTROL="RATE_CONTROL~";
	public static final String VALUE="VALUE";
	public static final String TIMESTAMP="TIMESTAMP";
	
	private String createKeyForMinutes(String feature, String departmentUUID, String value) {
		Long currentTimeInMillis = new Date().getTime();
		Long minutes = currentTimeInMillis/(1000*60);
		minutes = minutes - minutes%2;
		String key = RATE_CONTROL+feature+"-"+DEPARTMENT_UUID+":"+departmentUUID+"~"+VALUE+":"+value+"~"+TIMESTAMP+":"+minutes;
		LOGGER.info("key={}", key);
		return key;
	}
	
	private String createKeyForDay(String feature, String departmentUUID, String value) {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String key = RATE_CONTROL+feature+"-"+DEPARTMENT_UUID+":"+departmentUUID+"~"+VALUE+":"+value+"~"+TIMESTAMP+":"+sdf.format(date);
		LOGGER.info("key={}", key);
		return key;
	}
	
	public Integer getUsageInMinutes(String departmentUUID, CommunicationsFeature communicationsFeature, String communicationValue) {
		try {
			String ans = (String) stringRedisTemplate.opsForValue().get(createKeyForMinutes(communicationsFeature.name(), departmentUUID, communicationValue));
			if(ans!=null && !ans.isEmpty()) {
				return Integer.parseInt(ans);
			}
			return 0;
		} catch (Exception e) {
			LOGGER.warn("Error in getRateControl for department_uuid={} feature={} communication_value={} ", 
					departmentUUID, communicationsFeature.name(), communicationValue, e);
			return 0;
		}
	}
	
	
	public void updateUsageInMinutes(String departmentUUID, CommunicationsFeature communicationsFeature, String communicationValue) {
		try {
			String key = createKeyForMinutes(communicationsFeature.name(), departmentUUID, communicationValue);
			stringRedisTemplate.opsForValue().increment(key, 1);
			stringRedisTemplate.expire(key, expiryMinutes, TimeUnit.MINUTES);
		} catch (Exception e) {
			LOGGER.warn("Error in updateUsage for department_uuid={} feature={} communication_value={} ", 
					departmentUUID, communicationsFeature.name(), communicationValue, e);
		}
	}
	
	public Integer getUsageInADay(String departmentUUID, CommunicationsFeature communicationsFeature, String communicationValue) {
		try {
			String ans = (String) stringRedisTemplate.opsForValue().get(createKeyForDay(communicationsFeature.name(), departmentUUID, communicationValue));
			if(ans!=null && !ans.isEmpty()) {
				return Integer.parseInt(ans);
			}
			return 0;
		} catch (Exception e) {
			LOGGER.warn("Error in getUsageInADay for department_uuid={} feature={} communication_value={} ", 
					departmentUUID, communicationsFeature.name(), communicationValue, e);
			return 0;
		}
	}
	
	
	public void updateUsageInADay(String departmentUUID, CommunicationsFeature communicationsFeature, String communicationValue) {
		try {
			String key = createKeyForDay(communicationsFeature.name(), departmentUUID, communicationValue);
			stringRedisTemplate.opsForValue().increment(key, 1);
			stringRedisTemplate.expire(key, expiryDays, TimeUnit.DAYS);
		} catch (Exception e) {
			LOGGER.warn("Error in updateUsageInADay for department_uuid={} feature={} communication_value={} ", 
					departmentUUID, communicationsFeature.name(), communicationValue, e);
		}
	}
}

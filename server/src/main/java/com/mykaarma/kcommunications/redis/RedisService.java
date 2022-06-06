package com.mykaarma.kcommunications.redis;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


@Service
public class RedisService {

	
	
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	
	private static long defaultThreadOwnerExpiry = 1;

	public final static String DEFAULT_THREAD_OWNER_MAPPING = "KCOMM_API_DEFAULT_THREAD_OWNER_MAPPING";
	
	Logger LOGGER = LoggerFactory.getLogger(RedisService.class);
	
	public void  pushDefaultThreadOwnerUserUUIDForDepartmentUUIDAndUserUUID(String departmentUUID, String userUUID,
			String defaultThreadOwnerUserUUID) {
		if(departmentUUID!=null &&userUUID!=null  && defaultThreadOwnerUserUUID!=null){
			LOGGER.info(String.format("pushDefaultThreadOwnerUserUUIDForDepartmentUUIDAndUserUUID default_thread_owner_user_uuid=%s for dealer_department_uuid=%s user_uuid=%s ", defaultThreadOwnerUserUUID,departmentUUID,userUUID));
			stringRedisTemplate.opsForValue().set(getRedisKeyForDealerAssociateIDDefaultThreadOwnerDAIDMapping(departmentUUID,userUUID), defaultThreadOwnerUserUUID, defaultThreadOwnerExpiry,TimeUnit.HOURS);
		}
	}
	
	public String getDefaultThreadOwnerUserUUIDForUserUUIDAndDealerAssociateUUID(String departmentUUID, String userUUID){
		Object defaultThreadOwnerUserUUID =stringRedisTemplate.opsForValue().get(getRedisKeyForDealerAssociateIDDefaultThreadOwnerDAIDMapping(departmentUUID,userUUID));
		if(defaultThreadOwnerUserUUID!=null) {
			LOGGER.info(String.format("getDefaultThreadOwnerDAIDForDealerAssociateID default_thread_owner_user_uuid=%s for department_uuid=%s user_uuid=%s", defaultThreadOwnerUserUUID, departmentUUID,userUUID));
			return  ((String) defaultThreadOwnerUserUUID);
		} else {
			LOGGER.info(String.format("getDefaultThreadOwnerDAIDForDealerAssociateID default_thread_owner_user_uuid=%s for department_uuid=%s user_uuid=%s", defaultThreadOwnerUserUUID, departmentUUID,userUUID));
			return null;
		}
	}
	
	public static String getRedisKeyForDealerAssociateIDDefaultThreadOwnerDAIDMapping(String departmentUUID, String userUuid){
		return DEFAULT_THREAD_OWNER_MAPPING + "~" +departmentUUID + "~" +userUuid;
	}
	
	public void addToMessageSet(Long messageID, Long timestamp, Long dealerID, String zeroDate){
         stringRedisTemplate.opsForZSet().add("MessageReceivedSet:" + dealerID + ":" + zeroDate, messageID.toString(), timestamp);
         stringRedisTemplate.expire("MessageReceivedSet:" + dealerID + ":" + zeroDate, 2, TimeUnit.DAYS);
	}
	
}



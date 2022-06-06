package com.mykaarma.kcommunications.redis;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class VoiceCredentialsService {

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public Long addVoiceCredential(Long departmentID, String brokerNumber) {
		return stringRedisTemplate.boundListOps("VoiceCredentials_"+departmentID).rightPush(brokerNumber);
	}
	
	
	public String reQueueVoiceCredential(Long departmentID) {
		String lruBrokerNumber = stringRedisTemplate.boundListOps("VoiceCredentials_"+departmentID).leftPop();
		addVoiceCredential(departmentID, lruBrokerNumber);
		return lruBrokerNumber;
	}
	
	
	public void removeVoiceCredential(Long departmentID, String brokerNumber) {
		stringRedisTemplate.boundListOps("VoiceCredentials_"+departmentID).remove(-1, brokerNumber);
	}
	
	
	public List<String> getVoiceCredentials(Long departmentID) {
		return stringRedisTemplate.boundListOps("VoiceCredentials_"+departmentID).range(0, -1);
	}
	
}

package com.mykaarma.kcommunications.redis;

import java.util.concurrent.locks.Lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.integration.redis.util.RedisLockRegistry;

@Service
public class CustomerLockRedisService {

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	private static long lockExpiry = 10000;
	private static long customerMergeLockExpiry = 120000;

	public void unLock(Lock lock) {
		if(null!=lock){
			lock.unlock();
		}
	}

	public Lock obtainLockOnCustomerMessaging(String redisKey) {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, RedisKeys.CUSTOMERSENDMESSAGELOCK.name(), lockExpiry);
		Lock lock = registry.obtain(redisKey);
		lock.lock();
		return lock;
	}
	
	public Lock obtainLockForCustomerMongoMerge(String customerGuid) {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, RedisKeys.CUSTOMER_MERGING_MONGO_LOCK.name(), customerMergeLockExpiry);
		Lock lock = registry.obtain(customerGuid);
		return lock;
	}
}

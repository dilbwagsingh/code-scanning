package com.mykaarma.kcommunications.redis;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import com.mykaarma.kcommunications.model.redis.DoubleOptInDeploymentStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;


@Service
public class OptOutRedisService {

    @Autowired
    @Qualifier("communicationsStringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Autowired
    @Qualifier("communicationsJedisConnectionFactory")
    private JedisConnectionFactory jedisConnectionFactory;

    private static final Logger LOGGER = LoggerFactory.getLogger(OptOutRedisService.class);
    private static final String OPTOUT_LOOP = "OPTOUT_LOOP";
    private static final String DOUBLE_OPTIN_DEPLOYMENT = "DOUBLE_OPTIN_DEPLOYMENT";
    private static final String OPTIN_AWAITING_MESSAGE_QUEUE = "OPTIN_AWAITING_MESSAGE_QUEUE";
    private static final long DOUBLE_OPTIN_DEPLOYMENT_KEY_EXPIRATION_MINS = 10L;
    private static final long OPTOUT_LOOP_KEY_EXPIRATION_MINS = 5L;
    private static final long OPTIN_AWAITING_MESSAGE_QUEUE_KEY_EXPIRATION_DAYS = 2L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String createOptOutLoopProcessingKey(Long dealerID, Long departmentID, String communicationValue, String autoreponderType) {
        return String.format("%s~%s:%s:%s:%s", 
            OPTOUT_LOOP, dealerID, departmentID, communicationValue, autoreponderType);
    }

    public Long getAutoreponderCount(Long dealerID, Long departmentID, String communicationValue, String autoreponderType) {
        String key = createOptOutLoopProcessingKey(dealerID, departmentID, communicationValue, autoreponderType);
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0 : Long.parseLong(value);
        } catch (Exception e) {
            LOGGER.error("Exception in getAutoreponderCount for key={}", key, e);
            return null;
        }
    }

    public void incrementAutoreponderCount(Long dealerID, Long departmentID, String communicationValue, String autoreponderType) {
        String key = createOptOutLoopProcessingKey(dealerID, departmentID, communicationValue, autoreponderType);
        Long count;
        try {
            String value = redisTemplate.opsForValue().get(key);
            count = value == null ? 0 : Long.parseLong(value);
            count += 1;
            redisTemplate.opsForValue().set(key, count.toString(), OPTOUT_LOOP_KEY_EXPIRATION_MINS, TimeUnit.MINUTES);
        } catch (Exception e) {
            LOGGER.error("Exception in incrementAutoreponderCount for key={}", key, e);
        }
    }

    public void deleteAutoreponderCount(Long dealerID, Long departmentID, String communicationValue, String autoreponderType) {
        String key = createOptOutLoopProcessingKey(dealerID, departmentID, communicationValue, autoreponderType);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            LOGGER.error("Exception in deleteAutoreponderCount for key={}", key, e);
        }
    }

    private String createDoubleOptInDeploymentProcessingKey(Long dealerID, Long customerID, String communicationValue) {
        return String.format("%s~%s:%s:%s", DOUBLE_OPTIN_DEPLOYMENT, dealerID, customerID, communicationValue);
    }

    private String createOptInAwaitingMessageQueueKey(Long dealerID, String communicationValue) {
        return String.format("%s~%s:%s", DOUBLE_OPTIN_DEPLOYMENT, dealerID, communicationValue);
    }

    /**
     * Synchronization Policy : Must call {@link #obtainLockOnDoubleOptInDeploymentStatus(Long, Long, String)}
     * before to obtain lock
     * @param dealerID
     * @param customerID
     * @param communicationValue
     * @return DoubleOptInDeploymentStatus
     */
    public DoubleOptInDeploymentStatus getDoubleOptInDeploymentStatus(Long dealerID, Long customerID, String communicationValue) {
        String key = createDoubleOptInDeploymentProcessingKey(dealerID, customerID, communicationValue);
        try {
            String value = redisTemplate.opsForValue().get(key);
            return OBJECT_MAPPER.readValue(value, DoubleOptInDeploymentStatus.class);
        } catch (Exception e) {
            LOGGER.error("Exception in getDoubleOptInDeploymentStatus for key={}", key, e);
            return null;
        }
    }

    public Lock obtainLockOnDoubleOptInDeploymentStatus(Long dealerID, Long customerID, String communicationValue) {
        String key = createDoubleOptInDeploymentProcessingKey(dealerID, customerID, communicationValue);
        RedisLockRegistry redisLockRegistry = new RedisLockRegistry(jedisConnectionFactory, DOUBLE_OPTIN_DEPLOYMENT);
        Lock lock = redisLockRegistry.obtain(key);
        lock.lock();
        return lock;
    }

    /**
     * Synchronization Policy : Must call {@link #obtainLockOnDoubleOptInDeploymentStatus(Long, Long, String)}
     * before to obtain lock
     * @param dealerID
     * @param customerID
     * @param communicationValue
     * @param doubleOptInDeploymentStatus
     */
    public void setDoubleOptInDeploymentStatus(Long dealerID, Long customerID, String communicationValue, DoubleOptInDeploymentStatus doubleOptInDeploymentStatus) {
        String key = createDoubleOptInDeploymentProcessingKey(dealerID, customerID, communicationValue);
        try {
            redisTemplate.opsForValue().set(key, OBJECT_MAPPER.writeValueAsString(doubleOptInDeploymentStatus), DOUBLE_OPTIN_DEPLOYMENT_KEY_EXPIRATION_MINS, TimeUnit.MINUTES);
        } catch (Exception e) {
            LOGGER.error("Exception in setDoubleOptInDeploymentStatus for key={}", key, e);
        }
    }


    public Lock obtainLockOnOptInAwaitingMessageQueue(Long dealerID, String communicationValue) {
        String key = createOptInAwaitingMessageQueueKey(dealerID, communicationValue);
        RedisLockRegistry redisLockRegistry = new RedisLockRegistry(jedisConnectionFactory, OPTIN_AWAITING_MESSAGE_QUEUE);
        Lock lock = redisLockRegistry.obtain(key);
        lock.lock();
        return lock;
    }

    /**
     * Synchronization Policy : Must call {@link #obtainLockOnOptInAwaitingMessageQueue(Long, String)}
     * @param dealerID
     * @param communicationValue
     * @param messageUUIDList
     */
    public void setOptInAwaitingMessageQueue(Long dealerID, String communicationValue, List<String> messageUUIDList) {
        String key = createOptInAwaitingMessageQueueKey(dealerID, communicationValue);
        try {
            redisTemplate.opsForValue().set(key, OBJECT_MAPPER.writeValueAsString(messageUUIDList), OPTIN_AWAITING_MESSAGE_QUEUE_KEY_EXPIRATION_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            LOGGER.error("Exception in setOptInAwaitingMessageQueue for key={}", key, e);
        }
    }

    /**
     * Synchronization Policy : Must call {@link #obtainLockOnOptInAwaitingMessageQueue(Long, String)}
     * @param dealerID
     * @param communicationValue
     * @return List<String>
     */
    public List<String> getOptInAwaitingMessageQueue(Long dealerID, String communicationValue) {
        String key = createOptInAwaitingMessageQueueKey(dealerID, communicationValue);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if(value == null) {
                return null;
            }
            return OBJECT_MAPPER.readValue(value, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            LOGGER.error("Exception in getOptInAwaitingMessageQueue for key={}", key, e);
            return null;
        }
    }

    public void removeMessageUUIDListFromOptinAwaitingMessageQueue(Long dealerID, String communicationValue, List<String> messageUUIDList) {
        Lock lock = null;
        try {
            lock = obtainLockOnOptInAwaitingMessageQueue(dealerID, communicationValue);
            List<String> optInAwaitingMessageQueue = getOptInAwaitingMessageQueue(dealerID, communicationValue);
            if(optInAwaitingMessageQueue != null) {
                optInAwaitingMessageQueue.removeAll(messageUUIDList);
                setOptInAwaitingMessageQueue(dealerID, communicationValue, optInAwaitingMessageQueue);
            }
        } catch (Exception e) {
            LOGGER.error("Exception in removeMessageUUIDListFromOptinAwaitingMessageQueue for dealer_id={} communication_value={}", dealerID, communicationValue, e);
        } finally {
            if(lock != null) {
                lock.unlock();
            }
        }
    }

}

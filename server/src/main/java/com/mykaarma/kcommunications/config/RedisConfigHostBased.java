package com.mykaarma.kcommunications.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;

import redis.clients.jedis.JedisPoolConfig;

@Configuration
@Profile({"default","prod-de"})
public class RedisConfigHostBased {

	@Value("${redis.host.name}")
	private String redisHost;
	
	@Value("${redis.port}")
	private int redisPort;
	
	@Value("${redis-max-total}")
	private int redisMaxTotal;
	
	@Value("${redis-max-idle}")
	private int redisMaxIdle;
	
	@Value("${redis-password}")
	private String redisPassword;
	
	@Value("${redis-timeout}")
	private int redisTimeout;
	
	@Value("${redis.env}")
	private String redisEnv;
	
	private static final String REDIS_ENV_AWS = "aws-redis";
	
	@Bean
	public JedisPoolConfig jedisPoolConfig() {
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setMaxIdle(redisMaxIdle);
		jedisPoolConfig.setMaxTotal(redisMaxTotal);
		return jedisPoolConfig;
	}
	
	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		JedisClientConfigurationBuilder jccb = JedisClientConfiguration.builder();
		jccb.connectTimeout(Duration.ofSeconds(redisTimeout));
		jccb.usePooling().poolConfig(jedisPoolConfig());
		if (REDIS_ENV_AWS.equalsIgnoreCase(redisEnv)) {
			jccb.useSsl();
		}
		return new JedisConnectionFactory(redisStandaloneConfig(), jccb.build());
	}
	
	@Bean
	public RedisStandaloneConfiguration redisStandaloneConfig() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
		config.setPassword(RedisPassword.of(redisPassword));
		return config;
	}
	
	@Bean
	public StringRedisTemplate stringRedisTemplate() {
		return new StringRedisTemplate(redisConnectionFactory());
	}
}
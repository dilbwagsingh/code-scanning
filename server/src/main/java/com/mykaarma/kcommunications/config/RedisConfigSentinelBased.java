package com.mykaarma.kcommunications.config;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPoolConfig;


@Configuration("kcommunicationsredis")
@Profile({"devvm","qa02","qa-aws","prod"})
public class RedisConfigSentinelBased {
	
	@Value("${redis-max-total}")
	private int redisMaxTotal;
	
	@Value("${redis-max-idle}")
	private int redisMaxIdle;
	
	
	@Value("${redis-master:mymaster}")
	private String redisMaster;
	
	@Value("${redis-sentinel1:}")
	private String redisSentinel1;
	
	@Value("${redis-sentinel2:}")
	private String redisSentinel2;
	
	@Value("${redis-sentinel3:}")
	private String redisSentinel3;
	
	@Value("${redis-password}")
	private String redisPassword;
	
	@Value("${redis-timeout}")
	private int redisTimeout;
	
	@Value("${redis.env:local}")
	private String redisEnv;
	
	@Value("${redis.host.name:dev.kaar-ma.com}")
	private String redisHostName;
	
	@Value("${redis.port:6379}")
	private int redisPort;
	
    @Bean
	@Primary
    public JedisPoolConfig jedisPoolConfig()
    {
    		JedisPoolConfig jpc = new JedisPoolConfig();
    		jpc.setMaxIdle(redisMaxIdle);
    		jpc.setMaxTotal(redisMaxTotal);
    		return jpc;
    }
	@Bean
	@Primary
	public JedisConnectionFactory jedisConnectionFactory()
	{
		JedisClientConfigurationBuilder jccb = JedisClientConfiguration.builder();
		jccb.connectTimeout(Duration.ofSeconds(redisTimeout));
		jccb.usePooling().poolConfig(jedisPoolConfig());
		JedisConnectionFactory jcf = new JedisConnectionFactory(redisSentinelConfig(), jccb.build());
		
		return jcf;
	}
	@Bean
	public RedisSentinelConfiguration redisSentinelConfig()
	{
		Set<String> sentinels = new HashSet<String>();
		sentinels.add(redisSentinel1);
		sentinels.add(redisSentinel2);
		sentinels.add(redisSentinel3);
		RedisSentinelConfiguration rsc = new RedisSentinelConfiguration("mymaster",sentinels);
		rsc.setPassword(RedisPassword.of(redisPassword));
		return rsc;
	}
	
	@Bean
	public StringRedisTemplate stringRedisTemplate()
	{
		return new StringRedisTemplate(jedisConnectionFactory());
	}

}

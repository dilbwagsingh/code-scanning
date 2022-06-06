package com.mykaarma.kcommunications.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class CommunicationsRedisConfig {

    @Value("${communications-redis.password}")
    private String redisPassword;

    @Value("${communications-redis.timeout}")
    private int redisTimeOut;

    @Value("${communications-redis.max-total}")
    private int redisMaxTotal;

    @Value("${communications-redis.max-idle}")
    private int redisMaxIdle;

    @Value("${communications-redis.host-name}")
    private String hostName;

    @Value("${communications-redis.port}")
    private int port;

    @Value("${communications-redis.master}")
    private String redisMaster;

    @Value("${communications-redis.useSSL}")
    private boolean useSSL;

    @Bean(name = "communicationsJedisPoolConfig")
    public JedisPoolConfig communicationsJedisPoolConfig() {
        JedisPoolConfig jpc = new JedisPoolConfig();
        jpc.setMaxIdle(redisMaxIdle);
        jpc.setMaxTotal(redisMaxTotal);
        return jpc;
    }

    @Bean(name = "communicationsJedisConnectionFactory")
    public JedisConnectionFactory communicationsJedisConnectionFactory() {

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(hostName, port);
        redisStandaloneConfiguration.setPassword(RedisPassword.of(redisPassword));
        JedisConnectionFactory jcf= new JedisConnectionFactory(redisStandaloneConfiguration);
        jcf.setUseSsl(useSSL);
        jcf.setTimeout(redisTimeOut);
        jcf.setUsePool(true);
        jcf.setPoolConfig(communicationsJedisPoolConfig());
        return jcf;
    }

    @Bean(name = "communicationsStringRedisTemplate")
    public StringRedisTemplate communicationsStringRedisTemplate()
    {
        return new StringRedisTemplate(communicationsJedisConnectionFactory());
    }
}
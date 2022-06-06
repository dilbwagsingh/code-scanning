package com.mykaarma.kcommunications.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JasyptConfig {

	@Value("${jasypt.encryptor.password}")
    private String password;

    @Value("${jasypt.encryptor.algorithm}")
    private String algorithm;

    @Value("${jasypt.encryptor.key-obtention-iterations}")
    private String keyObtentionIterations;

    @Value("${jasypt.encryptor.pool-size}")
    private String poolSize;

    @Value("${jasypt.encryptor.provider-name}")
    private String providerName;

    @Value("${jasypt.encryptor.salt-generator-classname}")
    private String saltGeneratorClassName;

    @Value("${jasypt.encryptor.iv-generator-classname}")
    private String ivGeneratorClassName;

    @Value("${jasypt.encryptor.string-output-type}")
    private String stringOutputType;

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(this.password);
        config.setAlgorithm(this.algorithm);
        config.setKeyObtentionIterations(this.keyObtentionIterations);
        config.setPoolSize(this.poolSize);
        config.setProviderName(this.providerName);
        config.setSaltGeneratorClassName(this.saltGeneratorClassName);
        config.setIvGeneratorClassName(this.ivGeneratorClassName);
        config.setStringOutputType(this.stringOutputType);
        encryptor.setConfig(config);
        return encryptor;
    }

}
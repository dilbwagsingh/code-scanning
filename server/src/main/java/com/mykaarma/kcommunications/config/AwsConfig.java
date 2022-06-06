package com.mykaarma.kcommunications.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Configuration
public class AwsConfig {
	
	@Value("${aws.kcommunication-server.accesskey}")
	private String awsReportingAccessKey;
	
	@Value("${aws.kcommunication-server.secretkey}")
	private String awsReportingSecretKey;
	
	@Value("${aws.kcommunication-server.region}")
	private String awsReportingRegion;
	
	@Bean
	public AwsBasicCredentials awsCreds() {
		return AwsBasicCredentials.create(awsReportingAccessKey, awsReportingSecretKey);
	}
	
	@Bean 
	public EventBridgeClient awsEventBridgeClient() throws Exception {
	
		EventBridgeClient eventBridgeClient = EventBridgeClient.builder()
				.region(Region.of(awsReportingRegion))
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds()))
				.build();
		
		return eventBridgeClient;
	}
}

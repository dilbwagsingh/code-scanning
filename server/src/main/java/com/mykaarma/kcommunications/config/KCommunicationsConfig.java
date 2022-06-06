/**
 * 
 */
package com.mykaarma.kcommunications.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.web.client.RestTemplate;

/**
 * @author root
 *
 */
@Configuration
public class KCommunicationsConfig {

	
	@Bean(name="kCommunicationsRestTemplate")
	public RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setReadTimeout(0);
		requestFactory.setConnectTimeout(0);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		return restTemplate;
	}
	
	@Bean("kCommunicationsHeaderMapper")
	public DefaultAmqpHeaderMapper kCommunicationsHeaderMapper() {
		DefaultAmqpHeaderMapper amqpHeaderMapper = DefaultAmqpHeaderMapper.inboundMapper();
		amqpHeaderMapper.setRequestHeaderNames("*");
		amqpHeaderMapper.setReplyHeaderNames("*");
		return amqpHeaderMapper;
	}
	
}

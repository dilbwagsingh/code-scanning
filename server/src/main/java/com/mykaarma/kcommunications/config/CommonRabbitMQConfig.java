package com.mykaarma.kcommunications.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class CommonRabbitMQConfig implements RabbitListenerConfigurer {

	@Value("${rabbitmq.common.host}")
	private String commonRmqHost;

	@Value("${rabbitmq.common.port}")
	private int commonRmqPort;

	@Value("${rabbitmq.common.username}")
	private String commonRmqUsername;

	@Value("${rabbitmq.common.password}")
	private String commonRmqPassword;
	
	@Value("${rabbitmq.common.consumers-min}")
	private int commonRmqMinConsumers;
	
	@Value("${rabbitmq.common.consumers-max}")
	private int commonRmqMaxConsumers;
	
	@Value("${rabbitmq.common.prefetch-count}")
	private int commonRmqPrefetchCount;
	
	@Value("${rabbitmq.common.dead-letter-exchange}")
	private String commonDeadLetterExchange;

	
	/* RMQ configuration */
	@Bean("commonRabbitJsonMessageConverter")
	public MessageConverter commonRabbitJsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean("commonRabbitConsumerJackson2MessageConverter")
	public MappingJackson2MessageConverter commonRabbitConsumerJackson2MessageConverter() {
		return new MappingJackson2MessageConverter();
	}

	@Bean("commonRabbitMessageHandlerMethodFactory")
	public DefaultMessageHandlerMethodFactory commonRabbitMessageHandlerMethodFactory() {
		DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
		factory.setMessageConverter(commonRabbitConsumerJackson2MessageConverter());
		return factory;
	}

	@Override
	public void configureRabbitListeners(final RabbitListenerEndpointRegistrar registrar) {
		registrar.setMessageHandlerMethodFactory(commonRabbitMessageHandlerMethodFactory());
	}
	
	@Bean(name = "commonRabbitConnectionFactory")
	public ConnectionFactory commonRabbitConnectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(commonRmqHost,
				commonRmqPort);
		connectionFactory.setUsername(commonRmqUsername);
		connectionFactory.setPassword(commonRmqPassword);
		return connectionFactory;
	}
	
	@Bean(name = "commonAMQPAdmin")
    public AmqpAdmin commonAMQPAdmin() {
        return new RabbitAdmin(commonRabbitConnectionFactory());
    }

	
	@Bean(name = "commonRmqTemplate")
	public RabbitTemplate commonRmqTemplate(@Qualifier("commonRabbitConnectionFactory") final ConnectionFactory connectionFactory) {
		final RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(commonRabbitJsonMessageConverter());
		return template;
	}
	
	@Bean(name = "commonRmqInterceptor")
	RetryOperationsInterceptor commonRmqInterceptor(@Qualifier(value = "commonRmqTemplate") RabbitTemplate rabbitTemplate) {
		RepublishMessageRecoverer republishMessageRecoverer = new RepublishMessageRecoverer(rabbitTemplate, commonDeadLetterExchange);
		republishMessageRecoverer.errorRoutingKeyPrefix("fail.");
		return RetryInterceptorBuilder.stateless()
				.maxAttempts(4)
				.backOffOptions(1000, 10, 100000)
				.recoverer(republishMessageRecoverer)
				.build();
	}
	
	@Bean(name = "commonRabbitListenerContainerFactory")
	public RabbitListenerContainerFactory<SimpleMessageListenerContainer> commonRabbitListenerContainerFactory(
			@Qualifier("commonRabbitConnectionFactory") ConnectionFactory connectionFactory,
			@Qualifier(value = "commonRmqInterceptor") RetryOperationsInterceptor commonRmqInterceptor) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setConcurrentConsumers(commonRmqMinConsumers);
		factory.setMaxConcurrentConsumers(commonRmqMaxConsumers);
		factory.setPrefetchCount(commonRmqPrefetchCount);
		factory.setAdviceChain(commonRmqInterceptor);
		return factory;
	}
}

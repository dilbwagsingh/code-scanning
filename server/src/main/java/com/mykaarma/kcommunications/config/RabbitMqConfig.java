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
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMqConfig  implements RabbitListenerConfigurer {

	@Value("${rabbitmq.communications.host}")
	private String communicationsRmqHost;

	@Value("${rabbitmq.communications.port}")
	private int communicationsRmqHostPort;

	@Value("${rabbitmq.communications.username}")
	private String communicationsRmqUsername;

	@Value("${rabbitmq.communications.password}")
	private String communicationsRmqPassword;
	
	@Value("${rabbitmq.communications.consumers-min}")
	private int communicationsRmqMinConsumers;
	
	@Value("${rabbitmq.communications.consumers-max}")
	private int communicationsRmqMaxConsumers;
	
	@Value("${rabbitmq.communications.prefetch-count}")
	private int communicationsRmqPrefetchCount;
	
	@Value("${rabbitmq.communications.dead-letter-exchange}")
	private String communicationsDeadLetterExchange;
	
	/* RMQ configuration */
	@Bean("jsonMessageConverter")
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean("consumerJackson2MessageConverter")
	public MappingJackson2MessageConverter consumerJackson2MessageConverter() {
		return new MappingJackson2MessageConverter();
	}

	@Bean("messageHandlerMethodFactory")
	public DefaultMessageHandlerMethodFactory messageHandlerMethodFactory() {
		DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
		factory.setMessageConverter(consumerJackson2MessageConverter());
		return factory;
	}

	@Override
	public void configureRabbitListeners(final RabbitListenerEndpointRegistrar registrar) {
		registrar.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
	}

	@Bean(name = "rabbitConnectionFactory")
	public ConnectionFactory rabbitConnectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(communicationsRmqHost,
				communicationsRmqHostPort);
		connectionFactory.setUsername(communicationsRmqUsername);
		connectionFactory.setPassword(communicationsRmqPassword);
		return connectionFactory;
	}
	
	@Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(rabbitConnectionFactory());
    }

	@Primary
	@Bean(name = "rabbitTemplate")
	public RabbitTemplate rabbitTemplate(@Qualifier("rabbitConnectionFactory") final ConnectionFactory connectionFactory) {
		final RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jsonMessageConverter());
		return template;
	}
	
	@Bean(name = "rabbitInterceptor")
	RetryOperationsInterceptor rabbitInterceptor(@Qualifier(value = "rabbitTemplate") RabbitTemplate rabbitTemplate) {
		RepublishMessageRecoverer republishMessageRecoverer = new RepublishMessageRecoverer(rabbitTemplate, communicationsDeadLetterExchange);
		republishMessageRecoverer.errorRoutingKeyPrefix("fail.");
		return RetryInterceptorBuilder.stateless()
				.maxAttempts(4)
				.backOffOptions(1000, 10, 100000)
				.recoverer(republishMessageRecoverer)
				.build();
	}
	
	@Bean(name = "rabbitListenerContainerFactory")
	public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(
			@Qualifier("rabbitConnectionFactory") ConnectionFactory connectionFactory,
			@Qualifier(value = "rabbitInterceptor") RetryOperationsInterceptor rmqInterceptor) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setConcurrentConsumers(communicationsRmqMinConsumers);
		factory.setMaxConcurrentConsumers(communicationsRmqMaxConsumers);
		factory.setPrefetchCount(communicationsRmqPrefetchCount);
		factory.setAdviceChain(rmqInterceptor);
		return factory;
	}
}

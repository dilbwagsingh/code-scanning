package com.mykaarma.kcommunications.utils.TranscriptionHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskPoolConfiguration {
	
	private static int corePoolSize = 5;
	private static int maxPoolSize = 25;
	private static int queueCapacity = 50;
	
	@Bean(name = "transcribeJobExecutor")
	public ThreadPoolTaskExecutor executorA() {
		
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		
		return executor;
	}
	
	@Bean(name = "transcribeJobPollExecutor")
	public ThreadPoolTaskExecutor executorB() {
		
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		
		return executor;
	}
}

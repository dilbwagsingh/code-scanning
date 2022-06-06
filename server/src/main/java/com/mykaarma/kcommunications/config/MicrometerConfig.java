package com.mykaarma.kcommunications.config;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class MicrometerConfig {
	
	@Bean
	MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
	  return registry -> registry.config().commonTags("application", "kcommunications-api", "instance",
			  "kcommunications-api");
	}

}

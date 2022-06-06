package com.mykaarma.kcommunications.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mykaarma.workflow.client.WorkflowClientService;

/**
 * WorkflowClientServiceConfig
 */
@Configuration
public class WorkflowClientServiceConfig {

  @Value("${workflow_api_url}")
  private String workflowUrl;
  
  @Bean
  public WorkflowClientService workflowClientService() {
    return new WorkflowClientService(workflowUrl, null, null);
  }
}
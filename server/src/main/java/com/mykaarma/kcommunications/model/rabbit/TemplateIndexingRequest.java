package com.mykaarma.kcommunications.model.rabbit;

import com.mykaarma.kcommunications.utils.TemplateType;

import lombok.Data;

@Data
public class TemplateIndexingRequest {

	private String templateUuid;
	
	private TemplateType templateType;
	
	private Integer expiration;
}

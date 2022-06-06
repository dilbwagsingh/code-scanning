package com.mykaarma.kcommunications_model.dto;

import lombok.Data;

@Data
public class TemplateDTO {

	private String protocol;

	private Boolean isManual;
	
	private String title;
	
	private String body;
	
	private String dealerUuid;
	
	private String departmentUuid;
	
	private String locale;
	
	private String uuid;
}

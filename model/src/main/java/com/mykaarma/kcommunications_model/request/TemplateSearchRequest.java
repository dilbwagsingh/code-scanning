package com.mykaarma.kcommunications_model.request;

import lombok.Data;

@Data
public class TemplateSearchRequest {

	private String templateSearchContext;
	
	private String locale;
	
	private String templateType;
	
	private int limit=150;
}

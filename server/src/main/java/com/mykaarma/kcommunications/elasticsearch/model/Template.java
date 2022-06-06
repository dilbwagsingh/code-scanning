package com.mykaarma.kcommunications.elasticsearch.model;

import org.springframework.data.annotation.Id;
import lombok.Data;

@Data
public class Template {

	@Id
	private Long id;
	
	private String protocol;

	private Boolean isManual;
	
	private String title;
	
	private String body;
	
	private String dealerUuid;
	
	private String departmentUuid;
	
	private String locale;
	
	private String slug;
	
	private String uuid;
	
	private int sortOrder=0;

}

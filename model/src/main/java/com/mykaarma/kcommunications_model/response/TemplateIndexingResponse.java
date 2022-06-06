package com.mykaarma.kcommunications_model.response;

import java.io.Serializable;

import lombok.Data;

@Data
public class TemplateIndexingResponse extends Response implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String requestUuid;
	private boolean requestStatus=false;

}

package com.mykaarma.kcommunications_model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultThreadOwnerResponse extends Response {
	
	private String requestUUID;
	private String defaultThreadOwnerUserUUID;
	
	public String getRequestUUID() {
		return requestUUID;
	}
	public void setRequestUUID(String requestUUID) {
		this.requestUUID = requestUUID;
	}
	
	public String getDefaultThreadOwnerUserUUID() {
		return defaultThreadOwnerUserUUID;
	}
	public void setDefaultThreadOwnerUserUUID(String defaultThreadOwnerUserUUID) {
		this.defaultThreadOwnerUserUUID = defaultThreadOwnerUserUUID;
	}
	
	
	
	
}

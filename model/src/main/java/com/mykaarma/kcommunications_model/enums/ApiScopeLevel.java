package com.mykaarma.kcommunications_model.enums;

public enum ApiScopeLevel {
	
	
	DEPARTMENT_LEVEL("DepartmentScope"),
	DEALER_LEVEL("DealerScope"),
	SERVICE_SUBSCRIBER_LEVEL("ServiceSubscriberScope");
	
	private String apiScopeLevel;
	
	private ApiScopeLevel(String apiScopeLevel) {
		this.apiScopeLevel = apiScopeLevel;
	}

	public String getApiScopeName() {
		return this.apiScopeLevel;
	}
}



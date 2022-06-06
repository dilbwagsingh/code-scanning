package com.mykaarma.kcommunications.model.kre;


public class KaarmaRoutingResponse {

	private RoutingRuleResponse routingRuleResponse;
	private OutOfOfficeResponse outOfOfficeResponse;
	private OptinResponse optinResponse;
	
	public RoutingRuleResponse getRoutingRuleResponse() {
		return routingRuleResponse;
	}
	public void setRoutingRuleResponse(RoutingRuleResponse routingRuleResponse) {
		this.routingRuleResponse = routingRuleResponse;
	}
	public OutOfOfficeResponse getOutOfOfficeResponse() {
		return outOfOfficeResponse;
	}
	public void setOutOfOfficeResponse(OutOfOfficeResponse outOfOfficeResponse) {
		this.outOfOfficeResponse = outOfOfficeResponse;
	}
	public OptinResponse getOptinResponse() {
		return optinResponse;
	}
	public void setOptinResponse(OptinResponse optinResponse) {
		this.optinResponse = optinResponse;
	}
}

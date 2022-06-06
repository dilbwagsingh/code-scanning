package com.mykaarma.kcommunications.model.kre;

public class OptinResponse {

	private Boolean isOptin = false;
	private Boolean isStop = false;
	
	public Boolean getIsOptin() {
		return isOptin;
	}
	public void setIsOptin(Boolean isOptin) {
		this.isOptin = isOptin;
	}
	public Boolean getIsStop() {
		return isStop;
	}
	public void setIsStop(Boolean isStop) {
		this.isStop = isStop;
	}
	
	public OptinResponse() {
		
	}
	
	public OptinResponse(Boolean isOptin, Boolean isStop, Double optoutScore) {
		super();
		this.isOptin = isOptin;
		this.isStop = isStop;
	}


}

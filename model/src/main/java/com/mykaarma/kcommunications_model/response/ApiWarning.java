package com.mykaarma.kcommunications_model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement
public class ApiWarning {

	public ApiWarning(){
		
	}
	
	private String warningCode;

	private String warningDescription;

	@JsonProperty("warningCode")
	public String getWarningCode() {
		return warningCode;
	}

	@JsonProperty("warningDescription")
	public String getWarningDescription() {
		return warningDescription;
	}

	public void setWarningCode(String warningCode) {
		this.warningCode = warningCode;
	}

	public void setWarningDescription(String warningDescription) {
		this.warningDescription = warningDescription;
	}

	public ApiWarning(String warningCode, String warningDescription) {
		super();
		this.warningCode = warningCode;
		this.warningDescription = warningDescription;
	}
}

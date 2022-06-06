package com.mykaarma.kcommunications_model.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetDepartmentUUIDResponse extends Response {
	
	private String departmentUUID;
	
	public String getDepartmentUUID() {
		return departmentUUID;
	}

	public void setDepartmentUUID(String departmentUUID) {
		this.departmentUUID = departmentUUID;
	}
	
}

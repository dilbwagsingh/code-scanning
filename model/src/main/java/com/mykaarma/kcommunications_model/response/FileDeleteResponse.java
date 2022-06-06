package com.mykaarma.kcommunications_model.response;

import io.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
public class FileDeleteResponse extends Response {
	
	@ApiModelProperty(notes = "Whether the file for deleted successfully")
	private Boolean isDeleted;
	
	public Boolean getIsDeleted() {
		return isDeleted;
	}
	
	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

}

package com.mykaarma.kcommunications_model.response;

import io.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
public class FileUploadResponse extends Response {
	
	@ApiModelProperty(notes = "URL of the uploaded file")
	private String uploadUrl;

	public String getUploadUrl() {
		return uploadUrl;
	}

	public void setUploadUrl(String uploadUrl) {
		this.uploadUrl = uploadUrl;
	}

}

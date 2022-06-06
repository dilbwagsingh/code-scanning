package com.mykaarma.kcommunications_model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@JsonInclude(Include.NON_NULL)
@Data
public class UploadAttachmentResponse extends Response{
	
	private String attachmentURL;
	
	private String mediaPreviewURL;
	
	private String thumbnailURL;
	
	private String extension;
	
	private String contentType;
	
	private String docSize;
	
}

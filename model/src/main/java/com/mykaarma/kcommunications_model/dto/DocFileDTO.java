package com.mykaarma.kcommunications_model.dto;

import java.io.Serializable;

import lombok.Data;

@Data
public class DocFileDTO implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private String docFileName;
	
	private String fileExtension;
	
	private String mimeType;
	
	private String originalFileName;
	
	private String docSize;
	
	private String thumbnailFileName;
	
	private String mediaPreviewURL;
	
	private String messageUuid;
}

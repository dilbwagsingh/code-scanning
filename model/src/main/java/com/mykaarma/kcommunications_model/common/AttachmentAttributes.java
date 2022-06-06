package com.mykaarma.kcommunications_model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mykaarma.kcommunications_model.enums.AttachmentExtension;

public class AttachmentAttributes {
	
	@JsonProperty("fileURL")
	private String fileURL;
	
	@JsonProperty("attachmentExtension")
	private String attachmentExtension;
	
	@JsonProperty("mimeType")
	private String mimeType;
	
	@JsonProperty("originalFileName")
	private String originalFileName;
	
	@JsonProperty("docSize")
	private String docSize;
	
	@JsonProperty("thumbnailURL")
	private String thumbnailURL;
	
	@JsonProperty("mediaPreviewURL")
	private String mediaPreviewURL;

	public String getFileURL() {
		return fileURL;
	}

	public void setFileURL(String fileURL) {
		this.fileURL = fileURL;
	}

	public String getAttachmentExtension() {
		return attachmentExtension;
	}

	public void setAttachmentExtension(String attachmentExtension) {
		this.attachmentExtension = attachmentExtension;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}

	public String getDocSize() {
		return docSize;
	}

	public void setDocSize(String docSize) {
		this.docSize = docSize;
	}
	
	public String getThumbnailURL() {
		return thumbnailURL;
	}
	
	public void setThumbnailURL(String thumbnailURL) {
		this.thumbnailURL = thumbnailURL;
	}
	
	public String getMediaPreviewURL() {
		return mediaPreviewURL;
	}
	
	public void setMediaPreviewURL(String mediaPreviewURL) {
		this.mediaPreviewURL = mediaPreviewURL;
	}
	
}

package com.mykaarma.kcommunications_model.request;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UploadAttachmentsToS3Request {

	@ApiModelProperty(notes = "file to upload in byte format")
	List<byte[]> fileItems;

	@ApiModelProperty(notes = "content type of file to be uploaded")
	String contentType;

	@ApiModelProperty(notes = "original name of file to be uploaded")
	String fileName;

	@ApiModelProperty(notes = "extension of file to be uploaded")
	String extension;

	@ApiModelProperty(notes = "folder path to which file should be uploaded")
	String folderPrefix;

	@ApiModelProperty(notes = "url from which file should be uploaded")
	String mediaUrl;

	@ApiModelProperty(notes = "flag for whether to use byte file or file url to upload (default upload: using byte file)")
	Boolean uploadFromUrl;
	
}

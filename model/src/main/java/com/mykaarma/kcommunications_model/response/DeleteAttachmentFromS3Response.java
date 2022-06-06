package com.mykaarma.kcommunications_model.response;

import java.util.List;

public class DeleteAttachmentFromS3Response extends Response {
	
	private List<String> attachmentsDeletedList;
	
	public List<String> getAttachmentsDeletedList() {
		return attachmentsDeletedList;
	}
	
	public void setAttachmentsDeletedList(List<String> attachmentsDeletedList) {
		this.attachmentsDeletedList = attachmentsDeletedList;
	}

}

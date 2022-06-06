package com.mykaarma.kcommunications_model.request;

import java.util.List;

public class DeleteAttachmentFromS3Request {

    private List<String> attachmentUrlList;

    public List<String> getAttachmentUrlList() {
        return attachmentUrlList;
    }

    public void setAttachmentUrlList(List<String> attachmentUrlList) {
        this.attachmentUrlList = attachmentUrlList;
    }
}

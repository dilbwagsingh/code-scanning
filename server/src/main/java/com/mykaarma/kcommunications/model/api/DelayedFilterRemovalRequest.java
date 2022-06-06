package com.mykaarma.kcommunications.model.api;

import com.mykaarma.kcommunications_model.enums.DraftStatus;

public class DelayedFilterRemovalRequest {
	
	private Long fromDealerID;
	private Long toDealerID;
	private Long batchSize=500l;
	private Long offset=0l;
	private String draftStatus=DraftStatus.FAILED.name();
	private Integer expiration;
	
	public Long getFromDealerID() {
		return fromDealerID;
	}
	public void setFromDealerID(Long fromDealerID) {
		this.fromDealerID = fromDealerID;
	}
	public Long getToDealerID() {
		return toDealerID;
	}
	public void setToDealerID(Long toDealerID) {
		this.toDealerID = toDealerID;
	}
	public Long getBatchSize() {
		return batchSize;
	}
	public void setBatchSize(Long batchSize) {
		this.batchSize = batchSize;
	}
	public Long getOffset() {
		return offset;
	}
	public void setOffset(Long offset) {
		this.offset = offset;
	}
	public Integer getExpiration() {
		return expiration;
	}
	public void setExpiration(Integer expiration) {
		this.expiration = expiration;
	}
	public String getDraftStatus() {
		return draftStatus;
	}
	public void setDraftStatus(String draftStatus) {
		this.draftStatus = draftStatus;
	}

}

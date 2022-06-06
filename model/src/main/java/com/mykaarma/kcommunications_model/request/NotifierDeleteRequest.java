package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotifierDeleteRequest {

	@JsonProperty("dealerAssociateID")
	private Long dealerAssociateID;

	@JsonProperty("dealerID")
	private Long dealerID;

	@JsonProperty("deleteAllEntriesForUser")
	private boolean deleteAllEntriesForUser = false;

	public boolean getDeleteAllEntriesForUser() {
		return deleteAllEntriesForUser;
	}
	public void setDeleteAllEntriesForUser(boolean deleteAllEntriesForUser) {
		this.deleteAllEntriesForUser = deleteAllEntriesForUser;
	}
	public Long getDealerAssociateID() {
		return dealerAssociateID;
	}
	public void setDealerAssociateID(Long dealerAssociateID) {
		this.dealerAssociateID = dealerAssociateID;
	}
	public Long getDealerID() {
		return dealerID;
	}
	public void setDealerID(Long dealerID) {
		this.dealerID = dealerID;
	}
}
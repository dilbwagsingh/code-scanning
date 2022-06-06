package com.mykaarma.kcommunications_model.response;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultThreadOwnerForDealerResponse  extends Response {
	
	private String requestUUID;
	private HashMap<Long,Long> dealerAssociateDefaultThreadOwnerMap;
	private List<Long> emptyDefaultThreadOwnerDealerAssociateList;
	
	public String getRequestUUID() {
		return requestUUID;
	}
	public void setRequestUUID(String requestUUID) {
		this.requestUUID = requestUUID;
	}
	public HashMap<Long, Long> getDealerAssociateDefaultThreadOwnerMap() {
		return dealerAssociateDefaultThreadOwnerMap;
	}
	public void setDealerAssociateDefaultThreadOwnerMap(HashMap<Long, Long> dealerAssociateDefaultThreadOwnerMap) {
		this.dealerAssociateDefaultThreadOwnerMap = dealerAssociateDefaultThreadOwnerMap;
	}
	public List<Long> getEmptyDefaultThreadOwnerDealerAssociateList() {
		return emptyDefaultThreadOwnerDealerAssociateList;
	}
	public void setEmptyDefaultThreadOwnerDealerAssociateList(List<Long> emptyDefaultThreadOwnerDealerAssociateList) {
		this.emptyDefaultThreadOwnerDealerAssociateList = emptyDefaultThreadOwnerDealerAssociateList;
	}

}

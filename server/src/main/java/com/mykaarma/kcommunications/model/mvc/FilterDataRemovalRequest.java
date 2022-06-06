package com.mykaarma.kcommunications.model.mvc;

import java.io.Serializable;
import java.util.HashMap;

public class FilterDataRemovalRequest implements Serializable{
	
	HashMap<Long,Long> messageIdDealerIdMap;
	
	String collectionName;

	public HashMap<Long, Long> getMessageIdDealerIdMap() {
		return messageIdDealerIdMap;
	}

	public void setMessageIdDealerIdMap(HashMap<Long, Long> messageIdDealerIdMap) {
		this.messageIdDealerIdMap = messageIdDealerIdMap;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

}

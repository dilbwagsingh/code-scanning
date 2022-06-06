package com.mykaarma.kcommunications.model.rabbit;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import lombok.Data;

@Data
public class PostMariaDBMergeRequest {
	
	private Long primaryCustomerID;
	private HashMap<Long, HashMap<String,Set<Long>>> mapUpdatedRecords;
	private Long mergedByDealerAssociateID;
	private List<Long> mergedCustomerIDs;
	private Integer expiration;
	private Boolean isReconciliation;
	private String primaryCustomerGUID;

}

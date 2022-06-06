package com.mykaarma.kcommunications.model.mongo;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class CustomerMergeLog {
	private Long primaryCustomerID;
	private String primaryCustomerGUID;
	private Long dealerID;
	private HashMap<Long, HashMap<String,Set<String>>> updatedMongoRecords;
	private HashMap<String, HashMap<Long,Set<String>>> extendedRecords;
	private Set<Long> mergedCustomerIDs = new HashSet<>();
	private Date createdDate = null;
	private Date updatedDate = null;
	private boolean isMongoProcessed = false;
	
	public enum MergeProgress{
	    MONGO("isMongoProcessed")
	    ; 
	    
	    private String property;
	    private MergeProgress(String property) {
	        this.property = property;
	    }
	    public String getProperty() {
	        return property;
	    }
	}
}

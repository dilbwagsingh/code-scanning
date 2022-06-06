package com.mykaarma.kcommunications.model.mongo;

public enum MergeIgnoreCollection {
	KNOTIFICATIONMESSAGE,KORDERRAW,
	KAARMAVEHICLE,
	KAARMASERVICEAPPOINTMENTREQUEST,
	KAARMAPARTSORDER,
	KAARMADEALERORDER,
	KAARMACUSTOMER,
	DMSVEHICLEXML,
	DMSPARTSXML,
	DMSCUSTOMERXML,
	DMSAPPOINTMENTXML,
	DMSROXML,
	DMSAPPOINTMENTCACHE,
	CUSTOMER_MERGE_HISTORY,
	CUSTOMER_MERGE_LOG,
	COUNTERS,
	NOTIFIER_COLLECTION_OLD,
	MKORDER,
	DMSCLOSEDROXML;
	
	public static MergeIgnoreCollection exists(String collectionName) {
		for(MergeIgnoreCollection mic : MergeIgnoreCollection.values()) {
			if(mic.name().equalsIgnoreCase(collectionName))
				return mic;
		}
		return null;
	}
}

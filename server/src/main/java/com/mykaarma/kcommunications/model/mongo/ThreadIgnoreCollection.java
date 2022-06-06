package com.mykaarma.kcommunications.model.mongo;

public enum ThreadIgnoreCollection {
	DELAYED_COLLECTION,EXTERNAL_SUBSCRIPTION,FAILED_DRAFT_COLLECTION,
	FOLLOWED_UP_HELPER,INTERNAL_SUBSCRIPTION,KNOTIFICATIONMESSAGE,KNOTIIFICATIONMESSAGE,LOANER_VISUAL_SERVICE_CHECKIN_COLLECTION,
	LOCATION_HISTORY_COLLECTION,NOTIFIER_COLLECTION,NO_WELCOME_TEXT_HELPER,READY_FOR_FOLLOW_UP_HELPER,NOTIFIER_COLLECTION_OLD,
	SENT_COLLECTION,UNRESPONDED_HELPER,VISUAL_SERVICE_CHECKIN_COLLECTION;
	
	public static ThreadIgnoreCollection exists(String collectionName) {
		for(ThreadIgnoreCollection tic : ThreadIgnoreCollection.values()) {
			if(tic.name().equalsIgnoreCase(collectionName))
				return tic;
		}
		return null;
	}
}

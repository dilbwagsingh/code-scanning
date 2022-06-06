package com.mykaarma.kcommunications_model.response;

import java.util.List;

import com.mykaarma.kcommunications_model.common.SubscriberInfo;


public class SubscriptionSaveResponse extends Response{

	List<SubscriberInfo> internalSubscriptionsFailed;
	List<SubscriberInfo> externalSubscriptionsFailed;
	
	public List<SubscriberInfo> getInternalSubscriptionsFailed() {
		return internalSubscriptionsFailed;
	}
	public void setInternalSubscriptionsFailed(List<SubscriberInfo> internalSubscriptionsFailed) {
		this.internalSubscriptionsFailed = internalSubscriptionsFailed;
	}
	public List<SubscriberInfo> getExternalSubscriptionsFailed() {
		return externalSubscriptionsFailed;
	}
	public void setExternalSubscriptionsFailed(List<SubscriberInfo> externalSubscriptionsFailed) {
		this.externalSubscriptionsFailed = externalSubscriptionsFailed;
	}
}

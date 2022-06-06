package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.mykaarma.kcommunications_model.common.SubscriberInfo;

public class SubscriptionSaveRequest {

	private List<SubscriberInfo> internalSubscriptionsRevoked;
	private List<SubscriberInfo> externalSubscriptionsRevoked;
	private List<SubscriberInfo> internalSubscriptionsAdded;
	private List<SubscriberInfo> externalSubscriptionsAdded;
	private Long dealerAssociateUuid;
	private Boolean isHistoricalSubbscription = false;
	
	public List<SubscriberInfo> getInternalSubscriptionsRevoked() {
		return internalSubscriptionsRevoked;
	}
	public void setInternalSubscriptionsRevoked(List<SubscriberInfo> internalSubscriptionsRevoked) {
		this.internalSubscriptionsRevoked = internalSubscriptionsRevoked;
	}
	public List<SubscriberInfo> getExternalSubscriptionsRevoked() {
		return externalSubscriptionsRevoked;
	}
	public void setExternalSubscriptionsRevoked(List<SubscriberInfo> externalSubscriptionsRevoked) {
		this.externalSubscriptionsRevoked = externalSubscriptionsRevoked;
	}
	public List<SubscriberInfo> getInternalSubscriptionsAdded() {
		return internalSubscriptionsAdded;
	}
	public void setInternalSubscriptionsAdded(List<SubscriberInfo> internalSubscriptionsAdded) {
		this.internalSubscriptionsAdded = internalSubscriptionsAdded;
	}
	public List<SubscriberInfo> getExternalSubscriptionsAdded() {
		return externalSubscriptionsAdded;
	}
	public void setExternalSubscriptionsAdded(List<SubscriberInfo> externalSubscriptionsAdded) {
		this.externalSubscriptionsAdded = externalSubscriptionsAdded;
	}
	public Long getDealerAssociateUuid() {
		return dealerAssociateUuid;
	}
	public void setDealerAssociateUuid(Long dealerAssociateUuid) {
		this.dealerAssociateUuid = dealerAssociateUuid;
	}
	public Boolean getIsHistoricalSubbscription() {
		return isHistoricalSubbscription;
	}
	public void setIsHistoricalSubbscription(Boolean isHistoricalSubbscription) {
		this.isHistoricalSubbscription = isHistoricalSubbscription;
	}
	
}

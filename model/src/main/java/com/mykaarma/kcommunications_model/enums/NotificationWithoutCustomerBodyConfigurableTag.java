package com.mykaarma.kcommunications_model.enums;

public enum NotificationWithoutCustomerBodyConfigurableTag {
	CUSTOMER_NAMES("_customer_names");
	
	private String tagKey;
	
	private NotificationWithoutCustomerBodyConfigurableTag(String tagKey) {
		this.tagKey = tagKey;
	}
	
	public String getTagKey() {
		return tagKey;
	}
}

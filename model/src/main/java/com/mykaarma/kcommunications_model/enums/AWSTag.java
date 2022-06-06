package com.mykaarma.kcommunications_model.enums;

public enum AWSTag {

	DEALER_ID("dealer_id");
	
	private String tagName;
	
	private AWSTag(String tagName) {
		this.tagName = tagName;
	}

	public String getTagName() {
		return this.tagName;
	}
}

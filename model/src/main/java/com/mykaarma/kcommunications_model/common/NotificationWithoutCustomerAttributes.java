package com.mykaarma.kcommunications_model.common;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mykaarma.kcommunications_model.enums.NotificationWithoutCustomerBodyConfigurableTag;

@SuppressWarnings("serial")
public class NotificationWithoutCustomerAttributes extends NotificationAttributes {
	
	@JsonProperty("bodyTagsForSpecialRendering")
	private HashMap<NotificationWithoutCustomerBodyConfigurableTag, String> bodyTagsForSpecialRendering;
	
	public NotificationWithoutCustomerAttributes() {
		
	}

	public HashMap<NotificationWithoutCustomerBodyConfigurableTag, String> getBodyTagsForSpecialRendering() {
		return bodyTagsForSpecialRendering;
	}

	public void setBodyTagsForSpecialRendering(
			HashMap<NotificationWithoutCustomerBodyConfigurableTag, String> bodyTagsForSpecialRendering) {
		this.bodyTagsForSpecialRendering = bodyTagsForSpecialRendering;
	}
}

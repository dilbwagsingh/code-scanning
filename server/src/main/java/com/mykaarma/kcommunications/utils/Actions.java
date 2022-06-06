package com.mykaarma.kcommunications.utils;

public enum Actions {

	CLICK_ADD_TO_WAITING_FOR_RESPONSE("Add_To_Waiting_For_Response_Button"),
	CLICK_DISMISS_FROM_WAITING_FOR_RESPONSE_AS_RESPONDED("Dismiss_From_Waiting_For_Response_As_Responded"),
	CLICK_DISMISS_FROM_WAITING_FOR_RESPONSE_AS_NOT_WFR("Add_To_Waiting_For_Response_Button_As_Not_WFR"),
	ADD_TO_MISSED_CALL("ADD_TO_MISSED_CALL");
	
	private String value;

	private Actions(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
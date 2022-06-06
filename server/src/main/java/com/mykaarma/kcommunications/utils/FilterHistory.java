package com.mykaarma.kcommunications.utils;

import java.io.Serializable;
import java.util.HashMap;


public class FilterHistory implements Serializable{
	
	private String eventRaisedByUUID;
    private ActionTaken actionTaken;
    private Long dealerID;
    private Long dealerDepartmentID;
    private String actionSource;
    private HashMap<Long,Long> customerIDAndThreadID;
    private boolean isCall = false;
    
    
    public Long getDealerID() {
		return dealerID;
	}
	public void setDealerID(Long dealerID) {
		this.dealerID = dealerID;
	}
	public Long getDealerDepartmentID() {
		return dealerDepartmentID;
	}
	public void setDealerDepartmentID(Long dealerDepartmentID) {
		this.dealerDepartmentID = dealerDepartmentID;
	}
	public String getActionSource() {
		return actionSource;
	}
	public void setActionSource(String actionSource) {
		this.actionSource = actionSource;
	}
	public HashMap<Long, Long> getCustomerIDAndThreadID() {
		return customerIDAndThreadID;
	}
	public void setCustomerIDAndThreadID(HashMap<Long, Long> customerIDAndThreadID) {
		this.customerIDAndThreadID = customerIDAndThreadID;
	}
	public static enum ActionTaken {
    	
    	DISMISS_AS_NOT_WAITING_FOR_RESPONSE("DISMISS_AS_NOT_WAITING_FOR_RESPONSE"), DISMISS_AS_ALREADY_RESPONDED("DISMISS_AS_NOT_WAITING_FOR_RESPONSE"),
    	ADD_TO_WAITING_FOR_RESPONSE("ADD_TO_WAITING_FOR_RESPONSE"), ADD_TO_MISSED_CALL("ADD_TO_MISSED_CALL"), MESSAGE_SENT("MESSAGE_SENT");
    	private String value;
    	
    	private ActionTaken(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
    
    }
	
	public String getEventRaisedByUUID() {
		return eventRaisedByUUID;
	}
	public void setEventRaisedByUUID(String eventRaisedByUUID) {
		this.eventRaisedByUUID = eventRaisedByUUID;
	}
	public ActionTaken getActionTaken() {
		return actionTaken;
	}
	public void setActionTaken(ActionTaken actionTaken) {
		this.actionTaken = actionTaken;
	}
	public boolean getIsCall() {
		return isCall;
	}
	public void setIsCall(boolean isCall) {
		this.isCall = isCall;
	}
}
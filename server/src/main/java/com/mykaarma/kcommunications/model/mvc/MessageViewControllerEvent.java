package com.mykaarma.kcommunications.model.mvc;

import java.io.Serializable;

import lombok.Data;

@Data
public class MessageViewControllerEvent implements Serializable{

	private String eventName;
	private Long customerID;
	private String deviceID;
	private Long dealerID;
	private Long dealerDepartmentID;
	private Long eventRaisedBy;
}

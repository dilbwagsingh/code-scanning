package com.mykaarma.kcommunications.utils;
import java.io.Serializable;

import lombok.Data;

@Data
public class FilterDataRequest implements Serializable {
	
	private Long id;
	private Long dealerID;
	private Long customerID;
	private Long dealerAssociateID;
	private Long dealerDepartmentID;
	private String communicationUID;
	private Long threadID;
	private String messageType;

}
package com.mykaarma.kcommunications_model.dto;

import java.io.Serializable;

import lombok.Data;

@Data
public class CustomerLockDTO implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private String customerUuid;

	private String dealerDepartmentUuid;
	
	private String lockByDealerAssociateUuid;
	
	private String lockedByName;
	
	private String lockType;
}

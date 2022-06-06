package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;

import lombok.Data;

@Data
public class DepartmentUrlUpdateFailure implements Serializable{

	private String failureReason;
	private Long dealerDepartmentId;
	private String accountSid;
	private String brokerNumber;
}

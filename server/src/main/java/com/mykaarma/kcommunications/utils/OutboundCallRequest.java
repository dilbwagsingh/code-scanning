package com.mykaarma.kcommunications.utils;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class OutboundCallRequest {

	private Long dealerID;
	private Long dealerDepartmentID;
	private Long dealerAssociateID;

}

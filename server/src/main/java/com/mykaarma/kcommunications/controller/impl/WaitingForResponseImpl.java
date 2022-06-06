package com.mykaarma.kcommunications.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WaitingForResponseImpl {

	@Autowired
	GeneralRepository generalRepo;
	
	public Boolean getWaitingForResponseStauts(String customerUuid,String departmentUuid) {
		
		Long customerId=generalRepo.getCustomerIDForUUID(customerUuid);
		Long departmentId=generalRepo.getDepartmentIDForUUID(departmentUuid);
		
		return generalRepo.waitingForResponseStatusForCustomerAndDepartment(customerId, departmentId);
		
	}
}

package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.exception.CustomerClaimException;
import com.mykaarma.kcommunications.jpa.repository.CustomerLockRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.jpa.CustomerLock;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.redis.CustomerLockRedisService;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications_model.dto.CustomerLockDTO;
import com.mykaarma.kcommunications_model.request.FetchCustomerLockRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CustomerLockService {
	

	@Autowired
	CustomerLockRedisService customerLockRedisService;
	
	@Autowired
	CustomerLockRepository customerLockRepository;
	
	@Autowired
	GeneralRepository generalRepo;
	
	@Autowired
	MessagingViewControllerHelper messagingViewControllerHelper;
	
	private ObjectMapper objectMapper=new ObjectMapper();

	public void obtainCustomerLock(Message message) throws Exception {
		Lock messagingLock = customerLockRedisService.obtainLockOnCustomerMessaging(message.getCustomerID()+"");
		CustomerLock customerLock = customerLockRepository.findFirstByCustomerIDAndDealerDepartmentIDAndLockType(message.getCustomerID(), 
				message.getDealerDepartmentId(), APIConstants.MESSAGING_LOCK);
		if(customerLock==null || customerLock.getLockByDealerAssociateID().longValue()==message.getDealerAssociateID().longValue()) {
			if(customerLock==null) {
				customerLock= new CustomerLock();
				customerLock.setCustomerID(message.getCustomerID());
				customerLock.setDealerDepartmentID(message.getDealerDepartmentId());
				customerLock.setLockByDealerAssociateID(message.getDealerAssociateID());
				customerLock.setLockType(APIConstants.MESSAGING_LOCK);
				customerLock.setLockedByName(message.getFromName());
				customerLockRepository.saveAndFlush(customerLock);
			}
			messagingViewControllerHelper.publishCustomerMessageLockEvent(message);
			customerLockRedisService.unLock(messagingLock);
		}else{
			customerLockRedisService.unLock(messagingLock);
			throw new CustomerClaimException();
		}

	}
	
	public List<CustomerLockDTO> getCustomerLockInfoForRequest(String departmentUuid,FetchCustomerLockRequest fetchCustomerLockRequest) throws JsonProcessingException {
		Long departmentId=generalRepo.getDepartmentIDForUUID(departmentUuid);
		List<Long> customerIdList=new ArrayList<Long>();
		HashMap<Long,String> customerUuidIdMap=new HashMap<Long,String>();
		
		for(String customerUuid:fetchCustomerLockRequest.getCustomerUuids()) {
			if(StringUtils.isBlank(customerUuid)) {
				continue;
			}
			Long customerId=generalRepo.getCustomerIDForUUID(customerUuid);
			customerIdList.add(customerId);
			customerUuidIdMap.put(customerId,customerUuid);
		}
		List<CustomerLock> customerLockList = customerLockRepository.findAllByDealerDepartmentIDAndLockTypeAndCustomerIDIn(departmentId, APIConstants.MESSAGING_LOCK, customerIdList);
		List<CustomerLockDTO> customerLockDTOList=new ArrayList<CustomerLockDTO>();
		if(customerLockList!=null && !customerLockList.isEmpty()) {
			for(CustomerLock customerLock : customerLockList) {
				CustomerLockDTO customerLockDTO=new CustomerLockDTO();
				customerLockDTO.setCustomerUuid(customerUuidIdMap.get(customerLock.getCustomerID()));
				customerLockDTO.setDealerDepartmentUuid(departmentUuid);
				customerLockDTO.setLockByDealerAssociateUuid(generalRepo.getDealerAssociateUuidFromDealerAssociateId(customerLock.getLockByDealerAssociateID()));
				customerLockDTO.setLockedByName(customerLock.getLockedByName());
				customerLockDTO.setLockType(customerLock.getLockType());
				customerLockDTOList.add(customerLockDTO);
			}
			
		}
		log.info("in getCustomerLockInfoForRequest for department_uuid={} request={} customer_lock_info_list={}",departmentUuid,
				objectMapper.writeValueAsString(fetchCustomerLockRequest),objectMapper.writeValueAsString(customerLockDTOList));
		return customerLockDTOList;
	}
	
}

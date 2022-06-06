package com.mykaarma.kcommunications.utils;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.mykaarma.kcommunications.jpa.repository.ForwardingBrokerNumberMappingHelper;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.jpa.ForwardingBrokerNumberMapping;
import com.mykaarma.kcommunications.model.kre.KaarmaRoutingResponse;

@Component
public class OutOfOfficeHelper {
	
	@Autowired
	ForwardingBrokerNumberMappingHelper forwardingBrokerNumberMappingHelper;
	
	@Autowired
	GeneralRepository generalRepository;	
	
	@Autowired
	RestTemplate restTemplate;
	
	@Value("${out-of-office.receiver.url}")
	String outOfOfficeReceiverUrl;
	
	private static Logger LOGGER = LoggerFactory.getLogger(OutOfOfficeHelper.class);
	
	public ForwardingBrokerNumberMapping getForwardingBrokerNumberMapping(
			Long dealerId, Long dealerAssociateID, String dealerAssociateTextNumber, Long customerID,
			String phoneNumber) {
		return getForwardingBrokerNumberMapping(dealerId, dealerAssociateID, dealerAssociateTextNumber, customerID, phoneNumber, null);
	}

	public ForwardingBrokerNumberMapping getForwardingBrokerNumberMapping(
			Long dealerId, Long dealerAssociateID, String dealerAssociateTextNumber, Long customerID,
			String phoneNumber, String daForwardingPhoneNumber) {
		
		if(daForwardingPhoneNumber == null || daForwardingPhoneNumber.isEmpty()) {
			daForwardingPhoneNumber = dealerAssociateTextNumber;
		}
		
		LOGGER.info("Fetching Existing Broker Number information for DealerID : {},DealerAssociateID : {}  for Customer Phone Number : {}", dealerId, dealerAssociateID, phoneNumber);
		
		List<ForwardingBrokerNumberMapping> brokerNumberMappingList = null;
		ForwardingBrokerNumberMapping brokerNumMapping = null;
		
		brokerNumberMappingList = forwardingBrokerNumberMappingHelper.getBrokerNumber(dealerId, dealerAssociateID,daForwardingPhoneNumber, customerID, phoneNumber);
		
		if(brokerNumberMappingList!=null && brokerNumberMappingList.size()>0) {
			brokerNumMapping = brokerNumberMappingList.get(0);
		}
				
		if (null == brokerNumMapping) {
			LOGGER.info("Unable to find existing Broker Number Mappings, Creating new one from Pool dealerID = {}, dealerAssociaateID = {}, customerID = {}", dealerId, dealerAssociateID, customerID);
			brokerNumMapping = fetchBrokerNumberFromPool(dealerId,
					dealerAssociateID, customerID, phoneNumber, daForwardingPhoneNumber);
		}
		return brokerNumMapping;
	}

	private ForwardingBrokerNumberMapping fetchBrokerNumberFromPool(
			Long dealerId, Long dAID, Long customerID, String customerPhoneNumber, String daForwardingNumber ) {
		ForwardingBrokerNumberMapping brokerNumMap = null;
		List<ForwardingBrokerNumberMapping> brokerNumMapList = null;
		LOGGER.info("Trying to get unused Broker Number from Pool dealerID = {}, dealerAssociateID = {}", dealerId, dAID);
		
		List<String> listOfPhoneNumbers = forwardingBrokerNumberMappingHelper.getNumbersUsedByServiceAdvisor(dAID, daForwardingNumber);

		String brokerNumber = generalRepository
				.fetchNewBrokerNumberFromPool(listOfPhoneNumbers);

		if (null == brokerNumber) {
			LOGGER.info("Unable to fetch any used Broker Number from Pool dealerID = {}, dAID = {}, updating the least recently used Mapping", dealerId, dAID);
			
			brokerNumMapList = forwardingBrokerNumberMappingHelper
					.fetchOldestBrokerNumberForDealerAssociate(dAID);
			
			brokerNumMap = brokerNumMapList.size() > 0 ? (ForwardingBrokerNumberMapping) brokerNumMapList.get(0) : null;
			
			if(null == brokerNumMap) {
				LOGGER.info("Unable to recycle Broker Number for dealerID = {}, dealerAssociateID = {}", dealerId, dAID);
			}
			
			if (brokerNumMap != null) {
				brokerNumMap.setDealerAssociateID(dAID);
				brokerNumMap.setCustomerID(customerID);
				brokerNumMap.setCreatedTimeStamp(new Date());
				brokerNumMap.setLastMessageOn(null);
				brokerNumMap.setCustomerPhoneNumber(customerPhoneNumber);
				brokerNumMap.setDealerAssociatePhoneNumber(daForwardingNumber);
				
				forwardingBrokerNumberMappingHelper.saveAndFlush(brokerNumMap);

			}
		} else {

			LOGGER.info("Using Random unused BrokerNumberMapping for dealerID = {}, dealerAssociateID = {}", dealerId, dAID);
			
			brokerNumMap = new ForwardingBrokerNumberMapping();
			brokerNumMap.setBrokerNumber(brokerNumber);
			brokerNumMap.setCreatedTimeStamp(new Date());
			brokerNumMap.setDealerID(dealerId);
			brokerNumMap.setDealerAssociateID(dAID);
			brokerNumMap.setCustomerPhoneNumber(customerPhoneNumber);
			brokerNumMap
					.setCustomerID(customerID);
			brokerNumMap.setLastMessageOn(null);
			brokerNumMap.setDealerAssociatePhoneNumber(daForwardingNumber);
			forwardingBrokerNumberMappingHelper.saveAndFlush(brokerNumMap);
		}
		return brokerNumMap;
	}

	
	public void sendDelegationRequest(KaarmaRoutingResponse response) {
		try {
			String url= outOfOfficeReceiverUrl+"/delegation";
			Long fromDate=0l;     //Since only used to calculate delay
			LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<String,String>();
			map.add("DealerID", response.getRoutingRuleResponse().getDealerID().toString());
			map.add("DealerDepartmentID", response.getRoutingRuleResponse().getDealerDepartmentID().toString());
			map.add("DelegateFrom", response.getOutOfOfficeResponse().getDelegateFrom().toString());
			map.add("DelegateTo", response.getOutOfOfficeResponse().getDelegateTo().toString());
			map.add("StartDate",fromDate.toString());
			map.add("NotifyDelegatee", "false");
			LOGGER.info("OutOfOffice: Sending delegation request DealerID: {}, DelegateFrom: {}, DelegateTo: {} CustomerID: {}", response.getRoutingRuleResponse().getDealerID(), response.getOutOfOfficeResponse().getDelegateFrom(), response.getOutOfOfficeResponse().getDelegateTo(), response.getRoutingRuleResponse().getCustomerID());
			restTemplate.postForObject(url, map, Boolean.class);
		} catch (Exception e) {
			LOGGER.error("OutOfOffice: Error making delegation request. DealerID: {}, DelegateFrom: {}, DelegateTo: {} CustomerID: {}",
					response.getRoutingRuleResponse().getDealerID(), response.getOutOfOfficeResponse().getDelegateFrom(), response.getOutOfOfficeResponse().getDelegateTo(), response.getRoutingRuleResponse().getCustomerID(), e);
		}
	}

}

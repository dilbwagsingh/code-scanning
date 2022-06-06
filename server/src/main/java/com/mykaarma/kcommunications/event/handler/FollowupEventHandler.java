package com.mykaarma.kcommunications.event.handler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mykaarma.dms.common.enumerations.OrderType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.FeatureKeys;
import com.mykaarma.global.OrderStatus;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageMetaDataRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageMetaData;
import com.mykaarma.kcommunications.model.rabbit.MessageUpdateOnEvent;
import com.mykaarma.kcommunications.model.utils.LaborOpCodeFilteringParameter;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.AppConfigHelper;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCustomerApiHelperV2;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.KOrderApiHelper;
import com.mykaarma.kcommunications.utils.MessageMetaDataConstants;
import com.mykaarma.kcustomer_model.dto.CustomerInfoWithDealerOrderInfoList;
import com.mykaarma.kcustomer_model.enums.CustomerSearch;
import com.mykaarma.kmanage.model.dto.json.response.GetDepartmentResponseDTO;
import com.mykaarma.kcustomer_model.dto.VehicleInfo;
import com.mykaarma.korder_model.common.StandardOrder;
import com.mykaarma.korder_model.request.DealerOrderInfo;

@Service
public class FollowupEventHandler {

	@Autowired
	private MessageRepository messageRepository;
	
	@Autowired
	private KCustomerApiHelperV2 kCustomerApiHelperV2;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	@Autowired
	private AppConfigHelper appConfigHelper;
	
	@Autowired
	private MessageMetaDataRepository messageMetaDataRepository;
	
	@Autowired
	private Helper helper;
	
	@Autowired
	private KOrderApiHelper kOrderApiHelper;

	@Autowired 
	private LaborOpcodeRules laborOpcodeRules;
	
	@Autowired
	private KManageApiHelper kManageApiHelper;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(FollowupEventHandler.class);
	
	public static final String FOLLOWUP_RULES="FOLLOWUP_RULES";
	public static final String FOLLOWUP_RULES_FAILED="FOLLOWUP_RULES_FAILED";
	public static final String FOLLOWUP_RULES_PASSED="FOLLOWUP_RULES_PASSED";
	public static final Integer MAX_VEHICLES=20;
	
	public void followUpEventHandler(MessageUpdateOnEvent messageEvent) throws Exception {
		MDC.put(APIConstants.MESSAGE_UUID, messageEvent.getMessageUUID());
		LOGGER.info("Inside followUpEventHandler for message_uuid={}", messageEvent.getMessageUUID());
		Message message = messageRepository.findByuuid(messageEvent.getMessageUUID());
		String customerUUID = generalRepository.getCustomerUUIDFromCustomerID(message.getCustomerID());
        String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(message.getDealerDepartmentId());
    	GetDepartmentResponseDTO getDepartmentResponseDTO = kManageApiHelper.getDealerDepartment(departmentUUID);
        String dealerUUID = getDepartmentResponseDTO.getDepartmentExtendedDTO().getDealerMinimalDTO().getUuid();
		Date lookbackDate = getLookbackStartDate(dealerUUID, message);
		CustomerInfoWithDealerOrderInfoList customer = null;
		LaborOpCodeFilteringParameter laborOpCodeFilteringParameter = laborOpcodeRules.getLaborOpCodeFilteringParameters(message.getDealerID(), FeatureKeys.AUTO_FOLLOW_UP);
		DealerOrderInfo dealerOrderInfo = fireCustomerRulesAndGetCustomer(customerUUID, dealerUUID, message, lookbackDate, customer, laborOpCodeFilteringParameter, departmentUUID);
		if(dealerOrderInfo==null) {
			dealerOrderInfo = fireCommunicationValueRules(message.getToNumber(), dealerUUID, message, lookbackDate, laborOpCodeFilteringParameter, departmentUUID);
			if(dealerOrderInfo==null) {
				dealerOrderInfo = fireVINRules(dealerUUID, message, lookbackDate, customer, laborOpCodeFilteringParameter, departmentUUID);
			} 
		}
		LOGGER.info("{} all rules fired for message_uuid={} message_id={} ", FOLLOWUP_RULES, message.getUuid(), message.getId());
		if(dealerOrderInfo!=null) {
			LOGGER.info("{} follow up RO matched for dealer_order_uuid={} message_uuid={} message_id={} ", FOLLOWUP_RULES, dealerOrderInfo.getDealerOrder_uuid(),
					message.getUuid(), message.getId());
			onDealerOrderMatched(message, dealerOrderInfo);
		} else {
			LOGGER.info("{} follow up rules failed for message_uuid={} ", FOLLOWUP_RULES, message.getUuid());
		}	
	}
	
	public DealerOrderInfo fireCustomerRulesAndGetCustomer(String customerUUID, String dealerUUID, Message message, Date lookbackDate, 
			CustomerInfoWithDealerOrderInfoList customer, LaborOpCodeFilteringParameter laborOpCodeFilteringParameter, String departmentUUID) throws Exception {
		try {
			HashSet<String> customerKeyList = new HashSet<>();
			customerKeyList.add(customerUUID);
			List<CustomerInfoWithDealerOrderInfoList> customers = kCustomerApiHelperV2.callKCustomerExactApiSearchForParameters(CustomerSearch.GUID, customerKeyList, dealerUUID, OrderType.SERVICE);
			if(customers==null|| customers.isEmpty()) {
				LOGGER.info("{}=Customer No customer found for customer_uuid={} customer_id={} message_uuid={} in kcustomer-api", FOLLOWUP_RULES_FAILED, customerUUID, message.getCustomerID(), message.getUuid());			
				return null;
			}
			customer = customers.get(0);
			LOGGER.info("Customer found customer_uuid={}", customer.getCustomerInfo().getUuid());
			List<DealerOrderInfo> dealerOrders = customer.getDealerOrderInfoList();
			if(dealerOrders==null || dealerOrders.isEmpty()) {
				LOGGER.info("{}=Customer No dealer orders found for customer_uuid={} customer_id={} message_uuid={} in kcustomer-api", FOLLOWUP_RULES_FAILED, customerUUID, message.getCustomerID(), message.getUuid());
				return null;
			}
			DealerOrderInfo dealerOrder = applyDealerOrderRules(dealerOrders, message, lookbackDate, laborOpCodeFilteringParameter, departmentUUID);
			if(dealerOrder==null) {
				LOGGER.info("{}=Customer  for customer_uuid={} customer_id={} message_uuid={} ", FOLLOWUP_RULES_FAILED, customerUUID, message.getCustomerID(), message.getUuid());
				return null;
			}
			LOGGER.info("{}=Customer for customer_uuid={} customer_id={} message_uuid={} dealer_order_uuid={} ", FOLLOWUP_RULES_PASSED, 
					customerUUID, message.getCustomerID(), message.getUuid(), dealerOrder.getDealerOrder_uuid());
			return dealerOrder;
		} catch (Exception e) {
			LOGGER.error("Error in running customer rules for customer_uuid={} message_uuid={} ", customerUUID, message.getUuid(), e);
			throw e;
		}
	}
	
	public DealerOrderInfo fireCommunicationValueRules(String communicationValue, String dealerUUID, Message message, 
			Date lookbackDate, LaborOpCodeFilteringParameter laborOpCodeFilteringParameter, String departmentUUID) throws Exception{
		try {
			HashSet<String> commValueList = new HashSet<>();
			commValueList.add(communicationValue);
			List<CustomerInfoWithDealerOrderInfoList> customers = kCustomerApiHelperV2.callKCustomerExactApiSearchForParameters(CustomerSearch.COMMUNICATIONVALUE_EXACT, commValueList , dealerUUID,  OrderType.SERVICE);
			if(customers==null|| customers.isEmpty()) {
				LOGGER.info("{}=Communication_value No customers found for communication_value={} customer_id={} message_uuid={} in kcustomer-api", 
						FOLLOWUP_RULES_FAILED, communicationValue, message.getCustomerID(), message.getUuid());
				return null;
			}
			List<DealerOrderInfo> dealerOrders = new ArrayList<DealerOrderInfo>();
			for(CustomerInfoWithDealerOrderInfoList customer: customers) {
				dealerOrders.addAll(customer.getDealerOrderInfoList());
			}
			if(dealerOrders==null || dealerOrders.isEmpty()) {
				LOGGER.info("{}=Communication_value No dealer orders found for communication_value={} customer_id={} message_uuid={} in kcustomer-api", 
						FOLLOWUP_RULES_FAILED, communicationValue, message.getCustomerID(), message.getUuid());
				return null;
			}
			DealerOrderInfo dealerOrder = applyDealerOrderRules(dealerOrders, message, lookbackDate, laborOpCodeFilteringParameter, departmentUUID);
			if(dealerOrder==null) {
				LOGGER.info("{}=Communication_value  for communication_value={} customer_id={} message_uuid={} ", 
						FOLLOWUP_RULES_FAILED, communicationValue, message.getCustomerID(), message.getUuid());
				return null;
			}
			LOGGER.info("{}=Communication_value for communication_value={} customer_id={} message_uuid={} dealer_order_uuid={} ",
					FOLLOWUP_RULES_PASSED, communicationValue, message.getCustomerID(), message.getUuid(), dealerOrder.getDealerOrder_uuid());
			return dealerOrder;
		} catch (Exception e) {
			LOGGER.error("Error in running customer rules for communication_value={} message_uuid={} ", 
					communicationValue, message.getUuid(), e);
			throw e;
		}
	}
	
	public DealerOrderInfo fireVINRules(String dealerUUID, Message message, Date lookbackDate, 
			CustomerInfoWithDealerOrderInfoList customerOriginal, LaborOpCodeFilteringParameter laborOpCodeFilteringParameter, String departmentUUID) throws Exception{
		try {
			HashSet<String> vinList = new HashSet<>();
			if(customerOriginal!=null && customerOriginal.getCustomerInfo().getVehicles()!=null && !customerOriginal.getCustomerInfo().getVehicles().isEmpty()) {
				for(VehicleInfo vehicle: customerOriginal.getCustomerInfo().getVehicles()) {
					if(vehicle.getVin()!=null && !vehicle.getVin().isEmpty()) {
						vinList.add(vehicle.getVin());
					}
				}
			} else {
				LOGGER.info("{}=VIN No vehicles found for customer_id={} message_uuid={} in kcustomer-api", 
						FOLLOWUP_RULES_FAILED,  message.getCustomerID(), message.getUuid());
				return null;
			}
			if(vinList.isEmpty()) {
				LOGGER.info("{}=VIN No vehicles found for customer_id={} message_uuid={} in kcustomer-api", 
						FOLLOWUP_RULES_FAILED,  message.getCustomerID(), message.getUuid());
				return null;
			}
			if(vinList.size()>MAX_VEHICLES) {
				LOGGER.info("{}=VIN Too many vehicles found for customer_id={} message_uuid={} in kcustomer-api",
						FOLLOWUP_RULES_FAILED,  message.getCustomerID(), message.getUuid());
				return null;
			}
			List<CustomerInfoWithDealerOrderInfoList> customers = kCustomerApiHelperV2.callKCustomerExactApiSearchForParameters(CustomerSearch.VIN_EXACT, vinList, dealerUUID,  OrderType.SERVICE);
			if(customers==null|| customers.isEmpty()) {
				LOGGER.info("{}=VIN No customers found for vin_list={} customer_id={} message_uuid={} in kcustomer-api",
						FOLLOWUP_RULES_FAILED, new ObjectMapper().writeValueAsString(vinList) , message.getCustomerID(), message.getUuid());
				return null;
			}
			List<DealerOrderInfo> dealerOrders = new ArrayList<DealerOrderInfo>();
			for(CustomerInfoWithDealerOrderInfoList customer: customers) {
				dealerOrders.addAll(customer.getDealerOrderInfoList());
			}
			if(dealerOrders==null || dealerOrders.isEmpty()) {
				LOGGER.info("{}=VIN No dealer orders found for vin_list={} customer_id={} message_uuid={} in kcustomer-api", 
						FOLLOWUP_RULES_FAILED, new ObjectMapper().writeValueAsString(vinList), message.getCustomerID(), message.getUuid());
				return null;
			}
			DealerOrderInfo dealerOrder = applyDealerOrderRules(dealerOrders, message, lookbackDate, laborOpCodeFilteringParameter, departmentUUID);
			if(dealerOrder==null) {
				LOGGER.info("{}=VIN  for vin_list={} customer_id={} message_uuid={} ",
						FOLLOWUP_RULES_FAILED, new ObjectMapper().writeValueAsString(vinList), message.getCustomerID(), message.getUuid());
				return null;
			}
			LOGGER.info("{}=VIN for vin_list={} customer_id={} message_uuid={} dealer_order_uuid={} ", 
					FOLLOWUP_RULES_PASSED, new ObjectMapper().writeValueAsString(vinList), message.getCustomerID(), message.getUuid(), dealerOrder.getDealerOrder_uuid());
			return dealerOrder;
		} catch (Exception e) {
			LOGGER.error("Error in running vin rules for customer_id={} message_uuid={} ", message.getCustomerID(), message.getUuid(), e);
			throw e;
		}
	}
	
	public DealerOrderInfo applyDealerOrderRules(List<DealerOrderInfo> dealerOrders, Message message, Date lookbackDate,
			LaborOpCodeFilteringParameter laborOpCodeFilteringParameter, String departmentUUID) {
		DealerOrderInfo dealerOrder = null;
		Date dealerOrderCloseDate = null;
		for(DealerOrderInfo ro: dealerOrders) {
			if(OrderType.SERVICE.getValue().equalsIgnoreCase(ro.getOrder_type()) && OrderStatus.CLOSED.getOrderStatus().equalsIgnoreCase(ro.getOrder_status())) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date orderDate = null, closeDate = null;
				try {
		             orderDate = formatter.parse(ro.getOrder_date());
		             if(ro.getClose_date()!=null) {
		            	 closeDate = formatter.parse(ro.getClose_date());
		             }
		             LOGGER.info("close_date={} order_date={} f_close_date={} f_order_date={} message_uuid={}", ro.getClose_date(), ro.getOrder_date(), 
		            		 closeDate, orderDate,message.getUuid());
				} catch (Exception e) {
					LOGGER.warn("Error in converting date order_date={} close_date={}", ro.getOrder_date(), ro.getClose_date(),e);
				}
				if((orderDate!=null && orderDate.before(message.getSentOn())) && (closeDate!=null && closeDate.after(lookbackDate))) {
					if(dealerOrder==null || dealerOrderCloseDate.before(closeDate)) {
						if(applyLaborOpCodeRules(ro, departmentUUID, laborOpCodeFilteringParameter)) {
							dealerOrder = ro;
							dealerOrderCloseDate = closeDate;
							LOGGER.info("{} candidate dealer_order_id={} for message_uuid={}", FOLLOWUP_RULES, ro.getDealerOrder_id(),message.getId());
						} else {
							LOGGER.info("{} Dealer Order rules failed=OpCode DealerOrder for dealer_order_uuid={} order_date={} close_date={} message_uuid={} lookback_date={} sent_on={} previous_close_date={} ", 
									FOLLOWUP_RULES, ro.getDealerOrder_uuid(),ro.getOrder_date(), ro.getClose_date(), message.getUuid(), lookbackDate, message.getSentOn(), dealerOrderCloseDate);
						}
					} else {
						LOGGER.info("{} Dealer Order rules failed=NonLatest DealerOrder for dealer_order_uuid={} order_date={} close_date={} message_uuid={} lookback_date={} sent_on={} previous_close_date={} ", 
								FOLLOWUP_RULES, ro.getDealerOrder_uuid(),ro.getOrder_date(), ro.getClose_date(), message.getUuid(), lookbackDate, message.getSentOn(), dealerOrderCloseDate);
					}
				} else {
					LOGGER.info("{} Dealer Order rules failed=Dates are not valid for dealer_order_uuid={} order_date={} close_date={} message_uuid={} lookback_date={} sent_on={} ", 
							FOLLOWUP_RULES, ro.getDealerOrder_uuid(),ro.getOrder_date(), closeDate, message.getUuid(), lookbackDate, message.getSentOn());
				}
			} else {
				LOGGER.info("{} Dealer Order rules failed=OrderStatus is not correct for dealer_order_uuid={} order_type={} order_status={} message_uuid={}", 
						FOLLOWUP_RULES, ro.getDealerOrder_uuid(), ro.getOrder_type(), ro.getOrder_status(), message.getUuid());
			}
		}
		return dealerOrder;
	}
	
	private Boolean applyLaborOpCodeRules(DealerOrderInfo dealerOrderInfo, String departmentUUID, LaborOpCodeFilteringParameter parameters) {
		try {
			StandardOrder dealerOrder = kOrderApiHelper.getDealerOrder(departmentUUID, dealerOrderInfo.getDealerOrder_uuid());
			parameters.setDepartmentType(dealerOrder.getDepartmentType());
			parameters.setLaborOpTypes(dealerOrder.getLabourOpTypes());
			parameters.setLaborTypes(dealerOrder.getLaborTypes());
			Boolean laborOpCode = laborOpcodeRules.applyDepartmentLabourOpFilter(dealerOrderInfo.getDealerOrder_uuid(), parameters);
			if(laborOpCode){
				LOGGER.info("{} labor op code rules successful for parameters={} dealer_order_uuid={} ", 
						FOLLOWUP_RULES, new ObjectMapper().writeValueAsString(parameters), dealerOrder.getUuid());
				return true;
			} else {
				LOGGER.info("{} labor op code rules failed for parameters={} dealer_order_uuid={} ", 
						FOLLOWUP_RULES, new ObjectMapper().writeValueAsString(parameters), dealerOrder.getUuid());
				return false;
			}
		} catch (Exception e) {
			LOGGER.error("Error in applyLaborOpCodeRules for dealer_order_uuid={} department_uuid={} ", dealerOrderInfo.getDealerOrder_uuid(), departmentUUID, e);
		}
		return false;
	}
	private void onDealerOrderMatched(Message message, DealerOrderInfo dealerOrderInfo) throws Exception {
		MessageMetaData mmd = messageMetaDataRepository.findByMessageID(message.getId());
		HashMap<String, String> metaData = new HashMap<String, String>();
		if(mmd!=null && mmd.getMetaData()!=null && !mmd.getMetaData().isEmpty()) {
			metaData = helper.getMessageMetaDatMap(mmd.getMetaData());
			LOGGER.info("orig_meta_date={} message_uuid={}", new ObjectMapper().writeValueAsString(metaData), message.getUuid());
		}
		metaData.put(MessageMetaDataConstants.DEALER_ORDER_UUID, dealerOrderInfo.getDealerOrder_uuid());
		String messageMetaData = helper.getMessageMetaData(metaData);
		LOGGER.info("string_meta_date={} message_uuid={}", messageMetaData, message.getUuid());
		messageMetaDataRepository.upsertMessageMetaData(message.getId(), messageMetaData);
	}
	
	private Date getLookbackStartDate(String dealerUUID, Message message) throws Exception{
		String lookbackDays = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.COMMUNICATIONS_MANUAL_FOLLOWUP_LOOKBACK_DAYS.getOptionKey());
		Integer lookback = 15;
		if(lookbackDays!=null && !lookbackDays.isEmpty()) {
			try {
				lookback = Integer.parseInt(lookbackDays);
			} catch (Exception e) {
				LOGGER.warn("COMMUNICATIONS_MANUAL_FOLLOWUP_LOOKBACK_DAYS is incorrectly set, defaulting to 15 COMMUNICATIONS_MANUAL_FOLLOWUP_LOOKBACK_DAYS={} ", lookbackDays);
			}
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(message.getSentOn());
		cal.add(Calendar.DAY_OF_YEAR, lookback*-1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		Date lookbackStart = cal.getTime();
		LOGGER.info("{} lookback_date={} for message_uuid={} lookback_days={} message_sent_on={} ", 
				FOLLOWUP_RULES, lookbackStart, message.getUuid(), lookbackDays, message.getSentOn());
		return lookbackStart;
	}
}

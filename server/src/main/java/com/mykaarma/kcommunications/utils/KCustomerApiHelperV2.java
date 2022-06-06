package com.mykaarma.kcommunications.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.mykaarma.dms.common.enumerations.OrderType;
import com.mykaarma.kcustomer_client.KCustomerApiClientService;
import com.mykaarma.kcustomer_client.KMultiStoreCustomerApiClientService;
import com.mykaarma.kcustomer_model.dto.CustomerInfoWithDealerOrderInfoList;
import com.mykaarma.kcustomer_model.enums.CustomerSearch;
import com.mykaarma.kcustomer_model.enums.QueryOperator;
import com.mykaarma.kcustomer_model.lombokresponse.CustomerSaveResponse;
import com.mykaarma.kcustomer_model.lombokresponse.CustomerWithVehiclesResponse;
import com.mykaarma.kcustomer_model.dto.CustomerUpdateRequest;
import com.mykaarma.kcustomer_model.request.UpdateCustomerSentimentStatusRequest;
import com.mykaarma.kcustomer_model.response.CheckCustomerSentimentResponse;
import com.mykaarma.kcustomer_model.lombokresponse.CustomerWithDealerOrderExactSearchResponse;
import com.mykaarma.kcustomer_model.response.Response;

@Component
@Service
public class KCustomerApiHelperV2 {
	private final static Logger LOGGER = LoggerFactory.getLogger(KCustomerApiHelperV2.class);
	private static String username ;
	private static String password ;
	private static String base_url ;
	
	private static KCustomerApiClientService clientService = null;
	private static KMultiStoreCustomerApiClientService kMultiStoreCustomerApiClientService = null;
	
	@Value("${kcommunications_basic_auth_user}")
	public  void setUsername(String username) {
		KCustomerApiHelperV2.username = username;
	}

	
    @Value("${kcommunications_basic_auth_pass}")
	public  void setPassword(String password) {
    	KCustomerApiHelperV2.password = password;
	}

    @Value("${kcustomer_api_url_v2}")
	public  void setBase_url(String base_url) {
    	KCustomerApiHelperV2.base_url = base_url;
	}


	private static KCustomerApiClientService getKCustomerClientService(){
		if(clientService==null){
			clientService = new KCustomerApiClientService(base_url,username,password);
		}
		return clientService;
		
	}
	
	private static KMultiStoreCustomerApiClientService getKMultiStoreCustomerClientService(){
		if(kMultiStoreCustomerApiClientService==null){
			kMultiStoreCustomerApiClientService = new KMultiStoreCustomerApiClientService(base_url,username,password);
		}
		return kMultiStoreCustomerApiClientService;
		
	}
	
	public static CustomerWithVehiclesResponse getCustomer(String departmentUUID, String customerUUID) {
		try {
			CustomerWithVehiclesResponse customer = getKCustomerClientService().getCustomer(departmentUUID, customerUUID);
			return customer;
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getCustomer customer_uuid=%s department_uuid=%s ", 
					customerUUID,departmentUUID),e);
			return null;
		}
	}

	public static CustomerWithVehiclesResponse getCustomerWithoutVehicle(String departmentUUID, String customerUUID) {
		try {
			CustomerWithVehiclesResponse customer = getKCustomerClientService().getCustomerWithoutVehicles(departmentUUID, customerUUID);
			return customer;
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getCustomerWithoutVehicle customer_uuid=%s department_uuid=%s ", 
					customerUUID,departmentUUID),e);
			return null;
		}
	}
	
	public static String saveCustomer(String departmentUUID, CustomerUpdateRequest customerUpdateRequest) {
		try {
			CustomerSaveResponse customer = getKCustomerClientService().saveCustomer(departmentUUID, customerUpdateRequest);
			return customer.getCustomerId();
		} catch (Exception e) {
			LOGGER.error(String.format("Error while savingCustomer for department_uuid=%s ", 
					departmentUUID),e);
			return null;
		}
	}
	
	public static List<CustomerInfoWithDealerOrderInfoList> callKCustomerExactApiSearchForParameters(CustomerSearch customerSearchParameter, HashSet<String> searchStrings, String dealerUuid, 
			OrderType orderType) throws Exception{
		HashMap<CustomerSearch,Set<String>> searchMap = new HashMap<CustomerSearch,Set<String>>();		
		searchMap.put(customerSearchParameter, searchStrings);
		List<String>dealerUuidList = new  ArrayList<>();
		dealerUuidList.add(dealerUuid);
		return callKcustomerApiForSearch(dealerUuidList,searchMap,QueryOperator.AND,true,orderType);
		
	}
	
	
	private static  List<CustomerInfoWithDealerOrderInfoList> callKcustomerApiForSearch(List<String>dealerUuidList,
			HashMap<CustomerSearch,Set<String>> searchRequest, QueryOperator qo, Boolean includeDealerOrder,OrderType type) throws Exception{
		
		CustomerWithDealerOrderExactSearchResponse apiResponse;
		List<CustomerInfoWithDealerOrderInfoList> customerWithDOsList = null;
		
		try {
			apiResponse = getKMultiStoreCustomerClientService()
					.getCustomersListWithExactSearch(searchRequest, qo, includeDealerOrder, dealerUuidList, type);
					
			
			if(apiResponse!=null && (apiResponse.getErrors()==null || apiResponse.getErrors().isEmpty() )
					&& apiResponse.getCustomerInfoWithDealerOrderInfoList()!=null
					&& !apiResponse.getCustomerInfoWithDealerOrderInfoList().isEmpty()) {
				
				customerWithDOsList = apiResponse.getCustomerInfoWithDealerOrderInfoList();
			}
			else if (apiResponse.getErrors()!=null && !apiResponse.getErrors().isEmpty()){
				
				LOGGER.error(String.format(" Error while calling customer API . "
						+ " error_code=%s "
						+ " error_description=%s error ", 
						apiResponse.getErrors().get(0).getErrorCode(),
						apiResponse.getErrors().get(0).getErrorMessage()));
			}
		} catch (Exception e) {
			
			LOGGER.error("Error ocurred while fetching customer with dealer orders",e);
			throw e;
		}
		return customerWithDOsList;
	}

	public Boolean checkCustomerSentimentStatus(String customerUUID, String departmentUUID) throws Exception{
		
		Boolean result = false;
		try {
			LOGGER.info("In checkCustomerSentimentStatus for customer_uuid={} department_uuid={}", customerUUID,departmentUUID);
			
			CheckCustomerSentimentResponse checkCustomerSentimentResponse = getKCustomerClientService().checkCustomerSentimentStatus(departmentUUID, customerUUID);
			if(checkCustomerSentimentResponse!=null && checkCustomerSentimentResponse.getIsUpset()!=null) {
				result = checkCustomerSentimentResponse.getIsUpset();
			}
			else {
				LOGGER.error("Error in checkCustomerSentimentStatus for customer_uuid={} department_uuid={}",customerUUID, departmentUUID);
			}
			return result;
			
		} catch (Exception e) {
			LOGGER.error("Error in checkCustomerSentimentStatus for customer_uuid={} department_uuid={}",customerUUID, departmentUUID, e);
		}
		return result;
	}

	public void updateCustomerSentimentStatus(String customerUUID, String departmentUUID, UpdateCustomerSentimentStatusRequest updateCustomerSentimentStatusRequest) throws Exception{
		
		try {
			LOGGER.info("In updateCustomerSentimentStatus for customer_uuid={} department_uuid={}", customerUUID,departmentUUID);
			
			Response response = getKCustomerClientService().updateCustomerSentimentStatus(departmentUUID, customerUUID, updateCustomerSentimentStatusRequest);
			
			if(response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
				LOGGER.error("Error in updateCustomerSentimentStatus for customer_uuid={} department_uuid={}",customerUUID, departmentUUID);
				throw new Exception();
			}
			
		} catch (Exception e) {
			LOGGER.error("Error in updateCustomerSentimentStatus for customer_uuid={} department_uuid={}",customerUUID, departmentUUID, e);
			throw e;
		}
	}

}
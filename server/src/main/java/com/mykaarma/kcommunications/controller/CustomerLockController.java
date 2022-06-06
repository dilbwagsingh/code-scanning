package com.mykaarma.kcommunications.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.ApiAuthenticatorAndAuthorizer;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.CustomerLockService;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.dto.CustomerLockDTO;
import com.mykaarma.kcommunications_model.dto.MessageDTO;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.request.FetchCustomerLockRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.CustomerLockListResponse;
import com.mykaarma.kcommunications_model.response.MessagesFetchResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

@RestController
@Configuration
@Slf4j
@ComponentScan("com.mykaarma.kcommunications.services")
@Api(tags = "Customer Lock Controller", description = "Endpoints for fetching customer lock information")
public class CustomerLockController {

	private ObjectMapper objectMapper=new ObjectMapper();
	
	@Autowired
	ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;
	
	@Autowired
	CustomerLockService customerLockService;
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_CUSTOMER_LOCK_READ,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "fetch customer lock information for given customer identifiers list and department", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customerlock/list", method = RequestMethod.POST)
	public ResponseEntity<CustomerLockListResponse> fetchCustomerLockInfo(
			@PathVariable("departmentUUID") String departmentUuid,
			@RequestBody FetchCustomerLockRequest fetchCustomerLockRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		CustomerLockListResponse customerLockListResponse = new CustomerLockListResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "fetchCustomerLockInfo");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In fetchCustomerLockInfo request={} subscriber={}", objectMapper.writeValueAsString(fetchCustomerLockRequest), subscriberName);
			List<CustomerLockDTO> customerLockList = customerLockService.getCustomerLockInfoForRequest(departmentUuid, fetchCustomerLockRequest);
			customerLockListResponse.setCustomerLockInfoList(customerLockList);
			customerLockListResponse.setRequestUuid(requestID);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format(" time_taken=%d response=%s", elapsedTime,objectMapper.writeValueAsString(customerLockListResponse)));
			return new ResponseEntity<CustomerLockListResponse>(customerLockListResponse, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Exception in fetching customer lock info for department_uuid={} request={} ",departmentUuid,
					objectMapper.writeValueAsString(fetchCustomerLockRequest), e);
			
			customerLockListResponse.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            customerLockListResponse.setErrors(errors);
			return new ResponseEntity<CustomerLockListResponse>(customerLockListResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
}

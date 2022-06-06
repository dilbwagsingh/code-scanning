package com.mykaarma.kcommunications.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.DefaultThreadOwnerImpl;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.response.DefaultThreadOwnerForDealerResponse;
import com.mykaarma.kcommunications_model.response.DefaultThreadOwnerResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RestController
@Configuration
@ComponentScan("com.mykaarma.kcommunications.services")
public class DefaultThreadOwnerController {
	
	@Autowired
	private DefaultThreadOwnerImpl defaultThreadOwnerImpl;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(CommunicationsApiController.class);
	
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_DEFAULT_THREAD_OWNER_READ,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "get deafult thread owner for a user and a department", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUUID}/defaultThreadOwner", method = RequestMethod.GET)
	public ResponseEntity<DefaultThreadOwnerResponse> defaultThreadOwner(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("userUUID") String userUUID,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
			
		JsonObject loguuidJson = new JsonObject();
		loguuidJson.addProperty(APIConstants.METHOD, "defaultThreadOwner");
		loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
		loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
		loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
		loguuidJson.addProperty(APIConstants.USER_UUID, userUUID);
		String logUUID = loguuidJson.toString();

		MDC.put(APIConstants.LogUUID, logUUID);
		MDC.put(APIConstants.FILTER_REQUEST, "true");
		
		String defaultOwnerUserUUID= defaultThreadOwnerImpl.getDefaultThreadOwner(departmentUUID, userUUID);
		LOGGER.info("default_thread_owner_user_uuid={} for department_uuid={} user_uuid={}",defaultOwnerUserUUID,departmentUUID, userUUID);
		DefaultThreadOwnerResponse response = new DefaultThreadOwnerResponse();
		response.setRequestUUID(requestID);
		response.setDefaultThreadOwnerUserUUID(defaultOwnerUserUUID);
		return new ResponseEntity<DefaultThreadOwnerResponse>(response, HttpStatus.OK);	
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_DEFAULT_THREAD_OWNER_READ,apiScopeLevel = ApiScopeLevel.DEALER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "get deafult thread owner information for all the users of a dealership", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "dealer/{dealerUUID}/defaultThreadOwner", method = RequestMethod.GET)
	public ResponseEntity<DefaultThreadOwnerForDealerResponse> defaultThreadOwnerList(
			@PathVariable("dealerUUID") String dealerUUID,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
			
		JsonObject loguuidJson = new JsonObject();
		loguuidJson.addProperty(APIConstants.METHOD, "defaultThreadOwner");
		loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
		loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
		loguuidJson.addProperty(APIConstants.DEALER_TOKEN, dealerUUID);
		String logUUID = loguuidJson.toString();

		MDC.put(APIConstants.LogUUID, logUUID);
		MDC.put(APIConstants.FILTER_REQUEST, "true");
		
		LOGGER.info("in  defaultThreadOwnerList for dealer_uuid={}",dealerUUID );
		DefaultThreadOwnerForDealerResponse response = defaultThreadOwnerImpl.getDefaultThreadOwnerInfoForDealer(dealerUUID);
		response.setRequestUUID(requestID);
		
		LOGGER.info("in  defaultThreadOwnerList for dealer_uuid={} response={}",dealerUUID,new ObjectMapper().writeValueAsString(response) );
		return new ResponseEntity<DefaultThreadOwnerForDealerResponse>(response, HttpStatus.OK);	
	}
	
	
}

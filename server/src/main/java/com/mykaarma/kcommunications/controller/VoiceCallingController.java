package com.mykaarma.kcommunications.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.VoiceCallingControllerImpl;
import com.mykaarma.kcommunications_model.common.VoiceCallRequest;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcommunications_model.response.VoiceCallResponse;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import com.mykaarma.kcommunications.utils.APIConstants;

@RestController
@Configuration
@ComponentScan("com.mykaarma.kcommunications.services")
public class VoiceCallingController {

	@Autowired
	VoiceCallingControllerImpl voiceCallingControllerImpl;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(VoiceCallingController.class);	
	
	
	/**
     * creates voice call between dealership and customer based on passed parameters
     *
     * @param VoiceCallRequest voiceCallRequest
     * @return ResponseEntity<VoiceCallResponse> 
     */	
	@KCommunicationsAuthorize(apiScope = ApiScope.CREATE_CALL,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "create dealership to customer call", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customer/{customerUUID}/twilio/callContact", method = RequestMethod.POST) 
	public ResponseEntity<VoiceCallResponse> callContact(
			@PathVariable String departmentUUID,
			@PathVariable(value = "customerUUID", required = false) String customerUUID,			
			@RequestBody VoiceCallRequest voiceCallRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID
			) throws Exception{ 
			 
		ResponseEntity<VoiceCallResponse> response = null;
		
		try {
			
			JsonObject loguuidJson = new JsonObject(); 
			loguuidJson.addProperty(APIConstants.METHOD, "createCall");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
			
			String logUUID = loguuidJson.toString();
	
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In createCall departmentUUID = {}, customerUUID = {}, request={}", departmentUUID, customerUUID, new ObjectMapper().writeValueAsString(voiceCallRequest));
			
			response = voiceCallingControllerImpl.callContact(departmentUUID, customerUUID, voiceCallRequest);
			
		}catch (Exception e) {
			
			 LOGGER.error("Error while creating calldepartmentUUID = {}, customerUUID = {}, request={}", departmentUUID, customerUUID, new ObjectMapper().writeValueAsString(voiceCallRequest), e);
		}
		
		return response;
		
	}
	
	/**
     * cancels voice call between dealership and customer based on passed parameters
     *
     * @param String departmentUUID
     * @param String callSid
     * @return ResponseEntity<Response> 
     */	
	@KCommunicationsAuthorize(apiScope = ApiScope.CANCEL_CALL,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "cancel dealership to customer call", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/callSid/{callSID}/cancelCall", method = RequestMethod.POST) 
	public ResponseEntity<Response> cancelCall(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("callSID") String callSID,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID
			) throws Exception{ 
			 
		
		ResponseEntity<Response> response = null;
		
		try {
			
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "cancelCall");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();
	
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			response = voiceCallingControllerImpl.cancelCall(departmentUUID, callSID);
			
			LOGGER.info("In cancelCall departmentUUID = {}, callSid = {}", departmentUUID, callSID);
			
		}catch (Exception e) {			
			LOGGER.error("Error in cancelCall departmentUUID = {}, callSid = {}", departmentUUID, callSID, e);
		}
		
		return response;
		
	}

}

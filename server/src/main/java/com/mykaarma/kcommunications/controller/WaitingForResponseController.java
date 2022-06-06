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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.WaitingForResponseImpl;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.request.SubscriptionSaveRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.SearchTemplateResponse;
import com.mykaarma.kcommunications_model.response.SubscriptionSaveResponse;
import com.mykaarma.kcommunications_model.response.WaitingForResponseStatusResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

@RestController
@Configuration
@Slf4j
@ComponentScan("com.mykaarma.kcommunications.services")
@Api(tags = "Waiting For Response Controller", description = "Endpoints related to waiting for response feature")
public class WaitingForResponseController {
	
	@Autowired
	WaitingForResponseImpl waitingForResponseImpl;

	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_WAITING_FOR_RESPONSE_READ,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "fetch waiting of response status of customer's thread for a given department", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customer/{customerUuid}/waitingforresponse/status", method = RequestMethod.GET)
	public ResponseEntity<WaitingForResponseStatusResponse> fetchWaitingForResponseStatus(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("customerUuid") String customerUuid,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) {
		
		
		Date messageTimestamp = new Date();
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "fetchWaitingForResponseStatus");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUuid);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			log.info("In fetchWaitingForResponseStatus for customer_uuid={} department_uuid={} ", customerUuid, departmentUUID);
			Boolean isWaitingForResponse = waitingForResponseImpl.getWaitingForResponseStauts(customerUuid, departmentUUID);

			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format("time_taken=%d for fetching waiting for response status"
					+ " response=%s", elapsedTime,isWaitingForResponse));
			WaitingForResponseStatusResponse waitingForResponseStatusResponse=new WaitingForResponseStatusResponse();
			waitingForResponseStatusResponse.setInWaitingForResponse(isWaitingForResponse);
			return new ResponseEntity<WaitingForResponseStatusResponse>(waitingForResponseStatusResponse, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Exception in fetching waiting for response for customer_uuid={}  department_uuid={}", customerUuid, departmentUUID, e);
			WaitingForResponseStatusResponse waitingForResponseStatusResponse = new WaitingForResponseStatusResponse();
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            waitingForResponseStatusResponse.setErrors(errors);
			return new ResponseEntity<WaitingForResponseStatusResponse>(waitingForResponseStatusResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
}

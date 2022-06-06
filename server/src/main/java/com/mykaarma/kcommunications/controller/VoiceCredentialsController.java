package com.mykaarma.kcommunications.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.ApiAuthenticatorAndAuthorizer;
import com.mykaarma.kcommunications.controller.impl.VoiceCredentialsImpl;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.GetDepartmentUUIDResponse;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RestController
@Configuration
@ComponentScan("com.mykaarma.kcommunications.services")
public class VoiceCredentialsController {

	@Autowired
	ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;

	@Autowired
	VoiceCredentialsImpl voiceCredentialsImpl;

	private final static Logger LOGGER = LoggerFactory.getLogger(VoiceCredentialsController.class);

	@ResponseBody
	@ApiOperation(value = "Get departmentUUID for given broker number", authorizations = {
			@Authorization(value = "basicAuth") })
	@RequestMapping(value = "/voicecredential/{brokerNumber}", method = RequestMethod.GET)
	public ResponseEntity<GetDepartmentUUIDResponse> getDepartmentUUID(
			@PathVariable("brokerNumber") String brokerNumber, @RequestHeader("authorization") String authToken) {

		ResponseEntity<GetDepartmentUUIDResponse> response = null;

		Date messageTimestamp = new Date();

		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "getDepartmentUUID");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			loguuidJson.addProperty(APIConstants.BROKER_NUMBER, brokerNumber);

			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");

			apiAuthenticatorAndAuthorizer.checkServiceSubscriberToApiScopeAuthorization(subscriberName,
					com.mykaarma.kcommunications_model.enums.ApiScope.COMMUNICATIONS_VOICECREDENTIAL_READ);

			LOGGER.info("In getDepartmentUUID for broker_number={}", brokerNumber);
			response = voiceCredentialsImpl.getDepartmentUUID(brokerNumber);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			LOGGER.info(String.format("Response status %s sent. time_taken=%d response=%s", response.getStatusCode(),
					elapsedTime, new ObjectMapper().writeValueAsString(response)));

		} catch (Exception e) {
			LOGGER.error("Exception in getDepartmentUUID for broker_number={}",brokerNumber, e);
			GetDepartmentUUIDResponse getDepartmentUUIDResponse = new GetDepartmentUUIDResponse();
			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), "Internal server error");
			List<ApiError> errors = new ArrayList<ApiError>();
			errors.add(apiError);
			getDepartmentUUIDResponse.setErrors(errors);
			return new ResponseEntity<GetDepartmentUUIDResponse>(getDepartmentUUIDResponse,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
}

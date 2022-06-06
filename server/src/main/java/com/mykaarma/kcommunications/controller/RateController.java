package com.mykaarma.kcommunications.controller;

import java.util.Date;

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

import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.RateControllerImpl;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.CommunicationsFeature;
import com.mykaarma.kcommunications_model.response.Response;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RestController
@Configuration
@ComponentScan("com.mykaarma.kcommunications.services")
public class RateController {
	
	@Autowired
	private RateControllerImpl rateControllerImpl;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(CommunicationsApiController.class);
	
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_RATE_CONTROL,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "post usage", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/feature/{feature}/communicationvalue/{communicationValue}/usage", method = RequestMethod.POST)
	public ResponseEntity<Response> rateController(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("feature") CommunicationsFeature feature,
			@PathVariable("communicationValue") String communicationValue,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		Date messageTimestamp = new Date();
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "rateController");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.FEATURE, feature.name());
			loguuidJson.addProperty(APIConstants.COMMUNICATION_VALUE, communicationValue);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In rateController");
			response = rateControllerImpl.rateController(departmentUUID, feature, communicationValue);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			LOGGER.info(String.format("Response status %s sent. time_taken=%d ", response.getStatusCode(), elapsedTime));
		} catch (Exception e) {
			LOGGER.error("Exception in rateController ", e);
			Response nullResponse = null;
			return new ResponseEntity<Response>(nullResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
}

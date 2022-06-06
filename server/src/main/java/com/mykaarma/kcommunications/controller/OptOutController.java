package com.mykaarma.kcommunications.controller;

import java.util.Arrays;

import com.mykaarma.kcommunications.authorize.ApiAuthenticatorAndAuthorizer;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.OptOutImpl;
import com.mykaarma.kcommunications.model.api.ErrorCodes;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.request.CommunicationsOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.CustomersOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.DoubleOptInDeploymentRequest;
import com.mykaarma.kcommunications_model.request.PredictOptOutStatusCallbackRequest;
import com.mykaarma.kcommunications_model.request.UpdateOptOutStatusRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.OptOutResponse;
import com.mykaarma.kcommunications_model.response.OptOutStatusListResponse;
import com.mykaarma.kcommunications_model.response.OptOutStatusResponse;
import com.mykaarma.kcommunications_model.response.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@Configuration
@Api(tags = "opt-out", description="Endpoints to update, fetch and predict opt-out status of customer(s) / communication value(s)")
@ComponentScan("com.mykaarma.kcommunications.services")
public class OptOutController {
    
    @Autowired
    private OptOutImpl optOutImpl;

    @Autowired
	private ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;

    private final static Logger LOGGER = LoggerFactory.getLogger(OptOutController.class);
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_OPT_OUT, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "predict whether message is opt-out type", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/message/{messageUUID}/optout/predict", method = RequestMethod.POST)
	public ResponseEntity<OptOutResponse> predictOptOutForMessage(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("messageUUID") String messageUUID,
			@ApiIgnore @RequestHeader("authorization") String authToken,
			@ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<OptOutResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "predictOptOutForMessage");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.MESSAGE_UUID, messageUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In predict opt-out for department_uuid={} message_uuid={}", departmentUUID, messageUUID);
		    response = optOutImpl.predictOptOutForMessage(messageUUID, departmentUUID);
		    if(response!=null && response.getBody()!=null){
				response.getBody().setRequestUUID(requestID);
			}
			
		} catch (Exception e) {
			LOGGER.error("Exception in predict opt-out for department_uuid={} message_uuid={}", departmentUUID, messageUUID, e);
			return new ResponseEntity<OptOutResponse>(new OptOutResponse(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_OPT_OUT_READ, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "get opt-out status for communication type and value", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/communicationType/{communicationType}/communicationValue/{communicationValue}/optoutstatus", method = RequestMethod.GET)
	public ResponseEntity<OptOutStatusResponse> getOptOutStatus(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("communicationType") String communicationType,
			@PathVariable("communicationValue") String communicationValue,
			@ApiIgnore @RequestHeader("authorization") String authToken,
			@ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<OptOutStatusResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "getOptOutStatus");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.COMMUNICATION_TYPE, communicationType);
			loguuidJson.addProperty(APIConstants.COMMUNICATION_VALUE, communicationValue);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("in getOptOutStatus for department_uuid={} communication_type={} communication_value={}", departmentUUID, communicationType, communicationValue);
		    response = optOutImpl.getOptOutStatus(departmentUUID, communicationType, communicationValue);
		    if(response!=null && response.getBody()!=null){
				response.getBody().setRequestUUID(requestID);
			}
			return response;
		} catch (Exception e) {
			LOGGER.error("Exception while fetching opt-out status for department_uuid={} communication_type={} communication_value={}", departmentUUID, communicationType, communicationValue, e);
			OptOutStatusResponse errorResponse = new OptOutStatusResponse();
			errorResponse.setRequestUUID(requestID);
			errorResponse.setErrors(Arrays.asList(new ApiError(ErrorCodes.INTERNAL_ERROR.name(), e.getMessage())));
			return new ResponseEntity<OptOutStatusResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_OPT_OUT_READ, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "get opt-out status list for communication attributes", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/communications/optoutstatus/list", method = RequestMethod.POST)
	public ResponseEntity<OptOutStatusListResponse> getCommunicationsOptOutStatusList(
			@PathVariable("departmentUUID") String departmentUUID,
			@ApiIgnore @RequestHeader("authorization") String authToken,
			@RequestBody CommunicationsOptOutStatusListRequest request,
			@ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<OptOutStatusListResponse> response = null;
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "getCommunicationsOptOutStatusList");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("in getCommunicationsOptOutStatusList for department_uuid={} request={}", departmentUUID, OBJECT_MAPPER.writeValueAsString(request));
		    response = optOutImpl.getCommunicationsOptOutStatusList(departmentUUID, request);
		    if(response!=null && response.getBody()!=null) {
				response.getBody().setRequestUUID(requestID);
			}
			return response;
		} catch (Exception e) {
			LOGGER.error("Exception while fetching opt-out status list for department_uuid={} request={}", departmentUUID, OBJECT_MAPPER.writeValueAsString(request), e);
			OptOutStatusListResponse errorResponse = new OptOutStatusListResponse();
			errorResponse.setRequestUUID(requestID);
			errorResponse.setErrors(Arrays.asList(new ApiError(ErrorCodes.INTERNAL_ERROR.name(), e.getMessage())));
			return new ResponseEntity<OptOutStatusListResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_OPT_OUT_READ, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "get opt-out status list for customers", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customers/optoutstatus/list", method = RequestMethod.POST)
	public ResponseEntity<OptOutStatusListResponse> getCustomersOptOutStatusList(
			@PathVariable("departmentUUID") String departmentUUID,
			@ApiIgnore @RequestHeader("authorization") String authToken,
			@RequestBody CustomersOptOutStatusListRequest request,
			@ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<OptOutStatusListResponse> response = null;
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "getCustomersOptOutStatusList");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("in getCustomersOptOutStatusList for department_uuid={} request={}", departmentUUID, OBJECT_MAPPER.writeValueAsString(request));
		    response = optOutImpl.getCustomersOptOutStatusList(departmentUUID, request);
		    if(response!=null && response.getBody()!=null) {
				response.getBody().setRequestUUID(requestID);
			}
			return response;
		} catch (Exception e) {
			LOGGER.error("Exception while fetching opt-out status list for department_uuid={} request={}", departmentUUID, OBJECT_MAPPER.writeValueAsString(request), e);
			OptOutStatusListResponse errorResponse = new OptOutStatusListResponse();
			errorResponse.setRequestUUID(requestID);
			errorResponse.setErrors(Arrays.asList(new ApiError(ErrorCodes.INTERNAL_ERROR.name(), e.getMessage())));
			return new ResponseEntity<OptOutStatusListResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_OPT_OUT_WRITE, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "updates opt-out status with prediction of opt out message ai", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/message/{messageUUID}/optoutstatus/predict/callback", method = RequestMethod.POST)
	public ResponseEntity<Response> predictOptOutStatusCallback(
		@PathVariable("departmentUUID") String departmentUUID,
		@PathVariable("messageUUID") String messageUUID,
		@ApiIgnore @RequestHeader("authorization") String authToken,
		@RequestBody PredictOptOutStatusCallbackRequest request,
		@ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {

		ResponseEntity<Response> response = null;
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "predictOptOutStatusCallback");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);

			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");

			LOGGER.info("in predictOptOutStatusCallback for department_uuid={} message_uuid={} request={}", departmentUUID, messageUUID, OBJECT_MAPPER.writeValueAsString(request));
			response = optOutImpl.predictOptOutStatusCallback(departmentUUID, messageUUID, request);
			return response;
		} catch (Exception e) {
			LOGGER.error("Exception while updating opt-out status list for department_uuid={} message_uuid={} request={}", departmentUUID, messageUUID, OBJECT_MAPPER.writeValueAsString(request), e);
			Response errorResponse = new Response();
			errorResponse.setErrors(Arrays.asList(new ApiError(ErrorCodes.INTERNAL_ERROR.name(), e.getMessage())));
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_OPT_OUT_WRITE, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update opt-out status for a communication context", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/optoutstatus", method = RequestMethod.POST)
	public ResponseEntity<Response> updateOptOutStatus(
			@PathVariable("departmentUUID") String departmentUUID,
			@ApiIgnore @RequestHeader("authorization") String authToken,
			@RequestBody UpdateOptOutStatusRequest request,
			@ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {

		ResponseEntity<Response> response = null;
		try {
			String serviceSubscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updateOptOutStatus");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, serviceSubscriberName);

			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("in updateOptOutStatus for department_uuid={} request={}", departmentUUID, OBJECT_MAPPER.writeValueAsString(request));
		    response = optOutImpl.updateOptOutStatus(departmentUUID, request, serviceSubscriberName);
			return response;
		} catch (Exception e) {
			LOGGER.error("Exception while updating opt-out status list for department_uuid={} request={}", departmentUUID, OBJECT_MAPPER.writeValueAsString(request), e);
			Response errorResponse = new Response();
			errorResponse.setErrors(Arrays.asList(new ApiError(ErrorCodes.INTERNAL_ERROR.name(), e.getMessage())));
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_OPT_OUT_DEPLOY, apiScopeLevel = ApiScopeLevel.SERVICE_SUBSCRIBER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "rollout or rollback double opt-in for a dealership", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "dealer/{dealerUUID}/doubleOptin/deploy", method = RequestMethod.POST)
	public ResponseEntity<Response> deployDoubleOptIn(
			@PathVariable("dealerUUID") String dealerUUID,
			@ApiIgnore @RequestHeader("authorization") String authToken,
			@RequestBody DoubleOptInDeploymentRequest request,
			@ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "deployDoubleOptIn");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_TOKEN, dealerUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("in deployDoubleOptIn for dealer_uuid={} request={}", dealerUUID, OBJECT_MAPPER.writeValueAsString(request));
		    response = optOutImpl.deployDoubleOptIn(dealerUUID, request);
			return response;
		} catch (Exception e) {
			LOGGER.error("Exception while deploying doubleOptIn dealer_uuid={} request={}", dealerUUID, OBJECT_MAPPER.writeValueAsString(request), e);
			Response errorResponse = new Response();
			errorResponse.setErrors(Arrays.asList(new ApiError(ErrorCodes.INTERNAL_ERROR.name(), e.getMessage())));
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
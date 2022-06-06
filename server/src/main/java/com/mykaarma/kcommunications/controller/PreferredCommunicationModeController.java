package com.mykaarma.kcommunications.controller;

import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.PreferredCommunicationModeImpl;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.GetPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.MultipleCustomersPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.PredictPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcommunications_model.request.MultipleCustomersPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.PredictPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.UpdatePreferredCommunicationModeRequest;

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

import java.util.Arrays;

import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RestController
@Configuration
@Api(tags = "preferred-communication-mode", description="Endpoints to update, fetch and predict preferred communication mode of customer(s)")
@ComponentScan("com.mykaarma.kcommunications.services")
public class PreferredCommunicationModeController {
    
    @Autowired
    private PreferredCommunicationModeImpl preferredCommunicationModeImpl;

    private final static Logger LOGGER = LoggerFactory.getLogger(PreferredCommunicationModeController.class);

    /**
     * update preferred communication mode for customer
     * @param UpdatePreferredCommunicationModeRequest updatePreferredCommunicationModeRequest
     * @return ResponseEntity<Response>
    */
    @KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_PREFERRED_COMMUNICATION_MODE_WRITE, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update preferred communication mode for customer", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customer/{customerUUID}/preferredcommunicationmode", method = RequestMethod.POST)
	public ResponseEntity<Response> updatePreferredCommunicationMode(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("customerUUID") String customerUUID,
            @RequestHeader("authorization") String authToken,
            @RequestBody UpdatePreferredCommunicationModeRequest updatePreferredCommunicationModeRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updatePreferredCommunicationMode");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
            loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);            
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In update preferred communication mode for department_uuid={} customer_uuid={}", departmentUUID, customerUUID);
		    response = preferredCommunicationModeImpl.updatePreferredCommunicationMode(departmentUUID, customerUUID, updatePreferredCommunicationModeRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in update preferred communication mode for department_uuid={} customer_uuid={}", departmentUUID, customerUUID, e);
            Response errorResponse = new Response();
            ApiError error = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage());
            errorResponse.setErrors(Arrays.asList(error));
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}

    /**
     * get preferred communication mode for customer
     * @return ResponseEntity<GetPreferredCommunicationModeResponse>
    */
    @KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_PREFERRED_COMMUNICATION_MODE_READ, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "get preferred communication mode for customer", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customer/{customerUUID}/preferredcommunicationmode", method = RequestMethod.GET)
	public ResponseEntity<GetPreferredCommunicationModeResponse> getPreferredCommunicationMode(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("customerUUID") String customerUUID,
            @RequestHeader("authorization") String authToken,
            @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<GetPreferredCommunicationModeResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "getPreferredCommunicationMode");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
            loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);            
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In get preferred communication mode for department_uuid={} customer_uuid={}", departmentUUID, customerUUID);
		    response = preferredCommunicationModeImpl.getPreferredCommunicationMode(departmentUUID, customerUUID);
			if(response != null && response.getBody() != null){
				response.getBody().setRequestUUID(requestID);
			}
		} catch (Exception e) {
			LOGGER.error("Exception in get preferred communication mode for department_uuid={} customer_uuid={}", departmentUUID, customerUUID, e);
            GetPreferredCommunicationModeResponse errorResponse = new GetPreferredCommunicationModeResponse();
            ApiError error = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage());
			errorResponse.setRequestUUID(requestID);
            errorResponse.setErrors(Arrays.asList(error));
            return new ResponseEntity<GetPreferredCommunicationModeResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}

    /**
     * get preferred communication mode protocol for multiple customers
     * @param MultipleCustomersPreferredCommunicationModeRequest multipleCustomersPreferredCommunicationModeRequest
     * @return ResponseEntity<MultipleCustomersPreferredCommunicationModeResponse>
    */
    @KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_PREFERRED_COMMUNICATION_MODE_READ, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "get preferred communication mode protocol for multiple customers", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customers/preferredcommunicationmode/protocol", method = RequestMethod.POST)
	public ResponseEntity<MultipleCustomersPreferredCommunicationModeResponse> getMultipleCustomersPreferredCommunicationModeProtocol(
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestHeader("authorization") String authToken,
            @RequestBody MultipleCustomersPreferredCommunicationModeRequest multipleCustomersPreferredCommunicationModeRequest,
            @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<MultipleCustomersPreferredCommunicationModeResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "getMultipleCustomersPreferredCommunicationModeProtocol");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In get multiple customers preferred communication mode protocol for department_uuid={}", departmentUUID);
		    response = preferredCommunicationModeImpl.getMultipleCustomersPreferredCommunicationModeProtocol(departmentUUID, multipleCustomersPreferredCommunicationModeRequest);
			if(response != null && response.getBody() != null){
				response.getBody().setRequestUUID(requestID);
			}
		} catch (Exception e) {
			LOGGER.error("Exception in get preferred communication mode for department_uuid={} customer_uuid={}", departmentUUID, e);
            MultipleCustomersPreferredCommunicationModeResponse errorResponse = new MultipleCustomersPreferredCommunicationModeResponse();
            ApiError error = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage());
            errorResponse.setRequestUUID(requestID);
            errorResponse.setErrors(Arrays.asList(error));
			return new ResponseEntity<MultipleCustomersPreferredCommunicationModeResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
    
    /**
     * predict preferred communication mode for customer
     * @param PredictPreferredCommunicationModeRequest predictPreferredCommunicationModeRequest
     * @return ResponseEntity<PredictPreferredCommunicationModeResponse>
    */
    @KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_PREFERRED_COMMUNICATION_MODE_WRITE, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "predict preferred communication mode for customer", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customer/{customerUUID}/preferredcommunicationmode/predict", method = RequestMethod.POST)
	public ResponseEntity<PredictPreferredCommunicationModeResponse> predictPreferredCommunicationMode(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("customerUUID") String customerUUID,
            @RequestHeader("authorization") String authToken,
            @RequestBody PredictPreferredCommunicationModeRequest predictPreferredCommunicationModeRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<PredictPreferredCommunicationModeResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "predictPreferredCommunicationMode");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
            loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
            loguuidJson.addProperty(APIConstants.MESSAGE_UUID, predictPreferredCommunicationModeRequest.getMessageUUID());
            
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In predict preferred communication mode for department_uuid={} message_uuid={}", departmentUUID, predictPreferredCommunicationModeRequest.getMessageUUID());
		    response = preferredCommunicationModeImpl.predictPreferredCommunicationMode(departmentUUID, customerUUID, predictPreferredCommunicationModeRequest);
		    if(response != null && response.getBody() != null){
				response.getBody().setRequestUUID(requestID);
			}
			
		} catch (Exception e) {
            PredictPreferredCommunicationModeResponse errorResponse = new PredictPreferredCommunicationModeResponse();
            ApiError error = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage());
            errorResponse.setErrors(Arrays.asList(error));
            errorResponse.setRequestUUID(requestID);
			LOGGER.error("Exception in predict preferred communication mode for department_uuid={} message_uuid={}", departmentUUID, predictPreferredCommunicationModeRequest.getMessageUUID(), e);
			return new ResponseEntity<PredictPreferredCommunicationModeResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}

}
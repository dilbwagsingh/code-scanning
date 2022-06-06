package com.mykaarma.kcommunications.controller;

import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.CustomerSentimentImpl;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.request.UpdateCustomerSentimentStatusRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessageSentimentPredictionRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.Response;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Configuration
@ComponentScan("com.mykaarma.kcommunications.services")
public class CustomerSentimentController {
    
    @Autowired
	CustomerSentimentImpl customerSentimentImpl;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(CustomerSentimentController.class);

	
	@KCommunicationsAuthorize(apiScope = ApiScope.UPDATE_CUSTOMER_SENTIMENT, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "recieves a request to perform sentiment analysis on incomung text messages which is routed to ksentiment-api", 
				  authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/message/{messageUUID}/sentiment/predict", method = RequestMethod.POST)
	public ResponseEntity<Response> sentimentPredictionForMessage(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("messageUUID") String messageUUID,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "sentimentPredictionForMessage");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.MESSAGE_UUID, messageUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info(String.format("In predict sentiment for a message for department_uuid=%s message_uuid=%s ",departmentUUID, messageUUID));
			
		    response = customerSentimentImpl.sentimentPredictionForMessage(departmentUUID, messageUUID);
			
		} catch (Exception e) {
			LOGGER.error("Exception in predict sentiment for a message",  e);
			List<ApiError> errors = new ArrayList<>();
			ApiError error = new ApiError();
			error.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.name());
			error.setErrorDescription(String.format("Internal Server Error %s while predicting Message sentiment ", e.getMessage()));
			errors.add(error);
			Response errorResponse = new Response();
			errorResponse.setErrors(errors);
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);

			
		} finally {
			MDC.clear();
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.UPDATE_CUSTOMER_SENTIMENT, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "recieves the sentiment prediction and marks customer upset for negative sentiment and saves the prediction", 
						   authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/message/{messageUUID}/sentiment/prediction", method = RequestMethod.POST)
	public ResponseEntity<Response> updateMessageSentimentPrediction(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("messageUUID") String messageUUID,
			@RequestHeader("authorization") String authToken,
			@RequestBody UpdateMessageSentimentPredictionRequest updateMessageSentimentPredictionRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updateMessageSentimentPrediction");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.MESSAGE_UUID, messageUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info(String.format("In update sentiment prediction for a message for department_uuid=%s message_uuid=%s ",departmentUUID, messageUUID));
			
		    response = customerSentimentImpl.updateMessageSentimentPrediction(departmentUUID, messageUUID, updateMessageSentimentPredictionRequest.getSentimentScore());
			
		} catch (Exception e) {
			LOGGER.error("Exception in update sentiment prediction for a message",  e);
			List<ApiError> errors = new ArrayList<>();
			ApiError error = new ApiError();
			error.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.name());
			error.setErrorDescription(String.format("Internal Server Error %s while updating Message sentiment prediction ", e.getMessage()));
			errors.add(error);
			Response errorResponse = new Response();
			errorResponse.setErrors(errors);
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);

			
		} finally {
			MDC.clear();
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.UPDATE_CUSTOMER_SENTIMENT, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update customer sentiment status for department", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customer/sentiment/status", method = RequestMethod.POST)
	public ResponseEntity<Response> updateCustomerSentimentStatus(
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestBody UpdateCustomerSentimentStatusRequest updateCustomerSentimentStatusRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updateCustomerSentimentStatus");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEPARTMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info(String.format("In update sentiment status for department_uuid=%s sentiment=%s",
					departmentUUID, updateCustomerSentimentStatusRequest.getCustomerSentiment()));
			
		    response = customerSentimentImpl.updateCustomerSentimentStatus(updateCustomerSentimentStatusRequest, departmentUUID);
			
		} catch (Exception e) {
			LOGGER.error(String.format("Exception in update sentiment status for department_uuid=%s sentiment=%s "
					, departmentUUID, updateCustomerSentimentStatusRequest.getCustomerSentiment()) ,e);
			List<ApiError> errors = new ArrayList<>();
			ApiError error = new ApiError();
			error.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.name());
			error.setErrorDescription(String.format("Internal Server Error %s while update sentiment status for department_uuid=%s sentiment=%s ", 
					e.getMessage(), departmentUUID, updateCustomerSentimentStatusRequest.getCustomerSentiment()));
			errors.add(error);
			Response errorResponse = new Response();
			errorResponse.setErrors(errors);
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);

			
		} finally {
			MDC.clear();
		}
		return response;
	}

}
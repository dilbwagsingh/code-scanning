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
import org.springframework.web.bind.annotation.PostMapping;
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
import com.mykaarma.kcommunications.controller.impl.CommunicationsWithoutCustomerApiImpl;
import com.mykaarma.kcommunications.controller.impl.ValidateRequest;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.common.RestURIConstants;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.request.SendEmailRequest;
import com.mykaarma.kcommunications_model.request.SendMessageWithoutCustomerRequest;
import com.mykaarma.kcommunications_model.request.SendNotificationWithoutCustomerRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.SendEmailResponse;
import com.mykaarma.kcommunications_model.response.SendMessageWithoutCustomerResponse;
import com.mykaarma.kcommunications_model.response.SendNotificationWithoutCustomerResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@Configuration
@ComponentScan("com.mykaarma.kcommunications.services")
public class CommunicationsWithoutCustomerApiController {


	@Autowired
	CommunicationsWithoutCustomerApiImpl communicationsWithoutCustomerApiImpl;
	
	@Autowired
	ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;
	
	@Autowired 
	private ValidateRequest validateRequest;
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_SEND_EMAIL_WITHOUT_CUSTOMER,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "send Email without customer reference", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/email", method = RequestMethod.POST)
	public ResponseEntity<SendEmailResponse> sendEmailWithoutCustomer(
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestHeader("authorization") String authToken,
			@RequestBody SendEmailRequest sendEmailRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<SendEmailResponse> response = null;
		
		Date messageTimestamp = new Date();
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "sendEmailWithoutCustomer");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			response = communicationsWithoutCustomerApiImpl.sendEmail(departmentUUID,sendEmailRequest);
			
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format("Response status %s sent. time_taken=%d response=%s", response.getStatusCode(), elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			log.error(String.format("Exception in sendMessageWithoutCustomer error: %s exception %s", e.getMessage(),e.getStackTrace()));
			SendEmailResponse sendEmailResponse = new SendEmailResponse();
			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name() ,String.format("Internal error %s while sending email without customer", e.getMessage()));
			List<ApiError> errors = sendEmailResponse.getErrors();
			errors.add(apiError);
			sendEmailResponse.setErrors(errors);
			return new ResponseEntity<SendEmailResponse>(sendEmailResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_SEND_NOTIFICATION_WITHOUT_CUSTOMER, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "send Notification without single customer context", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUUID}/notify", method = RequestMethod.POST)
	public ResponseEntity<SendNotificationWithoutCustomerResponse> notify(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("userUUID") String userUUID,
			@RequestBody SendNotificationWithoutCustomerRequest notificationRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<SendNotificationWithoutCustomerResponse> response = null;
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "sendNotificationWithoutCustomer");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.USER_UUID, userUUID);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In notifyWithoutCustomer request={} subscriber={}", new ObjectMapper().writeValueAsString(notificationRequest), subscriberName);
			response = communicationsWithoutCustomerApiImpl.sendNotification(departmentUUID, userUUID, notificationRequest);
			if (response != null && response.getBody() != null) {
				response.getBody().setRequestUUID(requestID);
			}
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format("Response status %s sent. time_taken=%d response=%s", response.getStatusCode(), elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			log.error("Exception in sendNotificationWithoutCustomer ", e);
			SendNotificationWithoutCustomerResponse sendMessageResponse = new SendNotificationWithoutCustomerResponse();
            sendMessageResponse.setRequestUUID(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            sendMessageResponse.setErrors(errors);
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(sendMessageResponse);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_SEND_MESSAGE_WITHOUT_CUSTOMER,apiScopeLevel = ApiScopeLevel.SERVICE_SUBSCRIBER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "send Message without customer and sender reference", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "/" + RestURIConstants.EXTERNAL + "/" + RestURIConstants.MESSAGE, method = RequestMethod.POST)
	public ResponseEntity<SendMessageWithoutCustomerResponse> sendMessageWithoutCustomer(
			@ApiIgnore @RequestHeader("authorization") String authToken,
			@RequestBody SendMessageWithoutCustomerRequest sendMessageRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		SendMessageWithoutCustomerResponse response = null;
		
		Date messageTimestamp = new Date();
		
		try {
			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "sendMessageWithoutCustomer");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			response = validateRequest.validateSendMessageWithoutCustomerRequest(sendMessageRequest);
			if(response.getErrors()!=null && !response.getErrors().isEmpty()) {
				response.setStatus(Status.FAILURE);
				response.setRequestUUID(requestID);
				return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
			}
			response = communicationsWithoutCustomerApiImpl.sendMessage(sendMessageRequest);
			response.setRequestUUID(requestID);
			
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format("Response status %s sent. time_taken=%d response=%s", response.getStatus(), elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			log.error(String.format("Exception in sendMessageWithoutCustomer error: %s exception %s", e.getMessage(),e.getStackTrace()));
			SendMessageWithoutCustomerResponse sendMessageResponse = new SendMessageWithoutCustomerResponse();
			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name() ,String.format("Internal error %s while sending message without customer", e.getMessage()));
			List<ApiError> errors = sendMessageResponse.getErrors();
			errors.add(apiError);
			sendMessageResponse.setErrors(errors);
			sendMessageResponse.setRequestUUID(requestID);
			return new ResponseEntity<>(sendMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
}
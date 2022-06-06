package com.mykaarma.kcommunications.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.ApiAuthenticatorAndAuthorizer;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.MessageImpl;
import com.mykaarma.kcommunications.model.api.ErrorCodes;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.dto.MessageDTO;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.FetchMessageTypes;
import com.mykaarma.kcommunications_model.request.FetchMessagesForCommunicationIdentifierListRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesForCustomerRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.CustomerMessagesFetchResponse;
import com.mykaarma.kcommunications_model.response.MessagesFetchResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

@RestController
@Configuration
@Slf4j
@ComponentScan("com.mykaarma.kcommunications.services")
@Api(tags = "Message Controller", description = "Endpoints for fetching messages")
public class MessageController {
	
	@Autowired
	ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;
	
	@Autowired
	MessageImpl messageImpl;
	
	private ObjectMapper objectMapper=new ObjectMapper();

	@KCommunicationsAuthorize(apiScope = ApiScope.FETCH_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "fetch messages for given ids/uuids", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUuid}/message/list", method = RequestMethod.POST)
	public ResponseEntity<MessagesFetchResponse> fetchMessages(
			@PathVariable("departmentUUID") String departmentUuid,
			@PathVariable("userUuid") String userUuid,
			@RequestBody FetchMessagesRequest fetchMessagesRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		MessagesFetchResponse messagesFetchResponse = new MessagesFetchResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "fetchMessages");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In fetchMessages request={} subscriber={}", objectMapper.writeValueAsString(fetchMessagesRequest), subscriberName);
			List<MessageDTO> messages = messageImpl.getMessages(fetchMessagesRequest, userUuid);
			messagesFetchResponse.setMessageDTOList(messages);
			messagesFetchResponse.setRequestUuid(requestID);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format(" time_taken=%d to fetch response", elapsedTime));
			return new ResponseEntity<MessagesFetchResponse>(messagesFetchResponse, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Exception in fetch messages for department_uuid={} user_uuid={} request={} ",departmentUuid,
					userUuid,objectMapper.writeValueAsString(fetchMessagesRequest), e);
			
			messagesFetchResponse.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            messagesFetchResponse.setErrors(errors);
			return new ResponseEntity<MessagesFetchResponse>(messagesFetchResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.FETCH_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "fetch message for given communication uids - "
			+ "unique identifier provided by third part APIs like Twilio for text, call and Postmark for emails  ", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUuid}/communicationidentifier/message/list", method = RequestMethod.POST)
	public ResponseEntity<MessagesFetchResponse> fetchMessageForCommunicationIdentifiersList(
			@PathVariable("departmentUUID") String departmentUuid,
			@PathVariable("userUuid") String userUuid,
			@RequestBody FetchMessagesForCommunicationIdentifierListRequest fetchMessagesForCommunicationIdentifierList,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		MessagesFetchResponse messagesFetchResponse = new MessagesFetchResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "fetchMessageForCommunicationIdentifiersList");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUuid);
			loguuidJson.addProperty(APIConstants.USER_UUID_TOKEN, departmentUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In fetchMessageForCommunicationIdentifiersList"
					+ " subscriber={} request={}", subscriberName, objectMapper.writeValueAsString(fetchMessagesForCommunicationIdentifierList));
			
			if(fetchMessagesForCommunicationIdentifierList==null 
					|| fetchMessagesForCommunicationIdentifierList.getCommunicationIdentifierList()==null
					|| fetchMessagesForCommunicationIdentifierList.getCommunicationIdentifierList().isEmpty()) {
				log.info("invalid request body received={} not fetching messages",objectMapper.writeValueAsString(fetchMessagesForCommunicationIdentifierList));
				messagesFetchResponse.setRequestUuid(requestID);
	            List<ApiError> errors = new ArrayList<ApiError>();
	            errors.add(new ApiError(ErrorCodes.INSUFFICIENT_DETAILS.name(), ErrorCodes.INSUFFICIENT_DETAILS.getErrorDescription()));
	            messagesFetchResponse.setErrors(errors);
				return new ResponseEntity<MessagesFetchResponse>(messagesFetchResponse, HttpStatus.BAD_REQUEST);
			}
			
			List<MessageDTO> messages = messageImpl.fetchMessageForCommunicationIdentifiersList(departmentUuid,userUuid, fetchMessagesForCommunicationIdentifierList);
			messagesFetchResponse.setMessageDTOList(messages);
			messagesFetchResponse.setRequestUuid(requestID);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format(" time_taken=%d to fetch response", elapsedTime));
			return new ResponseEntity<MessagesFetchResponse>(messagesFetchResponse, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Exception in fetch messages for department_uuid={} user_uuid={} request={} ",departmentUuid,
					userUuid,objectMapper.writeValueAsString(fetchMessagesForCommunicationIdentifierList), e);
			
			messagesFetchResponse.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            messagesFetchResponse.setErrors(errors);
			return new ResponseEntity<MessagesFetchResponse>(messagesFetchResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.FETCH_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "fetch messages,drafts for given customer", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUuid}/customer/{customerUuid}/message/list", method = RequestMethod.POST)
	public ResponseEntity<CustomerMessagesFetchResponse> fetchMessagesForCustomer(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("userUuid") String userUuid,
			@PathVariable("customerUuid") String customerUuid,
			@RequestBody FetchMessagesForCustomerRequest fetchMessagesForCustomerRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		CustomerMessagesFetchResponse customerMessagesFetchResponse = new CustomerMessagesFetchResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "fetchMessagesForCustomer");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.USER_UUID_TOKEN, userUuid);
			loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In fetchMessagesForCustomer request={} subscriber={}", 
					objectMapper.writeValueAsString(fetchMessagesForCustomerRequest), subscriberName);
			HashMap<FetchMessageTypes, List<MessageDTO>> messagesMap = messageImpl.getMessagesForCustomer(fetchMessagesForCustomerRequest, userUuid, customerUuid, departmentUUID);
			customerMessagesFetchResponse.setMessagesMap(messagesMap);
			customerMessagesFetchResponse.setRequestUuid(requestID);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format(" time_taken=%d to fetch response", elapsedTime));
			return new ResponseEntity<CustomerMessagesFetchResponse>(customerMessagesFetchResponse, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Exception in fetch messages for request={} ",customerUuid,departmentUUID,
					objectMapper.writeValueAsString(fetchMessagesForCustomerRequest), e);
			
			customerMessagesFetchResponse.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            customerMessagesFetchResponse.setErrors(errors);
			return new ResponseEntity<CustomerMessagesFetchResponse>(customerMessagesFetchResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
}

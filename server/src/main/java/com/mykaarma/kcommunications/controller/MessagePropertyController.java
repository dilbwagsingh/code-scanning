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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.CommunicationsApiImpl;
import com.mykaarma.kcommunications.controller.impl.MessagePropertyImpl;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.Event;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RestController
@Configuration
@ComponentScan("com.mykaarma.kcommunications.services")
public class MessagePropertyController {


	@Autowired
	MessagePropertyImpl messagePropertyImpl;

	private final static Logger LOGGER = LoggerFactory.getLogger(MessagePropertyController.class);

	@KCommunicationsAuthorize(apiScope = ApiScope.UPDATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update Message", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/event/{event}/message/{messageUUID}", method = RequestMethod.PUT)
	public ResponseEntity<Response> updateMessageAttributesForEvent(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("event") Event event,
			@PathVariable("messageUUID") String messageUUID,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {

		ResponseEntity<Response> response = null;

		Date messageTimestamp = new Date();

		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updateMessageAttributesForEvent");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.EVENT_NAME, event.name());
			loguuidJson.addProperty(APIConstants.MESSAGE_UUID, messageUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");

			LOGGER.info("In updateMessageForEvent");
			response = messagePropertyImpl.updateMessageForEvent(departmentUUID, messageUUID, event);

			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			LOGGER.info("Response_status={} sent. time_taken={} ", response.getStatusCode(), elapsedTime);
		} catch (Exception e) {
			LOGGER.error("Exception in updateMessageForEvent ", e);
			Response resp = new Response();
			return new ResponseEntity<Response>(resp, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}


}

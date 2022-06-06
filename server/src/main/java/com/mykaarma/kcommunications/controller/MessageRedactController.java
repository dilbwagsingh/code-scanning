package com.mykaarma.kcommunications.controller;

import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.MessageRedactImpl;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.response.MessageRedactResponse;

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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RestController
@Configuration
@ComponentScan("com.mykaarma.kcommunications.services")
public class MessageRedactController {

    @Autowired
    private MessageRedactImpl messageRedactImpl;

    private final static Logger LOGGER = LoggerFactory.getLogger(MessageRedactController.class);

    @KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_REDACT_MESSAGE, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "redact sensitive information from message", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/message/{messageUUID}/redact", method = RequestMethod.POST)
	public ResponseEntity<MessageRedactResponse> redactMessage(
        @PathVariable("departmentUUID") String departmentUUID,
        @PathVariable("messageUUID") String messageUUID,
        @RequestHeader("authorization") String authToken,
        @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<MessageRedactResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "redactMessage");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.MESSAGE_UUID, messageUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In redact message for department_uuid={} message_uuid={}", departmentUUID, messageUUID);
		    response = messageRedactImpl.redactMessage(messageUUID);
		    if(response!=null && response.getBody()!=null){
				response.getBody().setRequestUUID(requestID);
			}
			
		} catch (Exception e) {
			LOGGER.error("Exception in redact message for department_uuid={} message_uuid={}", departmentUUID, messageUUID, e);
			return new ResponseEntity<MessageRedactResponse>(new MessageRedactResponse(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
}
package com.mykaarma.kcommunications.controller;

import java.util.Collections;
import java.util.Date;

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
import com.mykaarma.kcommunications.controller.impl.ForwardedAndBotMessageImpl;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.common.RestURIConstants;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.request.ForwardMessageRequest;
import com.mykaarma.kcommunications_model.request.SaveBotMessageRequest;
import com.mykaarma.kcommunications_model.request.SendBotMessageRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.BotMessageResponse;
import com.mykaarma.kcommunications_model.response.ForwardMessageResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@Configuration
@Api(authorizations = {@Authorization(value = "basicAuth")})
@ComponentScan("com.mykaarma.kcommunications.services")
public class ForwardedAndBotMessageController {

    @Autowired
    private ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;

    @Autowired
    private ForwardedAndBotMessageImpl forwardedAndBotMessageImpl;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param departmentUuid unique id of department
     * @param userUuid unique id of department
     * @param messageUuid unique id of message
     * @param request request for forwarding bot message
     * @return response for forwarding bot message
     */
    @KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_FORWARDED_MESSAGE_WRITE, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
    @ResponseBody
    @ApiOperation(value = "forward an incoming message from customer", notes = "only supports text currently")
    @RequestMapping(value = RestURIConstants.DEPARTMENT + "/" + RestURIConstants.DEPARTMENT_PATH_VARIABLE + "/"
        + RestURIConstants.USER + "/" + RestURIConstants.USER_PATH_VARIABLE + "/" + RestURIConstants.MESSAGE + "/"
        + RestURIConstants.MESSAGE_PATH_VARIABLE + "/" + RestURIConstants.FORWARD, method = RequestMethod.POST)
    public ResponseEntity<ForwardMessageResponse> forwardMessage(
        @PathVariable(RestURIConstants.DEPARTMENT_UUID) String departmentUuid,
        @PathVariable(RestURIConstants.USER_UUID) String userUuid,
        @PathVariable(RestURIConstants.MESSAGE_UUID) String messageUuid,
        @ApiIgnore @RequestHeader(RestURIConstants.AUTHORIZATION) String authToken,
        @RequestBody ForwardMessageRequest request,
        @ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {

        ResponseEntity<ForwardMessageResponse> response = null;

        Date messageTimestamp = new Date();

        try {
            JsonObject loguuidJson = new JsonObject();
            loguuidJson.addProperty(APIConstants.METHOD, "forwardMessage");
            loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
            loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
            loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUuid);
            String logUUID = loguuidJson.toString();

            MDC.put(APIConstants.LogUUID, logUUID);
            MDC.put(APIConstants.FILTER_REQUEST, "true");
            response = forwardedAndBotMessageImpl.forwardMessage(departmentUuid,  userUuid, messageUuid, request);
            response.getBody().setRequestUuid(requestID);
            long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
            log.info("Response status {} sent. time_taken={} response={}", response.getStatusCode(), elapsedTime, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Exception in forwardMessage", e);
            ForwardMessageResponse errorResponse = new ForwardMessageResponse();
            errorResponse.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name() ,String.format("Internal error %s while forwarding message", e.getMessage()))
            ));
            errorResponse.setRequestUuid(requestID);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    /**
     * @param departmentUuid unique id of department
     * @param userUuid unique id of department
     * @param request request for sending bot message
     * @return response for sending bot message
     */
    @KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_BOT_MESSAGE_WRITE, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
    @ResponseBody
    @ApiOperation(value = "send a bot message to user", notes = "only supports text currently")
    @RequestMapping(value = RestURIConstants.DEPARTMENT + "/" + RestURIConstants.DEPARTMENT_PATH_VARIABLE + "/"
        + RestURIConstants.USER + "/" + RestURIConstants.USER_PATH_VARIABLE + "/" + RestURIConstants.BOT_MESSAGE + "/"
        + RestURIConstants.SEND, method = RequestMethod.POST)
    public ResponseEntity<BotMessageResponse> sendBotMessage(
        @PathVariable(RestURIConstants.DEPARTMENT_UUID) String departmentUuid,
        @PathVariable(RestURIConstants.USER_UUID) String userUuid,
        @ApiIgnore @RequestHeader(RestURIConstants.AUTHORIZATION) String authToken,
        @RequestBody SendBotMessageRequest request,
        @ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) {

        ResponseEntity<BotMessageResponse> response = null;

        Date messageTimestamp = new Date();

        try {
            JsonObject loguuidJson = new JsonObject();
            loguuidJson.addProperty(APIConstants.METHOD, "sendBotMessage");
            loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
            loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
            loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUuid);
            String logUUID = loguuidJson.toString();

            MDC.put(APIConstants.LogUUID, logUUID);
            MDC.put(APIConstants.FILTER_REQUEST, "true");
            response = forwardedAndBotMessageImpl.sendBotMessage(departmentUuid,  userUuid, request);
            response.getBody().setRequestUuid(requestID);
            long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
            log.info("Response status {} sent. time_taken={} response={}", response.getStatusCode(), elapsedTime, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Exception in sendBotMessage", e);
            BotMessageResponse errorResponse = new BotMessageResponse();
            errorResponse.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name() ,String.format("Internal error %s while sending bot message", e.getMessage()))
            ));
            errorResponse.setRequestUuid(requestID);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    /**
     * @param request request for saving bot message
     * @return response for saving bot message
     */
    @KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_BOT_MESSAGE_WRITE, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
    @ResponseBody
    @ApiOperation(value = "save a bot message")
    @RequestMapping(value = RestURIConstants.DEPARTMENT + "/" + RestURIConstants.DEPARTMENT_PATH_VARIABLE + "/"
        + RestURIConstants.USER + "/" + RestURIConstants.USER_PATH_VARIABLE + "/" + RestURIConstants.BOT_MESSAGE, method = RequestMethod.PUT)
    public ResponseEntity<BotMessageResponse> saveBotMessage(
        @PathVariable(RestURIConstants.DEPARTMENT_UUID) String departmentUuid,
        @PathVariable(RestURIConstants.USER_UUID) String userUuid,
        @ApiIgnore @RequestHeader(RestURIConstants.AUTHORIZATION) String authToken,
        @RequestBody SaveBotMessageRequest request,
        @ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {

        ResponseEntity<BotMessageResponse> response = null;

        Date messageTimestamp = new Date();

        try {
            JsonObject loguuidJson = new JsonObject();
            loguuidJson.addProperty(APIConstants.METHOD, "saveBotMessage");
            loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
            loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
            String logUUID = loguuidJson.toString();

            MDC.put(APIConstants.LogUUID, logUUID);
            MDC.put(APIConstants.FILTER_REQUEST, "true");
            response = forwardedAndBotMessageImpl.saveBotMessage(departmentUuid, userUuid, request);
            response.getBody().setRequestUuid(requestID);
            long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
            log.info("Response status {} sent. time_taken={} response={}", response.getStatusCode(), elapsedTime, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Exception in saveBotMessage", e);
            BotMessageResponse errorResponse = new BotMessageResponse();
            errorResponse.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name() ,String.format("Internal error %s while saving bot message", e.getMessage()))
            ));
            errorResponse.setRequestUuid(requestID);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

}

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
import com.mykaarma.kcommunications.controller.impl.TranslateServiceImpl;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.TemplateType;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.request.TranslateTextRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.TranslateLanguagesResponse;
import com.mykaarma.kcommunications_model.response.TranslateTextResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

@RestController
@Configuration
@Slf4j
@ComponentScan("com.mykaarma.kcommunications.services")
@Api(tags = "Translate Controller", description = "Endpoints for getting language options available and fetching translations for given text")
public class TranslateController {
	
	@Autowired
	private ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;
	
	@Autowired
	private TranslateServiceImpl translateServiceImpl;
	
	private ObjectMapper objectMapper=new ObjectMapper();
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_TRANSLATE,apiScopeLevel = ApiScopeLevel.SERVICE_SUBSCRIBER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "get languages available for translation support", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "translate/languages", method = RequestMethod.GET)
	public ResponseEntity<TranslateLanguagesResponse> getLanguages(
			@ApiParam(value = "Basic Authentication Header")
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "getLanguages");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In getLanguages subscriber={}", subscriberName);
			
			ResponseEntity<TranslateLanguagesResponse> response=translateServiceImpl.getSupportedLanguages();
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
		
			log.info(String.format("Request successful. time_taken=%d response=%s", elapsedTime,new ObjectMapper().writeValueAsString(response)));
			
			return response;
		} catch (Exception e) {
			log.error("Exception in fetching languages available for translation ", e);
            List<ApiError> errors = new ArrayList<ApiError>();
            TranslateLanguagesResponse translateLanguagesResponse=new TranslateLanguagesResponse();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            translateLanguagesResponse.setErrors(errors);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(translateLanguagesResponse);
			
		}
		
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_TRANSLATE,apiScopeLevel = ApiScopeLevel.SERVICE_SUBSCRIBER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "translate given text in requested language", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "translate/text", method = RequestMethod.POST)
	public ResponseEntity<TranslateTextResponse> translateText(
			@ApiParam(value = "Basic Authentication Header")
			@RequestHeader("authorization") String authToken,
			@RequestBody TranslateTextRequest translateTextRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "translateText");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In translateText request={} subscriber={}",objectMapper.writeValueAsString(translateTextRequest) , subscriberName);
			
			ResponseEntity<TranslateTextResponse> response=translateServiceImpl.translateText(translateTextRequest);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
		
			log.info(String.format("Request successful. time_taken=%d response=%s", elapsedTime,new ObjectMapper().writeValueAsString(response)));
			
			return response;
		} catch (Exception e) {
			log.error("Exception in translating text for request={} ",objectMapper.writeValueAsString(translateTextRequest), e);
            List<ApiError> errors = new ArrayList<ApiError>();
            TranslateTextResponse translateTextResponse=new TranslateTextResponse();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            translateTextResponse.setErrors(errors);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(translateTextResponse);
			
		}
		
	}
}

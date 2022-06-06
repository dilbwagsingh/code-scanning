package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.mykaarma.kcommunications.redis.RateControllerRedisService;
import com.mykaarma.kcommunications_model.enums.CommunicationsFeature;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.Response;

@Service
public class RateControllerImpl {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(RateControllerImpl.class);	
	
	@Value("${rate-limit-outgoing-text:20}")
    private int outgoingTextLimit;
	
	@Value("${rate-limit-outgoing-email:20}")
    private int outgoingEmailLimit;
	
	@Value("${rate-limit-incoming-call:6}")
    private int incomingCallLimit;

	@Value("${rate-limit-text-forwarding:20}")
    private int textForwardingLimit;
	
	@Value("${rate-limit-auto-email-responder:5}")
    private int autoEmailResponder;
	
	@Autowired
	private RateControllerRedisService rateControllerRedisService;

	public Boolean rateLimitReached(String departmentUUID, CommunicationsFeature feature, String communicationValue) {
		try {
			ResponseEntity<Response> response = rateController(departmentUUID, feature, communicationValue);
			if(response!=null && response.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
				LOGGER.warn("rateLimitReached for department_uuid={} feature={} communication_value={} ", departmentUUID, feature.name(), communicationValue);
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Error in rateLimitReached for department_uuid={} feature={} communication_value={} ", departmentUUID, feature.name(), communicationValue);
		}
		return false;
	}
	
	public ResponseEntity<Response> rateController(String departmentUUID, CommunicationsFeature feature, String communicationValue) {
		Response response = new Response();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		if(feature==null) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_FEATURE.name(), String.format("feature=%s is invalid ", feature));
			errors.add(apiError);
		}
		if(communicationValue==null || communicationValue.isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_COMMUNICATION_VALUE.name(), String.format("communication_value=%s is invalid ", communicationValue));
			errors.add(apiError);
		}
		if(errors!=null && !errors.isEmpty()) {
			response.setErrors(errors);
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);  
		}
		Integer limit = null;
		TimeUnit timeUnit = TimeUnit.MINUTES;
		switch (feature) {
			case OUTGOING_TEXT: 
				limit = outgoingTextLimit;
				timeUnit = TimeUnit.MINUTES;
				break;
			case OUTGOING_EMAIL:
				limit = outgoingEmailLimit;
				timeUnit = TimeUnit.MINUTES;
				break;
			case INCOMING_CALL:
				limit = incomingCallLimit;
				timeUnit = TimeUnit.MINUTES;
				break;
			case INTERNAL_TEXT_FORWARDING:
				limit = textForwardingLimit;
				timeUnit = TimeUnit.MINUTES;
				break;
			case TEXT_TO_TEXT_FORWARDING:
				limit = textForwardingLimit;
				timeUnit = TimeUnit.MINUTES;
				break;
			case AUTO_RESPONDER_EMAIL:
				limit = autoEmailResponder;
				timeUnit = TimeUnit.DAYS;
				break;
			default:
				limit = 1000;
				LOGGER.warn("no limit present for the feature, setting to 1000 for feature={}", feature.name());
				break;
			
		}
		if(TimeUnit.DAYS.equals(timeUnit)) {
			Integer usage = rateControllerRedisService.getUsageInADay(departmentUUID, feature, communicationValue);
			LOGGER.info("department_uuid={} usage={} feature={} communication_value={} limit={} ", departmentUUID, usage, feature, communicationValue, limit);
			if(usage.compareTo(limit)>0) {
				ApiError apiError = new ApiError(ErrorCode.RATE_LIMIT_REACHED.name(), String.format("rate limit reached for feature=%s limit=%s is invalid ", 
						feature, limit));
				errors.add(apiError);
				response.setErrors(errors);
				return new ResponseEntity<Response>(response, HttpStatus.TOO_MANY_REQUESTS); 
			}
			rateControllerRedisService.updateUsageInADay(departmentUUID, feature, communicationValue);
		} else {
			Integer usage = rateControllerRedisService.getUsageInMinutes(departmentUUID, feature, communicationValue);
			LOGGER.info("department_uuid={} usage={} feature={} communication_value={} limit={} ", departmentUUID, usage, feature, communicationValue, limit);
			if(usage.compareTo(limit)>0) {
				ApiError apiError = new ApiError(ErrorCode.RATE_LIMIT_REACHED.name(), String.format("rate limit reached for feature=%s limit=%s is invalid ", 
						feature, limit));
				errors.add(apiError);
				response.setErrors(errors);
				return new ResponseEntity<Response>(response, HttpStatus.TOO_MANY_REQUESTS); 
			}
			rateControllerRedisService.updateUsageInMinutes(departmentUUID, feature, communicationValue);
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK); 
	}
	
	
	
}

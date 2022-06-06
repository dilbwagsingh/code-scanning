package com.mykaarma.kcommunications.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.KCommunicationsException;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.Response;

@ControllerAdvice
public class CommunicationsApiControllerAdvice {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(CommunicationsApiControllerAdvice.class);
	
	@ExceptionHandler(KCommunicationsException.class) 
	public ResponseEntity <Response> KCommunicationsException(HttpServletRequest httpServletRequest,final KCommunicationsException e) {
        return handleKorderException(httpServletRequest,e);
    }
	
	private ResponseEntity<Response> handleKorderException(HttpServletRequest httpServletRequest,KCommunicationsException e) {
		// TODO Auto-generated method stub
		
		
		switch(e.getCustomError()) {
			case WRONG_CREDENTIAL:
				
				return returnErrorResponse(ErrorCode.WRONG_CREDENTIAL.name(),"Credentials are wrong, Authorization refused "
						+ "for provided credentials. "
						+ "Please contact Communications API support."
						+ " Reference MessageID: "
						+ httpServletRequest.getAttribute(APIConstants.REQUEST_ID),HttpStatus.UNAUTHORIZED);
				
			case CREDENTIALS_ARE_NULL:
				 
				return returnErrorResponse(ErrorCode.CREDENTIALS_ARE_NULL.name(),"Credentials are wrong, Authorization refused "
						+ "for provided credentials. "
						+ "Please contact Communications API support."
						+ " Reference MessageID: "
						+ httpServletRequest.getAttribute(APIConstants.REQUEST_ID),HttpStatus.UNAUTHORIZED);
			
			case INVALID_SERVICE_SUBSCRIBER: 
				
				return returnErrorResponse(ErrorCode.INVALID_SERVICE_SUBSCRIBER.name(),"Credentials are wrong, Authorization refused "
						+ "for provided credentials. "
						+ "Please contact Communications API support."
						+ " Reference MessageID: "
						+ httpServletRequest.getAttribute(APIConstants.REQUEST_ID),HttpStatus.UNAUTHORIZED);
				
			case NOT_AUTHORIZED_REQUEST:
				
				return returnErrorResponse(ErrorCode.NOT_AUTHORIZED_REQUEST.name(),"Credentials are wrong, Authorization refused "
						+ "for provided credentials. "
						+ "Please contact Communications API support."
						+ " Reference MessageID: "
						+ httpServletRequest.getAttribute(APIConstants.REQUEST_ID),HttpStatus.UNAUTHORIZED);
				
			
			case MISSING_MESSAGE_BODY: 
				
				return returnErrorResponse(ErrorCode.NOT_AUTHORIZED_REQUEST.name(),String.format("Dealer department %s is invalid. "
						+ "Reference MessageID: %s ", 
						httpServletRequest.getAttribute(APIConstants.DEALER_DEPARMENT_TOKEN)),HttpStatus.BAD_REQUEST);
				
			case MISSING_MESSAGE_TYPE:
				
				return returnErrorResponse(ErrorCode.MISSING_MESSAGE_TYPE.name(), e.getData(), HttpStatus.BAD_REQUEST);
				
			case MISSING_MESSAGE_PROTOCOL:
				
				return returnErrorResponse(ErrorCode.MISSING_MESSAGE_PROTOCOL.name(), e.getData(), HttpStatus.BAD_REQUEST);
			
			case MISSING_MESSAGE_ATTRIBUTES:
				
				return returnErrorResponse(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), e.getData(), HttpStatus.INTERNAL_SERVER_ERROR);
					
			case MISSING_IS_MANUAL:
				
				return returnErrorResponse(ErrorCode.MISSING_IS_MANUAL.name(), e.getData(), HttpStatus.BAD_REQUEST);
	
			case MISSING_DRAFT_ATTRIBUTES:
				
				return returnErrorResponse(ErrorCode.MISSING_DRAFT_ATTRIBUTES.name(), e.getData(), HttpStatus.BAD_REQUEST);
				
			case INVALID_DRAFT_DATE:
				
				return returnErrorResponse(ErrorCode.INVALID_DRAFT_DATE.name(), e.getData(), HttpStatus.BAD_REQUEST);
				
			case INVALID_CUSTOMER: 
				return returnErrorResponse(ErrorCode.INVALID_CUSTOMER.name(), e.getData(), HttpStatus.BAD_REQUEST);
				
			case INVALID_USER:
				return returnErrorResponse(ErrorCode.INVALID_USER.name(), e.getData(), HttpStatus.BAD_REQUEST);
			
			case INVALID_COMMUNICATION_VALUE: 
				
				return returnErrorResponse(ErrorCode.INVALID_COMMUNICATION_VALUE.name(),e.getData(), HttpStatus.BAD_REQUEST);
				
			case OPTED_OUT_COMMUNICATION_VALUE:
				
				return returnErrorResponse(ErrorCode.OPTED_OUT_COMMUNICATION_VALUE.name(), e.getData(), HttpStatus.BAD_REQUEST);
				
			case DEALERSHIP_HOLIDAY: 
				return returnErrorResponse(ErrorCode.DEALERSHIP_HOLIDAY.name(), e.getData(), HttpStatus.BAD_REQUEST);
				
			case MISMATCH_DRAFT_STATUS_FAILURE_REASON:
				return returnErrorResponse(ErrorCode.MISMATCH_DRAFT_STATUS_FAILURE_REASON.name(), e.getData(), HttpStatus.BAD_REQUEST);
			
			case USER_DOES_NOT_HAVE_AUTHORITY: 
				
				return returnErrorResponse(ErrorCode.USER_DOES_NOT_HAVE_AUTHORITY.name(),e.getData(), HttpStatus.BAD_REQUEST);
				
			case CUSTOMER_LOCKED:
				
				return returnErrorResponse(ErrorCode.CUSTOMER_LOCKED.name(), e.getData(), HttpStatus.BAD_REQUEST);
				
			case TWILIO_SENDING_FAILURE: 
				return returnErrorResponse(ErrorCode.TWILIO_SENDING_FAILURE.name(), e.getData(), HttpStatus.BAD_REQUEST);
				
			case MISSING_VOICE_CREDENTIALS:
				return returnErrorResponse(ErrorCode.MISSING_VOICE_CREDENTIALS.name(), e.getData(), HttpStatus.BAD_REQUEST);
			
			default:
				
				return returnErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.name(), "Something went wrong please "
						+ "contact mykaarma supportPlease contact Communications API Support. "
						+ "Reference MessagedID: "+ httpServletRequest.getAttribute(APIConstants.REQUEST_ID), 
						HttpStatus.INTERNAL_SERVER_ERROR);
		}	
	}
	
	private ResponseEntity<Response> returnErrorResponse(String errorCode, String errorDescription,HttpStatus status){
		
		ApiError error = new ApiError();
		List<ApiError> errors = new ArrayList<>();
		Response errorResponse = new Response();
		error.setErrorCode(errorCode);
		error.setErrorDescription(errorDescription);
		errors.add(error);
		errorResponse.setErrors(errors);
		return new ResponseEntity<Response>(errorResponse, status);	
	}
}




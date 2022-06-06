package com.mykaarma.kcommunications.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.ApiAuthenticatorAndAuthorizer;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.CommunicationsApiImpl;
import com.mykaarma.kcommunications.controller.impl.SubscriptionsApiImpl;
import com.mykaarma.kcommunications.controller.impl.UniversalMessagingService;
import com.mykaarma.kcommunications.model.api.DelayedFilterRemovalRequest;
import com.mykaarma.kcommunications.model.api.FailedMessagesRequest;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.AWSClientUtil;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications_model.common.RestURIConstants;
import com.mykaarma.kcommunications_model.dto.DocFileDTO;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.request.AutoCsiLogEventRequest;
import com.mykaarma.kcommunications_model.request.CommunicationCountRequest;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryRequest;
import com.mykaarma.kcommunications_model.request.CommunicationsBillingRequest;
import com.mykaarma.kcommunications_model.request.DeleteAttachmentFromS3Request;
import com.mykaarma.kcommunications_model.request.DeleteSubscriptionsRequest;
import com.mykaarma.kcommunications_model.request.MultipleMessageRequest;
import com.mykaarma.kcommunications_model.request.RecordingRequest;
import com.mykaarma.kcommunications_model.request.RecordingUpdateRequest;
import com.mykaarma.kcommunications_model.request.RecordingVerifyRequest;
import com.mykaarma.kcommunications_model.request.SaveMessageRequestList;
import com.mykaarma.kcommunications_model.request.SendDraftRequest;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcommunications_model.request.SubscriptionSaveRequest;
import com.mykaarma.kcommunications_model.request.SubscriptionUpdateRequest;
import com.mykaarma.kcommunications_model.request.ThreadFollowRequest;
import com.mykaarma.kcommunications_model.request.TwilioDealerIDRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionFeedbackRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionFeedbackRequestNew;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionRequest;
import com.mykaarma.kcommunications_model.request.UploadAttachmentsToS3Request;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.CommunicationCountResponse;
import com.mykaarma.kcommunications_model.response.CommunicationHistoryResponse;
import com.mykaarma.kcommunications_model.response.DeleteAttachmentFromS3Response;
import com.mykaarma.kcommunications_model.response.GetDepartmentsUsingKaarmaTwilioURLResponse;
import com.mykaarma.kcommunications_model.response.MediaPreviewURLFetchResponse;
import com.mykaarma.kcommunications_model.response.FailedMessageResponse;
import com.mykaarma.kcommunications_model.response.NotifierDeleteResponse;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcommunications_model.response.SaveMessageListResponse;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;
import com.mykaarma.kcommunications_model.response.SendMultipleMessageResponse;
import com.mykaarma.kcommunications_model.response.SubscriptionSaveResponse;
import com.mykaarma.kcommunications_model.response.SubscriptionsUpdateResponse;
import com.mykaarma.kcommunications_model.response.ThreadFollowResponse;
import com.mykaarma.kcommunications_model.response.ThreadFollowers;
import com.mykaarma.kcommunications_model.response.UpdateMessagePredictionFeedbackResponse;
import com.mykaarma.kcommunications_model.response.UpdateVoiceUrlResponse;
import com.mykaarma.kcommunications_model.response.UploadAttachmentResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@Configuration
@ComponentScan("com.mykaarma.kcommunications.services")
@Api(tags = "Communications Controller", description = "Endpoints for all things communications")
public class CommunicationsApiController {


	@Autowired
	CommunicationsApiImpl communicationsApiImpl;
	
	@Autowired
	AWSClientUtil awsClientUtil;
	
	@Autowired
	ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;
	
	@Autowired
	private KCommunicationsUtils kCommunicationsUtils;
	
	@Autowired
	private SubscriptionsApiImpl subscriptionsApiImpl;

	@Autowired
	private UniversalMessagingService universalMessagingService;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(CommunicationsApiController.class);

	private final static ObjectMapper mapper = new ObjectMapper();

	/**
	 * this endpoint can be used to communicate between any two mykaarma entities, i.e., recipient as well as requester can be either USER or Customer
	 * @param sendMessageRequest 
	 * @return ResponseEntity<SendMessageResponse>
	 */
	@KCommunicationsAuthorize(apiScope = ApiScope.CREATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "create/send Message/Notification", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = RestURIConstants.DEPARTMENT+"/"+RestURIConstants.DEPARTMENT_PATH_VARIABLE+"/"+RestURIConstants.CUSTOMER+"/"+RestURIConstants.CUSTOMER_PATH_VARIABLE+"/" + RestURIConstants.MESSAGE, method = RequestMethod.POST)
	public ResponseEntity<SendMessageResponse> createUniversalMessage(
		@PathVariable("departmentUUID") String departmentUUID,
		@PathVariable("customerUUID") String customerUUID,
		@RequestBody SendMessageRequest sendMessageRequest,
		@ApiIgnore @RequestHeader("authorization") String authToken,
		@ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) {

		ResponseEntity<SendMessageResponse> response = null;

		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject logUuidJson = new JsonObject();
			logUuidJson.addProperty(APIConstants.METHOD, "createUniversalMessage");
			logUuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			logUuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
			logUuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			logUuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			logUuidJson.addProperty(APIConstants.REQUEST_ID, requestID);

			String logUUID = logUuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");

			LOGGER.info("In createUniversalMessage request={}", mapper.writeValueAsString(sendMessageRequest));
			response = universalMessagingService.createMessage(customerUUID, departmentUUID, sendMessageRequest, subscriberName);

			return response;

		} catch (Exception e) {
			LOGGER.error("Exception in createMessageNew ", e);
			List<ApiError> errors = new ArrayList<>();
			errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));

			SendMessageResponse sendMessageResponse = new SendMessageResponse();
			sendMessageResponse.setErrors(errors);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(sendMessageResponse);

		}

	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.LOG_AUTO_CSI,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL) 
	@ResponseBody
	@ApiOperation(value="logging auto-csi failure messages",authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value="department/{departmentUUID}/dealerOrderUUID/{dealerOrderUUID}/autocsi/log", method = RequestMethod.POST)
	public ResponseEntity<Response> logAutoCsiStatus(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("dealerOrderUUID") String dealerOrdeUUID,
			@RequestBody AutoCsiLogEventRequest logAutoCsiRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "logAutoCsi");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();
			
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			LOGGER.info("In logAutoCsi_request={}", new ObjectMapper().writeValueAsString(logAutoCsiRequest));

			response = communicationsApiImpl.logAutoCsiStatus(departmentUUID,dealerOrdeUUID,logAutoCsiRequest) ; 
			
		}
		catch(Exception e) {
			LOGGER.error("Exception in logAutoCsi ", e);
			Response response2 = new Response();
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            response2.setErrors(errors);
			return new ResponseEntity<Response>(response2, HttpStatus.INTERNAL_SERVER_ERROR);

		}
		
		finally {
			MDC.clear();
		}
		
		return response; 
	}
	
	
	@KCommunicationsAuthorize(apiScope = ApiScope.CREATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "create/send Message/Notification", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUUID}/customer/{customerUUID}/message", method = RequestMethod.POST)
	public ResponseEntity<SendMessageResponse> createMessage(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("userUUID") String userUUID,
			@PathVariable("customerUUID") String customerUUID,
			@RequestBody SendMessageRequest sendMessageRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<SendMessageResponse> response = null;
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "createMessage");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.USER_UUID, userUUID);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			LOGGER.info("In createMessage request={} subscriber={}", new ObjectMapper().writeValueAsString(sendMessageRequest), subscriberName);
			response = communicationsApiImpl.createMessage(customerUUID, departmentUUID, userUUID, sendMessageRequest, subscriberName);
			if(response!=null && response.getBody()!=null){
				response.getBody().setRequestUUID(requestID);
			}
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			LOGGER.info(String.format("Response status %s sent. time_taken=%d response=%s", response.getStatusCode(), elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			LOGGER.error("Exception in createMessage ", e);
			SendMessageResponse sendMessageResponse = new SendMessageResponse();
            sendMessageResponse.setRequestUUID(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            sendMessageResponse.setErrors(errors);
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(sendMessageResponse);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.CREATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "send Message", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/message/{messageUUID}", method = RequestMethod.POST)
	public ResponseEntity<SendMessageResponse> sendMessage(
            @RequestBody SendDraftRequest sendDraftRequest,
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("messageUUID") String messageUUID,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<SendMessageResponse> response = null;
		
		Date messageTimestamp = new Date();
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "sendMessage");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.MESSAGE_UUID, messageUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In sendMessage message_uuid={} request={}", messageUUID, new ObjectMapper().writeValueAsString(sendDraftRequest));
			response = communicationsApiImpl.sendMessage(departmentUUID, messageUUID, sendDraftRequest, false);
			if(response!=null && response.getBody()!=null){
				response.getBody().setRequestUUID(requestID);
			}
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			LOGGER.info(String.format("Response status %s sent. time_taken=%d response=%s", response.getStatusCode(), elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			LOGGER.error("Exception in sendMessage ", e);
			SendMessageResponse sendMessageResponse = new SendMessageResponse();
            sendMessageResponse.setRequestUUID(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            sendMessageResponse.setErrors(errors);
			return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
    @KCommunicationsAuthorize(apiScope = ApiScope.CREATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "edit draft message", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUUID}/customer/{customerUUID}/message/{messageUUID}", method = RequestMethod.POST)
	public ResponseEntity<SendMessageResponse> editDraft(
            @PathVariable("departmentUUID") String departmentUUID,
            @PathVariable("userUUID") String userUUID,
            @PathVariable("customerUUID") String customerUUID,
            @PathVariable("messageUUID") String messageUUID,
            @RequestBody SendMessageRequest sendMessageRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<SendMessageResponse> response = null;
		
		Date messageTimestamp = new Date();
		ObjectMapper om = new ObjectMapper();
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "sendMessage");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
            loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
            loguuidJson.addProperty(APIConstants.USER_UUID, userUUID);
            loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
			loguuidJson.addProperty(APIConstants.MESSAGE_UUID, messageUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In editDraft request={} department_uuid={} user_uuid={} customer_uuid={} message_uuid={}", om.writeValueAsString(sendMessageRequest), departmentUUID, userUUID, customerUUID, messageUUID);
			response = communicationsApiImpl.editDraft(departmentUUID, userUUID, customerUUID, messageUUID, sendMessageRequest);
			if(response!=null && response.getBody()!=null){
				response.getBody().setRequestUUID(requestID);
			}
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			LOGGER.info(String.format("Response status %s sent. time_taken=%d response=%s", response.getStatusCode(), elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			LOGGER.error("Exception in sendMessage ", e);
            SendMessageResponse sendMessageResponse = new SendMessageResponse();
            sendMessageResponse.setRequestUUID(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            sendMessageResponse.setErrors(errors);
			return new ResponseEntity<SendMessageResponse>(sendMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
    }
    
	@KCommunicationsAuthorize(apiScope = ApiScope.DELETE_NOTIFIER_ENTRIES,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "delete all notifier entries for a dealerAssociate", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUUID}/notifier", method = RequestMethod.DELETE)
	public ResponseEntity<NotifierDeleteResponse> deleteNotifierEntries(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("userUUID") String userUUID,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<NotifierDeleteResponse> response = null;
		
		Date messageTimestamp = new Date();
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "deleteNotifierEntries");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In deleteNotifierEntries");
			response = communicationsApiImpl.deleteNotifierEntries(userUUID, departmentUUID);
			
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			LOGGER.info(String.format("Response status=%s sent. time_taken=%d ", response.getStatusCode(), elapsedTime));
		} catch (Exception e) {
			LOGGER.error("Exception in deleting notification entries ", e);
			NotifierDeleteResponse notifierDeleteResponse = null;
			return new ResponseEntity<NotifierDeleteResponse>(notifierDeleteResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@ResponseBody
	@RequestMapping(value = "twilio/VoiceUrl", method = RequestMethod.POST)
	public ResponseEntity<UpdateVoiceUrlResponse> updateVoiceURL(
			@RequestBody TwilioDealerIDRequest twilioDealerIDRequest) throws Exception {
		
		ResponseEntity<UpdateVoiceUrlResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "twilioDealerIDRequest");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In updating voice urls for dealer");
			response = communicationsApiImpl.updateVoiceUrlForDealers(twilioDealerIDRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in updating voice urls for dealer ", e);
			UpdateVoiceUrlResponse updateVoiceUrlResponse = null;
			return new ResponseEntity<UpdateVoiceUrlResponse>(updateVoiceUrlResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}

	@ResponseBody
	@RequestMapping(value = "twilio/getDepartmentsUsingKaarmaTwilioURL", method = RequestMethod.GET)
	public ResponseEntity<GetDepartmentsUsingKaarmaTwilioURLResponse> getDepartmentsUsingKaarmaTwilioURL(
		@RequestParam Boolean getOnlyValidDealers,
		@RequestParam Long minDealerID,
		@RequestParam Long maxDealerID) throws Exception {

		ResponseEntity<GetDepartmentsUsingKaarmaTwilioURLResponse> response = null;

		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "getDepartmentsUsingKaarmaTwilioURL");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");

			LOGGER.info("In getDepartmentsUsingKaarmaTwilioURL for check_only_valid_dealer={} min_dealer_id={} max_dealer_id={}", getOnlyValidDealers, minDealerID, maxDealerID);
			response = communicationsApiImpl.getDepartmentsUsingKaarmaTwilioURL(getOnlyValidDealers, minDealerID, maxDealerID);

		} catch (Exception e) {
			LOGGER.error("Exception in getDepartmentsUsingKaarmaTwilioURL", e);
			GetDepartmentsUsingKaarmaTwilioURLResponse errorResponse = new GetDepartmentsUsingKaarmaTwilioURLResponse();
			errorResponse.setErrors(Arrays.asList(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage())));
			return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.CREATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "create multiple messages", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUUID}/multiple/message", method = RequestMethod.POST)
	public ResponseEntity<SendMultipleMessageResponse> createMultipleMessages(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("userUUID") String userUUID,
			@RequestBody MultipleMessageRequest multipleMessageRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {		
		
		try {
			ResponseEntity<SendMultipleMessageResponse> response = null;
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "createMultipleMessages");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.USER_UUID, userUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In createMultipleMessages for request={} department_uuid={} user_uuid={}", new ObjectMapper().writeValueAsString(multipleMessageRequest), departmentUUID, userUUID);
			
			response = communicationsApiImpl.sendMultipleMessages(multipleMessageRequest, departmentUUID, userUUID);
			if(response != null && response.getBody() != null) {
				response.getBody().setRequestUUID(requestID);
			}
			return response;
		} catch (Exception e) {
			LOGGER.error("Exception in createMultipleMessages ", e);
            SendMultipleMessageResponse sendMultipleMessageResponse = new SendMultipleMessageResponse();
            sendMultipleMessageResponse.setRequestUUID(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            sendMultipleMessageResponse.setErrors(errors);
			return new ResponseEntity<SendMultipleMessageResponse>(sendMultipleMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@ResponseBody
	@ApiOperation(value = "save multiple messages data")
	@RequestMapping(value = "department/{departmentUUID}/request/{requestUUID}/message/response", method = RequestMethod.POST)
	public ResponseEntity<Response> saveMessageResponse(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("requestUUID") String requestUUID,
			@RequestBody SendMessageResponse sendMessageResponse,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		Response response = null;
		LOGGER.info("In saveMessageResponse1");

		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "saveMessageResponse");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestUUID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In saveMessageResponse");
			communicationsApiImpl.saveMessageResponse(requestUUID, departmentUUID, sendMessageResponse);
		} catch (Exception e) {
			LOGGER.error("Exception in sendMessage ", e);
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_POST_MESSAGE_RECEIVED,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "post incoming message received", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/message/{messageUUID}/received", method = RequestMethod.PUT)
	public ResponseEntity<Response> postMessageReceived(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("messageUUID") String messageUUID,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "postMessageReceived");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.MESSAGE_UUID, messageUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In post incoming message received for department_uuid={} message_uuid={}",departmentUUID, messageUUID);
			
		    response = communicationsApiImpl.postMessageReceived(messageUUID,departmentUUID);
			
		} catch (Exception e) {
			LOGGER.error("Exception in post incoming message received processing for department_uuid={} message_uuid={}",departmentUUID, messageUUID,  e);
			Response errorResponse = null;
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_POST_MESSAGE_SENT,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "verify Recording Url for Dealer", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/dealer/verify/recording/url", method = RequestMethod.POST)
	public ResponseEntity<Response> verifyRecordingForDealers(
			@RequestBody RecordingVerifyRequest updateRecordingRequest,
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID
			) throws Exception {
		
		ResponseEntity<Response> response = null;
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updateRecordingForDealers");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In updating recording urls for dealer_ids={} fromDate={} endDate={}",updateRecordingRequest.getDealerIDs(), updateRecordingRequest.getStartDate(), updateRecordingRequest.getEndDate());
			response = communicationsApiImpl.updateRecordingForDealers(updateRecordingRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in updating recording urls for dealer_ids={} fromDate={} endDate={}",updateRecordingRequest.getDealerIDs(), updateRecordingRequest.getStartDate(), updateRecordingRequest.getEndDate() , e);
			Response errorResponse = null;
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_POST_MESSAGE_SENT,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update Recording Url for Dealer", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/dealer/recording/url", method = RequestMethod.POST)
	public ResponseEntity<Response> updateRecordingForDealers(
			@RequestBody RecordingUpdateRequest updateRecordingRequest,
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID
			) throws Exception {
		
		ResponseEntity<Response> response = null;
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updateRecordingForDealers");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In updating recording urls for dealer_ids={} fromDate={} endDate={}",updateRecordingRequest.getDealerIDs(), updateRecordingRequest.getStartDate(), updateRecordingRequest.getEndDate());
			response = communicationsApiImpl.updateRecordingForDealers(updateRecordingRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in updating recording urls for dealer_ids={} fromDate={} endDate={}",updateRecordingRequest.getDealerIDs(), updateRecordingRequest.getStartDate(), updateRecordingRequest.getEndDate() , e);
			Response errorResponse = null;
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_POST_MESSAGE_SENT,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update Recording url for message", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/message/{messageUUID}/recording/url", method = RequestMethod.POST)
	public ResponseEntity<Response> updateRecordingForMessage(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("messageUUID") String messageUUID,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updateRecordingForMessage");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.MESSAGE_UUID, messageUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In updating recording url for message_uuid={}", messageUUID);
			
		    response = communicationsApiImpl.updateURLForMessage(messageUUID);
			
		} catch (Exception e) {
			LOGGER.error("Exception in updating recording url for message_uuid={}",messageUUID,  e);
			Response errorResponse = null;
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.CREATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "saving Inbound Failed messages", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/inboundFailedMessages", method = RequestMethod.POST)
	public ResponseEntity<FailedMessageResponse> inboundFailedMessage(
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestBody FailedMessagesRequest failedMessagesRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<FailedMessageResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "saveInboundFailedMessages");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In saving failed inbound messages for accountSid={} and sidList={}", failedMessagesRequest.getAccountSid(), failedMessagesRequest.getMessageSidList().size());
			
		    response = communicationsApiImpl.saveFailedMessages(failedMessagesRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in saving failed inbound messages for accountSid={}",failedMessagesRequest.getAccountSid(),  e);
			FailedMessageResponse inboundFailedMessageResponse = null;
			return new ResponseEntity<FailedMessageResponse>(inboundFailedMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.UPDATE_WFR_FEEDBACK,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update message prediction feedback", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUUID}/messagepredictionfeedback", method = RequestMethod.PUT)
	public ResponseEntity<Response> updateMessagePredictionFeedback(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("userUUID") String userUUID,
			@RequestBody UpdateMessagePredictionFeedbackRequest updateMessagePredictionFeedbackRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) {
				
		ResponseEntity<Response> response = null;
		
		try {
			response = communicationsApiImpl.updateMessagePredictionFeedback(departmentUUID, userUUID, updateMessagePredictionFeedbackRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in updating message prediction feedback for department_uuid={} and user_uuid={} and message_prediction_id={} "
					,departmentUUID, userUUID, updateMessagePredictionFeedbackRequest.getMessagePredictionID(), e);
			Response errorResponse = new Response();

			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name() ,String.format("Internal error %s while updating message prediction feedback ", e));
			
			List<ApiError> errors = errorResponse.getErrors();
			errors.add(apiError);
			errorResponse.setErrors(errors);
	
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}	
		return response;
		
	}

	@KCommunicationsAuthorize(apiScope = ApiScope.UPDATE_WFR_FEEDBACK,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update message prediction feedback and change thread status if required", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/user/{userUUID}/messagepredictionfeedbacknew", method = RequestMethod.PUT)
	public ResponseEntity<UpdateMessagePredictionFeedbackResponse> updateMessagePredictionFeedbackNew(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("userUUID") String userUUID,
			@RequestBody UpdateMessagePredictionFeedbackRequestNew updateMessagePredictionFeedbackRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) {
				
		ResponseEntity<UpdateMessagePredictionFeedbackResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updateMessagePredictionFeedbackNew");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			response = communicationsApiImpl.updateMessagePredictionFeedbackNew(departmentUUID, userUUID, updateMessagePredictionFeedbackRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in updating message prediction feedback for department_uuid={} and user_uuid={} and message_uuid={} prediction_feature={}"
					,departmentUUID, userUUID, updateMessagePredictionFeedbackRequest.getMessageUUID(), updateMessagePredictionFeedbackRequest.getPredictionFeature(), e);
			UpdateMessagePredictionFeedbackResponse errorResponse = new UpdateMessagePredictionFeedbackResponse();

			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name() ,String.format("Internal error %s while updating message prediction feedback ", e));
			
			List<ApiError> errors = errorResponse.getErrors();
			errors.add(apiError);
			errorResponse.setErrors(errors);
	
			return new ResponseEntity<UpdateMessagePredictionFeedbackResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			MDC.clear();
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_POST_MESSAGE_SENT,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "verify Recordings for call", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/verifyRecordings", method = RequestMethod.POST)
	public ResponseEntity<Response> verifyRecording(
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestBody RecordingRequest verificationRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "saveInboundFailedMessages");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In verify Recordings for messages for all dealers");
			
		    response = communicationsApiImpl.verifyRecordingsForDealer(verificationRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in verify Recordings for messages for all dealer",  e);
			Response errorResponse = null;
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.CUST_CONV_GET,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "printing thread for customer", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customer/{customerUUID}/printThreadForCustomer", method = RequestMethod.POST)
	public ResponseEntity<CommunicationHistoryResponse> printThreadForCustomer(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("customerUUID") String customerUUID,
			@RequestBody CommunicationHistoryRequest communicationHistoryRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<CommunicationHistoryResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "printThreadForCustomer");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In printing Thread For Customer");
			
		    response = communicationsApiImpl.printThreadForCustomer(departmentUUID, customerUUID, communicationHistoryRequest);
		} catch (Exception e) {
			LOGGER.error("Exception in printing thread for customer",  e);
			CommunicationHistoryResponse errorResponse = null;
			return new ResponseEntity<CommunicationHistoryResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.CUST_CONV_GET,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "fetchMessageCountForCustomer", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customer/{customerUUID}/messageCount", method = RequestMethod.POST)
	public ResponseEntity<CommunicationCountResponse> fetchMessageCountForCustomerInGivenRange(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("customerUUID") String customerUUID,
			@RequestBody CommunicationCountRequest communicationCountRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<CommunicationCountResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "fetchMessageCountForCustomer");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In fetchMessageCountForCustomer");
			
		    response = communicationsApiImpl.fetchMessageCountForCustomerInGivenRange(departmentUUID, customerUUID, communicationCountRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in fetchMessageCountForCustomer",  e);
			CommunicationCountResponse errorResponse = null;
			return new ResponseEntity<CommunicationCountResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.DELETE_SUBSCRIPTIONS,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "delete subsriptions for invalid user", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "/department/{departmentUUID}/subscriptions", method = RequestMethod.POST)
	public ResponseEntity<Response> deleteSubscriptionsForUsers(
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestBody DeleteSubscriptionsRequest deleteSubscriptionsRequest) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "deleteSubsriptionsForInvalidUser");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();
	
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In delete subsriptions for invalid user");
			
		    response = communicationsApiImpl.deleteSubscriptionsForUsers(deleteSubscriptionsRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in In delete subsriptions for invalid users",  e);
			Response errorResponse = null;
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_ATTACHMENTS_WRITE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "upload attachments to s3", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "/department/{departmentUUID}/attachments", method = RequestMethod.POST)
	public ResponseEntity<UploadAttachmentResponse> uplodadAttachmentsToS3(
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestBody UploadAttachmentsToS3Request uploadAttachmentsToS3Request) throws Exception {
		
		ResponseEntity<UploadAttachmentResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "uplodadAttachmentsToS3");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();
	
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In upload attachments for dealer");
			
		    response = communicationsApiImpl.uploadAttachmentsToS3(uploadAttachmentsToS3Request, departmentUUID);
			
		} catch (Exception e) {
			LOGGER.error("Exception in uploading attachments to s3",  e);
			
			UploadAttachmentResponse errorResponse = new UploadAttachmentResponse();
			errorResponse.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.UPLOAD_ATTACHMENT_FAILED.name())));
			return new ResponseEntity<UploadAttachmentResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_ATTACHMENTS_WRITE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL) 
	@ResponseBody
	@ApiOperation(value="uploads attachment to s3 and returns media preview url",authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value="department/{departmentUUID}/attachment/preview/url/generate", method = RequestMethod.POST)
	public ResponseEntity<MediaPreviewURLFetchResponse> uploadMediaToAWSAndFetchMediaURL(
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestBody DocFileDTO docFileDTO,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		ResponseEntity<MediaPreviewURLFetchResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "uploadMediaToAWS");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();
			
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			LOGGER.info("In uploadMediaToAWS for docfile={}", new ObjectMapper().writeValueAsString(docFileDTO));

			response = communicationsApiImpl.uploadMediaToAWSAndFetchMediaURL(docFileDTO) ; 
			
		}
		catch(Exception e) {
			LOGGER.error("Exception in uploadMediaToAWS ", e);
			MediaPreviewURLFetchResponse response2 = new MediaPreviewURLFetchResponse();
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            response2.setErrors(errors);
			return new ResponseEntity<MediaPreviewURLFetchResponse>(response2, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		finally {
			MDC.clear();
		}
		
		return response; 
	}


	@KCommunicationsAuthorize(apiScope = ApiScope.DELETE_SUBSCRIPTIONS,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "delete messages from delayed filter", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "/department/{departmentUUID}/delayedFilter", method = RequestMethod.DELETE)
	public ResponseEntity<Response> removeMessagesFromDelayedFilter(
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestBody DelayedFilterRemovalRequest delayedFilterRemovalRequest) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "removeMessagesFromDelayedFilter");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();
	
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In removeMessagesFromDelayedFilter");
			
		    response = communicationsApiImpl.removeMessagesFromDelayedFilter(delayedFilterRemovalRequest);
			
		} catch (Exception e) {
			LOGGER.error("Exception in messages from delayed filter",  e);
			Response errorResponse = null;
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}


	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_ATTACHMENTS_DELETE, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "delete attachment from s3", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "/department/{departmentUUID}/attachments/delete", method = RequestMethod.POST)
	public ResponseEntity<DeleteAttachmentFromS3Response> deleteAttachmentFromS3(
		@RequestBody DeleteAttachmentFromS3Request deleteAttachmentFromS3Request,
		@PathVariable("departmentUUID") String departmentUUID) throws Exception {
	
		ResponseEntity<DeleteAttachmentFromS3Response> response = null;
		
		try {
			
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "deleteAttachmentFromS3");
	
			LOGGER.info("In delete attachment from S3");
			LOGGER.info(String.format("departmentUUID : ", departmentUUID));
			LOGGER.info(String.format("deleteAttachmentFromS3Request : ", deleteAttachmentFromS3Request.toString()));
			
			String logUUID = loguuidJson.toString();
				
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			response = communicationsApiImpl.deleteAttachmentFromS3(deleteAttachmentFromS3Request, departmentUUID);
		    
		} catch (Exception e) {
				
			LOGGER.error(String.format("Exception in deleting attachments from s3 for departmentUUID : %s", departmentUUID),  e);
				
			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name() ,
					String.format("Internal error %s while deleting attachments from s3", e));
			List<ApiError> errors = new ArrayList<>();
			errors.add(apiError);
				
			DeleteAttachmentFromS3Response errorResponse = new DeleteAttachmentFromS3Response();
			errorResponse.setErrors(errors);
				
			return new ResponseEntity<DeleteAttachmentFromS3Response>(errorResponse,
			HttpStatus.INTERNAL_SERVER_ERROR);		
		}
			
		return response;
			
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.DELETE_SUBSCRIPTIONS, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update subscriptions for dealer", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "/department/{departmentUUID}/subscriptions", method = RequestMethod.PUT)
	public ResponseEntity<SubscriptionsUpdateResponse> updateSubscriptionsForDealers(
		@RequestBody SubscriptionUpdateRequest subscriptionDeleteRequest,
		@PathVariable("departmentUUID") String departmentUUID) throws Exception {
	
		ResponseEntity<SubscriptionsUpdateResponse> response = null;
		
		try {
			
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "deleteSubscriptions");
	
			LOGGER.info("In delete subscriptions for dealers");
			
			String logUUID = loguuidJson.toString();
				
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			response = communicationsApiImpl.updateSubscriptionsForDealers(subscriptionDeleteRequest);
		    
		} catch (Exception e) {
				
			LOGGER.error(String.format("Exception in updating subscriptions for departmentUUID : %s", departmentUUID),  e);
				
			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name() ,
					String.format("Internal error %s while updating subscription", e));
			List<ApiError> errors = new ArrayList<>();
			errors.add(apiError);
				
			SubscriptionsUpdateResponse errorResponse = new SubscriptionsUpdateResponse();
			errorResponse.setErrors(errors);
				
			return new ResponseEntity<SubscriptionsUpdateResponse>(errorResponse,
			HttpStatus.INTERNAL_SERVER_ERROR);		
		}
			
		return response;
			
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.UPDATE_MESSAGE_PREDICTION, apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update message prediction table for predictions made on incoming text message", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/message/prediction", method = RequestMethod.POST)
	public ResponseEntity<Response> updateMessagePrediction(
			@PathVariable("departmentUUID") String departmentUUID,
			@RequestBody UpdateMessagePredictionRequest updateMessagePredictionRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updateMessagePrediction");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEPARTMENT_TOKEN, departmentUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info(String.format("In update message prediction table for department_uuid=%s  message_id=%s prediction_feature=%s ",
					departmentUUID, updateMessagePredictionRequest.getMessageID(), updateMessagePredictionRequest.getPredictionFeature()));
			
		    response = communicationsApiImpl.updateMessagePrediction(updateMessagePredictionRequest, departmentUUID);
			
		} catch (Exception e) {
			LOGGER.error(String.format("Exception update message prediction table for department_uuid=%s  message_id=%s prediction_feature=%s "
					, departmentUUID, updateMessagePredictionRequest.getMessageID(), updateMessagePredictionRequest.getPredictionFeature()) ,e);
			List<ApiError> errors = new ArrayList<>();
			ApiError error = new ApiError();
			error.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.name());
			error.setErrorDescription(String.format("Internal Server Error %s update message prediction table for department_uuid=%s  message_id=%s prediction_feature=%s ", 
					e.getMessage(), departmentUUID, updateMessagePredictionRequest.getMessageID(), updateMessagePredictionRequest.getPredictionFeature()));
			errors.add(error);
			Response errorResponse = new Response();
			errorResponse.setErrors(errors);
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
			
		} finally {
			MDC.clear();
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_VERIFY_BILLING, apiScopeLevel = ApiScopeLevel.DEALER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "verify if calls and texts were billied correctly in mykaarma", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "dealer/{dealerUUID}/billing", method = RequestMethod.POST)
	public ResponseEntity<Response> verifyBilling(
			@PathVariable("dealerUUID") String dealerUUID,
			@RequestBody CommunicationsBillingRequest communicationsBillingRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		ResponseEntity<Response> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "verifyBilling");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_TOKEN, dealerUUID);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info(String.format("In verify billing for verification_type=%s dealer_id=%s start_date=%s end_date=%s",
					communicationsBillingRequest.getVerificationType(), communicationsBillingRequest.getDealerIds(),
					communicationsBillingRequest.getStartDate(), communicationsBillingRequest.getEndDate()));
			
		    response = communicationsApiImpl.verifyCommunicationsBilling(communicationsBillingRequest);
			
		} catch (Exception e) {
			LOGGER.error(String.format("Exception verify for verification_type=%s dealer_department_id=%s start_date=%s end_date=%s "
					, communicationsBillingRequest.getVerificationType(), communicationsBillingRequest.getDealerIds(),
					communicationsBillingRequest.getStartDate(), communicationsBillingRequest.getEndDate()) ,e);
			List<ApiError> errors = new ArrayList<>();
			ApiError error = new ApiError();
			error.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.name());
			errors.add(error);
			Response errorResponse = new Response();
			errorResponse.setErrors(errors);
			return new ResponseEntity<Response>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
			
		} finally {
			MDC.clear();
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.CREATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "send Message", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customer/{customerUUID}/communication/historical", method = RequestMethod.PUT)
	public ResponseEntity<SaveMessageListResponse> saveHistoricalMessage(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("customerUUID") String customerUUID,
			@RequestBody SaveMessageRequestList saveMessageRequestList,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) {
		
		ResponseEntity<SaveMessageListResponse> response = null;
		
		Date messageTimestamp = new Date();
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "saveHistoricalMessage");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In saveHistoricalMessages for customer_uuid={} department_uuid={} request_size={}", customerUUID, departmentUUID,
					saveMessageRequestList.getSaveMessageRequestList().size());
			response = communicationsApiImpl.saveMessages(departmentUUID, customerUUID, saveMessageRequestList);

			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			
		} catch (Exception e) {
			LOGGER.error("Exception in save historical messages for customer_uuid={}  department_uuid={}", customerUUID, departmentUUID, e);
			SaveMessageListResponse saveMessageResponse = new SaveMessageListResponse();
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            saveMessageResponse.setErrors(errors);
			return new ResponseEntity<SaveMessageListResponse>(saveMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.CREATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "save subscriptions", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/customer/{customerUUID}/subscription", method = RequestMethod.PUT)
	public ResponseEntity<SubscriptionSaveResponse> saveSubscription(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("customerUUID") String customerUUID,
			@RequestBody SubscriptionSaveRequest subscriptionSaveRequest,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) {
		
		ResponseEntity<SubscriptionSaveResponse> response = null;
		
		Date messageTimestamp = new Date();
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "saveSubscription");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In savingSubscriptions for customer_uuid={} department_uuid={} request={}", customerUUID, departmentUUID,
					new ObjectMapper().writeValueAsString(subscriptionSaveRequest));
			response = subscriptionsApiImpl.saveSubscriptions(departmentUUID, customerUUID, subscriptionSaveRequest);

			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			LOGGER.info(String.format("Response status %s sent. time_taken=%d for save subscriptions response=%s", response.getStatusCode(), elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			LOGGER.error("Exception in save historical messages for customer_uuid={}  department_uuid={}", customerUUID, departmentUUID, e);
			SubscriptionSaveResponse subscriptionSaveResponse = new SubscriptionSaveResponse();
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            subscriptionSaveResponse.setErrors(errors);
			return new ResponseEntity<SubscriptionSaveResponse>(subscriptionSaveResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	/**
	 * add followers to a thread based on certain events
	 * @return ResponseEntity<ThreadFollowResponse>
	 */
	@KCommunicationsAuthorize(apiScope = ApiScope.CREATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "add or revoke followers to a thread", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = RestURIConstants.DEPARTMENT+"/"+RestURIConstants.DEPARTMENT_PATH_VARIABLE+"/"+RestURIConstants.CUSTOMER+"/"+RestURIConstants.CUSTOMER_PATH_VARIABLE+"/" + RestURIConstants.FOLLOWERS, method = RequestMethod.POST)
	public ResponseEntity<ThreadFollowResponse> followUnfollowThread(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("customerUUID") String customerUUID,
			@RequestBody ThreadFollowRequest threadFollowRequest,
			@ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) {
		
		ResponseEntity<ThreadFollowResponse> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "follow/unfollow thread");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In follow thread for customer_uuid={} department_uuid={} request={}", customerUUID, departmentUUID,
					new ObjectMapper().writeValueAsString(threadFollowRequest));
			response = subscriptionsApiImpl.followOrUnfollowThread(departmentUUID, customerUUID, threadFollowRequest);

			LOGGER.info(String.format("Response status %s sent for save subscriptions response=%s", response.getStatusCode(), new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			LOGGER.error("Exception in follow/unfollow thread for customer_uuid={}  department_uuid={}", customerUUID, departmentUUID, e);
			ThreadFollowResponse threadFollowResponse = new ThreadFollowResponse();
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            threadFollowResponse.setErrors(errors);
			return new ResponseEntity<ThreadFollowResponse>(threadFollowResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
	
	/**
	 * get followers to a thread
	 * @return ResponseEntity<ThreadFollowers>
	 */
	@KCommunicationsAuthorize(apiScope = ApiScope.CREATE_MESSAGE,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "get followers to a thread", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = RestURIConstants.DEPARTMENT+"/"+RestURIConstants.DEPARTMENT_PATH_VARIABLE+"/"+RestURIConstants.CUSTOMER+"/"+RestURIConstants.CUSTOMER_PATH_VARIABLE+"/" + RestURIConstants.FOLLOWERS, method = RequestMethod.GET)
	public ResponseEntity<ThreadFollowers> getFollowersToAthread(
			@PathVariable("departmentUUID") String departmentUUID,
			@PathVariable("customerUUID") String customerUUID,
			@ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID) {
		
		ResponseEntity<ThreadFollowers> response = null;
		
		try {
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "get followers to a thread");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_DEPARMENT_TOKEN, departmentUUID);
			loguuidJson.addProperty(APIConstants.CUSTOMER_UUID, customerUUID);
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
			LOGGER.info("In get followers for a thread for customer_uuid={} department_uuid={}", customerUUID, departmentUUID);
			response = subscriptionsApiImpl.getFollowersForThread(departmentUUID, customerUUID);

			LOGGER.info(String.format("Response status %s sent for save subscriptions response=%s", response.getStatusCode(), new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			LOGGER.error("Exception in save historical messages for customer_uuid={}  department_uuid={}", customerUUID, departmentUUID, e);
			ThreadFollowers threadFollowerResponse = new ThreadFollowers();
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            threadFollowerResponse.setErrors(errors);
			return new ResponseEntity<ThreadFollowers>(threadFollowerResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}
}


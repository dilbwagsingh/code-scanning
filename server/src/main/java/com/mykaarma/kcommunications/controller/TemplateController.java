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
import com.mykaarma.kcommunications.controller.impl.TemplateImpl;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.TemplateType;
import com.mykaarma.kcommunications_model.common.RestURIConstants;
import com.mykaarma.kcommunications_model.dto.TemplateDTO;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.request.CreateFreemarkerTemplatesRequest;
import com.mykaarma.kcommunications_model.request.DealersTemplateIndexRequest;
import com.mykaarma.kcommunications_model.request.EnableFreemarkerTemplatesRequest;
import com.mykaarma.kcommunications_model.request.TemplateTagsRequest;
import com.mykaarma.kcommunications_model.request.TemplateSearchRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.CreateFreemarkerTemplatesResponse;
import com.mykaarma.kcommunications_model.response.EnableFreemarkerTemplatesResponse;
import com.mykaarma.kcommunications_model.response.TemplateTagsResponse;
import com.mykaarma.kcommunications_model.response.SearchTemplateResponse;
import com.mykaarma.kcommunications_model.response.TemplateIndexingResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

@RestController
@Configuration
@Slf4j
@ComponentScan("com.mykaarma.kcommunications.services")
@Api(tags = "Template Controller", description = "Endpoints for all things related to templates")
public class TemplateController {
	
	@Autowired
	ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;
	
	@Autowired
	RabbitHelper rabbitHelper;
	
	@Autowired
	TemplateImpl templateImpl;
	
	private ObjectMapper objectMapper=new ObjectMapper();

	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_TEMPLATE_INDEX,apiScopeLevel = ApiScopeLevel.DEALER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "update/insert template for given dealer, type and identifier in elastic search", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "dealer/{dealerUUID}/type/{templateType}/template/{templateUuid}/index", method = RequestMethod.PUT)
	public ResponseEntity<TemplateIndexingResponse> indexTemplate(
			@PathVariable("dealerUUID") String dealerUuid,
			@PathVariable("templateType") String templateType,
			@PathVariable("templateUuid") String templateUuid,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		TemplateIndexingResponse response = new TemplateIndexingResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "indexTemplate");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_TOKEN, dealerUuid);
			loguuidJson.addProperty(APIConstants.TEMPLATE_UUID, templateUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In indexTemplate subscriber={}", subscriberName);
			TemplateType templateTyoeObj=null;
			
			if(TemplateType.AUTOMATIC.toString().equalsIgnoreCase(templateType)){
				templateTyoeObj=TemplateType.AUTOMATIC;
			} else if(TemplateType.MANUAL.toString().equalsIgnoreCase(templateType)){
				templateTyoeObj=TemplateType.MANUAL;
			}
			rabbitHelper.pushToTemplateIndexingQueue(templateTyoeObj, templateUuid);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			response.setRequestStatus(true);
			response.setRequestUuid(requestID);
			log.info(String.format("Request successful. time_taken=%d response=%s", elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			log.error("Exception in indexing template ", e);
			response.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            response.setErrors(errors);
			
		}
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_TEMPLATE_INDEX,apiScopeLevel = ApiScopeLevel.DEALER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "delete template for given dealer and identifier in elastic search", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "dealer/{dealerUUID}/type/{templateType}/template/{templateUuid}/index", method = RequestMethod.DELETE)
	public ResponseEntity<TemplateIndexingResponse> deleteTemplateFromIndex(
			@PathVariable("dealerUUID") String dealerUuid,
			@PathVariable("templateType") String templateType,
			@PathVariable("templateUuid") String templateUuid,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		TemplateIndexingResponse response = new TemplateIndexingResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "deleteTemplateFromIndex");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_TOKEN, dealerUuid);
			loguuidJson.addProperty(APIConstants.TEMPLATE_UUID, templateUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In deleteTemplateFromIndex subscriber={}", subscriberName);
			TemplateType templateTyoeObj=null;
			if(TemplateType.AUTOMATIC.toString().equalsIgnoreCase(templateType)){
				templateTyoeObj=TemplateType.AUTOMATIC;
			} else if(TemplateType.MANUAL.toString().equalsIgnoreCase(templateType)){
				templateTyoeObj=TemplateType.MANUAL;
			}
			Boolean deletionStatus=templateImpl.deleteTemplateFromElasticSearch(templateUuid, templateTyoeObj);
			response.setRequestStatus(deletionStatus);
			response.setRequestUuid(requestID);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format("Request successful. time_taken=%d response=%s", elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			log.error("Exception in deleting template from index ", e);
			response.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            response.setErrors(errors);
			
		}
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_TEMPLATE_INDEX,apiScopeLevel = ApiScopeLevel.DEALER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "indexs template for all the given dealers in the request", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "dealer/{dealerUUID}/template/index", method = RequestMethod.POST)
	public ResponseEntity<TemplateIndexingResponse> indexTemplatesForDealers(
			@PathVariable("dealerUUID") String dealerUuid,
			@RequestBody DealersTemplateIndexRequest dealersTemplateIndexRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		TemplateIndexingResponse response = new TemplateIndexingResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "indexTemplatesForDealers");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_TOKEN, dealerUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In indexTemplatesForDealers subscriber={} for request={}", subscriberName,objectMapper.writeValueAsString(dealersTemplateIndexRequest));
			
			Boolean deletionStatus=templateImpl.indexTemplatesForDealers(dealersTemplateIndexRequest);
			response.setRequestStatus(deletionStatus);
			response.setRequestUuid(requestID);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format("Request successful. time_taken=%d response=%s", elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			log.error("Exception in indexing all templates for dealers in request={} ",objectMapper.writeValueAsString(dealersTemplateIndexRequest), e);
			response.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            response.setErrors(errors);
			
		}
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_TEMPLATE_INDEX,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "indexs template for all the given dealers in the request", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/template/index", method = RequestMethod.POST)
	public ResponseEntity<TemplateIndexingResponse> indexTemplatesForDepartment(
			@PathVariable("departmentUUID") String departmentUuid,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		TemplateIndexingResponse response = new TemplateIndexingResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "indexTemplatesForDepartment");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEPARTMENT_TOKEN, departmentUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In indexTemplatesForDepartment subscriber={} ", subscriberName);
			
			Boolean deletionStatus=templateImpl.reindexTemplatesForDepartment(departmentUuid);
			response.setRequestStatus(deletionStatus);
			response.setRequestUuid(requestID);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format("Request successful. time_taken=%d response=%s", elapsedTime,new ObjectMapper().writeValueAsString(response)));
		} catch (Exception e) {
			log.error("Exception in indexing templates for dealer_department_uuid={} ",departmentUuid, e);
			response.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            response.setErrors(errors);
			
		}
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_TEMPLATE_SEARCH,apiScopeLevel = ApiScopeLevel.DEPARTMENT_LEVEL)
	@ResponseBody
	@ApiOperation(value = "search templates for given department and string ", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "department/{departmentUUID}/template/search", method = RequestMethod.POST)
	public ResponseEntity<SearchTemplateResponse> searchTemplates(
			@PathVariable("departmentUUID") String departmentUuid,
			@RequestBody TemplateSearchRequest templateSearchRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		SearchTemplateResponse searchTemplateResponse=new SearchTemplateResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "searchTemplates");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_TOKEN, departmentUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In searchTemplates subscriber={} ", subscriberName);
			
			List<TemplateDTO> templateDTOList = templateImpl.searchTemplatesFromElasticSearch(departmentUuid, templateSearchRequest);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			
			searchTemplateResponse.setTemplateDTOList(templateDTOList);
			searchTemplateResponse.setRequestUuid(requestID);
			
			log.info(String.format("Request successful. time_taken=%d response=%s", elapsedTime,new ObjectMapper().writeValueAsString(searchTemplateResponse)));
			
			return new ResponseEntity<SearchTemplateResponse>(searchTemplateResponse, HttpStatus.OK);
			
			
		} catch (Exception e) {
			log.error("Exception in searching template ", e);
			searchTemplateResponse.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            searchTemplateResponse.setErrors(errors);
			
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(searchTemplateResponse);
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_CREATE_FREEMARKER_TEMPLATE,apiScopeLevel = ApiScopeLevel.DEALER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "create freemarker templates for the template type and all the given dealers in the request", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = RestURIConstants.DEALER+"/"+RestURIConstants.DEALER_PATH_VARIABLE+"/"+RestURIConstants.FREEMARKER+"/"+RestURIConstants.TEMPLATE, method = RequestMethod.POST)
	public ResponseEntity<CreateFreemarkerTemplatesResponse> createFreemarkerTemplates(
			@PathVariable(RestURIConstants.DEALER_UUID) String dealerUuid,
			@RequestBody CreateFreemarkerTemplatesRequest createFreemarkerTemplatesRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		CreateFreemarkerTemplatesResponse response = new CreateFreemarkerTemplatesResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "createFreemarkerTemplates");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_TOKEN, dealerUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In createFreemarkerTemplates subscriber={} for request={}", subscriberName,objectMapper.writeValueAsString(createFreemarkerTemplatesRequest));
			
			Boolean requestStatus=templateImpl.createFreemarkerTemplates(createFreemarkerTemplatesRequest);
			response.setRequestStatus(requestStatus);
			response.setRequestUuid(requestID);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format("Request to create freemarker templates successful. time_taken=%d response=%s", elapsedTime,objectMapper.writeValueAsString(response)));

			return ResponseEntity.status(HttpStatus.OK).body(response);
			
		} catch (Exception e) {
			log.error("Exception in creating freemarker templates for dealers in request={} ",objectMapper.writeValueAsString(createFreemarkerTemplatesRequest), e);
			response.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            response.setErrors(errors);
            
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);	
		}
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_CREATE_FREEMARKER_TEMPLATE,apiScopeLevel = ApiScopeLevel.DEALER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "get templates tags for the list template type given in the request for all dealers", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = RestURIConstants.DEALER+"/"+RestURIConstants.DEALER_PATH_VARIABLE+"/"+RestURIConstants.TEMPLATE+"/"+RestURIConstants.TAGS, method = RequestMethod.GET)
	public ResponseEntity<TemplateTagsResponse> getTemplateTags(
			@PathVariable(RestURIConstants.DEALER_UUID) String dealerUuid,
			@RequestBody TemplateTagsRequest getTemplateTagsRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		TemplateTagsResponse response = new TemplateTagsResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "getTemplateTags");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_TOKEN, dealerUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In getTemplateTags subscriber={} for request={}", subscriberName,objectMapper.writeValueAsString(getTemplateTagsRequest));
			
			response = templateImpl.getTemplateTags(getTemplateTagsRequest);
			response.setRequestUuid(requestID);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format("Request to get templates tags successful. time_taken=%d response=%s", elapsedTime,objectMapper.writeValueAsString(response)));

			return ResponseEntity.status(HttpStatus.OK).body(response);
			
		} catch (Exception e) {
			log.error("Exception in fetching tags for templates in request={} ",objectMapper.writeValueAsString(getTemplateTagsRequest), e);
			response.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            response.setErrors(errors);
            
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);	
		}
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_CREATE_FREEMARKER_TEMPLATE,apiScopeLevel = ApiScopeLevel.DEALER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "enable freemarker templates for the service given in the request for all dealers in the request", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = RestURIConstants.DEALER+"/"+RestURIConstants.DEALER_PATH_VARIABLE+"/"+RestURIConstants.FREEMARKER+"/"+RestURIConstants.TEMPLATE+"/"+RestURIConstants.ENABLE, method = RequestMethod.POST)
	public ResponseEntity<EnableFreemarkerTemplatesResponse> updateFreemarkerTemplateDealerSetupOption(
			@PathVariable(RestURIConstants.DEALER_UUID) String dealerUuid,
			@RequestBody EnableFreemarkerTemplatesRequest enableFreemarkerTemplatesRequest,
			@RequestHeader("authorization") String authToken,
			@RequestAttribute(APIConstants.REQUEST_ID) String requestID) throws Exception {
		
		EnableFreemarkerTemplatesResponse response = new EnableFreemarkerTemplatesResponse();
		
		Date messageTimestamp = new Date();
		
		try {

			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "updateFreemarkerTemplateDealerSetupOption");
			loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
			loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
			loguuidJson.addProperty(APIConstants.DEALER_TOKEN, dealerUuid);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();

			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			log.info("In updateFreemarkerTemplateDealerSetupOption subscriber={} for request={}", subscriberName,objectMapper.writeValueAsString(enableFreemarkerTemplatesRequest));

			Boolean requestStatus=templateImpl.updateFreemarkerTemplateDealerSetupOption(enableFreemarkerTemplatesRequest);
			response.setRequestStatus(requestStatus);
			response.setRequestUuid(requestID);
			long elapsedTime = (new Date()).getTime() - messageTimestamp.getTime();
			log.info(String.format("Request to enable freemarker templates successful. time_taken=%d response=%s", elapsedTime,objectMapper.writeValueAsString(response)));

			return ResponseEntity.status(HttpStatus.OK).body(response);
			
		} catch (Exception e) {
			log.error("Exception in enabling freemarker templates for request={} ",objectMapper.writeValueAsString(enableFreemarkerTemplatesRequest), e);
			response.setRequestUuid(requestID);
            List<ApiError> errors = new ArrayList<ApiError>();
            errors.add(new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), e.getMessage()));
            response.setErrors(errors);
            
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);	
		}
	}
	
}

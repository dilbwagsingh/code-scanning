package com.mykaarma.kcommunications.controller;

import java.util.Map;
import java.util.Set;

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
import com.mykaarma.kcommunications.controller.impl.DefaultThreadOwnerImpl;
import com.mykaarma.kcommunications.controller.impl.ThreadImpl;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications_model.common.RestURIConstants;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.request.CommunicationsOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.ThreadCountRequest;
import com.mykaarma.kcommunications_model.response.ThreadCountResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@Configuration
@Api(tags = "thread-controller", description="Endpoints related to threads")
@ComponentScan("com.mykaarma.kcommunications.services")
public class ThreadController {
	
	 @Autowired
	 private ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;
	 
	 @Autowired
	 private ThreadImpl threadImpl;

	 private final static Logger LOGGER = LoggerFactory.getLogger(ThreadController.class);
	 
	 @KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_THREAD_READ,apiScopeLevel = ApiScopeLevel.SERVICE_SUBSCRIBER_LEVEL)
	 @ResponseBody
	 @ApiOperation(value = "Get number of threads assigned to a list of dealer associates", authorizations = {@Authorization(value = "basicAuth")})
	 @RequestMapping(value = RestURIConstants.DEPARTMENT + "/" + RestURIConstants.USER + "/" + RestURIConstants.USER_PATH_VARIABLE + "/" + RestURIConstants.THREAD + "/" + RestURIConstants.COUNT, method = RequestMethod.POST)
	 public ResponseEntity<ThreadCountResponse> getNumberOfThreads(
			 @PathVariable(RestURIConstants.USERUUID) String userUUID,
			 @RequestBody ThreadCountRequest request,
			 @ApiIgnore @RequestHeader("authorization") String authToken,
			 @ApiIgnore @RequestAttribute(APIConstants.REQUEST_ID) String requestID
			 ) throws Exception {

		 JsonObject loguuidJson = new JsonObject();
		 loguuidJson.addProperty(APIConstants.METHOD, "getNumberOfThreads");
		 loguuidJson.addProperty(APIConstants.COMMUNICATIONS, "mykaarmaapi");
		 loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
		 String logUUID = loguuidJson.toString();

		 MDC.put(APIConstants.LogUUID, logUUID);
		 MDC.put(APIConstants.FILTER_REQUEST, "true");

		 if(request!=null && request.getDepartmentUuids()!=null && userUUID!=null) {
			 ObjectMapper mapper = new ObjectMapper();
			 LOGGER.info("Get number of threads for user_uuid={} and request={}",userUUID, mapper.writeValueAsString(request));
			 Set<String> deptUuidList = request.getDepartmentUuids();
			 for(String deptUuid: deptUuidList) {

				 LOGGER.info("in getNumberOfThreads for departmet_uuid={} user_uuid={}",deptUuid, userUUID);
			 }
		 }

		 ThreadCountResponse response = threadImpl.getNumberOfThreads(userUUID, request);
		 response.setRequestUUID(requestID);

		 LOGGER.info("in getNumberOfThreads for response={}",new ObjectMapper().writeValueAsString(response) );
		 return new ResponseEntity<ThreadCountResponse>(response, HttpStatus.OK);	
	 }

}

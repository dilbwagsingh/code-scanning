package com.mykaarma.kcommunications.controller;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.authorize.ApiAuthenticatorAndAuthorizer;
import com.mykaarma.kcommunications.authorize.KCommunicationsAuthorize;
import com.mykaarma.kcommunications.controller.impl.FileUploadService;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.FileType;
import com.mykaarma.kcommunications_model.response.FileDeleteResponse;
import com.mykaarma.kcommunications_model.response.FileUploadResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

@RestController
@ComponentScan("com.mykaarma.kcommunications.services")
@Api(tags = "File Operations", description = "Endpoints for updating or deleting files")
public class FileController {
	
	@Autowired
	private ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;
	
	@Autowired
	private FileUploadService fileUploadService;
	
	@Autowired
	private KCommunicationsUtils kCommunicationsUtils;
	
	private final static Logger log = LoggerFactory.getLogger(FileController.class);
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_FILE_UPLOAD, apiScopeLevel = ApiScopeLevel.SERVICE_SUBSCRIBER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "upload file to s3 in common bucket, i.e., not under a dealer's bucket", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "file", method = RequestMethod.PUT)
	public ResponseEntity<FileUploadResponse> uplodadFileToS3(
			@ApiParam(value = "Multipart file to be uploaded")
			@RequestParam(name = "file") MultipartFile file,
			@ApiParam(value = "Choose a value of fileType from the provided values")
			@RequestParam(name = "fileType", defaultValue = "OTHER") FileType fileType,
			@ApiParam(value = "Valid contentType of file, e.g. image/png or audio/mpeg etc. If not provided then we'll try to get it from the provided multipart file's properties.")
			@RequestParam(name = "contentType", required = false) String contentType,
			@ApiParam(value = "Basic Authentication Header")
			@RequestHeader("authorization") String authToken) throws Exception {
		
		ResponseEntity<FileUploadResponse> response = null;
		try {
			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "uplodadFileToS3");
			loguuidJson.addProperty(APIConstants.MYKAARMAAPI, APIConstants.COMMUNICATIONS);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
		    response = fileUploadService.uploadFileToS3(file, contentType, fileType);
			
		} catch (Exception e) {
			log.error("Exception in uploading file to s3",  e);
			FileUploadResponse errorResponse = new FileUploadResponse();
			errorResponse.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.FILE_UPLOAD_FAILED.name())));
			response = new ResponseEntity<FileUploadResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			MDC.clear();
		}
		return response;
	}
	
	@KCommunicationsAuthorize(apiScope = ApiScope.COMMUNICATIONS_FILE_DELETE, apiScopeLevel = ApiScopeLevel.SERVICE_SUBSCRIBER_LEVEL)
	@ResponseBody
	@ApiOperation(value = "delete file from common bucket of s3", authorizations = {@Authorization(value = "basicAuth")})
	@RequestMapping(value = "file", method = RequestMethod.DELETE)
	public ResponseEntity<FileDeleteResponse> deleteFileFromS3(
			@ApiParam(value = "complete fileUrl as received in response of its upload")
			@RequestParam(name = "fileUrl") String fileUrl,
			@ApiParam(value = "Basic Authentication Header")
			@RequestHeader("authorization") String authToken) throws Exception {
		
		ResponseEntity<FileDeleteResponse> response = null;
		try {
			String subscriberName = apiAuthenticatorAndAuthorizer.getServiceSubscriberName(authToken);
			JsonObject loguuidJson = new JsonObject();
			loguuidJson.addProperty(APIConstants.METHOD, "deleteFileFromS3");
			loguuidJson.addProperty(APIConstants.MYKAARMAAPI, APIConstants.COMMUNICATIONS);
			loguuidJson.addProperty(APIConstants.SUBSCRIBER_NAME, subscriberName);
			
			String logUUID = loguuidJson.toString();
			MDC.put(APIConstants.LogUUID, logUUID);
			MDC.put(APIConstants.FILTER_REQUEST, "true");
			
		    response = fileUploadService.deleteFileFromS3(fileUrl);
			
		} catch (Exception e) {
			log.error("Exception in deleting file from s3 fileUrl={}", fileUrl, e);
			FileDeleteResponse errorResponse = new FileDeleteResponse();
			errorResponse.setIsDeleted(false);
			errorResponse.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.FILE_DELETE_FAILED.name())));
			response = new ResponseEntity<FileDeleteResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			MDC.clear();
		}
		return response;
	}

}

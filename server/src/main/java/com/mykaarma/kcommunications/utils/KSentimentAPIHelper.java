package com.mykaarma.kcommunications.utils;

import java.nio.charset.Charset;

import org.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.mykaarma.global.ModuleLogCodes;
import com.mykaarma.kcommunications.controller.impl.CommunicationsApiImpl;
import com.mykaarma.kcommunications_model.response.Response;

@Service
public class KSentimentAPIHelper {

	@Value("${ksentiment_api_url}")
	private String ksentiment_api_url;

	@Value("${kcommunications_basic_auth_user}")
	private String username;
	
	@Value("${kcommunications_basic_auth_pass}")
	private String password;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(KSentimentAPIHelper.class);	
	
	public Boolean hitSentimentApi(String departmentUUID, String messageUUID, String messageBody)
	{
		if(messageBody==null || messageBody.isEmpty()){
			LOGGER.info(String.format("in hitSentimentApi not hitting ksentiment API since empty message body for message_uuid=%s message_body=%s ", 
					messageUUID, messageBody));
			return true;
		}
		
		HttpHeaders headers =  createHeadersWithBasicAuth();
		
		try {
			String ksentimentApiUrl = String.format("%sdepartment/%s/message/%s/predict", ksentiment_api_url, departmentUUID, messageUUID);

			LOGGER.info(String.format("ksentimentApiUrl=%s username=%s ", ksentimentApiUrl, username));

			RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());
			JSONObject sentimentAnalysisRequestJSON = new JSONObject();
			sentimentAnalysisRequestJSON.put("messageBody", messageBody);

			HttpEntity<String> httprequest = 
					new HttpEntity<String>(sentimentAnalysisRequestJSON.toString(), headers);

			ResponseEntity<Response> sentimentAnalysisResponse = 
					restTemplate.exchange(ksentimentApiUrl, HttpMethod.POST, httprequest, Response.class);
			
			Response sentimentAnalysisResponseBody = sentimentAnalysisResponse.getBody();
			HttpStatus sentimentAnalysisResponseStatusCode = sentimentAnalysisResponse.getStatusCode();
			
			if(sentimentAnalysisResponseStatusCode != HttpStatus.OK) {
				LOGGER.error(String.format("error in ksentiment API for message_uuid=%s error=%s response_status_code=%s ", 
						messageUUID, new ObjectMapper().writeValueAsString(sentimentAnalysisResponseBody.getErrors()), sentimentAnalysisResponseStatusCode));
				return false;
			}
			else if(sentimentAnalysisResponseBody.getErrors()!=null && sentimentAnalysisResponseBody.getErrors().size()!=0) {
				LOGGER.error(String.format("error in ksentiment API for message_uuid=%s error=%s  ", 
						messageUUID, new ObjectMapper().writeValueAsString(sentimentAnalysisResponseBody.getErrors())));
				return false;
			} else {
				LOGGER.info(String.format("Hit sentiment api successfully for message_uuid=%s response_status_code=%s warnings=%s ",
						messageUUID, sentimentAnalysisResponseStatusCode, new ObjectMapper().writeValueAsString(sentimentAnalysisResponseBody.getWarnings())));
				return true;
			}
		} catch (Exception e) {
			LOGGER.error(String.format("error in getting response from ksentiment API for message_uuid=%s message_body=%s ", 
					messageUUID, messageBody), e);
			return false;
		}
	}


    private SimpleClientHttpRequestFactory getClientHttpRequestFactory() {
    	
	    SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
	    
	    //Connect timeout
	    clientHttpRequestFactory.setConnectTimeout(10000); // time in ms

	    //Read timeout
	    clientHttpRequestFactory.setReadTimeout(10000);

	    return clientHttpRequestFactory;
	}
	
	private HttpHeaders createHeadersWithBasicAuth(){
		
		String plainCreds = username + ":" + password;
		byte[] plainCredsBytes = plainCreds.getBytes((Charset.forName("US-ASCII")));
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);
		HttpHeaders headers =  new HttpHeaders();
		headers.add("Authorization", "Basic " + base64Creds);
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    
		return headers;
	}
}

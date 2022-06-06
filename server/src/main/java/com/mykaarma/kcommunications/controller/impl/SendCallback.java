package com.mykaarma.kcommunications.controller.impl;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mykaarma.kcommunications_model.response.SendMessageResponse;

@Service
public class SendCallback {

	@Value("${kcommunications_basic_auth_user}")
	String username;

	@Value("${kcommunications_basic_auth_pass}")
	String password;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	CommunicationsApiImpl communicationsApiImpl;
	
	public void sendCallback(String callbackURL, SendMessageResponse sendMessageResponse) {
		if(callbackURL!=null && !callbackURL.isEmpty()) {
			if(callbackURL.contains("communications")) {
				String[] params = callbackURL.split("/");
				String departmentUUID= params[2];
				String requestUUID=  params[4];
				communicationsApiImpl.saveMessageResponse(requestUUID, departmentUUID, sendMessageResponse);
			} else {
				restTemplate.postForObject(callbackURL, sendMessageResponse, String.class);
			}
		}
	}

	public void sendCallback(String callbackURL, HttpMethod httpMethod, SendMessageResponse sendMessageResponse, boolean addBasicAuthHeader) {

		if(addBasicAuthHeader) {
			restTemplate.exchange(callbackURL, httpMethod, new HttpEntity<>(sendMessageResponse, getRequestHeader()), Object.class);
		} else {
			restTemplate.exchange(callbackURL, httpMethod, new HttpEntity<>(sendMessageResponse), Object.class);
		}

	}

	private HttpHeaders getRequestHeader() {
		String basicAuthCreds = String.format("%s:%s", username, password);
		String basicAuth64Creds = new String(Base64.encodeBase64(basicAuthCreds.getBytes()));

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + basicAuth64Creds);

		return headers;
	}

}

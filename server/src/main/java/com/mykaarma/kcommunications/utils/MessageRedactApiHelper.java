package com.mykaarma.kcommunications.utils;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.model.api.MessageRedactListRequest;
import com.mykaarma.kcommunications.model.api.MessageRedactListResponse;
import com.mykaarma.kcommunications.model.api.MessageRedactRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.global.OptOutCampaign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MessageRedactApiHelper {
    
    @Value("${message_redact_url}")
    private String messageRedactUrl;
    
    @Autowired
	RestTemplate restTemplate;
	
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageRedactApiHelper.class);

    public String getRedactedMessage(String messageUUID, String messageBody, com.mykaarma.kcommunications_model.response.MessageRedactResponse messageRedactResponse) {
        List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		messageRedactResponse.setErrors(errors);
		messageRedactResponse.setWarnings(warnings);
		String redactedMessageBody = new String();
        if(messageBody == null || messageBody.isEmpty()) {
            return redactedMessageBody;
        }
        
        try {
			ObjectMapper om = new ObjectMapper();
			MessageRedactRequest message = new MessageRedactRequest();
			message.setMessageID(messageUUID);
			message.setMessageBody(messageBody);
			List<MessageRedactRequest> messageList = new ArrayList<MessageRedactRequest>();
			messageList.add(message);
			
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", "application/json");
			MessageRedactListRequest request = new MessageRedactListRequest();
			request.setMessages(messageList);
			LOGGER.info("requesting redaction for messages={} with campaign={}", messageList, om.writeValueAsString(OptOutCampaign.DEFAULT_NER));
			request.setCampaignID(Long.valueOf(OptOutCampaign.DEFAULT_NER.getCampaignId()));
			HttpEntity<MessageRedactListRequest> requestEntity = new HttpEntity<MessageRedactListRequest>(request, headers);
			LOGGER.info("making request to external redact api with message_redact_request={}", om.writeValueAsString(request));
			MessageRedactListResponse response = restTemplate.exchange(messageRedactUrl, HttpMethod.POST, requestEntity, MessageRedactListResponse.class).getBody();
			LOGGER.info("request to external redact api completed with message_redact_response={}", om.writeValueAsString(response));

			if(response.getMessages() != null && !response.getMessages().isEmpty()) {
				redactedMessageBody =  response.getMessages().get(0).getMessageBody();
			} else {
                LOGGER.error("Redact Response from flask API did not have messages set for message_uuid={} message_body={}", messageUUID, messageBody);
                ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), "Redact Response from flask API did not have messages set");
                errors.add(apiError);
                messageRedactResponse.setErrors(errors);
            }
		} catch(Exception e) {
            LOGGER.error("Exception in getting redacted message for message_uuid={} message_body={}", messageUUID, messageBody, e);
            ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), "Exception in getting redacted message from Flask API" + e.getMessage());
            errors.add(apiError);
            messageRedactResponse.setErrors(errors);
		}
		return redactedMessageBody;
    }
}
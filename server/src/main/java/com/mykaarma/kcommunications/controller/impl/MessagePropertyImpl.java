package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.MessageType;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.rabbit.MessageUpdateOnEvent;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCustomerApiHelperV2;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications_model.enums.Event;
import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;

@Service
public class MessagePropertyImpl {
	
	@Autowired
	private KCustomerApiHelperV2 kCustomerApiHelperV2;
	
	@Autowired
	private KManageApiHelper kManageApiHelper;
	
	@Autowired 
	private ValidateRequest validateRequest;
	
	@Autowired
	private MessageRepository messageRepository;
	
	@Autowired
	private Helper helper;
	
	@Autowired
	private RabbitHelper rabbitHelper;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(MessagePropertyImpl.class);	
	
	public ResponseEntity<Response> updateMessageForEvent(String departmentUUID, String messageUUID, Event event) throws Exception{
		
		Message message = messageRepository.findByuuid(messageUUID);
		switch(event) {
		case MANUAL_FOLLOWUP: 
			return processFollowupMessage(message, departmentUUID, messageUUID);
		
		default:

			throw new Exception(String.format("Unknown event={} for message_uuid={} ", event.name(), messageUUID));
		}
		
	}
	
	public ResponseEntity<Response> processFollowupMessage(Message message, String departmentUUID, String messageUUID) throws Exception {
		Response response = new Response();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		Long departmentID = generalRepository.getDepartmentIDForUUID(departmentUUID);
		validateRequest.applyManualFollowUpEventRules(departmentUUID, response, message, departmentID, messageUUID);
		if(response.getErrors()!=null && !response.getErrors().isEmpty()) {
			LOGGER.info("Bad request in processFollowupMessage error={}", new ObjectMapper().writeValueAsString(response.getErrors()));
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		}
		MessageUpdateOnEvent messageUpdateOnEvent = new MessageUpdateOnEvent();
		messageUpdateOnEvent.setEvent(Event.MANUAL_FOLLOWUP);
		messageUpdateOnEvent.setMessageUUID(messageUUID);
		messageUpdateOnEvent.setExpiration(0);
		pushToQueue(messageUpdateOnEvent);
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
	
	private void pushToQueue(MessageUpdateOnEvent messageUpdateOnEvent) throws Exception {
		rabbitHelper.pushMessageToEventProcessingQueue(messageUpdateOnEvent);
	}
		
}

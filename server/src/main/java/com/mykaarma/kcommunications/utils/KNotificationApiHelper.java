package com.mykaarma.kcommunications.utils;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.Twilio.CallStatus;
import com.mykaarma.kcommunications.model.kne.KNotificationMessage;

@Service
public class KNotificationApiHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(KNotificationApiHelper.class);

	@Autowired
	RestTemplate restTemplate;
	
	@Value("${notification-engine-url}")
	private String notificationEngineUrl;
	
	public void pushToPubnub(KNotificationMessage kNotificationMessage) {
		
		String url = notificationEngineUrl+"/pushToPubnub";
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		restTemplate.postForObject(builder.build().encode().toUri(), kNotificationMessage, Boolean.class);
	}
	
	public KNotificationMessage getKNotificationMessage(Long messageID, Long customerID, Long dealerAssociateID, Long dealerID, Long dealerDepartmentID, Set<Long> concernedDAIDSet,
			Set<Long> notificationDAIDSet, Set<Long> viewDAIDSet, Set<Long> phoneNotificationDAIDSet, Set<Long> internalSubscriptionDAIDSet, String eventType, Boolean notifierNotification, Boolean viewNotification) {
		 KNotificationMessage kNotificationMessage = new KNotificationMessage();
		 kNotificationMessage.setMessageID(messageID);
         kNotificationMessage.setCustomerID(customerID);
		 kNotificationMessage.setDealerID(dealerID);
		 kNotificationMessage.setMessageUUID(UUID.randomUUID().toString());
		 kNotificationMessage.setConcernedDAIDSet(concernedDAIDSet);
		 kNotificationMessage.setNotificationDAIDSet(phoneNotificationDAIDSet);
		 kNotificationMessage.setDepartmentID(dealerDepartmentID);
		 kNotificationMessage.setNotificationDAIDSet(notificationDAIDSet);
		 kNotificationMessage.setPhoneNotificationDAIDSet(phoneNotificationDAIDSet);
		 kNotificationMessage.setViewDAIDSet(viewDAIDSet);
		 kNotificationMessage.setInternalSubscriptionDAIDSet(internalSubscriptionDAIDSet);
		 kNotificationMessage.setEventType(eventType);
		 kNotificationMessage.setNotifierNotification(notifierNotification);
		 kNotificationMessage.setViewNotification(viewNotification);
		 return kNotificationMessage;
	}
	
	public void broadcastCallUpdateEvent(Long dealerID,String callSid, int callStatusId) throws Exception {
		 
		  LOGGER.info("Inside broadcastCallUpdateEvent call_sid={}  dealer_id={} call_status_id={} ",callSid, dealerID, callStatusId);
		  
		  
		  //Push status to PubNub
		  Long callStatusIDLong = Long.valueOf(callStatusId);
		  HashMap<String, Object> params=new HashMap<String, Object>();
		  params.put("dealerid", dealerID);
		  params.put("callstatus", CallStatus.getByID(callStatusIDLong).getMessage());
		  params.put("callsid", callSid);
		  params.put("messageType", "BROADCAST_CALL_UPDATE");
		 
		  String url =  notificationEngineUrl + "/publish";
		  UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		  
		  String paramsAsString = new ObjectMapper().writeValueAsString(params);
		  
		  MessageRequest messageRequest = new MessageRequest();
		  messageRequest.setChannel(String.valueOf(dealerID));
		  messageRequest.setFrom(String.valueOf(dealerID));
		  messageRequest.setMessageBody(paramsAsString);
		
		  boolean published = restTemplate.postForObject(url, messageRequest, Boolean.class);
		  
		  if(published) {
			  LOGGER.info("Message successfully pushed call_sid = {}  dealer_id={} call_status_id = {}", callSid, dealerID, callStatusId);
			  
		  }else {
			  LOGGER.warn("Error while pushing message to PubNub for call_sid={}  dealer_id={} call_status_id = {}", callSid, dealerID, callStatusId);
		  }
		  return;
	}
}

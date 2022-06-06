package com.mykaarma.kcommunications.utils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.mvc.ManualNoteSaveEventData;
import com.mykaarma.kcommunications_model.common.User;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.request.SubscriptionRequest;
import com.mykaarma.kcommunications_model.mvc.SubscriptionList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.CustomerSentiment;
import com.mykaarma.global.DraftTypes;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.mvc.CustomerMessagingLockEventData;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.mvc.ExtendedDealerOrderSaveEventData;
import com.mykaarma.kcommunications.model.mvc.ExtendedThreadSaveEventData;
import com.mykaarma.kcommunications.model.mvc.FilterDataRemovalRequest;
import com.mykaarma.kcommunications.model.mvc.MessageSaveEventData;
import com.mykaarma.kcommunications.model.mvc.SubscriptionSaveEventData;
import com.mykaarma.kcommunications.model.mvc.ThreadSaveEventData;
import com.mykaarma.kcommunications_model.common.NotificationWithoutCustomerEventData;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.request.SendNotificationWithoutCustomerRequest;
import com.mykaarma.kcommunications_model.request.ThreadInWaitingForResponseQueueRequest;

@Service
public class MessagingViewControllerHelper {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(MessagingViewControllerHelper.class);
	
	@Value("${view-controller-url}")
	private String messagingViewControllerUrl;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	private Helper helper;

	@Autowired
	private GeneralRepository generalRepository;
	
	public EventName getEventName(Message m) {
		
		if("F".equalsIgnoreCase(m.getMessageType()) && "F".equalsIgnoreCase(m.getMessagePurpose())) {
			if(m.getDraftMessageMetaData()!=null && m.getDraftMessageMetaData().getStatus().equalsIgnoreCase(DraftTypes.SCHEDULED.name()) || 
					m.getDraftMessageMetaData()!=null && m.getDraftMessageMetaData().getStatus().equalsIgnoreCase(DraftTypes.DRAFTED.name())) {
				return EventName.DRAFT_MESSAGE_SAVED;
			} else if (m.getDraftMessageMetaData()!=null && m.getDraftMessageMetaData().getStatus().equalsIgnoreCase(DraftTypes.FAILED.name())) {
				return EventName.DRAFT_MESSAGE_FAILED;
			}
		}
		
		if (MessageType.NOTE.getMessageType().equalsIgnoreCase(m.getMessageType()) && !m.getIsManual()) {
			return EventName.SYSTEM_NOTIFICATION;
		}

		if (MessageType.NOTE.getMessageType().equalsIgnoreCase(m.getMessageType()) &&
				MessageProtocol.NONE.getMessageProtocol().equalsIgnoreCase(m.getProtocol()) && m.getIsManual()) {
			return EventName.MANUAL_NOTE;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "T".equalsIgnoreCase(m.getProtocol())) {
			return EventName.INTERNAL_APP_NOTIFICATION;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "X".equalsIgnoreCase(m.getProtocol()) && "FDNOTIFY".equals(m.getMessagePurpose())) {
			return EventName.FRAUD_DETECTION_MESSAGE;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "S".equalsIgnoreCase(m.getProtocol())) {
			return EventName.PAYMENT_BLOCKED_NOTIFICATION;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "B".equalsIgnoreCase(m.getProtocol())) {
			return EventName.FRAUD_DETECTION_NOTIFICATION;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "R".equalsIgnoreCase(m.getProtocol()) ) {
			return EventName.READY_TO_SEND_NOTIFICATION_NEW;
		}
		if("I".equalsIgnoreCase(m.getMessageType()) && "P".equalsIgnoreCase(m.getProtocol()) ) {
			return EventName.PAYMENT_NOTIFICATION_NEW;
		}
		
		if("S".equalsIgnoreCase(m.getMessageType()) && "W".equalsIgnoreCase(m.getMessagePurpose())) {
			return EventName.AUTO_WELCOME_TEXT_SENT;
		}

		if("D".equalsIgnoreCase(m.getMessageType()) && "F".equalsIgnoreCase(m.getMessagePurpose())) {
			return EventName.DRAFT_MESSAGE_DISCARD;
		}
		if("S".equalsIgnoreCase(m.getMessageType()) && "F".equalsIgnoreCase(m.getMessagePurpose())) {
			return EventName.DRAFT_MESSAGE_SENT;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "C".equalsIgnoreCase(m.getProtocol())) {
			return EventName.INTERNAL_NOTE_NEW;
		}
		
		if("S".equalsIgnoreCase(m.getMessageType())) {
			return EventName.MESSAGE_SENT;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "M".equalsIgnoreCase(m.getProtocol()) ) {
			return EventName.MDL_NOTIFICATION;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && !"P".equalsIgnoreCase(m.getProtocol())
				&& !"R".equalsIgnoreCase(m.getProtocol()) && !"S".equalsIgnoreCase(m.getProtocol()) 
				&& !"A".equalsIgnoreCase(m.getProtocol()) && !"M".equalsIgnoreCase(m.getProtocol()) &&m.getIsManual()) {
			return EventName.INCOMING_MESSAGE_NEW;
		}
		
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "H".equalsIgnoreCase(m.getProtocol())) {
			return EventName.INCOMING_MESSAGE_NEW;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "U".equalsIgnoreCase(m.getProtocol()) ) {
			return EventName.VEHICLE_PICKUPDROPOFF_NOTIFICATION;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "G".equalsIgnoreCase(m.getProtocol()) ) {
			return EventName.APPOINTMENT_NOTIFICATION;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "N".equalsIgnoreCase(m.getProtocol()) ) {
			return EventName.INSPECTION_NOTIFICATION;
		}
		
		if("I".equalsIgnoreCase(m.getMessageType()) && "O".equalsIgnoreCase(m.getProtocol()) ) {
			return EventName.APPOINTMENT_CUSTOMER_ARRIVAL_NOTIFICATION;
		}
			
		return null;
	
	}
	
	public void publishThreadCreatedEvent(com.mykaarma.kcommunications.model.jpa.Thread thread, Long dealerID) {
		ThreadSaveEventData threadSaveEventData=new ThreadSaveEventData();			
		threadSaveEventData.setCurrentThreadOwnerDAID(thread.getDealerAssociateID());
		threadSaveEventData.setCustomerID(thread.getCustomerID());
		threadSaveEventData.setDealerDepartmentID(thread.getDealerDepartmentID());
		threadSaveEventData.setDealerID(dealerID);
		threadSaveEventData.setLastMessageOn(thread.getLastMessageOn());
		threadSaveEventData.setThreadID(thread.getId());
		threadSaveEventData.setThreadUpdatedDate(new Date());
		threadSaveEventData.setEventName(EventName.THREAD_CREATED.name());
		threadSaveEventData.setEventRaisedBy(thread.getDealerAssociateID());
		publishThreadSaveEnvent(threadSaveEventData);
	}
	
	public void publishUpsetCustomerEvent(com.mykaarma.kcommunications.model.jpa.Thread thread, Long dealerID, String sentiment) {
		ThreadSaveEventData threadSaveEventData=new ThreadSaveEventData();			
		threadSaveEventData.setCurrentThreadOwnerDAID(thread.getDealerAssociateID());
		threadSaveEventData.setCustomerID(thread.getCustomerID());
		threadSaveEventData.setDealerDepartmentID(thread.getDealerDepartmentID());
		threadSaveEventData.setDealerID(dealerID);
		threadSaveEventData.setLastMessageOn(thread.getLastMessageOn());
		threadSaveEventData.setThreadID(thread.getId());
		threadSaveEventData.setThreadUpdatedDate(new Date());
		
		if (CustomerSentiment.UPSET.name().equalsIgnoreCase(sentiment))
			threadSaveEventData.setEventName(EventName.MARK_CUSTOMER_UPSET.name());
		else
			threadSaveEventData.setEventName(EventName.MARK_CUSTOMER_NOT_UPSET.name());
		
		threadSaveEventData.setEventRaisedBy(null);
		publishThreadSaveEnvent(threadSaveEventData);
	}
	
	public void publishDelegationEvent(com.mykaarma.kcommunications.model.jpa.Thread thread, Long delegatedFrom, Message message) {
		ThreadSaveEventData threadSaveEventData=new ThreadSaveEventData();
		threadSaveEventData.setCurrentThreadOwnerDAID(thread.getDealerAssociateID());
		threadSaveEventData.setCustomerID(thread.getCustomerID());
		threadSaveEventData.setDealerDepartmentID(thread.getDealerDepartmentID());
		threadSaveEventData.setDealerID(message.getDealerID());
		threadSaveEventData.setIsThreadDelegated(true);
		threadSaveEventData.setLastMessageOn(thread.getLastMessageOn());
		threadSaveEventData.setPreviousThreadOwnerDAID(delegatedFrom);
		threadSaveEventData.setThreadID(thread.getId());
		threadSaveEventData.setThreadUpdatedDate(new Date());
		threadSaveEventData.setStatus("1".equalsIgnoreCase(message.getDeliveryStatus()));
		threadSaveEventData.setEventRaisedBy(message.getDealerAssociateID());
		threadSaveEventData.setEventName(EventName.THREAD_DELEGATED.name());
		publishThreadSaveEnvent(threadSaveEventData);
	}
	
	public void publishMessageSaveEvent(Message message, EventName eventName, com.mykaarma.kcommunications.model.jpa.Thread thread, Boolean updateThreadTimestamp) {
		MessageSaveEventData messageSaveEventData=new MessageSaveEventData();
		messageSaveEventData.setEventName(eventName.name());
		messageSaveEventData.setCustomerID(message.getCustomerID());  
		
		messageSaveEventData.setDealerDepartmentID(message.getDealerDepartmentId());
		messageSaveEventData.setDealerID(message.getDealerID());
		messageSaveEventData.setIsDelayed(false);
		messageSaveEventData.setMessageBody(message.getMessageExtn().getMessageBody());
		messageSaveEventData.setMessageSubject(message.getMessageExtn().getSubject());
		messageSaveEventData.setIsManual(message.getIsManual());
		messageSaveEventData.setMessageDate(message.getReceivedOn());
		messageSaveEventData.setMessageDealerAssociateID(message.getDealerAssociateID());
		messageSaveEventData.setMessageID(message.getId());
		messageSaveEventData.setMessagePurpose(message.getMessagePurpose());
		messageSaveEventData.setMessageType(message.getMessageType());
		messageSaveEventData.setProtocol(message.getProtocol());
		messageSaveEventData.setThreadDealerAssociateID(thread.getDealerAssociateID());
		messageSaveEventData.setThreadID(thread.getId());
		messageSaveEventData.setStatus("1".equalsIgnoreCase(message.getDeliveryStatus()));
		messageSaveEventData.setEventRaisedBy(message.getDealerAssociateID());
		messageSaveEventData.setUpdateThreadTimestamp(updateThreadTimestamp);
		publishMessageSaveEvent(messageSaveEventData);
	}
	
	public void publishHistoricalData(Message message, EventName eventName, com.mykaarma.kcommunications.model.jpa.Thread thread, Boolean updateThreadTimestamp) {
		MessageSaveEventData messageSaveEventData=new MessageSaveEventData();
		messageSaveEventData.setEventName(eventName.name());
		messageSaveEventData.setCustomerID(message.getCustomerID());  
		
		messageSaveEventData.setDealerDepartmentID(message.getDealerDepartmentId());
		messageSaveEventData.setDealerID(message.getDealerID());
		messageSaveEventData.setIsDelayed(false);
		messageSaveEventData.setMessageBody(message.getMessageExtn().getMessageBody());
		messageSaveEventData.setMessageDate(message.getReceivedOn());
		messageSaveEventData.setMessageDealerAssociateID(message.getDealerAssociateID());
		messageSaveEventData.setMessageID(message.getId());
		messageSaveEventData.setMessagePurpose(message.getMessagePurpose());
		messageSaveEventData.setMessageType(message.getMessageType());
		messageSaveEventData.setProtocol(message.getProtocol());
		messageSaveEventData.setThreadDealerAssociateID(thread.getDealerAssociateID());
		messageSaveEventData.setThreadID(thread.getId());
		messageSaveEventData.setStatus("1".equalsIgnoreCase(message.getDeliveryStatus()));
		messageSaveEventData.setEventRaisedBy(message.getDealerAssociateID());
		messageSaveEventData.setUpdateThreadTimestamp(updateThreadTimestamp);
		publishHistoricalData(messageSaveEventData);
	}
	
	public String publishNotificationWithoutCustomerEvent(SendNotificationWithoutCustomerRequest request, EventName eventName,
			Long dealerAssociateId, Long dealerId, Long dealerDepartmentId) {
		NotificationWithoutCustomerEventData eventData = new NotificationWithoutCustomerEventData();
		eventData.setEventName(eventName.name());
		eventData.setDate(new Date());
		eventData.setEventRaisedBy(dealerAssociateId);
		eventData.setDealerID(dealerId);
		eventData.setDealerDepartmentID(dealerDepartmentId);
		eventData.setNotificationMessageUUID(helper.getBase64EncodedSHA256UUID());
		eventData.setMessageAttributes(request.getMessageAttributes());
		eventData.setNotificationAttributes(request.getNotificationAttributes());
		publishNotificationWithoutCustomerEvent(eventData);
		return eventData.getNotificationMessageUUID();
	}
	
	public Boolean publishSubscriptionEvent(Long customerID, Long dealerID,
			Long departmentID, List<Long> internalSubscribers,
			List<Long> externalSubscribers, List<Long> internalSubscribersRevoked, 
			List<Long> externalSubscribersRevoked, Long dealerAssociateID) {
		SubscriptionSaveEventData eventData=new SubscriptionSaveEventData();
		eventData.setCustomerID(customerID);
		eventData.setDealerDepartmentID(departmentID);
		eventData.setDealerID(dealerID);
		eventData.setEventRaisedBy(dealerAssociateID);
		if(internalSubscribersRevoked!=null) {
			for(Long is:internalSubscribersRevoked) {
				eventData.setRevokedDAID(is);
				eventData.setEventName(EventName.INTERNAL_SUBSRICTION_REVOKED.name());
				publishSubscriptionSaveEvent(eventData);
			}
		}
		if(externalSubscribersRevoked!=null) {
			for(Long es:externalSubscribersRevoked) {
				eventData.setRevokedDAID(es);
				eventData.setEventName(EventName.EXTERNAL_SUBSRIPTION_REVOKED.name());
				publishSubscriptionSaveEvent(eventData);
			}
		}
		if(internalSubscribers!=null) {
			for(Long is:internalSubscribers) {
				eventData.setSubscriberDAID(is);
				eventData.setEventName(EventName.INTERNAL_SUBSRICTION_ADDED.name());
				publishSubscriptionSaveEvent(eventData);
			}
		}
		if(externalSubscribers!=null) {
			for(Long es:externalSubscribers) {
				eventData.setSubscriberDAID(es);
				eventData.setEventName(EventName.EXTERNAL_SUBSRIPTION_ADDED.name());
				publishSubscriptionSaveEvent(eventData);
			}
		}
		
		return true;
	}
	public void publishCustomerMessageLockEvent(Message message) {
		CustomerMessagingLockEventData eventData = new CustomerMessagingLockEventData();
		eventData.setCustomerID(message.getCustomerID());
		eventData.setDealerDepartmentID(message.getDealerDepartmentId());
		eventData.setDealerID(message.getDealerID());
		eventData.setEventName(EventName.CUSTOMER_MESSAGING_LOCKED.name());
		eventData.setEventRaisedBy(message.getDealerAssociateID());
		eventData.setIsDealerAssociateSpecficEvent(true);
		eventData.setLockedByDealerAssociateID(message.getDealerAssociateID());
		publishCustomerMessageLockEvent(eventData);
	}
	
	public void publishMessageSentEvent() {
		
	}
	
	public void draftSavedEvent() {
		
	}
	
	public Boolean publishThreadOpenEnvent(ExtendedThreadSaveEventData extendedThreadSaveEventData) {
		Boolean response = makeRequest("/registerthreadopenevent", extendedThreadSaveEventData);
		return response;
	}
	
	public Boolean publishThreadSaveEnvent(ThreadSaveEventData threadSaveEventData) {
		Boolean response = makeRequest("/registerthreadevent", threadSaveEventData);
		return response;
	}
	
	
	public Boolean publishMessageSaveEvent(MessageSaveEventData messageSaveEventData) {
		Boolean response = makeRequest("/registermessageevent", messageSaveEventData);
		return response;
	}
	
	public Boolean publishDealerOrderCustomerUpdateEvent(ExtendedDealerOrderSaveEventData extendedDealerOrderSaveEventData) {
		Boolean response = makeRequest("/registerdealerordercustomerupdateevent", extendedDealerOrderSaveEventData);
		return response;
	}
	
	private Boolean publishNotificationWithoutCustomerEvent(NotificationWithoutCustomerEventData eventData) {
		return makeRequest("/notificationwithoutcustomer", eventData);
	}
	
	private Boolean publishCustomerMessageLockEvent(CustomerMessagingLockEventData eventData){
		Boolean response = makeRequest("/registercustomermessaginglockevent", eventData);
		return response;
	}
	
	public Boolean publishSubscriptionSaveEvent(SubscriptionSaveEventData subscriptionSaveEventData) {
		Boolean response = makeRequest("/registersubscriptionevent", subscriptionSaveEventData);
		return response;
	}
	
	public Boolean updateFilterData(FilterDataRemovalRequest filterDataRemovalRequest) {
		Boolean response = makeRequest("/filter/update", filterDataRemovalRequest);
		return response;
	}
	
	public Boolean publishHistoricalData(MessageSaveEventData messageSaveEventData) {
		Boolean response = makeRequest("/registerhistoricalmessageevent", messageSaveEventData);
		return response;
	}
	private Boolean makeRequest(String path, Serializable eventData){
		ObjectMapper mapper= new ObjectMapper();
		
		if(messagingViewControllerUrl.isEmpty()){
			try {
				LOGGER.error("MDVCCI E001 mykaarma.messageviewcontroller.url is not configured. Event Data {}", mapper.writeValueAsString(eventData));
			} catch (JsonProcessingException e) {
				LOGGER.error("MDVCCI E002 Error while converting event data to JSON", e);
			}
			return false;
		} 
		
		try {
			LOGGER.info("Path{} Event Data {}", path, mapper.writeValueAsString(eventData));
		} catch (JsonProcessingException e) {
			LOGGER.error("Error while printing event data", e);
		}
		
		Boolean response=false;
		try {
			response = restTemplate.postForObject(messagingViewControllerUrl+path, eventData, Boolean.class);
		} catch (Exception e) {
			try {
				LOGGER.error("MDVCCI E003. Failed to make POST request. Event Data {}",mapper.writeValueAsString(eventData),e);
			} catch (JsonProcessingException e1) {
				LOGGER.error("MDVCCI E002 Error while converting event data to JSON", e1);
			}
		}
		return response;
	}

	public SubscriptionList getSubscriptionForCustomer(SubscriptionRequest subscriptionRequest) {
		String url = messagingViewControllerUrl+"/getsubscriptionlist";
		LOGGER.info("getSubscriptionForCustomer for subscriptionRequest={} url={}", subscriptionRequest, url);

		SubscriptionList subscriptionList = null;
		try {
			RestTemplate restTemplate = new RestTemplate();
			subscriptionList = restTemplate.postForObject(url, subscriptionRequest, SubscriptionList.class);
		} catch (Exception e) {
			LOGGER.error("Error in getting subscription for subscriptionRequest={}", subscriptionRequest, e);
		}

		LOGGER.info("getSubscriptionForCustomer for subscriptionRequest={} subscriptionList={}", subscriptionRequest, subscriptionList);
		return subscriptionList;
	}

	public void updateSubscriptionsForCustomer(Long customerId, Long threadOwnerId, Long dealerDepartmentId) throws Exception{
		
		String url = messagingViewControllerUrl+"/updateCustomerSubscriptions?customerId="+customerId+"&dealerAssociateId="+threadOwnerId+"&dealerDepartmentId="+dealerDepartmentId;
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.delete(url);
	}

	public Long getThreadOwnerForCustomer(Long customerID, Long dealerDepartmentID) {
		
		String url = messagingViewControllerUrl+"/getthreadowner?customerID="+customerID+"&departmentID="+dealerDepartmentID;
		RestTemplate restTemplate = new RestTemplate();
		LOGGER.info("getting thread owner for ustomer_id={} department_id={} url={}",customerID,
							dealerDepartmentID, url);
		Long dealerAssociateID = restTemplate.getForObject(url, Long.class);
		LOGGER.info("thread_owner_da_id={} for customer_id={} department_id={}", dealerAssociateID, customerID,
				dealerDepartmentID);
		return dealerAssociateID;
	}

	public Boolean checkIfThreadIsInWaitingForResponseQueue(ThreadInWaitingForResponseQueueRequest threadInWaitingForResponseQueueRequest) {
		
		String url = messagingViewControllerUrl+"/waitingforreponse/status";
		RestTemplate restTemplate = new RestTemplate();
		LOGGER.info("getting thread wfr helper status for customer_id={} department_id={} url={}", 
				threadInWaitingForResponseQueueRequest.getCustomerID(), threadInWaitingForResponseQueueRequest.getDealerDepartmentID(), url);
		Boolean threadIsInWaitingForResponseQueue = restTemplate.postForObject(url, threadInWaitingForResponseQueueRequest, Boolean.class);
		LOGGER.info("checkIfThreadIsInWaitingForResponseQueue={} for customer_id={} department_id={}", threadIsInWaitingForResponseQueue, threadInWaitingForResponseQueueRequest.getCustomerID(), 
				threadInWaitingForResponseQueueRequest.getDealerDepartmentID());
		return threadIsInWaitingForResponseQueue;
	}

	public ManualNoteSaveEventData prepareInternalCommentSaveEvent(Message message, com.mykaarma.kcommunications.model.jpa.Thread thread,
			 List<User> usersToNotify, EventName eventName) {

		String dealerAssociateUuid = generalRepository.getDealerAssociateUuidFromDealerAssociateId(message.getDealerAssociateID());
		String departmentUuid = generalRepository.getDepartmentUUIDForDepartmentID(message.getDealerDepartmentId());

		User eventRaiseBy = helper.getUserForDealerAssociate(dealerAssociateUuid, departmentUuid);

		ManualNoteSaveEventData internalNoteSaveEventData = new ManualNoteSaveEventData();
		internalNoteSaveEventData.setUsersNotificationList(usersToNotify);
		internalNoteSaveEventData.setDealerUuid(generalRepository.getDealerUUIDFromDealerId(message.getDealerID()));
		internalNoteSaveEventData.setDealerDepartmentUuid(departmentUuid);
		internalNoteSaveEventData.setMessageUuid(message.getUuid());
		internalNoteSaveEventData.setCustomerUuid(generalRepository.getCustomerUUIDFromCustomerID(message.getCustomerID()));
		internalNoteSaveEventData.setThreadId(thread.getId());
		internalNoteSaveEventData.setEventRaiseBy(eventRaiseBy);
		internalNoteSaveEventData.setEventName(eventName.name());

		return internalNoteSaveEventData;
	}

	public Boolean publishInternalCommentSaveEvent(ManualNoteSaveEventData internalNoteSaveEventData) {
		return makeRequest("/registermanualnoteevent", internalNoteSaveEventData);
	}
	
}

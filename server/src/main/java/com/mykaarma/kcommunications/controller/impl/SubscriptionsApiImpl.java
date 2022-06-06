package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.mvc.SubscriptionSaveEventData;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications_model.common.CommunicationCategory;
import com.mykaarma.kcommunications_model.common.Event;
import com.mykaarma.kcommunications_model.common.Subscriber;
import com.mykaarma.kcommunications_model.common.SubscriberInfo;
import com.mykaarma.kcommunications_model.common.User;
import com.mykaarma.kcommunications_model.common.UserEvent;
import com.mykaarma.kcommunications_model.enums.CategoryEvent;
import com.mykaarma.kcommunications_model.enums.CommunicationCategoryName;
import com.mykaarma.kcommunications_model.enums.EditorType;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.NotificationType;
import com.mykaarma.kcommunications_model.mvc.SubscriptionList;
import com.mykaarma.kcommunications_model.request.SubscriptionRequest;
import com.mykaarma.kcommunications_model.request.SubscriptionSaveRequest;
import com.mykaarma.kcommunications_model.request.ThreadFollowRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.SubscriptionSaveResponse;
import com.mykaarma.kcommunications_model.response.ThreadFollowResponse;
import com.mykaarma.kcommunications_model.response.ThreadFollowers;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;

@Service
public class SubscriptionsApiImpl {

	@Autowired
	ValidateRequest validateRequest;
	
	@Autowired 
	GeneralRepository generalRepo;
	
	@Autowired
	Helper helper;
	
	@Autowired
	KCommunicationsUtils kCommUtils;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	MessagingViewControllerHelper messagingViewController;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(SubscriptionsApiImpl.class);	
	
	public ResponseEntity<SubscriptionSaveResponse> saveSubscriptions(String departmentUuid, String customerUuid, SubscriptionSaveRequest subscriptionSaveRequest) 
			throws Exception{
		
		LOGGER.info("received request for saving subbscriptions = {}", new ObjectMapper().writeValueAsString(subscriptionSaveRequest));
		SubscriptionSaveResponse saveSubscriptionResponse = new SubscriptionSaveResponse();
		saveSubscriptionResponse = validateRequest.validateSubscriptionSaveRequest(subscriptionSaveRequest);
		SubscriptionSaveEventData subscriptionSaveEventData = new SubscriptionSaveEventData();
		
			if(saveSubscriptionResponse!=null && saveSubscriptionResponse.getErrors()!=null) {
			return new ResponseEntity<SubscriptionSaveResponse>(saveSubscriptionResponse,  HttpStatus.BAD_REQUEST);
		}
		
		Long customerId = generalRepo.getCustomerIDForUUID(customerUuid);
		Long departmentId = generalRepo.getDepartmentIDForUUID(departmentUuid);
		Long dealerId = generalRepo.getDealerIDFromDepartmentUUID(departmentUuid);
		
		
		subscriptionSaveEventData = kCommUtils.getSubscriptionSaveEventDataFromSubscriberInfo(dealerId, customerId, departmentId, customerUuid);
		
		subscriptionSaveEventData.setIsHistoricalMessage(subscriptionSaveRequest.getIsHistoricalSubbscription());
		
		List<SubscriberInfo> externalSubscriberAdded = subscriptionSaveRequest.getExternalSubscriptionsAdded();
		List<SubscriberInfo> internalSubscriberAdded = subscriptionSaveRequest.getInternalSubscriptionsAdded();
		List<SubscriberInfo> externalSubscriberRevoked = subscriptionSaveRequest.getExternalSubscriptionsRevoked();
		List<SubscriberInfo> internalSubscriberRevoked = subscriptionSaveRequest.getInternalSubscriptionsRevoked();
		
		if(!helper.isListEmpty(externalSubscriberAdded)) {
			//validate  if fields are correctly populated
			for(SubscriberInfo externalAddedSubscriber: externalSubscriberAdded) {
				
				try {
					populateSubscriptionSaveObjectAndCallMessagingViewController(EventName.EXTERNAL_SUBSRIPTION_ADDED, externalAddedSubscriber,
						subscriptionSaveEventData, departmentId);
				}
				catch(Exception e) {
					LOGGER.error("unable to save EXTERNAL_SUBSRIPTION for customer_id={}, department_id={} dealer_id={}", customerId,
							departmentId, dealerId, e);
				}
			}
		}
		if(!helper.isListEmpty(internalSubscriberAdded)) {
			for(SubscriberInfo internalAddedSubscriber: internalSubscriberAdded) {
				
				try {
					populateSubscriptionSaveObjectAndCallMessagingViewController(EventName.INTERNAL_SUBSRICTION_ADDED, internalAddedSubscriber,
							subscriptionSaveEventData, departmentId);
				}
				catch(Exception e) {
					LOGGER.error("unable to save INTERNAL_SUBSRIPTION for customer_id={}, department_id={} dealer_id={}", customerId,
							departmentId, dealerId, e);
				}
			}	
		}
		if(!helper.isListEmpty(externalSubscriberRevoked)) {
			
			for(SubscriberInfo externalRevokedSubscriber: externalSubscriberRevoked) {
				
				try {
					populateSubscriptionSaveObjectAndCallMessagingViewController(EventName.EXTERNAL_SUBSRIPTION_REVOKED, externalRevokedSubscriber,
						subscriptionSaveEventData, departmentId);
				}
				catch(Exception e) {
					LOGGER.error("unable to remove EXTERNAL_SUBSRIPTION for customer_id={}, department_id={} dealer_id={}", customerId,
							departmentId, dealerId, e);
				}
			}	
		}
		if(!helper.isListEmpty(internalSubscriberRevoked)) {
			
			for(SubscriberInfo internaRevokedSubscriber: internalSubscriberRevoked) {
				
				try {
					populateSubscriptionSaveObjectAndCallMessagingViewController(EventName.INTERNAL_SUBSRICTION_REVOKED, internaRevokedSubscriber,
						subscriptionSaveEventData, departmentId);
				}
				catch(Exception e) {
					LOGGER.error("unable to remove INTERNAL_SUBSRIPTION for customer_id={}, department_id={} dealer_id={}", customerId,
							departmentId, dealerId, e);
				}
			}	
		}
		
		return new ResponseEntity<SubscriptionSaveResponse>(saveSubscriptionResponse,  HttpStatus.OK);
		
	}
	
	public void populateSubscriptionSaveObjectAndCallMessagingViewController(EventName eventName, SubscriberInfo subscriberInfo, 
			SubscriptionSaveEventData subscriptionSaveEventData, Long departmentId) throws Exception{
		
		LOGGER.info("publishing subscription save event for event_name={} customer_id={} dealer_id={} dealer_Associate_id={} "
				, eventName, subscriptionSaveEventData.getCustomerID(), subscriptionSaveEventData.getDealerID(), subscriptionSaveEventData.getDealerDepartmentID());
		
		subscriptionSaveEventData.setEventName(eventName.name());
		Long dealerAssociateId = generalRepo.getDealerAssociateIDForUserUUID(subscriberInfo.getUserUuid(), departmentId);
		if(EventName.EXTERNAL_SUBSRIPTION_ADDED.equals(eventName) || EventName.INTERNAL_SUBSRICTION_ADDED.equals(eventName)) {
			
			subscriptionSaveEventData.setSubscriberDAID(dealerAssociateId);
			if(EventName.EXTERNAL_SUBSRIPTION_ADDED.equals(eventName)) {
				if(subscriberInfo.getIsAsignee()!=null && subscriberInfo.getIsAsignee()) {
					subscriptionSaveEventData.setIsAssignee(true);
				}
				else {
					subscriptionSaveEventData.setIsAssignee(false);
				}
			}
		}
		else if(EventName.EXTERNAL_SUBSRIPTION_REVOKED.equals(eventName) || EventName.INTERNAL_SUBSRICTION_REVOKED.equals(eventName)) {
			subscriptionSaveEventData.setRevokedDAID(dealerAssociateId);
		}
		
		messagingViewController.publishSubscriptionSaveEvent(subscriptionSaveEventData);
		LOGGER.info("subscriber successfully pushed to mvc for event_name={} customer_id={} dealer_id={} dealer_Associate_id={} "
								, eventName, subscriptionSaveEventData.getCustomerID(), subscriptionSaveEventData.getDealerID(), subscriptionSaveEventData.getDealerDepartmentID());
	}
	
	public Long getSubscribersForCustomer(Long customerID, Long dealerDepartmentID){
		
		return messagingViewController.getThreadOwnerForCustomer(customerID, dealerDepartmentID);
	}

	public ResponseEntity<ThreadFollowResponse> followOrUnfollowThread(String departmentUUID, String customerUUID,
			ThreadFollowRequest threadFollowRequest) {
		
		ThreadFollowResponse threadFollowResponse = new ThreadFollowResponse();
		
		try {
			LOGGER.info("follow thread request received for threadFollowRequest={}",new ObjectMapper().writeValueAsString(threadFollowRequest));
			threadFollowResponse = validateRequest.validateThreadFollowRequest(threadFollowRequest);
			List<User> users = new ArrayList<User>();
			Set<Event> events = new HashSet<Event>();
			
			if(threadFollowResponse!=null && threadFollowResponse.getErrors()!=null && !threadFollowResponse.getErrors().isEmpty()) {
				return new ResponseEntity<ThreadFollowResponse>(threadFollowResponse,  HttpStatus.BAD_REQUEST);
			}
			
			processSubscriptionsForInternalNote(threadFollowRequest, threadFollowResponse);
			return new ResponseEntity<ThreadFollowResponse>(threadFollowResponse,  HttpStatus.OK);
		
		}
		catch (Exception e) {
			
			LOGGER.error("unable to follow/unfollow thread for customer_uuid={} department_uuid={}", customerUUID, departmentUUID, e);
			return new ResponseEntity<ThreadFollowResponse>(threadFollowResponse,  HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		
	}
	
	public void processSubscriptionsForInternalNote(ThreadFollowRequest threadFollowRequest, ThreadFollowResponse threadFollowResponse){
		
		for(UserEvent userEvent: threadFollowRequest.getUserEvents()) {
			
			try {
				DealerAssociateExtendedDTO dealerAssociateExtendedDTO = helper.getInternalUsersFromGlobalUser(userEvent.getUser());
				if(dealerAssociateExtendedDTO==null) {
					ApiWarning apiWarning = new ApiWarning(ErrorCode.INVALID_REQUEST.name(), String.format("User does not exist for user_uuid=%s", userEvent.getUser().getUuid()));
					threadFollowResponse.getWarnings().add(apiWarning);
				}
				else {
					SubscriptionSaveEventData saveEventData = getSubscriptionSaveEventData(dealerAssociateExtendedDTO, threadFollowRequest.getCustomerUuid(), userEvent.getAddedEvents(), userEvent.getRevokedEvents());
					LOGGER.info("subscriber successfully pushed to mvc for customer_id={} dealer_id={} dealer_Associate_id={} "
							,saveEventData.getCustomerID(), saveEventData.getDealerID(), saveEventData.getDealerDepartmentID());
					messagingViewController.publishSubscriptionSaveEvent(saveEventData);
				}
			}
			catch(Exception e) {
				threadFollowResponse.getWarnings().add(new ApiWarning(ErrorCode.FOLLOW_REQUEST_FAILED.name(), String.format("follow request failed for user_uuid=%s ",
						userEvent.getUser().getUuid())));
				
				try {
					LOGGER.error("unable to publish subscription save event for userEvent={}",new ObjectMapper().writeValueAsString(userEvent), e);
				} catch (Exception e1) {
					
					LOGGER.error("unable to publish subscription save event for user_uuid={}",userEvent.getUser().getUuid(), e);
				}
			}
		}
		
	}
	
	private SubscriptionSaveEventData getSubscriptionSaveEventData(DealerAssociateExtendedDTO dealerAssociateExtendedDTO, String customerUuid, List<Event> addedEvents, List<Event> revokedEvents) {
		
		SubscriptionSaveEventData subscriptionSaveEventData = new SubscriptionSaveEventData();
		Long customerId = generalRepo.getCustomerIDForUUID(customerUuid);
		
		subscriptionSaveEventData.setCustomerID(customerId);
		subscriptionSaveEventData.setCustomerUUID(customerUuid);
		subscriptionSaveEventData.setDealerDepartmentID(dealerAssociateExtendedDTO.getDepartmentExtendedDTO().getId());
		subscriptionSaveEventData.setDealerID(dealerAssociateExtendedDTO.getDepartmentExtendedDTO().getDealerMinimalDTO().getId());
		if(addedEvents!=null && !addedEvents.isEmpty()) {
			subscriptionSaveEventData.setSubscriberDAID(dealerAssociateExtendedDTO.getId());
			subscriptionSaveEventData.setEventName(EventName.INTERNAL_SUBSRICTION_ADDED.name());
		}
		else if(revokedEvents!=null && !revokedEvents.isEmpty()){
			subscriptionSaveEventData.setRevokedDAID(dealerAssociateExtendedDTO.getId());
			subscriptionSaveEventData.setEventName(EventName.INTERNAL_SUBSRICTION_REVOKED.name());
		}
		
		return subscriptionSaveEventData;
	}

	public ResponseEntity<ThreadFollowers> getFollowersForThread(String departmentUUID, String customerUUID) throws Exception{
		
		ThreadFollowers threadFollowers = new ThreadFollowers();
		threadFollowers.setCustomerUuid(customerUUID);
		threadFollowers.setDepartmentUuid(departmentUUID);
		SubscriptionList subscriptionList = new SubscriptionList();
		try {
			SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
			subscriptionRequest = helper.getSubscriptionRequest(customerUUID, departmentUUID);
			LOGGER.info("fetching subscription list for subscriptionRequest={}", new ObjectMapper().writeValueAsString(subscriptionRequest));
			subscriptionList = messagingViewController.getSubscriptionForCustomer(subscriptionRequest);
			convertSubscriptionListToThreadFollower(departmentUUID, subscriptionList, threadFollowers);
			
			return new ResponseEntity<ThreadFollowers>(threadFollowers,  HttpStatus.OK);
		}
		catch(Exception e) {
			LOGGER.error("unable to fetch followers for department_uuid={} customer_uuid={}", departmentUUID, customerUUID, e);
			return new ResponseEntity<ThreadFollowers>(threadFollowers,  HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}

	private void convertSubscriptionListToThreadFollower(String departmentUUID, SubscriptionList subscriptionList, ThreadFollowers threadFollowers) {

		List<Subscriber> subscriberInfoList = subscriptionList.getInternalSubscriberList();
		List<UserEvent> userEvents = new ArrayList<UserEvent>();
		List<Event> events = new ArrayList<Event>();
		List<CategoryEvent> categoryEvents = new ArrayList<CategoryEvent>();
	
		Event event = new Event();
		CommunicationCategory commCategory = new CommunicationCategory();
		
		categoryEvents.add(CategoryEvent.INTERNAL_NOTE);
		
		commCategory.setName(CommunicationCategoryName.MANUAL);
		commCategory.setEvents(categoryEvents);
		
		event.setType(NotificationType.INTERNAL);
		event.setCategory(commCategory);
		events.add(event);
		
		for(Subscriber subscriber : subscriberInfoList) {
			
			UserEvent userEvent = new UserEvent();
			userEvent.setEvents(events);
			String dealerAssociateUuid = generalRepo.getDealerAssociateUuidFromDealerAssociateId(subscriber.getDealerAssociateID());
			String dealerDepartmentUuid = generalRepo.getDepartmentUuidForDealerAssociateId(subscriber.getDealerAssociateID());
			User user = new User();
			
			DealerAssociateExtendedDTO daDto = kManageApiHelper.getDealerAssociateForDealerAssociateUUID(dealerDepartmentUuid, dealerAssociateUuid);

			if(daDto != null) {
				user.setUuid(daDto.getUserUuid());
				user.setType(EditorType.USER);
				user.setName(subscriber.getDealerAssociateName());
				user.setDepartmentUuid(dealerDepartmentUuid);

				userEvent.setUser(user);
				userEvent.setEvents(events);
				userEvents.add(userEvent);
			}
		}
		
		threadFollowers.setUserEvents(userEvents);
	}
}

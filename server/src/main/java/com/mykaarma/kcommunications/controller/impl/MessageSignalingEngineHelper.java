package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mykaarma.global.MessageProtocol;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.LeadResponseRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageSignalingEngineRepository;
import com.mykaarma.kcommunications.model.jpa.LeadResponse;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageSignalingEngine;
import com.mykaarma.kcommunications.utils.AppConfigHelper;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.MessageSignalingEngineUtils;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcustomer_model.enums.Locale;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.workflow.client.WorkflowClientService;
import com.mykaarma.workflow.model.dto.ActionDTO;
import com.mykaarma.workflow.model.dto.ActorDTO;
import com.mykaarma.workflow.model.dto.NodeDTO;
import com.mykaarma.workflow.model.dto.TransitionDTO;
import com.mykaarma.workflow.model.response.GetNodeResponse;
import com.mykaarma.workflow.model.response.GetTransitionResponse;

@Service
public class MessageSignalingEngineHelper {
	
	private Logger LOGGER = LoggerFactory.getLogger(MessageSignalingEngineHelper.class);
	
	@Autowired
	MessageSignalingEngineRepository messageSignalingEngineRepository;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	WorkflowClientService workflowClientService;
	
	@Autowired
	LeadResponseRepository leadResponseRepository;
	
	@Autowired
	InternalCommentImpl internalCommentServiceImpl;
	
	@Autowired
	CommunicationsApiImpl communicationsApiImpl;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	AppConfigHelper appConfigHelper;
	
	@Autowired
	Helper helper;
	
	public TransitionDTO getTransistionDTOFromWorkFlowForGivenEvent(String workFlowUUID,String workflowEvent,String nodeFromUUID) throws Exception{
		List<String> workflowEvents=new ArrayList<String>();
		workflowEvents.add(workflowEvent);
		GetTransitionResponse transistionResponse = workflowClientService.getFinalTransitionForEvents(workFlowUUID, workflowEvents, nodeFromUUID);
		if(!validateTransitionResponse(transistionResponse)){
			return null;
		}
		return transistionResponse.getTransition();
	}
	
	public Boolean processWorkflowBasedActionsForGivenMessageInfo(String workFlowUUID,Message incomingMessage,String previousMessageMetaData,Long messageSignalingEngineId, String departmentUUID) throws Exception{
		HashMap<String,String> incomingKeywordAndEventMap=getKeywordEventByApplyingRulesForWorkflowOnMessage(workFlowUUID,incomingMessage,previousMessageMetaData);
		if(incomingKeywordAndEventMap==null){
			return false;
		}
		String incomingMessageKeywordEvent=incomingKeywordAndEventMap.get(MessageSignalingEngineUtils.INCOMING_MESSAGE_KEYWORD_EVENT);
		String incomingMessageKeyword=incomingKeywordAndEventMap.get(MessageSignalingEngineUtils.INCOMING_MESSAGE_KEYWORD);
		
		
		LOGGER.info(String.format("in processWorkflowBasedActionsForGivenMessageInfo for message_id=%s "
				+ "workflow_uuid=%s incoming_message_keyword_event=%s department_uuid=%s",incomingMessage.getId(),workFlowUUID,incomingMessageKeywordEvent,departmentUUID)); 
		NodeDTO startNode=getStartNodeForGivenWorkFlow(workFlowUUID);
		
		if(startNode==null || startNode.getUuid()==null || startNode.getUuid().isEmpty()){
			return false;
		}
		
		TransitionDTO responseTransition = getTransistionDTOFromWorkFlowForGivenEvent(workFlowUUID,incomingMessageKeywordEvent,startNode.getUuid());
		
		if(responseTransition==null || responseTransition.getNodeTo()==null 
				|| responseTransition.getNodeTo().getActions()==null 
				|| responseTransition.getNodeTo().getActions().isEmpty()){
			return false;
		}
		
		takeActionsForGivenCustomer(incomingMessage,responseTransition.getNodeTo().getActions(),departmentUUID);
		try{
			updateLeadResponseInDatabase(incomingMessage.getId(),messageSignalingEngineId,incomingMessageKeywordEvent);
		} catch (Exception e) {
			LOGGER.error(String.format("Error in updating lead response for message_id=%s "
					+ "message_signaling_engine_id=%s  incoming_message_keyword=%s", incomingMessage.getId(),messageSignalingEngineId,incomingMessageKeywordEvent), e);
		}
		return true;
	}
	
	private void updateLeadResponseInDatabase(Long messageId,Long messageSignalingEngineId,String incomingMessageKeywordEvent){
		LeadResponse leadResponse=new LeadResponse();
		leadResponse.setMessageEventKey(incomingMessageKeywordEvent);
		leadResponse.setMessageID(messageId);
		leadResponse.setMessageSignalingEngineID(messageSignalingEngineId);
		
		leadResponseRepository.save(leadResponse);
		
		LOGGER.info(String.format("in updateLeadResponseInDatabase successfully saved for message_id=%s message_signaling_engine_id=%s message_keyword_event=%s",
				messageId,messageSignalingEngineId,incomingMessageKeywordEvent));
	}
	
	public void takeActionsForGivenCustomer(Message incomingMessage,List<ActionDTO> actions, String departmentUUID) throws Exception{
		if(incomingMessage==null || actions==null || actions.isEmpty()){
			return;
		}
		String systemUserUUID=null;
		String currentDealerAssociateUUID=null;
		
		
		
		String customerUUID=generalRepository.getCustomerUUIDFromCustomerID(incomingMessage.getCustomerID());
		for(ActionDTO actionObj:actions){
			
			LOGGER.info(String.format("takeActionsForGivenCustomer customer_id=%s message_protocol=%s action_type=%s",
					incomingMessage.getCustomerID(),incomingMessage.getProtocol(),actionObj.getType()));
			if(actionObj.getDetails()==null){
				 continue;
			}
			String actionUserUUID=null;
			if(MessageSignalingEngineUtils.NOTIFY_ACTION_TYPE.equalsIgnoreCase(actionObj.getType())){
				if(checkIfActionAssigneeIsSystemUser(actionObj.getDetails())){
					if(systemUserUUID!=null){
						actionUserUUID=systemUserUUID;
					} else {
						GetDealerAssociateResponseDTO dealerAssociate = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);
						systemUserUUID=dealerAssociate.getDealerAssociate().getUserUuid();
						actionUserUUID=systemUserUUID;
					}
				} else {
					if(currentDealerAssociateUUID!=null){
						actionUserUUID=currentDealerAssociateUUID;
					} else {
						currentDealerAssociateUUID=generalRepository.getUserUUIDForDealerAssociateID(incomingMessage.getDealerAssociateID());
						actionUserUUID=currentDealerAssociateUUID;
					}
				}
				takeNotifyAction(incomingMessage,actionObj.getDetails(),actionObj.getActors(),departmentUUID,actionUserUUID,customerUUID);
			}
		}
	}
	
	private Boolean checkIfActionAssigneeIsSystemUser(Map<String, Object> actionDetailMap){
		if(actionDetailMap==null){
			return true;
		}
		if(actionDetailMap.get(MessageSignalingEngineUtils.SEND_AS_SYSTEM_USER_KEY)!=null){
			String sendAsSystemUser=actionDetailMap.get(MessageSignalingEngineUtils.SEND_AS_SYSTEM_USER_KEY).toString();
			if("false".equalsIgnoreCase(sendAsSystemUser)){
				return false;
			}
		}
		return true;
		
	}
	
	private List<Long> getListofLongFromString(String listOfLongAsString){
		if(listOfLongAsString==null || listOfLongAsString.isEmpty()){
			return null;
		}
		listOfLongAsString=listOfLongAsString.replace("[", "");
		listOfLongAsString=listOfLongAsString.replace("]", "");
		List<Long> list = Arrays.asList(listOfLongAsString.split(",")).stream().map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
		return list;
	}
	
	private void takeNotifyAction(Message incomingMessage, Map<String, Object> actionDetailMap, List<ActorDTO> actionActorsList,
			String departmentUUID,String userUUID,String customerUUID) throws Exception{
		ObjectMapper mapper = new ObjectMapper();
		String actionDetailType=null;
		String emailTemplateType=null;
		if(actionDetailMap!=null && actionDetailMap.get(MessageSignalingEngineUtils.NAME_KEY)!=null){
			actionDetailType=actionDetailMap.get(MessageSignalingEngineUtils.NAME_KEY).toString();
		}
		if(actionDetailMap!=null && actionDetailMap.get(MessageSignalingEngineUtils.EMAIL_TEMPLATE_TYPE)!=null){
			emailTemplateType=actionDetailMap.get(MessageSignalingEngineUtils.EMAIL_TEMPLATE_TYPE).toString();
		}
		
		LOGGER.info(String.format("takeNotifyAction customer_id=%s message_protocol=%s action_detail_type=%s "
				+ "email_template_type=%s action_detail_map=%s action_actors_list=%s"
				,incomingMessage.getCustomerID(),incomingMessage.getProtocol(),actionDetailType,emailTemplateType,mapper.writeValueAsString(actionDetailMap),mapper.writeValueAsString(actionActorsList)));
		
		if(MessageSignalingEngineUtils.NOTIFY_USERS.equalsIgnoreCase(actionDetailType)){
			List<Long> notificationDAIDList=new ArrayList<Long>();
			for(ActorDTO actorIterator:actionActorsList){
				if(actorIterator.getValue()!=null && !actorIterator.getValue().isEmpty()){
					List<Long> notificationDAIDs=getListofLongFromString(actorIterator.getValue());
					if(notificationDAIDs!=null){
						notificationDAIDList.addAll(notificationDAIDs);
					}
				}
			}
			LOGGER.info(String.format("takeNotifyAction action_detail_type=%s "
				+ "email_template_type=%s notifications_daid_list=%s",actionDetailType,emailTemplateType,notificationDAIDList));
			if(notificationDAIDList!=null && !notificationDAIDList.isEmpty()){
				publishInternalComment(MessageProtocol.T,userUUID,
						customerUUID,departmentUUID,emailTemplateType,incomingMessage.getDealerID(),notificationDAIDList,incomingMessage.getCustomerID());
			}
		} else if(MessageSignalingEngineUtils.NOTIFY_CUSTOMER.equalsIgnoreCase(actionDetailType)){
			sendAutomatedReplyToCustomer(userUUID,
					customerUUID,departmentUUID,emailTemplateType,incomingMessage.getDealerID(),incomingMessage.getCustomerID());
		}
		
	}
	
	private void sendAutomatedReplyToCustomer(String userUUID,String customerUUID,String departmentUUID,String emailTemplateType,Long dealerId,Long customerId) throws Exception{
		if( emailTemplateType==null || emailTemplateType.isEmpty()){
			return;
		}
		
		String customerPreferredLocale=getCustomerPreferredLocaleForCustomerId(customerId);
		String messageBody=getMessageBodyBasedOnEmailTemplateTypeAndDealerID(dealerId,emailTemplateType,customerPreferredLocale);
		if(messageBody==null || messageBody.isEmpty()){
			LOGGER.error(String.format("in sendAutomatedReplyToCustomer not sending message to customer_uuid=%s since emailTemplate of type=%s not configured "
					+ "for dealer_id=%s locale=%s",customerUUID,emailTemplateType,dealerId,customerPreferredLocale));
			return;
		}
		
		
		SendMessageRequest sendMessageRequest=new SendMessageRequest();
		MessageAttributes messageAttributes=new MessageAttributes();
		messageAttributes.setIsManual(Boolean.FALSE);
		messageAttributes.setProtocol(com.mykaarma.kcommunications_model.enums.MessageProtocol.TEXT);
		messageAttributes.setBody(messageBody);
		messageAttributes.setType(com.mykaarma.kcommunications_model.enums.MessageType.OUTGOING);
        MessageSendingAttributes messageSendingAttributes = new MessageSendingAttributes();
        messageSendingAttributes.setDelay(0);
        sendMessageRequest.setMessageAttributes(messageAttributes);
        sendMessageRequest.setMessageSendingAttributes(messageSendingAttributes);
		communicationsApiImpl.createMessage(customerUUID, departmentUUID, userUUID, sendMessageRequest, null);
	}
	
	private String getCustomerPreferredLocaleForCustomerId(Long customerId){
		String customerPreferredLocale=generalRepository.getPreferredLocaleForCustomerID(customerId);
		LOGGER.info(String.format("in getCustomerPreferredLocaleForCustomerId for customer_id=%s locale=%s",customerId,customerPreferredLocale)); 
		if(customerPreferredLocale==null || customerPreferredLocale.isEmpty()){
			customerPreferredLocale=Locale.ENUS.getValue();
		}
		return customerPreferredLocale;
	}
	
	private void publishInternalComment(MessageProtocol messageProtocol,String userUUID,String customerUUID,String departmentUUID,
			String emailTemplateType,Long dealerId,List<Long> oneTimeNotifiers,Long customerId) throws Exception{
		if(messageProtocol==null || emailTemplateType==null || emailTemplateType.isEmpty() 
				|| oneTimeNotifiers==null || oneTimeNotifiers.isEmpty()){
			return;
		}
		String customerPreferredLocale=getCustomerPreferredLocaleForCustomerId(customerId);
		String messageBody=getMessageBodyBasedOnEmailTemplateTypeAndDealerID(dealerId,emailTemplateType,customerPreferredLocale);
		if(messageBody==null || messageBody.isEmpty()){
			LOGGER.error(String.format("in publishInternalComment not posting internal comment for customer_uuid=%s emailTemplate of type=%s not configured "
					+ "for dealer_id=%s",customerUUID,emailTemplateType,dealerId));
			return;
		}
		
		
		internalCommentServiceImpl.publishInternalComment(messageBody, customerUUID, userUUID, departmentUUID,
				messageProtocol, null, null, null, oneTimeNotifiers, Boolean.FALSE);
		
		LOGGER.info(String.format("in publishInternalComment posted internal comment for customer_uuid=%s emailTemplate of type=%s "
				+ "for dealer_id=%s user_uuid=%s notification_list=%s message_body=%s",customerUUID,emailTemplateType,dealerId,
				userUUID,oneTimeNotifiers,messageBody));
	}
	
	private String getMessageBodyBasedOnEmailTemplateTypeAndDealerID(Long dealerId,String emailTemplateType,String locale) throws Exception{
		String messageBody=helper.getEmailTemplateTypeAndDealerID(dealerId,emailTemplateType,locale);
		LOGGER.info(String.format("in getMessageBodyBasedOnEmailTemplateTypeAndDealerID  message_body=%s for dealer_id=%s"
				+ " email_template_type=%s locale=%s",messageBody,dealerId,emailTemplateType,locale)); 
		return messageBody;
	}
	
	private HashMap<String,String> getKeywordEventByApplyingRulesForWorkflowOnMessage(String workFlowUUID,Message incomingMessage,String previousMessageMetaData) throws Exception{
		TransitionDTO transitionDTO=getTransistionDTOFromWorkFlowForGivenEvent(workFlowUUID,MessageSignalingEngineUtils.INCOMING_MESSAGE_RECEIVED_EVENT,null);
		
		if(transitionDTO==null || transitionDTO.getActions()==null || transitionDTO.getActions().isEmpty()){
			return null;
		}
		List<String> incomingMessageKeywordsList=null;
		List<String> previousMessageTagList=null;
		HashMap<String,String> keywordEventMap=null;
		ObjectMapper mapper = new ObjectMapper();
		for(ActionDTO actionsIterator:transitionDTO.getActions()){
			if( !MessageSignalingEngineUtils.APPLY_RULES_ACTION_TYPE.equalsIgnoreCase(actionsIterator.getType()) || actionsIterator.getDetails()==null || actionsIterator.getDetails().isEmpty()){
				continue;
			}
			
			Map<String, Object> actionDetails = actionsIterator.getDetails();
			
			incomingMessageKeywordsList=mapper.convertValue(actionDetails.get(MessageSignalingEngineUtils.INCOMING_MESSAGE_KEYWORD_LIST),new TypeReference<List<String>>(){});
			previousMessageTagList=mapper.convertValue(actionDetails.get(MessageSignalingEngineUtils.PREVIOUS_SENT_MESSAGE_TAG_LIST),new TypeReference<List<String>>(){});
			keywordEventMap=mapper.convertValue(actionDetails.get(MessageSignalingEngineUtils.MESSAGE_KEYWORD_EVENT_MAP),new TypeReference<HashMap<String,String>>(){});
			
			LOGGER.info(String.format("fetchRulesForWorkflowAndApplyOnMessage workflow_uuid=%s "
					+ "incoming_message_keyword_list=%s previous_message_tag_list=%s keyword_event_map=%s",workFlowUUID,incomingMessageKeywordsList,
					previousMessageTagList,mapper.writeValueAsString(keywordEventMap)));
	
		}
		
		if(incomingMessageKeywordsList==null || incomingMessageKeywordsList.isEmpty() 
				|| previousMessageTagList==null || previousMessageTagList.isEmpty()
				|| keywordEventMap==null || keywordEventMap.isEmpty()) {
			return null;
		}
		
		String incomingMessageKeyword=applyLeadMessageRulesOnMessagesAndGetKeyword(incomingMessage,previousMessageMetaData,incomingMessageKeywordsList,previousMessageTagList);
		String keywordEvent=keywordEventMap.get(incomingMessageKeyword);
		
		LOGGER.info("getKeywordEventByApplyingRulesForWorkflowOnMessage incoming_message_id={} "
				+ "previous_message_meta_data={} incoming_message_keyword_event={} for worrkflow_uuid={}",incomingMessage.getId(),previousMessageMetaData,keywordEvent,workFlowUUID);
		
		if(incomingMessageKeyword!=null && !incomingMessageKeyword.isEmpty() && keywordEvent!=null && !keywordEvent.isEmpty()){
			HashMap<String,String> incomingKeywordAndEventMap=new HashMap<String,String>();
			incomingKeywordAndEventMap.put(MessageSignalingEngineUtils.INCOMING_MESSAGE_KEYWORD_EVENT, keywordEvent);
			incomingKeywordAndEventMap.put(MessageSignalingEngineUtils.INCOMING_MESSAGE_KEYWORD, incomingMessageKeyword);
			return incomingKeywordAndEventMap;
		}
		
		return null;
		
	}
	
	
	private String applyLeadMessageRulesOnMessagesAndGetKeyword(Message incomingMessage,String previousMessageMetaData,
			List<String> incomingMessageKeywordsList,List<String> previousMessageTagList) throws Exception{
		if(incomingMessage==null || previousMessageMetaData==null || previousMessageMetaData.isEmpty() 
				|| incomingMessageKeywordsList==null || incomingMessageKeywordsList.isEmpty() 
				||  previousMessageTagList==null || previousMessageTagList.isEmpty()) {
			return null;
		}
		LOGGER.info(String.format("in applyLeadMessageRulesOnMessagesAndGetKeyword for "
				+ "incoming_message_keyword_list=%s previous_message_tag_list=%s"
				+ " message=%s previous_message_meta_data=%s",incomingMessageKeywordsList,previousMessageTagList,
				new ObjectMapper().writeValueAsString(incomingMessage),previousMessageMetaData));
		String incomingMessageKeyword=applyIncomingMessageRulesAndGetKeyword(incomingMessage,incomingMessageKeywordsList);
		if(incomingMessageKeyword!=null && !incomingMessageKeyword.isEmpty()){
			if(applyPreviousMessageRules(previousMessageMetaData,previousMessageTagList)){
				return incomingMessageKeyword;
			} else {
				return null;
			}
		} else {
			return null;
		}
		
	}
	
	private String applyIncomingMessageRulesAndGetKeyword(Message incomingMessage,List<String> incomingMessageKeywordsList){
		if(incomingMessage==null || incomingMessageKeywordsList==null || incomingMessageKeywordsList.isEmpty() ){
			return null;
		}
		
		if(incomingMessage.getMessageExtn()!=null 
				&& incomingMessage.getMessageExtn().getMessageBody()!=null 
				&& !incomingMessage.getMessageExtn().getMessageBody().isEmpty()){
			LOGGER.info(String.format("in applyIncomingMessageRulesAndGetKeyword for incoming_message_body=%s"
					+ " incoming_message_keyword_list=%s",incomingMessage.getMessageExtn().getMessageBody(),incomingMessageKeywordsList)); 
			String incomingMessageBody=incomingMessage.getMessageExtn().getMessageBody();
			incomingMessageBody=incomingMessageBody.trim();
			for(String incomingMessageKeyword:incomingMessageKeywordsList){
				if(incomingMessageKeyword.equalsIgnoreCase(incomingMessageBody)){
					return incomingMessageKeyword;
				}
			}
		}
		return null;
	}
	
	private Boolean applyPreviousMessageRules(String previousMessageMetaData,List<String> previousMessageTagList){
		if(previousMessageMetaData==null || previousMessageMetaData.isEmpty() || previousMessageTagList==null || previousMessageTagList.isEmpty() ){
			return false;
		}
		LOGGER.info(String.format("in applyPreviousMessageRules for previous_message_metadata=%s"
				+ " previous_message_tag_list=%s",previousMessageMetaData,previousMessageTagList)); 
		Map jsonJavaRootObject = new Gson().fromJson(previousMessageMetaData, Map.class);
		Object tagObj= jsonJavaRootObject.get(MessageSignalingEngineUtils.MESSAGE_TAG);
		
		if(tagObj!=null){
			String previousMessageTag=(String)tagObj;
			LOGGER.info(String.format("in applyPreviousMessageRules for previous_message_metadata=%s"
					+ " previous_message_tag_list=%s preivous_message_tags=%s ",previousMessageMetaData,previousMessageTagList,previousMessageTag));
			if(previousMessageTagList.contains(previousMessageTag)){
				 return true;
			 }
		 return false;
		} else {
			return false;
		}
		
	}
	
	
	public NodeDTO getStartNodeForGivenWorkFlow(String workFlowUUID) throws Exception{
		GetNodeResponse startNodeResponse = workflowClientService.getStartNode(workFlowUUID);
		if(!validateGetNodeResponse(startNodeResponse)){
			return null;
		}
		return startNodeResponse.getNode();
	}
	
	public List<MessageSignalingEngine> findMessageSignalingEngineEntriesForDealerID(Long dealerID){
		List<MessageSignalingEngine> messageSignalingEngineList=messageSignalingEngineRepository.findAllByDealerID(dealerID);
		
		if(messageSignalingEngineList==null || messageSignalingEngineList.isEmpty()){
			LOGGER.info(String.format("findMessageSignalingEngineEntriesForDealerID no entries found for dealer_id=%s",dealerID));
			return null;
		}
		LOGGER.info(String.format("findMessageSignalingEngineEntriesForDealerID result_size=%s dealer_id=%s",messageSignalingEngineList.size(),dealerID));
		
		return messageSignalingEngineList;
		
	}
	
	private boolean validateGetNodeResponse(GetNodeResponse response) {
	    if(response == null) {
	    	LOGGER.error("GetNodeResponse is null");
	      return false;
	    }

	    if (response.getErrors() != null && !response.getErrors().isEmpty()) {
	      response.getErrors().forEach(error -> LOGGER.error("getStartNode returned error {}", error.getErrorDescription() ));
	      return false;
	    }
	    return true;

	}
	
	 private boolean validateTransitionResponse(GetTransitionResponse response) {
		    if(response == null) {
		    	LOGGER.error("GetTransitionListResponse is null");
		      return false;
		    }

		    if (response.getErrors() != null && !response.getErrors().isEmpty()) {
		      response.getErrors().forEach(error -> LOGGER.error("getFinalTransitionForEvents returned error {}", error.getErrorDescription() ));
		      return false;
		    }
		    return true;

	}

}

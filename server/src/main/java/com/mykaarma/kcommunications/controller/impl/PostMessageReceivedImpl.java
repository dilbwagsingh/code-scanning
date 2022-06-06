package com.mykaarma.kcommunications.controller.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.MessageType;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageSignalingEngine;
import com.mykaarma.kcommunications.utils.Helper;

@Service
public class PostMessageReceivedImpl {

	private final static Logger LOGGER = LoggerFactory.getLogger(PostMessageReceivedImpl.class);	
	
	@Autowired
	MessageSignalingEngineHelper messageSignalingEngineHelper;
	
	@Autowired
	MessageRepository messageRepository;
	
	@Autowired
	private Helper helper;
	
	public void takePostMessageReceivedActions(String incomingMessageUUID,Long dealerId,String departmentUUID) throws Exception{
		List<MessageSignalingEngine> messageSignalingEngineEntries = messageSignalingEngineHelper.findMessageSignalingEngineEntriesForDealerID(dealerId);
		if(messageSignalingEngineEntries==null || messageSignalingEngineEntries.isEmpty()){
			return;
		}
		LOGGER.info("takePostMessageReceivedActions message_signaling_engine_entries_size={}",messageSignalingEngineEntries.size());
		processWorkflowBasedActionsForGivenMessage(incomingMessageUUID,messageSignalingEngineEntries,departmentUUID);
		
	}
	
	private void processWorkflowBasedActionsForGivenMessage(String incomingMessageUUID,List<MessageSignalingEngine> messageSignalingEngineEntries, String departmentUUID) throws Exception{
		
		if(incomingMessageUUID==null || incomingMessageUUID.isEmpty() || messageSignalingEngineEntries==null || messageSignalingEngineEntries.isEmpty()){
			LOGGER.info("processWorkflowBasedActionsForGivenMessage incoming_message_uuid={}",incomingMessageUUID);
			return;
		}
		
		
		
		Message incomingMessage=helper.fetchMessageForGivenUUID(incomingMessageUUID);
		if(incomingMessage==null){
			LOGGER.info("processWorkflowBasedActionsForGivenMessage no message present for given message_uuid={}",incomingMessageUUID);
			return;
		}
		LOGGER.info("processWorkflowBasedActionsForGivenMessage incoming message details message_id={} message_type={} customer_id={} "
				+ "protocol={} subject={} body={}",incomingMessage.getId(),incomingMessage.getMessageType(),incomingMessage.getCustomerID(),incomingMessage.getProtocol(),
				incomingMessage.getMessageExtn().getSubject(),incomingMessage.getMessageExtn().getMessageBody());
		Object[] previousMessageDetails =null;
		
		List<Object[]> resultList = messageRepository.findLatestPreviousMessageForGivenProtocolAndCustomerIDAndMessageID(incomingMessage.getId(), incomingMessage.getCustomerID(), incomingMessage.getProtocol());
		
		if(resultList!=null && !resultList.isEmpty()){
			previousMessageDetails=resultList.get(0);
		}
		
		if(previousMessageDetails==null){
			LOGGER.info("processWorkflowBasedActionsForGivenMessage  no previous message present for incoming_message_uuid={}",incomingMessageUUID);
			return;
		}
		
		LOGGER.info("processWorkflowBasedActionsForGivenMessage "
				+ "incoming_message_uuid={} previous_message_data={} first_object={} size={}",incomingMessageUUID,new ObjectMapper().writeValueAsString(previousMessageDetails),
				new ObjectMapper().writeValueAsString(previousMessageDetails[0]),previousMessageDetails.length); 
		
		applyMessageRulesAndProcessWorkflow(previousMessageDetails, messageSignalingEngineEntries, incomingMessageUUID, departmentUUID, incomingMessage);
	}
	
	private void applyMessageRulesAndProcessWorkflow(Object[] previousMessageDetails,List<MessageSignalingEngine> messageSignalingEngineEntries,
			String incomingMessageUUID, String departmentUUID,Message incomingMessage) throws Exception{
		if(previousMessageDetails==null || previousMessageDetails.length==0 
				|| messageSignalingEngineEntries==null || messageSignalingEngineEntries.isEmpty() 
				|| incomingMessageUUID==null || incomingMessageUUID.isEmpty() 
				|| departmentUUID==null || departmentUUID.isEmpty() || incomingMessage==null){
			return;
		}
		if(previousMessageDetails[0]!=null){
			String previousMessageType=(String)previousMessageDetails[0];
			if(MessageType.S.name().equalsIgnoreCase(previousMessageType)){
				if(previousMessageDetails[1]!=null){
					String previousMessageMetaData=(String)previousMessageDetails[1];
					LOGGER.info("applyMessageRulesAndProcessWorkflow incoming_message_uuid={} "
							+ "previous_message_meta_data={}",incomingMessageUUID,previousMessageMetaData);
					if(previousMessageMetaData!=null && !previousMessageMetaData.isEmpty()){
						for(MessageSignalingEngine mseIterator:messageSignalingEngineEntries){
							Boolean isCurrentWorkflowApplicable=messageSignalingEngineHelper.processWorkflowBasedActionsForGivenMessageInfo
									(mseIterator.getWorkflowUUID(), incomingMessage, previousMessageMetaData,mseIterator.getId(),departmentUUID);
							LOGGER.info("applyMessageRulesAndProcessWorkflow workflow_id={} workflow_uuid={} "
									+ "current_workflow_applicable={}",mseIterator.getId(),mseIterator.getWorkflowUUID(),isCurrentWorkflowApplicable);
						}
					} else {
						LOGGER.info("applyMessageRulesAndProcessWorkflow not processing since no metadata present "
								+ "for previous message to incoming_message_uuid={} "
								+ "previous_message_meta_data={}",incomingMessageUUID,previousMessageMetaData);
					}
				} else {
					LOGGER.info("applyMessageRulesAndProcessWorkflow not processing since no metadata present "
							+ "for previous message to incoming_message_uuid={}s",incomingMessageUUID);
				}
				
			}
		}
	}
}

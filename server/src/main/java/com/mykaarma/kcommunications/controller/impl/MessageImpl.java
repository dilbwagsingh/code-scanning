package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.DraftTypes;
import com.mykaarma.global.MessageType;
import com.mykaarma.kcommunications.jpa.repository.DocFileRepository;
import com.mykaarma.kcommunications.jpa.repository.DraftMessageMetaDataRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageExtnRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageMetaDataRepository;
import com.mykaarma.kcommunications.jpa.repository.MessagePredictionFeedbackRepository;
import com.mykaarma.kcommunications.jpa.repository.MessagePredictionRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.mapper.DocFileDTOMapper;
import com.mykaarma.kcommunications.mapper.DraftMessageMetaDataDTOMapper;
import com.mykaarma.kcommunications.mapper.MessageDTOMapper;
import com.mykaarma.kcommunications.mapper.MessageExtnDTOMapper;
import com.mykaarma.kcommunications.mapper.MessageMetaDataDTOMapper;
import com.mykaarma.kcommunications.mapper.MessagePredictionDTOMapper;
import com.mykaarma.kcommunications.mapper.MessagePredictionFeedbackDTOMapper;
import com.mykaarma.kcommunications.model.jpa.DocFile;
import com.mykaarma.kcommunications.model.jpa.DraftMessageMetaData;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.model.jpa.MessageMetaData;
import com.mykaarma.kcommunications.model.jpa.MessagePrediction;
import com.mykaarma.kcommunications.model.jpa.MessagePredictionFeedback;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications_model.dto.DocFileDTO;
import com.mykaarma.kcommunications_model.dto.DraftMessageMetaDataDTO;
import com.mykaarma.kcommunications_model.dto.MessageDTO;
import com.mykaarma.kcommunications_model.dto.MessageExtnDTO;
import com.mykaarma.kcommunications_model.dto.MessageMetaDataDTO;
import com.mykaarma.kcommunications_model.dto.MessagePredictionDTO;
import com.mykaarma.kcommunications_model.dto.MessagePredictionFeedbackDTO;
import com.mykaarma.kcommunications_model.dto.PredictionFeatureDTO;
import com.mykaarma.kcommunications_model.enums.FetchMessageTypes;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.request.FetchMessagesForCommunicationIdentifierListRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesForCustomerRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageImpl {

	@Autowired
	private MessageRepository messageRepository;
	
	@Autowired
	private MessageExtnRepository messageExtnRepository;
	
	@Autowired
	private DraftMessageMetaDataRepository draftMessageMetaDataRepository;
	
	@Autowired
	private MessageMetaDataRepository messageMetaDataRepository;
	
	@Autowired
	private DocFileRepository docFileRepository;
	
	@Autowired
	private MessagePredictionRepository messagePredictionRepository;
	
	@Autowired
	private MessagePredictionFeedbackRepository messagePredictionFeedbackRepository;
	
	@Autowired
	MessagePredictionDTOMapper messagePredictionDTOMapper;
	
	@Autowired
	MessageDTOMapper messageDTOMapper;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	MessageExtnDTOMapper messageExtnDTOMapper;
	
	@Autowired
	DocFileDTOMapper docFileDTOMapper;
	
	@Autowired
	MessageMetaDataDTOMapper messageMetaDataDTOMapper;
	
	@Autowired
	DraftMessageMetaDataDTOMapper draftMessageMetaDataDTOMapper;
	
	@Autowired
	MessagePredictionFeedbackDTOMapper messagePredictionFeedbackDTOMapper;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	
	
	public List<MessageDTO> getMessages(FetchMessagesRequest fetchMessagesRequest,String userUuid) throws Exception {
		List<MessageDTO> messagesList=getMessagesForUuids(fetchMessagesRequest.getMessageUuids(),userUuid);
		
		return messagesList;
	}
	
	public HashMap<FetchMessageTypes, List<MessageDTO>> getMessagesForCustomer(FetchMessagesForCustomerRequest fetchMessagesForCustomerRequest,
			String userUuid,String customerUuid,String departmentUuid) throws Exception {
		List<String> messageUuids=new ArrayList<String>();
		List<MessageProtocol> messageProtocolFetched = fetchMessagesForCustomerRequest.getProtocolsSupported();
		List<String> messageProtocolString = null;
		if(messageProtocolFetched!=null && !messageProtocolFetched.isEmpty()) {
			messageProtocolString = messageProtocolFetched.stream()
				    .map( item -> item.getMessageProtocol())
				    .collect(Collectors.toList());
			}
		log.info("message protocol list={} string_list={}", messageProtocolFetched, messageProtocolString);
		HashMap<FetchMessageTypes, List<MessageDTO>> messageResponseMap=new HashMap<FetchMessageTypes, List<MessageDTO>>();
		
		Long customerId=generalRepository.getCustomerIDForUUID(customerUuid);
		Long dealerId=generalRepository.getDealerIDFromDepartmentUUID(departmentUuid);
		
		log.info("in getMessagesForCustomer for customer_id={} dealer_id={}",customerId,dealerId);
		if(fetchMessagesForCustomerRequest.getLastMessageReceivedOn()!=null) {
			if(fetchMessagesForCustomerRequest.getMessageUuidsReceivedAtSameTime()!=null 
					&& !fetchMessagesForCustomerRequest.getMessageUuidsReceivedAtSameTime().isEmpty()) {
				messageUuids=messageRepository.getMessageUuidsForCustomerOlderThanGivenData(customerId,fetchMessagesForCustomerRequest.getLastMessageReceivedOn() ,
						 fetchMessagesForCustomerRequest.getMaxResults(),fetchMessagesForCustomerRequest.getMessageUuidsReceivedAtSameTime(), messageProtocolString);
			} else {
				messageUuids=messageRepository.getMessageUuidsForCustomerOlderThanGivenData(customerId,fetchMessagesForCustomerRequest.getLastMessageReceivedOn() ,
						 fetchMessagesForCustomerRequest.getMaxResults(), messageProtocolString);
			}
		} else {
			messageUuids=messageRepository.getMessageUuidsForCustomer(customerId, fetchMessagesForCustomerRequest.getMaxResults(), messageProtocolString 
					);
		}
		
		if(messageUuids!=null && !messageUuids.isEmpty()) {
			List<MessageDTO> messagesList=getMessagesForUuids(messageUuids,userUuid);
			log.info("in getMessagesForCustomer messages list size={} for customer_id={} dealer_id={}",messagesList.size(),customerId,dealerId);
			messageResponseMap.put(FetchMessageTypes.MESSAGES, messagesList);
		} else {
			log.info("in getMessagesForCustomer no messages found for customer_id={} dealer_id={}",customerId,dealerId);
		}
		
		
		if(fetchMessagesForCustomerRequest.isFetchDrafts()) {
			log.info("in getMessagesForCustomer fetching drafts for customer_id={} dealer_id={} ",customerId,dealerId);
			List<String> draftUuids=new ArrayList<String>();
			draftUuids=messageRepository.getMessageUuidsOfTypeAndLimit(customerId, MessageType.F.toString(), fetchMessagesForCustomerRequest.getMaxDraftsToBeFetched());
			if(draftUuids!=null && !draftUuids.isEmpty()){
				log.info("in getMessagesForCustomer drafts_uuids{} found for customer_id={} dealer_id={}",draftUuids.toString(),customerId,dealerId);
				List<MessageDTO> draftMessagesList=getMessagesForUuids(draftUuids,userUuid);
				draftMessagesList=sortDrafts(draftMessagesList);
				messageResponseMap.put(FetchMessageTypes.DRAFT, draftMessagesList);
			} else {
				log.info("in getMessagesForCustomer no drafts found for customer_id={} dealer_id={}",customerId,dealerId);
			}
		}
		
		return messageResponseMap;
	}
	
	
	public List<MessageDTO> fetchMessageForCommunicationIdentifiersList(String departmentUuid,String userUuid,FetchMessagesForCommunicationIdentifierListRequest fetchMessagesForCommunicationIdentifierList) {
			
		List<Message> messages=messageRepository.findByCommunicationUidInOrderByReceivedOnDesc(fetchMessagesForCommunicationIdentifierList.getCommunicationIdentifierList());
			
		if(messages!=null) {
			log.info("in fetchMessageForCommunicationIdentifiersList fetched message object for"
					+ " department_uuid={} user_uuid={} communication_uid_list={}",departmentUuid,userUuid,
					fetchMessagesForCommunicationIdentifierList.getCommunicationIdentifierList().toString());
		
			List<MessageDTO> messageDTOList = getMessageDTOListForGivenMessages(messages,userUuid);
			return messageDTOList;
		} else {
			log.info("in fetchMessageForCommunicationIdentifiersList no message found for"
					+ " department_uuid={} user_uuid={} communication_uid_list={}",departmentUuid,userUuid,fetchMessagesForCommunicationIdentifierList.getCommunicationIdentifierList().toString());
		}
		
		return null;
		
	}
	
	private List<MessageDTO> sortDrafts(List<MessageDTO> draftsList) {
		if(draftsList==null || draftsList.size()<2)
			return draftsList;
		List<MessageDTO> failedDrafts = new ArrayList<MessageDTO>();
		List<MessageDTO> scheduledDrafts = new ArrayList<MessageDTO>();
		List<MessageDTO> saveForLaterDrafts = new ArrayList<MessageDTO>();
		List<MessageDTO> drafts = new ArrayList<MessageDTO>();
		for(MessageDTO message : draftsList) {
			if(DraftTypes.FAILED.name().equalsIgnoreCase(message.getDraftMessageMetaDataDTO().getStatus())) {
				failedDrafts.add(message);
			} else if(DraftTypes.SCHEDULED.name().equalsIgnoreCase(message.getDraftMessageMetaDataDTO().getStatus())) {
				scheduledDrafts.add(message);
			} else if(DraftTypes.DRAFTED.name().equalsIgnoreCase(message.getDraftMessageMetaDataDTO().getStatus())) {
				saveForLaterDrafts.add(message);
			}
		}
		Collections.sort(failedDrafts, new Comparator<MessageDTO>() {
			public int compare(MessageDTO m1, MessageDTO m2) {
				if(m1!=null && m2!=null && m1.getDraftMessageMetaDataDTO()!=null && m2.getDraftMessageMetaDataDTO()!=null && m1.getDraftMessageMetaDataDTO().getScheduledOn()!=null && m2.getDraftMessageMetaDataDTO().getScheduledOn()!=null && m1.getDraftMessageMetaDataDTO().getScheduledOn().before(m2.getDraftMessageMetaDataDTO().getScheduledOn()))
					return 1;
				else
					return -1;
			}
		});
		Collections.sort(scheduledDrafts, new Comparator<MessageDTO>() {
			public int compare(MessageDTO m1, MessageDTO m2) {
				if(m1!=null && m2!=null && m1.getDraftMessageMetaDataDTO()!=null && m2.getDraftMessageMetaDataDTO()!=null && m1.getDraftMessageMetaDataDTO().getScheduledOn()!=null && m2.getDraftMessageMetaDataDTO().getScheduledOn()!=null && m1.getDraftMessageMetaDataDTO().getScheduledOn().after(m2.getDraftMessageMetaDataDTO().getScheduledOn()))
					return 1;
				else
					return -1;
			}
		});
		Collections.sort(saveForLaterDrafts, new Comparator<MessageDTO>() {
			@Override
			public int compare(MessageDTO m1, MessageDTO m2) {
				if(m1!=null && m2!=null && m1.getId()<m2.getId())
					return 1;
				else
					return -1;
			}
		});
		drafts.addAll(failedDrafts);
		drafts.addAll(scheduledDrafts);
		drafts.addAll(saveForLaterDrafts);
		return drafts;
	}
	
	public List<MessageDTO> getMessagesForUuids(List<String> messageUuids,String userUuid) throws JsonProcessingException{
		
		List<MessageDTO> messageDTOList=new ArrayList<MessageDTO>();
		
		List<Message> messages=messageRepository.findByuuidInOrderByReceivedOnDesc(messageUuids);
		
		messageDTOList=getMessageDTOListForGivenMessages(messages,userUuid);
		
		log.info("in getMessagesForUuids request={} user_uuid={} final response object messages_size={}", 
				messageUuids.toString(),userUuid,messageDTOList.size());
		return messageDTOList;
	}
	
	private List<MessageDTO> getMessageDTOListForGivenMessages(List<Message> messages,String userUuid) {
		
		List<MessageDTO> messageDTOList=new ArrayList<MessageDTO>();
		
		if(messages!=null && !messages.isEmpty()) {
			List<Long> messageIdList=new ArrayList<Long>();
			HashMap<Long,MessageDTO> messageIdMessageMap=new HashMap<Long,MessageDTO>();
			for(Message messageIterator:messages) {
				messageIdList.add(messageIterator.getId());
				MessageDTO messageDTO=messageDTOMapper.map(messageIterator);
				messageDTO.setCustomerUuid(generalRepository.getCustomerUUIDFromCustomerID(messageIterator.getCustomerID()));
				messageDTO.setCustomerName(generalRepository.getCustomerNameFromId(messageIterator.getCustomerID()));
				messageDTO.setDealerAssociateUuid(generalRepository.getDealerAssociateUuidFromDealerAssociateId(messageIterator.getDealerAssociateID()));
				messageDTO.setDealerDepartmentUuid(generalRepository.getDepartmentUUIDForDepartmentID(messageIterator.getDealerDepartmentId()));
				messageDTO.setDealerUuid(generalRepository.getDealerUUIDFromDealerId(messageIterator.getDealerID()));
				messageDTOList.add(messageDTO);
				messageIdMessageMap.put(messageIterator.getId(), messageDTO);
			}
			getMessageExtnForMessageIds(messageIdList,messageIdMessageMap);
			
			getDraftMessageMetaDataForMessageIds(messageIdList,messageIdMessageMap);
			
			getMessageMetaDataForMessageIds(messageIdList,messageIdMessageMap);
			
			getMessageAttachments(messageIdList,messageIdMessageMap);
			
			getMessagePredictionsAndFeedback(messageIdList,messageIdMessageMap,userUuid);
			
			
		}
		
		return messageDTOList;
	}
	
	private void getMessageExtnForMessageIds(List<Long> messageIdList,HashMap<Long,MessageDTO> messageIdMessageMap){
		log.info("in getMessageExtnForMessageIds before fetching for message_id_list={} ",messageIdList.toString());
		
		List<MessageExtn> messageExtnList = messageExtnRepository.findByMessageIDIn(messageIdList);
		if(messageExtnList!=null && !messageExtnList.isEmpty()) {
			for(MessageExtn messageExtnIterator:messageExtnList) {
				MessageDTO tempMessage=messageIdMessageMap.get(messageExtnIterator.getMessageID());
				MessageExtnDTO messageExtnDTO=messageExtnDTOMapper.map(messageExtnIterator);
				messageExtnDTO.setMessageUuid(tempMessage.getUuid());
				tempMessage.setMessageExtnDTO(messageExtnDTO);
			}
		}
		log.info("in getMessageExtnForMessageIds after fetching for message_id_list={} ",messageIdList.toString());
	}
	
	private void getDraftMessageMetaDataForMessageIds(List<Long> messageIdList,HashMap<Long,MessageDTO> messageIdMessageMap){
		log.info("in getDraftMessageMetaDataForMessageIds before fetching for message_id_list={} ",messageIdList.toString());
		
		List<DraftMessageMetaData> draftMessageMetaDataList = draftMessageMetaDataRepository.findByMessageIDIn(messageIdList);
		if(draftMessageMetaDataList!=null && !draftMessageMetaDataList.isEmpty()) {
			for(DraftMessageMetaData draftMessageMetaDataIterator:draftMessageMetaDataList) {
				MessageDTO tempMessage=messageIdMessageMap.get(draftMessageMetaDataIterator.getMessageID());
				DraftMessageMetaDataDTO draftMessageMetaDataDTO=draftMessageMetaDataDTOMapper.map(draftMessageMetaDataIterator);
				draftMessageMetaDataDTO.setMessageUuid(tempMessage.getUuid());
				tempMessage.setDraftMessageMetaDataDTO(draftMessageMetaDataDTO);
			}
		}
		
		log.info("in getDraftMessageMetaDataForMessageIds after fetching for message_id_list={} ",messageIdList.toString());
		
	}
	
	private void getMessageMetaDataForMessageIds(List<Long> messageIdList,HashMap<Long,MessageDTO> messageIdMessageMap){
		
		log.info("in getMessageMetaDataForMessageIds before fetching for message_id_list={} ",messageIdList.toString());
		
		List<MessageMetaData> messageMetaDataList = messageMetaDataRepository.findByMessageIDIn(messageIdList);
		if(messageMetaDataList!=null && !messageMetaDataList.isEmpty()) {
			for(MessageMetaData messageMetaDataIterator:messageMetaDataList) {
				MessageDTO tempMessage=messageIdMessageMap.get(messageMetaDataIterator.getMessageID());
				MessageMetaDataDTO messageMetaData=messageMetaDataDTOMapper.map(messageMetaDataIterator);
				messageMetaData.setMessageUuid(tempMessage.getUuid());
				tempMessage.setMessageMetaDataDTO(messageMetaData);
			}
		}
		
		log.info("in getMessageMetaDataForMessageIds after fetching for message_id_list={} ",messageIdList.toString());
		
	}
	
	private void getMessagePredictionsAndFeedback(List<Long> messageIdList,HashMap<Long,MessageDTO> messageIdMessageMap,String userUuid) {
		
		log.info("in getMessagePredictionsAndFeedback before fetching for message_id_list={} user_uuid={}",messageIdList.toString(),userUuid);
		List<MessagePrediction> messagePredictionList=messagePredictionRepository.findByMessageIDIn(messageIdList);
		
		if(messagePredictionList!=null && !messagePredictionList.isEmpty()) {
			List<Long> messagePredictionIdList=new ArrayList<Long>();
			HashMap<Long,MessagePredictionDTO> messagePredictionMap=new HashMap<Long,MessagePredictionDTO>();
			
			for(MessagePrediction messagePredictionIterator:messagePredictionList) {
				MessagePredictionDTO messagePredictionDTOIterator = messagePredictionDTOMapper.map(messagePredictionIterator);
				MessageDTO tempMessage=messageIdMessageMap.get(messagePredictionIterator.getMessageID());
				messagePredictionDTOIterator.setMessageUuid(tempMessage.getUuid());
				Set<MessagePredictionDTO> messagePredictionSet = tempMessage.getMessagePredictionDTOSet();
				if(messagePredictionSet==null) {
					messagePredictionSet=new HashSet<MessagePredictionDTO>();
				}
				messagePredictionSet.add(messagePredictionDTOIterator);
				tempMessage.setMessagePredictionDTOSet(messagePredictionSet);
				PredictionFeatureDTO predictionFeatureIterator = messagePredictionDTOMapper.map(messagePredictionIterator.getPredictionFeature());
				messagePredictionDTOIterator.setPredictionFeature(predictionFeatureIterator);
				messagePredictionMap.put(messagePredictionIterator.getId(), messagePredictionDTOIterator);
				messagePredictionIdList.add(messagePredictionIterator.getId());
			}
			List<MessagePredictionFeedback> messagePredictionFeedbackList=messagePredictionFeedbackRepository.
					findAllByMessagePredictionIDInAndUserUUID(messagePredictionIdList, userUuid);
			
			
			if(messagePredictionFeedbackList!=null && !messagePredictionFeedbackList.isEmpty()) {
				
				for(MessagePredictionFeedback messagePredictionFeedbackIterator:messagePredictionFeedbackList) {
					MessagePredictionDTO messagePredictionDTO = messagePredictionMap.get(messagePredictionFeedbackIterator.getMessagePredictionID());
					MessagePredictionFeedbackDTO messagePredictionFeedbackDTO=messagePredictionFeedbackDTOMapper.map(messagePredictionFeedbackIterator);
					Set<MessagePredictionFeedbackDTO> messagePredictionFeedbackDTOSet=new HashSet<MessagePredictionFeedbackDTO>();
					if(messagePredictionDTO.getMessagePredictionFeedback()!=null && !messagePredictionDTO.getMessagePredictionFeedback().isEmpty()) {
						messagePredictionFeedbackDTOSet=messagePredictionDTO.getMessagePredictionFeedback();
					}
					messagePredictionFeedbackDTOSet.add(messagePredictionFeedbackDTO);
					messagePredictionDTO.setMessagePredictionFeedback(messagePredictionFeedbackDTOSet);
				}
			}
		}
		
		log.info("in getMessagePredictionsAndFeedback after fetching for message_id_list={} user_uuid={}",messageIdList.toString(),userUuid);
		
	}
	
	private void getMessageAttachments(List<Long> messageIdList,HashMap<Long,MessageDTO> messageIdMessageMap){
		log.info("in getMessageAttachments before fetching for message_id_list={} ",messageIdList.toString());
		List<DocFile> docFileList = docFileRepository.findByMessageIdIn(messageIdList);
		if(docFileList!=null && !docFileList.isEmpty()) {
			for(DocFile docFileIterator:docFileList) {
				MessageDTO tempMessage=messageIdMessageMap.get(docFileIterator.getMessageId());
				Set<DocFileDTO> docFileForMessageSet=tempMessage.getDocFiles();
				if(docFileForMessageSet==null){
					docFileForMessageSet=new HashSet<DocFileDTO>();
				}
				DocFileDTO docFileDTO=docFileDTOMapper.map(docFileIterator);
				docFileDTO.setMessageUuid(tempMessage.getUuid());
				docFileForMessageSet.add(docFileDTO);
				tempMessage.setDocFiles(docFileForMessageSet);
			}
		}
		log.info("in getMessageAttachments after fetching for message_id_list={} ",messageIdList.toString());
		
	}
}

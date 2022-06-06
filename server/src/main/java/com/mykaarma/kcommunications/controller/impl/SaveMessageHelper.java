package com.mykaarma.kcommunications.controller.impl;

import java.util.HashMap;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessage;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessageExtn;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessageMetaData;
import com.mykaarma.kcommunications.communications.repository.ExternalMessageExtnRepository;
import com.mykaarma.kcommunications.communications.repository.ExternalMessageMetaDataRepository;
import com.mykaarma.kcommunications.communications.repository.ExternalMessageRepository;
import com.mykaarma.kcommunications.jpa.repository.DocFileRepository;
import com.mykaarma.kcommunications.jpa.repository.DraftMessageMetaDataRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageAttributesRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageExtnRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageMetaDataRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.model.jpa.DocFile;
import com.mykaarma.kcommunications.model.jpa.DraftMessageMetaData;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageAttributes;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.model.jpa.MessageMetaData;
import com.mykaarma.kcommunications_model.request.SaveMessageRequest;
import com.mykaarma.kcustomer_model.dto.Customer;

@Service
public class SaveMessageHelper {
	
	private Logger LOGGER = LoggerFactory.getLogger(SaveMessageHelper.class);
	
	@Autowired
	MessageRepository messageRepository;
	
	@Autowired
	MessageExtnRepository messageExtnRepository;
	
	@Autowired
	DraftMessageMetaDataRepository draftMessageMetaDataRepository;
	
	@Autowired
	MessageMetaDataRepository messageMetaDataRepository;
	
	@Autowired
	MessageAttributesRepository messageAttributesRepository;
	
	@Autowired
	DocFileRepository docFileRepository;
	
	@Autowired
	private ExternalMessageRepository externalMessageRepository;
	
	@Autowired
	private ExternalMessageExtnRepository externalMessageExtnRepository;
	
	@Autowired
	private ExternalMessageMetaDataRepository externalMessageMetaDataRepository;
	
	private ObjectMapper objectMapper = new ObjectMapper();
	
	public Message saveMessage(Message message) throws Exception{
		MessageExtn messageExtn = message.getMessageExtn();
		DraftMessageMetaData draftMessageMetaData=message.getDraftMessageMetaData();
		MessageMetaData messageMetaData=message.getMessageMetaData();
		MessageAttributes messageAttributes = message.getMessageAttributes();
		Set<DocFile> docFiles=message.getDocFiles();
		LOGGER.info("in saveMessage message_object before saving={}", objectMapper.writeValueAsString(message));
		message = messageRepository.save(message);
		LOGGER.info("in saveMessage message_object after saving={}", objectMapper.writeValueAsString(message));
		message.setMessageExtn(messageExtn);
		message.setDraftMessageMetaData(draftMessageMetaData);
		message.setMessageMetaData(messageMetaData);
		message.setMessageAttributes(messageAttributes);
		message.setDocFiles(docFiles);
		LOGGER.info("in saveMessage message_object after re initialising={}", objectMapper.writeValueAsString(message));
		messageExtn.setMessageID(message.getId());
		 
		messageExtnRepository.save(messageExtn);
		if(message.getDraftMessageMetaData()!=null) {
			message.getDraftMessageMetaData().setMessageID(message.getId());
			draftMessageMetaDataRepository.save(message.getDraftMessageMetaData());
		}
		if(message.getMessageMetaData()!=null) {
			message.getMessageMetaData().setMessageID(message.getId());
			messageMetaDataRepository.save(message.getMessageMetaData());
		}
		if(messageAttributes != null) {
			message.getMessageAttributes().setMessageID(message.getId());
			messageAttributesRepository.save(message.getMessageAttributes());
		}
		if(message.getDocFiles()!=null && !message.getDocFiles().isEmpty()) {
			for(DocFile df : message.getDocFiles()) {
				df.setMessageId(message.getId());
				docFileRepository.save(df);
			}
		}
		return message;
	}
	
	public ExternalMessage saveMessageSentWithoutCustomer(ExternalMessage message) throws Exception{
		ExternalMessageMetaData metaData = message.getMessageMetaData();
		message.setMessageMetaData(metaData);
		ExternalMessageExtn externalMessageExtn = message.getMessageExtn();
		message = externalMessageRepository.save(message);
		externalMessageExtn.setMessageID(message.getId());
		externalMessageExtn = externalMessageExtnRepository.save(externalMessageExtn);
		metaData.setMessageID(message.getId());
		metaData = externalMessageMetaDataRepository.save(metaData);
		message.setMessageExtn(externalMessageExtn);
		message.setMessageMetaData(metaData);
		return message;
	}

	public void processMessageSaveRequest(SaveMessageRequest saveMessageRequest, Customer customer, Long dealerId,
			Long departmentId, String dealerUuid, String departmentUuid, String prefferedTextValue,
			String prefferedEmailValue) {
		
		
		
	}

}

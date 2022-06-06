package com.mykaarma.kcommunications.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.MessageType;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessage;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessageExtn;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessageMetaData;
import com.mykaarma.kcommunications.communications.repository.MessagePurposeRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCredentialsRepository;
import com.mykaarma.kcommunications.model.jpa.DocFile;
import com.mykaarma.kcommunications.model.jpa.DraftMessageMetaData;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageAttributes;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.model.jpa.MessageMetaData;
import com.mykaarma.kcommunications_model.common.AttachmentAttributes;
import com.mykaarma.kcommunications_model.common.DraftAttributes;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.SendEmailRequestBody;
import com.mykaarma.kcommunications_model.enums.DraftStatus;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessagePurpose;
import com.mykaarma.kcommunications_model.request.SaveMessageRequest;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcommunications_model.request.SendMessageWithoutCustomerRequest;
import com.mykaarma.kcustomer_model.dto.Customer;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;

@Service
public class ConvertToJpaEntity {

	private final static Logger LOGGER = LoggerFactory.getLogger(ConvertToJpaEntity.class);
	
	@Autowired
	private Helper helper;
	
	@Autowired
	VoiceCredentialsRepository voiceCredentialsRepository;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	GeneralRepository generalRepository;

	@Autowired
	private AppConfigHelper appConfigHelper;
	
	@Autowired
	private MessagePurposeRepository messagePurposeRepository;

	public Message getMessageJpaEntity(SendMessageRequest sendMessageRequest, Customer customer, DealerAssociateExtendedDTO dealerAssociate, String communicationValue) throws Exception {
		
		Message message = new Message();
		message = getMessageJpaEntity(sendMessageRequest.getMessageAttributes(), customer, dealerAssociate, communicationValue);
		String dsoForCountryCode = appConfigHelper.getDealerSetupOptionValueFromConfigService(dealerAssociate.getDepartmentExtendedDTO().getDealerMinimalDTO().getId(), DealerSetupOption.COMMUNICATIONS_COUNTRYCODE_ROLLOUT.getOptionKey());
		if(com.mykaarma.kcommunications_model.enums.MessageType.INCOMING.getMessageType().equalsIgnoreCase(
				sendMessageRequest.getMessageAttributes().getType().getMessageType())) {
			String fromNumber = sendMessageRequest.getIncomingMessageAttributes().getFromNumber();
			if(!"true".equalsIgnoreCase(dsoForCountryCode) && message.getProtocol().equalsIgnoreCase(MessageProtocol.TEXT.getMessageProtocol()) && fromNumber.length() > 10) {
				fromNumber = fromNumber.substring(2);
			}

			message.setFromName(helper.getCustomerName(customer));
			message.setFromNumber(fromNumber);
			message.setToName(helper.getDealerAssociateName(dealerAssociate));
		}
		else if(com.mykaarma.kcommunications_model.enums.MessageType.NOTE.getMessageType().equalsIgnoreCase(
				sendMessageRequest.getMessageAttributes().getType().getMessageType()) &&
				sendMessageRequest.getMessageAttributes().getIsManual() != null &&
				sendMessageRequest.getMessageAttributes().getIsManual()) {

			message.setFromName(helper.getDealerAssociateName(dealerAssociate));
			message.setFromNumber(null);
			message.setToName(null);
			message.setToNumber(null);
		}
		else {
			if(!"true".equalsIgnoreCase(dsoForCountryCode) && (communicationValue != null && message.getProtocol().equalsIgnoreCase(MessageProtocol.TEXT.getMessageProtocol()) && communicationValue.length()>10)) {
				communicationValue = communicationValue.substring(2);
			}
			message.setFromName(helper.getDealerAssociateName(dealerAssociate));
			message.setToName(helper.getCustomerName(customer));
			message.setToNumber(communicationValue);
		}
		if(sendMessageRequest.getIncomingMessageAttributes() != null && sendMessageRequest.getIncomingMessageAttributes().getCommunicationUID() != null
				&& !sendMessageRequest.getIncomingMessageAttributes().getCommunicationUID().isEmpty()) {
			message.setCommunicationUid(sendMessageRequest.getIncomingMessageAttributes().getCommunicationUID());
		}

		if(sendMessageRequest.getIncomingMessageAttributes() != null && sendMessageRequest.getIncomingMessageAttributes().getForwardedEmailReference() != null
				&& !sendMessageRequest.getIncomingMessageAttributes().getForwardedEmailReference().isEmpty()) {
			message.setEmailMessageId(sendMessageRequest.getIncomingMessageAttributes().getForwardedEmailReference());
		}
		
		message.setMessageMetaData(getMessageMetaDataJpaEntity(sendMessageRequest, customer, dealerAssociate.getDepartmentExtendedDTO().getId()));
		return message;
	}
	
	public ExternalMessage getExternalMessageJpaEntity(SendMessageWithoutCustomerRequest sendMessageRequest, SendEmailRequestBody sendEmailRequest) throws Exception {
		ExternalMessage message = new ExternalMessage();
		HashMap<String,String> metaDataMap = new HashMap<>();
		
		metaDataMap.put(APIConstants.COMMUNICATION_ID, null);
		metaDataMap.put(APIConstants.DELIVERY_FAILURE_REASON, null);
		
		if(sendMessageRequest != null) {
			com.mykaarma.kcommunications.communications.model.jpa.MessagePurpose messagePurpose = messagePurposeRepository.findOneByUuid(sendMessageRequest.getMessagePurposeUuid());
			if(ObjectUtils.isEmpty(messagePurpose)) {
				throw new Exception("Message purpose do not exist with the passed uuid = " + sendMessageRequest.getMessagePurposeUuid());
			}
			message.setMessageSize(sendMessageRequest.getMessageBody().length());
			message.setMessagePurpose(messagePurpose);
			message.setFromValue(sendMessageRequest.getFromNumber());
			message.setToValue(sendMessageRequest.getToNumber());
			message.setSentOn(new Date());
			message.setUuid(helper.getBase64EncodedSHA256UUID());
			message.setDeliveryStatus(APIConstants.MESSAGE_DELIVERY_SUCCESSFUL);
			message.setMessageProtocol(MessageProtocol.TEXT.getMessageProtocol());
			ExternalMessageExtn messageExtn = getExternalMessageExtnJpaEntity(sendMessageRequest.getMessageBody(), sendMessageRequest.getMessageSubject());
			message.setMessageExtn(messageExtn);
		} else if(sendEmailRequest != null) {
			message.setMessageSize(sendEmailRequest.getMessage().length());
			message.setFromValue(sendEmailRequest.getFromEmail());
			message.setToValue(sendEmailRequest.getToList());
			message.setSentOn(new Date());
			message.setUuid(helper.getBase64EncodedSHA256UUID());
			message.setDeliveryStatus(APIConstants.MESSAGE_DELIVERY_SUCCESSFUL);
			message.setMessageProtocol(MessageProtocol.EMAIL.getMessageProtocol());
			ExternalMessageExtn messageExtn = getExternalMessageExtnJpaEntity(sendEmailRequest.getMessage(), sendEmailRequest.getSubject());
			message.setMessageExtn(messageExtn);
			
			if(sendEmailRequest.getMessagePurposeUuid() != null && !sendEmailRequest.getMessagePurposeUuid().isEmpty()) {
				com.mykaarma.kcommunications.communications.model.jpa.MessagePurpose messagePurpose = messagePurposeRepository.findOneByUuid(sendEmailRequest.getMessagePurposeUuid());
				if(ObjectUtils.isEmpty(messagePurpose)) {
					throw new Exception("Message purpose do not exist with the passed uuid = " + sendEmailRequest.getMessagePurposeUuid());
				}
				message.setMessagePurpose(messagePurpose);
			} else {
				message.setMessagePurpose(null);
			}
			
			if(sendEmailRequest.getCcList() != null) {
				metaDataMap.put(APIConstants.CC_LIST, sendEmailRequest.getCcList());
			}
			if(sendEmailRequest.getBccList() != null) {
				metaDataMap.put(APIConstants.BCC_LIST, sendEmailRequest.getBccList());
			}
			if(sendEmailRequest.getFromName() != null) {
				metaDataMap.put(APIConstants.FROM_NAME, sendEmailRequest.getFromName());
			}
			if(sendEmailRequest.getUseDealerEmailCredentials() != null) {
				metaDataMap.put(APIConstants.USE_DEALER_EMAIL_CREDENTIALS, sendEmailRequest.getUseDealerEmailCredentials().toString());
			}
			if(sendEmailRequest.getAttachmentUrlAndNameMap() != null) {
				metaDataMap.put(APIConstants.ATTACHMENT_URL_AND_NAME_MAP, helper.getMessageMetaData(sendEmailRequest.getAttachmentUrlAndNameMap()));
			}
			if(sendEmailRequest.getReference() != null) {
				metaDataMap.put(APIConstants.REFERENCE, sendEmailRequest.getReference());
			}
		}
		
		String metaDataString = helper.getMessageMetaData(metaDataMap);
		ExternalMessageMetaData externalMessageMetaData = new ExternalMessageMetaData();
		externalMessageMetaData.setMetaData(metaDataString);
		message.setMessageMetaData(externalMessageMetaData);
		return message;
	}
	
//	public Message getMessageJpaEntity(SendMessageRequest sendMessageRequest, Customer customer, DealerAssociateExtendedDTO dealerAssociate, String communicationValue) throws Exception {
//		
//		Message message = new Message();
//		message.setDealerDepartmentId(dealerAssociate.getDepartmentExtendedDTO().getId());
//		message.setIsManual(sendMessageRequest.getMessageAttributes().getIsManual());
//		
//		message.setMessageSize(sendMessageRequest.getMessageAttributes().getBody().length());
//		message.setMessageType(sendMessageRequest.getMessageAttributes().getType().getMessageType());
//		message.setNumberofMessageAttachments(sendMessageRequest.getMessageAttributes().getAttachments()!=null?sendMessageRequest.getMessageAttributes().getAttachments().size():0);
//		message.setProtocol(sendMessageRequest.getMessageAttributes().getProtocol().getMessageProtocol());
//		
//
//		if(!MessageType.F.toString().equalsIgnoreCase(message.getMessageType()) && !MessageType.D.toString().equalsIgnoreCase(message.getMessageType())) {
//			message.setReceivedOn(new Date());
//			message.setRoutedOn(new Date());
//			message.setSentOn(new Date());
//		}
//
////		String dsoForCountryCode = appConfigHelper.getDealerSetupOptionValueFromConfigService(dealerAssociate.getDepartmentExtendedDTO().getDealerMinimalDTO().getId(), DealerSetupOption.COMMUNICATIONS_COUNTRYCODE_ROLLOUT.getOptionKey());
//
//		if(communicationValue != null && message.getProtocol().equalsIgnoreCase(MessageProtocol.TEXT.getMessageProtocol()) && communicationValue.length()>10) {
//			communicationValue = communicationValue.substring(2);
//		}
//
//		if(sendMessageRequest.getMessageAttributes().getTags()!=null && !sendMessageRequest.getMessageAttributes().getTags().isEmpty()) {
//			message.setTags(helper.getTags(sendMessageRequest.getMessageAttributes().getTags()));
//		}
//		
//		message.setUuid(helper.getBase64EncodedSHA256UUID());
//		message.setCustomerID(customer.getId());
//		message.setDealerID(dealerAssociate.getDepartmentExtendedDTO().getDealerMinimalDTO().getId());
//		message.setDealerAssociateID(dealerAssociate.getId());
//
//		String dsoForCountryCode = appConfigHelper.getDealerSetupOptionValueFromConfigService(dealerAssociate.getDepartmentExtendedDTO().getDealerMinimalDTO().getId(), DealerSetupOption.COMMUNICATIONS_COUNTRYCODE_ROLLOUT.getOptionKey());
//
//		if(com.mykaarma.kcommunications_model.enums.MessageType.INCOMING.getMessageType().equalsIgnoreCase(
//				sendMessageRequest.getMessageAttributes().getType().getMessageType())) {
//			String fromNumber = sendMessageRequest.getIncomingMessageAttributes().getFromNumber();
//			if(!"true".equalsIgnoreCase(dsoForCountryCode) && message.getProtocol().equalsIgnoreCase(MessageProtocol.TEXT.getMessageProtocol()) && fromNumber.length() > 10) {
//				fromNumber = fromNumber.substring(2);
//			}
//
//			message.setFromName(helper.getCustomerName(customer));
//			message.setFromNumber(fromNumber);
//			message.setToName(helper.getDealerAssociateName(dealerAssociate));
//		} else {
//			if(!"true".equalsIgnoreCase(dsoForCountryCode) && (communicationValue != null && message.getProtocol().equalsIgnoreCase(MessageProtocol.TEXT.getMessageProtocol()) && communicationValue.length()>10)) {
//				communicationValue = communicationValue.substring(2);
//			}
//
//			message.setFromName(helper.getDealerAssociateName(dealerAssociate));
//			message.setToName(helper.getCustomerName(customer));
//			message.setToNumber(communicationValue);
//		}
//
//		if(sendMessageRequest.getIncomingMessageAttributes() != null && sendMessageRequest.getIncomingMessageAttributes().getCommunicationUID() != null
//				&& !sendMessageRequest.getIncomingMessageAttributes().getCommunicationUID().isEmpty()) {
//			message.setCommunicationUid(sendMessageRequest.getIncomingMessageAttributes().getCommunicationUID());
//		}
//
//		if(sendMessageRequest.getIncomingMessageAttributes() != null && sendMessageRequest.getIncomingMessageAttributes().getForwardedEmailReference() != null
//				&& !sendMessageRequest.getIncomingMessageAttributes().getForwardedEmailReference().isEmpty()) {
//			message.setEmailMessageId(sendMessageRequest.getIncomingMessageAttributes().getForwardedEmailReference());
//		}
//		
//		MessageExtn messageExtn = getMessageExtnJpaEntity(sendMessageRequest);
//
//		
//		if(communicationValue != null && message.getProtocol().equalsIgnoreCase(MessageProtocol.TEXT.getMessageProtocol()) && communicationValue.length()>10) {
//				communicationValue = communicationValue.substring(2);
//		}
//		
//		message.setToNumber(communicationValue);
//		message.setMessageMetaData(getMessageMetaDataJpaEntity(sendMessageRequest));
//
//		message.setMessageAttributes(getMessageAttributesJpaEntity(sendMessageRequest));
//		
//		HashSet<DocFile> attachments = getAttachments(sendMessageRequest);
//		if(attachments!=null && !attachments.isEmpty()) {
//			message.setDocFiles(attachments);
//			message.setNumberofMessageAttachments(attachments.size());
//		}
//		
//		message.setMessageExtn(messageExtn);
//		message.setDeliveryStatus("1");
//		
//		LOGGER.info("in getMessageJpaEntity attachments={} sendmessagerequest={} message_object={}",
//				new ObjectMapper().writeValueAsString(sendMessageRequest.getMessageAttributes().getAttachments()),new ObjectMapper().writeValueAsString(sendMessageRequest)
//				,new ObjectMapper().writeValueAsString(message));
//		return message;
//	}
	
	public Message getMessageJpaEntityForInternalComment(String messageBody,
			GetDealerAssociateResponseDTO dealerAssociate, Long customerID, com.mykaarma.global.MessageProtocol messageProtocol, 
			com.mykaarma.global.MessagePurpose messagePurpose, Boolean isManual, List<Long> oneTimeNotifiers) throws Exception {
		Message message = new Message();
		MessageExtn messageExtn = new MessageExtn();
		MessageMetaData messageMetaData = new MessageMetaData();
		messageExtn.setMessageBody(messageBody);
		message.setDealerAssociateID(dealerAssociate.getDealerAssociate().getId());
		message.setCustomerID(customerID);
		message.setDealerDepartmentId(dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO().getId());
		message.setDealerID(dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO().getDealerMinimalDTO().getId());
		message.setProtocol(messageProtocol.name());
		if(messagePurpose!=null){
			message.setMessagePurpose(messagePurpose.name());
		}
		message.setMessageType(MessageType.I.name());
		message.setFromName(helper.getDealerAssociateName(dealerAssociate.getDealerAssociate()));
		message.setIsManual(isManual);
		message.setMessageExtn(messageExtn);
		message.setFromNumber("");
		message.setToNumber("");
		message.setToName("");
		message.setIsRead(false);
		message.setSentOn(new Date());
		message.setReceivedOn(new Date());
		message.setRoutedOn(new Date());
		message.setUuid(helper.getBase64EncodedSHA256UUID());
		message.setDeliveryStatus( "1");
		message.setMessageSize(messageBody.length());
		message.setNumberofMessageAttachments(0);
		message.setMessageExtn(messageExtn);
		if(com.mykaarma.global.MessageProtocol.T.name().equalsIgnoreCase(messageProtocol.name()) && oneTimeNotifiers!=null 
				&& !oneTimeNotifiers.isEmpty()) {
			String metaData = helper.prepareMetaData(APIConstants.USER_LIST, oneTimeNotifiers);
			if(metaData!=null) {
				messageMetaData.setMetaData(metaData);
				message.setMessageMetaData(messageMetaData);
			}
		}
		return message;
	}
	
	
	public MessageExtn getMessageExtnJpaEntity(String body, String subject) {
		
		MessageExtn messageExtn = new MessageExtn();
		messageExtn.setMessageBody(body);
		messageExtn.setSubject(subject);
		return messageExtn;
	}
	
	public ExternalMessageExtn getExternalMessageExtnJpaEntity(String body, String subject) {
		
		ExternalMessageExtn messageExtn = new ExternalMessageExtn();
		messageExtn.setMessageBody(body);
		messageExtn.setSubject(subject);
		return messageExtn;
	}
	
	public DraftMessageMetaData getDraftMessageMetaDataJpaEntity(DraftAttributes draftAttributes, MessageSendingAttributes messageSendingAttributes) {
		
		DraftMessageMetaData draftMessageMetaData = new DraftMessageMetaData();
		DraftAttributes draft = draftAttributes;
		if(draft.getDraftFailureReason()!=null) {
			draftMessageMetaData.setReasonForLastFailure(draft.getDraftFailureReason().name());
		}
		if(!DraftStatus.DISCARDED.equals(draft.getDraftStatus()) && !DraftStatus.DRAFTED.equals(draft.getDraftStatus())) {
			draftMessageMetaData.setScheduledOn(helper.getPstDateFromIsoDate(draft.getScheduledOn()));
		}
		draftMessageMetaData.setStatus(draft.getDraftStatus().name());
		draftMessageMetaData.setAddSignature(false);
		if(messageSendingAttributes!=null && messageSendingAttributes.getAddSignature()) {
			draftMessageMetaData.setAddSignature(true);
		}
		return draftMessageMetaData;
	}
	
	public MessageMetaData getMessageMetaDataJpaEntity(SendMessageRequest sendMessageRequest, Customer customer, Long departmentId) throws Exception {
		String metaData = helper.prepareMetaData(sendMessageRequest, customer, departmentId);
		if(metaData!=null) {
			MessageMetaData messageMetaData = new MessageMetaData();
			messageMetaData.setMetaData(metaData);
			return messageMetaData;
		}
		return null;
	}
	
	public MessageAttributes getMessageAttributesJpaEntity(com.mykaarma.kcommunications_model.common.MessageAttributes messageeAttributesModel) throws Exception {
		MessageAttributes messageAttributes = null;
		if (messageeAttributesModel.getUpdateTotalMessageCount() != null || messageeAttributesModel.getShowInCustomerConversation() != null) {
			messageAttributes = new MessageAttributes();
			Boolean updateTotalMessageCount = messageeAttributesModel.getUpdateTotalMessageCount();
			messageAttributes.setCountInThreadMessageCount(updateTotalMessageCount == null ? true : updateTotalMessageCount);
			Boolean showInCustomerConversation = messageeAttributesModel.getShowInCustomerConversation();
			messageAttributes.setShowInCustomerConversation(showInCustomerConversation == null ? true : showInCustomerConversation);
		}
		return messageAttributes;
	}
	

	public Message createVoiceCallMessage(String dealerAssociateUUID, Long dealerID,Long dealerAssociateID,Long customerID,
			 String callSid, String contactNo, boolean fromDealer, String brokerNumber, String fromName, Long dealerDepartmentID) {
		
		Message m = new Message();
		try {
			
			String departmentUUID = voiceCredentialsRepository.getDepartmentUUIDForBrokerNumber(brokerNumber);
			DealerAssociateExtendedDTO dealerAssociate = kManageApiHelper.getDealerAssociateForDealerAssociateUUID(departmentUUID, dealerAssociateUUID);	
			
			MessageExtn messageExtn = new MessageExtn();
			
			m.setMessageExtn(messageExtn);
			m.setDealerID(dealerID);
			m.setDealerAssociateID(dealerAssociateID);
			m.setMessageType(com.mykaarma.kcommunications_model.enums.MessageType.OUTGOING.getMessageType());
			m.setProtocol(MessageProtocol.VOICE_CALL.getMessageProtocol());			
			m.setCustomerID(customerID);
			m.setIsRead(false);
			m.setSentOn(new Date());
			m.setReceivedOn(new Date());
			m.setNumberofMessageAttachments(0);
			m.setRoutedOn(new Date());
			m.setCommunicationUid(callSid); 
			m.setDeliveryStatus("1");
			m.setDealerDepartmentId(dealerDepartmentID);
			m.getMessageExtn().setMessageBody("Voice Call");
			m.setUuid(helper.getBase64EncodedSHA256UUID());

			if (fromDealer){
				
				m.setMessageType(com.mykaarma.kcommunications_model.enums.MessageType.OUTGOING.getMessageType());
			}
			else{
				
				m.setMessageType(com.mykaarma.kcommunications_model.enums.MessageType.INCOMING.getMessageType());
			}
			
			m.setIsManual(true);
			
			
			String dealerUserName = (dealerAssociate.getFirstName() == null ? "": dealerAssociate.getFirstName())
										+ " "+ (dealerAssociate.getLastName() == null ? "": dealerAssociate.getLastName());
			String customerName = "";
			
			if (customerID != null)
			{
				
				customerName = generalRepository.getCustomerNameFromId(customerID);
			}

			if (fromDealer) {
				
				m.setFromName(dealerUserName);
				
				if(fromName != null && fromName.length()>0){					
					m.setFromName(fromName);
				}
				if(brokerNumber == null){
					m.setFromNumber(m.getFromNumber());
				}
				else{					
					m.setFromNumber(brokerNumber);
				}
				
				m.setToName(customerName);
				m.setToNumber(contactNo);

			} else {
				
				m.setFromName(customerName);
				m.setFromNumber(contactNo);
				m.setToName(dealerUserName);
				m.setToNumber(m.getToNumber());

			}
		}catch (Exception e) {
			LOGGER.info("Error in creatingVoiceCall message for callSID = {}, dealerAssociateUUID = {}", callSid, dealerAssociateUUID, e);
		}

		return m;		
		
	}
	
	public Message prepareIncomingMessageObject(Long customerID, Long dealerID, Long  dealerAssociateID,String subject, String body, String fromNumber, String protocol, 
			String communicationUID, Integer numberOfMessageAttachments, Boolean deliveryFailed, String messageUUID, Long dealerDepartmentID){
				
		Message bizmsg = new Message();
		com.mykaarma.kcommunications.model.jpa.MessageExtn messageExtn = new com.mykaarma.kcommunications.model.jpa.MessageExtn();
		
		String customerName = generalRepository.getCustomerNameFromId(customerID);
		bizmsg.setMessageExtn(messageExtn);
		bizmsg.setMessageType(com.mykaarma.kcommunications_model.enums.MessageType.INCOMING.getMessageType());
		bizmsg.setProtocol(protocol);
		bizmsg.setCommunicationUid(communicationUID);
		bizmsg.setFromNumber(fromNumber);
		bizmsg.setFromName(customerName);
		bizmsg.getMessageExtn().setSubject(subject + customerName);
		bizmsg.getMessageExtn().setMessageBody(body);
		bizmsg.setMessageSize(body.length());
		bizmsg.setSentOn(new Date());
		bizmsg.setReceivedOn(new Date());
		bizmsg.setIsRead(false);
		bizmsg.setCustomerID(customerID);
		bizmsg.setNumberofMessageAttachments(numberOfMessageAttachments);
		bizmsg.setDealerID(dealerID);
		bizmsg.setDealerAssociateID(dealerAssociateID);
		bizmsg.setToName(generalRepository.getDealerAssociateName(dealerAssociateID));
		bizmsg.setDealerDepartmentId(dealerDepartmentID);
		bizmsg.setIsManual(true);
		bizmsg.setUuid(messageUUID);
		bizmsg.setDeliveryStatus("1");
		if(bizmsg.getUuid()==null) {
			bizmsg.setUuid(helper.getBase64EncodedSHA256UUID());
		}
		return bizmsg;
	}
	

	public HashSet<DocFile> getAttachments(List<AttachmentAttributes> attachments) {
		
		if(!helper.isListEmpty(attachments)) {
			HashSet<DocFile> attach = new HashSet<DocFile>();
			for(AttachmentAttributes attachment : attachments) {
				DocFile docFile = new DocFile();
				docFile.setDocFileName(attachment.getFileURL());
				docFile.setDocSize(attachment.getDocSize());
				docFile.setMimeType(attachment.getMimeType());
				docFile.setFileExtension(attachment.getAttachmentExtension());
				docFile.setOriginalFileName(attachment.getOriginalFileName());
				docFile.setThumbnailFileName(attachment.getThumbnailURL());
				docFile.setMediaPreviewURL(attachment.getMediaPreviewURL());
				attach.add(docFile);
			}
			return attach;
		}
		return null;
	}
	
    public Message getMessageJpaEntity(com.mykaarma.kcommunications_model.common.MessageAttributes messageeAttributes, Customer customer,
			DealerAssociateExtendedDTO dealerAssociate, String communicationValue) throws Exception{
    	
    	Message message = new Message();
    	message.setDealerDepartmentId(dealerAssociate.getDepartmentExtendedDTO().getId());
		message.setFromName(helper.getDealerAssociateName(dealerAssociate));
		message.setIsManual(messageeAttributes.getIsManual());
		
		message.setMessageSize(messageeAttributes.getBody().length());
		message.setMessageType(messageeAttributes.getType().getMessageType());
		message.setNumberofMessageAttachments(messageeAttributes.getAttachments()!=null?messageeAttributes.getAttachments().size():0);
		message.setProtocol(messageeAttributes.getProtocol().getMessageProtocol());
		
		if(!MessageType.F.toString().equalsIgnoreCase(message.getMessageType()) && !MessageType.D.toString().equalsIgnoreCase(message.getMessageType())) {
			message.setReceivedOn(new Date());
			message.setRoutedOn(new Date());
			message.setSentOn(new Date());
		}

		if(communicationValue != null && message.getProtocol().equalsIgnoreCase(MessageProtocol.TEXT.getMessageProtocol()) && communicationValue.length()>10) {
			communicationValue = communicationValue.substring(2);
		}
		
		if(messageeAttributes.getTags()!=null && !messageeAttributes.getTags().isEmpty()) {
			message.setTags(helper.getTags(messageeAttributes.getTags()));
		}
		message.setToName(helper.getCustomerName(customer));
		message.setUuid(helper.getBase64EncodedSHA256UUID());
		message.setCustomerID(customer.getId());
		message.setDealerID(dealerAssociate.getDepartmentExtendedDTO().getDealerMinimalDTO().getId());
		message.setDealerAssociateID(dealerAssociate.getId());
		MessageExtn messageExtn = getMessageExtnJpaEntity(messageeAttributes.getBody(), messageeAttributes.getSubject());
		
        if(MessageType.F.name().equalsIgnoreCase(message.getMessageType()) 
            || MessageType.D.name().equalsIgnoreCase(message.getMessageType())) {
			DraftMessageMetaData draftMessageMetaData = getDraftMessageMetaDataJpaEntity(messageeAttributes.getDraftAttributes(), null);
			message.setDraftMessageMetaData(draftMessageMetaData);
			message.setMessagePurpose(MessagePurpose.F.name());
		}
        
        if(messageeAttributes.getPurpose()!=null) {
			message.setMessagePurpose(messageeAttributes.getPurpose().name());
		}

		if(messageeAttributes.getDefaultReplyAction() != null) {
			message.setDefaultReplyAction(messageeAttributes.getDefaultReplyAction());
		}
		
		message.setMessageAttributes(getMessageAttributesJpaEntity(messageeAttributes));
		HashSet<DocFile> attachments = getAttachments(messageeAttributes.getAttachments());
		if(attachments!=null && !attachments.isEmpty()) {
			message.setDocFiles(attachments);
			message.setNumberofMessageAttachments(attachments.size());
		}
		message.setMessageExtn(messageExtn);
		if(MessageType.D.name().equalsIgnoreCase(message.getMessageType())) {
			message.setDeliveryStatus("0");
		} else {
			message.setDeliveryStatus("1");
		}
		LOGGER.info("in getMessageJpaEntity attachments={} sendmessagerequest={} message_object={}",
				new ObjectMapper().writeValueAsString(attachments),new ObjectMapper().writeValueAsString(messageeAttributes)
				,new ObjectMapper().writeValueAsString(message));
		return message;
		
    }
    
	public Message getMessageJpaEntityForSaveMessageRequest(SaveMessageRequest saveMessageRequest, Customer customer,
			DealerAssociateExtendedDTO dealerAssociate, String communicationValue) throws Exception {
		
		Message message = new Message();
		message = getMessageJpaEntity(saveMessageRequest.getMessageAttributes(), customer, dealerAssociate, communicationValue);
		
		if(communicationValue!= null && message.getProtocol().equalsIgnoreCase(MessageProtocol.TEXT.getMessageProtocol()) && communicationValue.length()>10) {
			communicationValue = communicationValue.substring(2);
		}	
		
		message.setToNumber(communicationValue);
		if(MessageType.I.name().equalsIgnoreCase(message.getMessageType())) {
			message.setToNumber(null);
			message.setFromNumber(communicationValue);
			message.setFromName(helper.getCustomerName(customer));
			message.setToName(helper.getDealerAssociateName(dealerAssociate));
		}
		
		message.setReceivedOn(saveMessageRequest.getReceivedOn());
		message.setRoutedOn(saveMessageRequest.getReceivedOn());
		message.setSentOn(saveMessageRequest.getSentOn());
		return message;
		
	}
}

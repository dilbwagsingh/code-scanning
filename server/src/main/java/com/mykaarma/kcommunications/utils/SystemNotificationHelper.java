package com.mykaarma.kcommunications.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.Authority;
import com.mykaarma.global.CustomerSentiment;
import com.mykaarma.global.Delegator;
import com.mykaarma.global.TemplateType;
import com.mykaarma.kcommunications.controller.impl.CommunicationsApiImpl;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications_model.common.AttachmentAttributes;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.NotificationAttributes;
import com.mykaarma.kcommunications_model.common.NotificationButton;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessagePurpose;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.NotificationButtonTheme;
import com.mykaarma.kcommunications_model.enums.Tag;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetEmailTemplateResponseDTO;


@Service
public class SystemNotificationHelper {
	
	private final static ObjectMapper objectMapper = new ObjectMapper();
	
	private final static Logger LOGGER = LoggerFactory.getLogger(SystemNotificationHelper.class);
	
	@Autowired
	private KManageApiHelper kManageApiHelper;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	@Autowired
	private CommunicationsApiImpl communicationsApiImpl;
	
	@Autowired
	private Helper helper;
	
	private static final String DELEGATED_FROM_NAME_PARAM = "_delegated_from_name";
	
	private static final String DELEGATEE_NAME_PARAM = "_delegatee_name";
	
	private static final String OLD_MESSAGE_BODY_PARAM = "_old_message_body";
	
	private static final String CUSTOMER_ID_KEY = "CUSTOMER_ID";
	
	private static final String MINIMIZE_NOTIFIER_KEY = "MinimizeNotifier";
	
	private static final String VIEW_BUTTON_TEXT = "VIEW";
	
	private static final String VIEW_BUTTON_TEXT_KEY = "unassignedViewButton";
	
	private static final String VIEW_BUTTON_WIDGET_KEY = "messagedisplay_widget";
	
	public void addNoteForOoODelegation(Message message, Delegator delegator, Long delegatedFromDAID, Long delegatedToDAID) {
		String dealerUUID = generalRepository.getDealerUUIDFromDealerId(message.getDealerID());
		String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(message.getDealerDepartmentId());
		String customerUUID = generalRepository.getCustomerUUIDFromCustomerID(message.getCustomerID());
		String delegatedToDAUUID = generalRepository.getUserUUIDForDealerAssociateID(delegatedToDAID);
		String dealerLocale = helper.getDealerPreferredLocale(dealerUUID);

		if(!validateNoteForOoODelegation(message, delegator, delegatedFromDAID, delegatedToDAID, dealerUUID)) {
			return;
		}
		
		String messageBody = null;
		try {
			GetEmailTemplateResponseDTO emailTemplateResponseDTO  = kManageApiHelper.getEmailTemplate(dealerUUID, TemplateType.OOO_DELEGATION_NOTE.getTitle(), dealerLocale);
			
			messageBody = emailTemplateResponseDTO.getEmailTemplate();
			if (messageBody == null || messageBody.trim().isEmpty()) {
				LOGGER.warn("Error while getting template for posting Note for delegation due to OoO. message_id={} dealer_id={}", message.getId(), message.getDealerID());
				return;
			}
			messageBody = messageBody.replace(DELEGATED_FROM_NAME_PARAM, generalRepository.getDealerAssociateName(delegatedFromDAID));
			messageBody = messageBody.replace(DELEGATEE_NAME_PARAM, generalRepository.getDealerAssociateName(delegatedToDAID));
		} catch (Exception e) {
			LOGGER.warn("Error while getting template for posting Note for delegation due to OoO. message_id={} dealer_id={}", message.getId(), message.getDealerID(), e);
			return;
		}
		
		MessageAttributes messageAttributes = getMessageAttributes(messageBody, false, MessageProtocol.NONE,
				MessagePurpose.DELEGATION_OOO_NOTE, null, null, MessageType.NOTE, false, false, true, null, null);
		MessageSendingAttributes messageSendingAttributes = getMessageSendingAttributes(null, null, false,
				false, false, null, null, 0, false, false, false, false);
		NotificationAttributes notificationAttributes = getNotificationAttributes(false, false, false, null, null);
		SendMessageRequest sendMessageRequest = getSendMessageRequest(messageAttributes, messageSendingAttributes, notificationAttributes);
		
		try {
			communicationsApiImpl.createMessage(customerUUID, departmentUUID, delegatedToDAUUID, sendMessageRequest, null);
		} catch (Exception e) {
			LOGGER.error("Error in Posting Note for delegation due to OoO message_id={} delegated_from_id={} delegated_to_id={}", message.getId(), delegatedFromDAID, delegatedToDAID, e);
		}
	}

	private boolean validateNoteForOoODelegation(Message message, Delegator delegator, Long delegatedFromDAID, Long delegatedToDAID, String dealerUUID) {
		if (!Delegator.OUT_OF_OFFICE_START.getDelegator().equalsIgnoreCase(delegator.getDelegator())) {
			LOGGER.info("Delegator is not OutOfOffice. Not posting Note in customer conversation message_id={} delegated_from_id={} delegated_to_id={} delegator={}",
					message.getId(), delegatedFromDAID, delegatedToDAID, delegator);
			return false;
		}

		return true;
	}
	
	public void saveUpsetCustomerOrWFRNotification(String departmentUUID, String messageBody, Long customerID, Long dealerAssociateID, String subject, MessagePurpose messagePurpose) {
		
		try {
			String customerUUID = generalRepository.getCustomerUUIDFromCustomerID(customerID);
			String userUUID = generalRepository.getUserUUIDForDealerAssociateID(dealerAssociateID);

			MessageAttributes messageAttributes = getMessageAttributes(messageBody, false, MessageProtocol.NONE,
					messagePurpose, subject, null, MessageType.NOTE, false, false, true, null, null);

			MessageSendingAttributes messageSendingAttributes = getMessageSendingAttributes(null, null, false,
					false, false, null, null, 0, false, false, false, false);

			NotificationAttributes notificationAttributes = getNotificationAttributes(false, false, false, null, null);

			SendMessageRequest sendMessageRequest = getSendMessageRequest(messageAttributes, messageSendingAttributes, notificationAttributes);

			communicationsApiImpl.createMessage(customerUUID, departmentUUID, userUUID, sendMessageRequest, null);
			
		} catch (Exception e) {
			LOGGER.error("Error while posting upset customer notification for customer_id={}", customerID, e);
		}
	}
	
	public void saveThreadOwnershipChangedNote(String departmentUUID, String messageBody, String customerUUID, Long dealerAssociateID, String subject, MessagePurpose messagePurpose) {
		
		try {
			String userUUID = generalRepository.getUserUUIDForDealerAssociateID(dealerAssociateID);

			MessageAttributes messageAttributes = getMessageAttributes(messageBody, false, MessageProtocol.NONE,
					messagePurpose, subject, null, MessageType.NOTE, false, false, true, null, null);

			MessageSendingAttributes messageSendingAttributes = getMessageSendingAttributes(null, null, false,
					false, false, null, null, 0, false, false, false, false);

			NotificationAttributes notificationAttributes = getNotificationAttributes(false, false, false, null, null);

			SendMessageRequest sendMessageRequest = getSendMessageRequest(messageAttributes, messageSendingAttributes, notificationAttributes);

			communicationsApiImpl.createMessage(customerUUID, departmentUUID, userUUID, sendMessageRequest, null);
			
		} catch (Exception e) {
			LOGGER.error("Error while posting upset customer notification for customer_uuid={}", customerUUID, e);
		}
	}
	
	public void sendUnassignedNotification(Message message) {
		String dealerUUID = generalRepository.getDealerUUIDFromDealerId(message.getDealerID());
		String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(message.getDealerDepartmentId());
		String dealerLocale = helper.getDealerPreferredLocale(dealerUUID);

		if(!validateUnassignedNotification(message, dealerUUID, departmentUUID)) {
			return;
		}
		
		List<String> departmentUuids = new ArrayList<>();
		departmentUuids.add(departmentUUID);
		Set<DealerAssociateExtendedDTO> dealerAssociatesWithAuth = kManageApiHelper.getDealerAssociatesHavingAuthority(Authority.NOTIFY_ALL_UNASSIGNED.getAuthority(), departmentUuids);
		
		try {
			List<String> notificationDAUUIDs = new ArrayList<>();
			if (dealerAssociatesWithAuth != null && !dealerAssociatesWithAuth.isEmpty()) {
				for (DealerAssociateExtendedDTO da : dealerAssociatesWithAuth) {
					notificationDAUUIDs.add(da.getUuid());
				}
				LOGGER.info("Users found with authority to receive notification for unassigned messages. Proceeding. "
						+ "failed_message_id={} dealer_id={} dealer_associate_uuids={}", message.getId(), message.getDealerID(), notificationDAUUIDs);

				String templateName = TemplateType.UNASSIGNED_MESSAGE_NOTIFICATION.getTitle();
				if (MessageProtocol.VOICE_CALL.getMessageProtocol().equalsIgnoreCase(message.getProtocol())) {
					templateName = TemplateType.UNASSIGNED_CALL_NOTIFICATION.getTitle();
				}

				GetEmailTemplateResponseDTO emailTemplateResponseDTO  = kManageApiHelper.getEmailTemplate(dealerUUID, templateName, dealerLocale);
				if(emailTemplateResponseDTO == null || emailTemplateResponseDTO.getEmailTemplate() == null || emailTemplateResponseDTO.getEmailTemplate().trim().isEmpty()) {
					LOGGER.warn("Error while getting template={} posting notification of unassigned message. message_id={} dealer_id={}", templateName, message.getId(), message.getDealerID());
					return;
				}

				String messageBody = emailTemplateResponseDTO.getEmailTemplate();
				messageBody = messageBody.replace(OLD_MESSAGE_BODY_PARAM, message.getMessageExtn().getMessageBody());

				String customerUUID = generalRepository.getCustomerUUIDFromCustomerID(message.getCustomerID());
				String userUUID = generalRepository.getUserUUIDForDealerAssociateID(message.getDealerAssociateID());
				
				MessageAttributes messageAttributes = getMessageAttributes(messageBody, false, MessageProtocol.NONE,
						MessagePurpose.UNASSIGNED_MESSAGE_NOTIFICATION, null, null, MessageType.NOTE, false, false, false, null, null);
				
				MessageSendingAttributes messageSendingAttributes = getMessageSendingAttributes(null, null, false,
						false, false, null, null, 0, false, false, false, false);
				
				NotificationAttributes notificationAttributes = getNotificationAttributes(false, false, false, notificationDAUUIDs, getNotificationButtonsForUnassignedNotification(message.getCustomerID()));
				
				SendMessageRequest sendMessageRequest = getSendMessageRequest(messageAttributes, messageSendingAttributes, notificationAttributes);
				
				communicationsApiImpl.createMessage(customerUUID, departmentUUID, userUUID, sendMessageRequest, null);
			} else {
				LOGGER.info("No dealerassociates found with authority {} Not posting notification for unassigned message message_id={}", Authority.NOTIFY_ALL_UNASSIGNED.getAuthority(), message.getId());
			}
		} catch (Exception e) {
			LOGGER.error("Error while sending notification for unassigned message. message_id={}", message.getId(), e);
		}
	}

 	private boolean validateUnassignedNotification(Message message, String dealerUUID, String departmentUUID) {
		if (!((MessageProtocol.TEXT.getMessageProtocol().equalsIgnoreCase(message.getProtocol()) || MessageProtocol.EMAIL.getMessageProtocol().equalsIgnoreCase(message.getProtocol())
				|| MessageProtocol.VOICE_CALL.getMessageProtocol().equalsIgnoreCase(message.getProtocol())) && MessageType.INCOMING.getMessageType().equalsIgnoreCase(message.getMessageType()))) {
			LOGGER.info("Message is not Incoming Type. Not posting Note for Unassigned Customer in customer conversation message_id={} ", message.getId());
			return false;
		}

		DealerAssociateExtendedDTO defaultDealerAssociate = null;
		GetDealerAssociateResponseDTO response = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);
		if(response != null && response.getDealerAssociate() != null) {
			defaultDealerAssociate = response.getDealerAssociate();
		}

		if (defaultDealerAssociate == null || defaultDealerAssociate.getId() == null) {
			LOGGER.warn("Default user not found. Not sending unassigned message notification for message_id={} dealer_department_uuid={}", message.getId(), departmentUUID);
			return false;
		} else if (!defaultDealerAssociate.getId().equals(message.getDealerAssociateID())) {
			LOGGER.warn("Message not assigned to default user. Not sending unassigned message notification for message_id={} dealer_department_uuid={}", message.getId(), departmentUUID);
			return false;
		}

		return true;
	}
	
	private SendMessageRequest getSendMessageRequest(MessageAttributes messageAttributes, MessageSendingAttributes messageSendingAttributes,
			NotificationAttributes notificationAttributes) {
		SendMessageRequest sendMessageRequest = new SendMessageRequest();
		sendMessageRequest.setMessageAttributes(messageAttributes);
		sendMessageRequest.setMessageSendingAttributes(messageSendingAttributes);
		sendMessageRequest.setNotificationAttributes(notificationAttributes);
		return sendMessageRequest;
	}
	
	private MessageAttributes getMessageAttributes(String messageBody, Boolean isManualMessage, MessageProtocol messageProtocol, MessagePurpose messagePurpose,
			String subject, List<Tag> tags, MessageType messageType, Boolean updateThreadTimestamp, Boolean updateTotalMessageCount, Boolean showInCustomerConversation,
			HashMap<String, String> metaData, ArrayList<AttachmentAttributes> attachments) {
		MessageAttributes messageAttributes = new MessageAttributes();
		messageAttributes.setBody(messageBody);
		messageAttributes.setIsManual(isManualMessage);
		messageAttributes.setProtocol(messageProtocol);
		messageAttributes.setPurpose(messagePurpose);
		messageAttributes.setSubject(subject);
		messageAttributes.setTags(tags);
		messageAttributes.setType(messageType);
		messageAttributes.setUpdateThreadTimestamp(updateThreadTimestamp);
		messageAttributes.setUpdateTotalMessageCount(updateTotalMessageCount);
		messageAttributes.setMetaData(metaData);
		messageAttributes.setAttachments(attachments);
		messageAttributes.setShowInCustomerConversation(showInCustomerConversation);
		return messageAttributes;
	}
	
	private MessageSendingAttributes getMessageSendingAttributes(List<String> listOfEmailsToBeCCed, List<String> listOfEmailsToBeBCCed,
			Boolean addFooter, Boolean addSignature, Boolean addTCPAFooter, String callbackURL, String communicationValueOfCustomer,
			Integer delay, Boolean overrideHolidays, Boolean overrideOptoutRules, Boolean sendSynchronously, Boolean sendVCard) {
		MessageSendingAttributes messageSendingAttributes = new MessageSendingAttributes();
		messageSendingAttributes.setListOfEmailsToBeCCed(listOfEmailsToBeCCed);
		messageSendingAttributes.setListOfEmailsToBeBCCed(listOfEmailsToBeBCCed);
		messageSendingAttributes.setAddFooter(addFooter);
		messageSendingAttributes.setAddSignature(addSignature);
		messageSendingAttributes.setAddTCPAFooter(addTCPAFooter);
		messageSendingAttributes.setCallbackURL(callbackURL);
		messageSendingAttributes.setCommunicationValueOfCustomer(communicationValueOfCustomer);
		messageSendingAttributes.setDelay(delay);
		messageSendingAttributes.setOverrideHolidays(overrideHolidays);
		messageSendingAttributes.setOverrideOptoutRules(overrideOptoutRules);
		messageSendingAttributes.setSendSynchronously(sendSynchronously);
		messageSendingAttributes.setSendVCard(sendVCard);
		return messageSendingAttributes;
	}
	
	private NotificationAttributes getNotificationAttributes(Boolean threadOwnerNotifierPop, Boolean externalSubscribersNotifierPop,
			Boolean internalSubscribersNotifierPop, List<String> additionalNotifierNotificationDAUUIDs, List<NotificationButton> notificationButtons) {
		NotificationAttributes notificationAttributes = new NotificationAttributes();
		notificationAttributes.setThreadOwnerNotifierPop(threadOwnerNotifierPop);
		notificationAttributes.setExternalSubscribersNotifierPop(externalSubscribersNotifierPop);
		notificationAttributes.setInternalSubscribersNotifierPop(internalSubscribersNotifierPop);
		notificationAttributes.setAdditionalNotifierNotificationDAUUIDs(additionalNotifierNotificationDAUUIDs);
		notificationAttributes.setNotificationButtons(notificationButtons);
		return notificationAttributes;
	}
	
	private List<NotificationButton> getNotificationButtonsForUnassignedNotification(Long customerId) {
		List<NotificationButton> notificationButtons = new ArrayList<NotificationButton>();
		
		NotificationButton notificationButton = new NotificationButton();
		HashMap<String, String> buttonActionEventData = new HashMap<String, String>();
		
		buttonActionEventData.put(CUSTOMER_ID_KEY, customerId+"");
		buttonActionEventData.put(MINIMIZE_NOTIFIER_KEY, "true");
		notificationButton.setButtonActionEventData(buttonActionEventData);
		notificationButton.setButtonDefaultText(VIEW_BUTTON_TEXT);
		notificationButton.setButtonTextTranslationKey(VIEW_BUTTON_TEXT_KEY);
		notificationButton.setButtonTextTranslationWidgetKey(VIEW_BUTTON_WIDGET_KEY);
		notificationButton.setButtonTheme(NotificationButtonTheme.PRIMARY);
		notificationButtons.add(notificationButton);
		
		return notificationButtons;
	}

}

package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.mykaarma.global.Authority;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.kcommunications.model.api.ErrorCodes;
import com.mykaarma.kcommunications.model.jpa.CommunicationStatus;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.KMessagingApiHelper;
import com.mykaarma.kcommunications_model.common.AttachmentAttributes;
import com.mykaarma.kcommunications_model.common.CommunicationAttributes;
import com.mykaarma.kcommunications_model.common.DraftAttributes;
import com.mykaarma.kcommunications_model.common.Event;
import com.mykaarma.kcommunications_model.common.IncomingMessageAttributes;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.MultipleMessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.User;
import com.mykaarma.kcommunications_model.common.UserEvent;
import com.mykaarma.kcommunications_model.common.VoiceCallRequest;
import com.mykaarma.kcommunications_model.enums.CategoryEvent;
import com.mykaarma.kcommunications_model.enums.CommunicationCategoryName;
import com.mykaarma.kcommunications_model.enums.DraftStatus;
import com.mykaarma.kcommunications_model.enums.EditorType;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessagePurpose;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.NotificationType;
import com.mykaarma.kcommunications_model.enums.OptOutState;
import com.mykaarma.kcommunications_model.enums.OptOutStatusUpdateEvent;
import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.enums.UpdateOptOutStatusRequestType;
import com.mykaarma.kcommunications_model.enums.WarningCode;
import com.mykaarma.kcommunications_model.request.CommunicationsOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.CustomersOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.DoubleOptInDeploymentRequest;
import com.mykaarma.kcommunications_model.request.ForwardMessageRequest;
import com.mykaarma.kcommunications_model.request.MultipleCustomersPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.MultipleMessageRequest;
import com.mykaarma.kcommunications_model.request.PredictOptOutStatusCallbackRequest;
import com.mykaarma.kcommunications_model.request.PredictPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.SaveBotMessageRequest;
import com.mykaarma.kcommunications_model.request.SaveMessageRequest;
import com.mykaarma.kcommunications_model.request.SendBotMessageRequest;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcommunications_model.request.SendMessageWithoutCustomerRequest;
import com.mykaarma.kcommunications_model.request.SendNotificationWithoutCustomerRequest;
import com.mykaarma.kcommunications_model.request.SubscriptionSaveRequest;
import com.mykaarma.kcommunications_model.request.SubscriptionUpdateRequest;
import com.mykaarma.kcommunications_model.request.ThreadFollowRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionRequest;
import com.mykaarma.kcommunications_model.request.UpdateOptOutStatusRequest;
import com.mykaarma.kcommunications_model.request.UpdatePreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.ForwardMessageResponse;
import com.mykaarma.kcommunications_model.response.GetDepartmentUUIDResponse;
import com.mykaarma.kcommunications_model.response.GetPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.MessageRedactResponse;
import com.mykaarma.kcommunications_model.response.MultipleCustomersPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.NotifierDeleteResponse;
import com.mykaarma.kcommunications_model.response.OptOutResponse;
import com.mykaarma.kcommunications_model.response.OptOutStatusListResponse;
import com.mykaarma.kcommunications_model.response.OptOutStatusResponse;
import com.mykaarma.kcommunications_model.response.PredictPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcommunications_model.response.SaveMessageListResponse;
import com.mykaarma.kcommunications_model.response.SaveMessageResponse;
import com.mykaarma.kcommunications_model.response.BotMessageResponse;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;
import com.mykaarma.kcommunications_model.response.SendMessageWithoutCustomerResponse;
import com.mykaarma.kcommunications_model.response.SendMultipleMessageResponse;
import com.mykaarma.kcommunications_model.response.SendNotificationWithoutCustomerResponse;
import com.mykaarma.kcommunications_model.response.SubscriptionSaveResponse;
import com.mykaarma.kcommunications_model.response.SubscriptionsUpdateResponse;
import com.mykaarma.kcommunications_model.response.ThreadFollowResponse;
import com.mykaarma.kcommunications_model.response.VoiceCallResponse;
import com.mykaarma.kcustomer_model.dto.Customer;
import com.mykaarma.kcustomer_model.dto.EmailDetails;
import com.mykaarma.kcustomer_model.dto.PhoneDetails;
import com.mykaarma.kcustomer_model.lombokresponse.CustomerWithVehiclesResponse;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;

@Service
public class ValidateRequest {

	private final static Logger LOGGER = LoggerFactory.getLogger(ValidateRequest.class);

	@Autowired
	private KCommunicationsUtils utils;

	@Autowired
	private KMessagingApiHelper kMessagingApiHelper;

	@Autowired
	private KManageApiHelper kManageApiHelper;

	@Autowired
	private KCommunicationsUtils kCommunicationsUtils;

	private final static Long MAX_REQUESTS_LIMIT = 500l;

	SendMessageResponse validateSendMessageRequestEditor(SendMessageRequest sendMessageRequest) {
		SendMessageResponse response = new SendMessageResponse();
		List<ApiError> errors = new ArrayList<>();
		List<ApiWarning> warnings = new ArrayList<>();
		response.setErrors(errors);
		response.setWarnings(warnings);

		if(sendMessageRequest == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "Message Attributes are missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}

		User editor = sendMessageRequest.getEditor();
		if(editor == null || !(StringUtils.hasLength(editor.getUuid()) && editor.getType() != null && StringUtils.hasLength(editor.getDepartmentUuid()))) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_USER.name(), String.format("Editor is null or One or more Editor Fields are invalid for editor=%s", editor));
			errors.add(apiError);
			response.setErrors(errors);
		}

		return response;
	}

	SendMessageResponse validateSendMessageRequest(SendMessageRequest sendMessageRequest) {
		SendMessageResponse response = new SendMessageResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);


		if (sendMessageRequest == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "Message Attributes are missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}

		
		MessageAttributes messageAttributes = sendMessageRequest.getMessageAttributes();
		if(messageAttributes==null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "Message Attributes are missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		
		if(messageAttributes.getType()==null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_TYPE.name(), "Message type is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		
		if(messageAttributes.getProtocol()==null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_PROTOCOL.name(), "Message protocol is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(messageAttributes.getIsManual()==null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_IS_MANUAL.name(), "Is manual attribute is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if((messageAttributes.getBody()==null || messageAttributes.getBody().isEmpty()) && 
				!(MessageType.INCOMING.getMessageType().equalsIgnoreCase(messageAttributes.getType().getMessageType()) &&
				messageAttributes.getAttachments() != null && !messageAttributes.getAttachments().isEmpty()) &&
				!(MessageType.NOTE.getMessageType().equalsIgnoreCase(messageAttributes.getType().getMessageType()) && messageAttributes.getIsManual() &&
				messageAttributes.getAttachments() != null && !messageAttributes.getAttachments().isEmpty())) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_BODY.name(), "Message body is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		validateDraftRequest(sendMessageRequest, response);
		if(response.getErrors()!=null && !response.getErrors().isEmpty()) {
			return response;
		}
		
		validateAttachments(sendMessageRequest, response);
		if(response.getErrors()!=null && !response.getErrors().isEmpty()) {
			return response;
		}
		response.setErrors(errors);
		response.setWarnings(warnings);
		return response;
	}
	
	public NotifierDeleteResponse validateNotifierDeleteRequest(String userUUID, String departmentUUID) {
		
		NotifierDeleteResponse response = new NotifierDeleteResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		if(userUUID==null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_NOTIFIER_ATTRIBUTES.name(), "User context missing in request");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(departmentUUID==null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_NOTIFIER_ATTRIBUTES.name(), "Department missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		return response;
	}
	void validateDraftRequest(SendMessageRequest sendMessageRequest, SendMessageResponse response) {
		List<ApiError> errors = response.getErrors();
		List<ApiWarning> warnings = response.getWarnings();
		if(MessageType.DRAFT.getMessageType().equalsIgnoreCase(sendMessageRequest.getMessageAttributes().getType().getMessageType())) {
			DraftAttributes draftAttributes = sendMessageRequest.getMessageAttributes().getDraftAttributes();
			if(draftAttributes==null || draftAttributes.getDraftStatus()==null) {
				ApiError apiError = new ApiError(ErrorCode.MISSING_DRAFT_ATTRIBUTES.name(), "Draft attributes are missing.");
				errors.add(apiError);
				response.setErrors(errors);
				return ;
			}
			if(draftAttributes.getScheduledOn()!=null && !draftAttributes.getScheduledOn().isEmpty()) {
				Date pstDate = utils.getPstDateFromIsoDate(draftAttributes.getScheduledOn());
				if(pstDate==null) {
					ApiError apiError = new ApiError(ErrorCode.INVALID_DRAFT_DATE.name(), "Draft date time is invalid. Please use ISO format for sending draft dates.");
					errors.add(apiError);
					response.setErrors(errors);
					return ;
				} else if(pstDate.before(new Date())) {
					ApiError apiError = new ApiError(ErrorCode.INVALID_DRAFT_DATE.name(), "Draft sending date is in the past.");
					errors.add(apiError);
					response.setErrors(errors);
					return ;
				}
			}
			if(!draftAttributes.getDraftStatus().equals(DraftStatus.FAILED) && draftAttributes.getDraftFailureReason()!=null) {
				ApiError apiError = new ApiError(ErrorCode.MISMATCH_DRAFT_STATUS_FAILURE_REASON.name(), "Failure reason should be absent if draft status is not failed");
				errors.add(apiError);
				response.setErrors(errors);
				return ;
			}
			if(draftAttributes.getDraftStatus().equals(DraftStatus.FAILED) && draftAttributes.getDraftFailureReason()==null) {
				ApiWarning apiWarning = new ApiWarning(WarningCode.MISSING_DRAFT_FAILURE_REASONs.name(), "Draft failure reason missing.");
				warnings.add(apiWarning);
				response.setWarnings(warnings);
            }
            if(DraftStatus.DISCARDED.equals(draftAttributes.getDraftStatus())) {
                ApiError apiError = new ApiError(ErrorCode.MISMATCH_DRAFT_STATUS.name(), "Draft Status does not match the intended.");
				errors.add(apiError);
				response.setErrors(errors);
				return ;
            }
        } else if(MessageType.DISCARDED_DRAFT.getMessageType().equalsIgnoreCase(sendMessageRequest.getMessageAttributes().getType().getMessageType())) {
            DraftAttributes draftAttributes = sendMessageRequest.getMessageAttributes().getDraftAttributes();
			if(draftAttributes == null || draftAttributes.getDraftStatus() == null) {
				ApiError apiError = new ApiError(ErrorCode.MISSING_DRAFT_ATTRIBUTES.name(), "Draft attributes are missing.");
				errors.add(apiError);
				response.setErrors(errors);
				return ;
			} else if(!DraftStatus.DISCARDED.equals(draftAttributes.getDraftStatus())) {
                ApiError apiError = new ApiError(ErrorCode.MISMATCH_DRAFT_STATUS.name(), "Draft Status does not match the intended DISARDED.");
				errors.add(apiError);
				response.setErrors(errors);
				return ;
            }
		} else {
			DraftAttributes draftAttributes = sendMessageRequest.getMessageAttributes().getDraftAttributes();
			if(draftAttributes!=null) {
				ApiWarning apiWarning = new ApiWarning(WarningCode.DRAFT_ATTRIBUTES_PRESENT.name(), "Draft attributes are present for message sending type. Ignoring Draft attributes.");
				warnings.add(apiWarning);
				response.setWarnings(warnings);
				sendMessageRequest.getMessageAttributes().setDraftAttributes(null);
			}
		}
		return ;
		
	}
	
	void validateIfMessageAlreadySent(Message message, SendMessageResponse response){
		if(message!=null
				&& "1".equalsIgnoreCase(message.getDeliveryStatus())
				&& message.getReceivedOn()!=null
				&& MessageType.OUTGOING.getMessageType().equalsIgnoreCase(message.getMessageType())){
			ApiError apiError = new ApiError(ErrorCode.MESSAGE_ALREADY_SENT.name(), String.format("Message has already been sent, "
					+ "not trying again for message_uuid=%s",message.getUuid()));
			List<ApiError> errors = response.getErrors();
			if(errors==null){
				errors=new ArrayList<ApiError>();
			}
			errors.add(apiError);
			response.setStatus(Status.FAILURE);
			response.setErrors(errors);
		}
	}
	
	
	void validateAttachments(SendMessageRequest sendMessageRequest, SendMessageResponse response) {
		List<ApiWarning> warnings = response.getWarnings();
		if(sendMessageRequest.getMessageAttributes().getAttachments()==null || sendMessageRequest.getMessageAttributes().getAttachments().isEmpty()) {
			return ;
		}
		List<AttachmentAttributes> attachments = sendMessageRequest.getMessageAttributes().getAttachments();
		ArrayList<AttachmentAttributes> finalAttachments = new ArrayList<AttachmentAttributes>();
		for(AttachmentAttributes attachment : attachments) {
			if(attachment.getAttachmentExtension()==null || attachment.getDocSize()==null || attachment.getFileURL()==null || attachment.getOriginalFileName()==null
					|| attachment.getFileURL().isEmpty() || attachment.getDocSize().isEmpty()) {
				ApiWarning apiWarning = new ApiWarning(WarningCode.MISSING_ATTACHMENT_DETAILS.name(), String.format("Missing attachment details for file_url=%s ", attachment.getFileURL()));
				warnings.add(apiWarning);
			} else {
				finalAttachments.add(attachment);
			}
		}
		MessageAttributes ma = sendMessageRequest.getMessageAttributes();
		ma.setAttachments(finalAttachments);
		response.setWarnings(warnings);
		
	}
	
	void validateMessageSendingAttributes(SendMessageRequest sendMessageRequest, SendMessageResponse response) {
		List<ApiWarning> warnings = response.getWarnings();
		if(sendMessageRequest.getMessageSendingAttributes()!=null) {
			MessageSendingAttributes messageSendingAttributes = sendMessageRequest.getMessageSendingAttributes();
			List<String> listOfCCEmails = messageSendingAttributes.getListOfEmailsToBeCCed();
			List<String> listOfBCCEmails = messageSendingAttributes.getListOfEmailsToBeBCCed();
			List<String> finalListOfCCEmails = new ArrayList<>();
			List<String> finalListOfBCCEmails = new ArrayList<>();
			if(listOfCCEmails!=null && listOfBCCEmails!=null) {
				for(String email: listOfCCEmails) {
					if(!utils.validateEmail(email)) {
						ApiWarning apiWarning = new ApiWarning(WarningCode.INVALID_EMAIL.name(), String.format("Invalid email format for email_id=%s ", email));
						warnings.add(apiWarning);
					} else {
						finalListOfCCEmails.add(email);
					}
				}

				for(String email: listOfBCCEmails) {
					if(!utils.validateEmail(email)) {
						ApiWarning apiWarning = new ApiWarning(WarningCode.INVALID_EMAIL.name(), String.format("Invalid email format for email_id=%s ", email));
						warnings.add(apiWarning);
					} else {
						finalListOfBCCEmails.add(email);
					}
				}
				messageSendingAttributes.setListOfEmailsToBeBCCed(finalListOfBCCEmails);
				messageSendingAttributes.setListOfEmailsToBeCCed(finalListOfCCEmails);
			}
		}
		
		response.setWarnings(warnings);
	}
	
	void validateCustomer(SendMessageRequest sendMessageRequest, SendMessageResponse response, CustomerWithVehiclesResponse customerWithVehicles, String customerUUID) {
		validateCustomerBasic(response, customerWithVehicles, customerUUID);
		if(response.getErrors() != null && !response.getErrors().isEmpty()) {
			return;
		}

		List<ApiError> errors = response.getErrors();

		Customer customer = customerWithVehicles.getCustomerWithVehicles().getCustomer();
		if(sendMessageRequest.getMessageSendingAttributes()!=null && sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer()!=null &&
				!sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer().isEmpty()) {
			String communicationValue = sendMessageRequest.getMessageSendingAttributes().getCommunicationValueOfCustomer();
			LOGGER.info("communicationValue = {}", communicationValue);
			if(sendMessageRequest.getMessageAttributes().getProtocol().equals(MessageProtocol.TEXT)) {
				for(PhoneDetails phone: customer.getPhoneNumbers()) {
					LOGGER.info("Customer phone numbers = {}", phone.getPhoneNumber());
					if(communicationValue.equalsIgnoreCase(phone.getPhoneNumber())) {
						return ;
					}
				}

			} else if(sendMessageRequest.getMessageAttributes().getProtocol().equals(MessageProtocol.EMAIL)) {
				for(EmailDetails email: customer.getEmails()) {
					if(communicationValue.equalsIgnoreCase(email.getEmailAddress()) && utils.validateEmail(communicationValue)) {
						validateMessageSendingAttributes(sendMessageRequest, response);
						return ;
					}
				}
			}

			ApiError apiError = new ApiError(ErrorCode.INVALID_COMMUNICATION_VALUE.name(), String.format("Phone Number/Email = %s for the customer is invalid", communicationValue));
			errors.add(apiError);
			response.setErrors(errors);
			return ;
		}
	}

	public void validateCustomerBasic(SendMessageResponse response, CustomerWithVehiclesResponse customerWithVehicles, String customerUUID) {
		List<ApiError> errors = response.getErrors();
		if(customerWithVehicles == null || customerWithVehicles.getCustomerWithVehicles() == null || customerWithVehicles.getCustomerWithVehicles().getCustomer() == null) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_CUSTOMER.name(), String.format("Customer_UUID=%s is invalid ", customerUUID));
			errors.add(apiError);
			response.setErrors(errors);
		}
	}
	
	void validateDealerAssociate(String userUUID, SendMessageRequest sendMessageRequest, SendMessageResponse response, GetDealerAssociateResponseDTO getDealerAssociateResponse) {
		List<ApiError> errors = response.getErrors();
		if(getDealerAssociateResponse == null || getDealerAssociateResponse.getDealerAssociate() == null) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_USER.name(), String.format("USER_UUID=%s is invalid ", userUUID));
			errors.add(apiError);
			response.setErrors(errors);
			return ;
		}
		
	}
	
	void validateIncomingMessageAttributes(SendMessageRequest sendMessageRequest, SendMessageResponse response) {
		IncomingMessageAttributes incomingMessageAttributes = sendMessageRequest.getIncomingMessageAttributes();
		List<ApiError> errors = response.getErrors();
		
		if(incomingMessageAttributes == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_INCOMING_MESSAGE_ATTRIBUTES.name(), "Incoming Message Attributes for the customer is null");
			errors.add(apiError);
			response.setErrors(errors);
			return ;
		}
		
		if(incomingMessageAttributes.getFromNumber() == null || incomingMessageAttributes.getFromNumber().isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_FROM_NUMBER.name(), "FromNumber for the customer is invalid");
			errors.add(apiError);
			response.setErrors(errors);
		}
	}
	
	void applyMessageSendingRules(SendMessageRequest sendMessageRequest, SendMessageResponse response, DealerAssociateExtendedDTO dealerAssociate,
			Customer customer, String communicationValue, String userUUID, String serviceSubscriberName) throws Exception {
		List<ApiWarning> warnings = response.getWarnings();
		List<ApiError> errors = response.getErrors();
		if((communicationValue==null || communicationValue.isEmpty())
				&& (!MessageProtocol.NONE.equals(sendMessageRequest.getMessageAttributes().getProtocol()))) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_COMMUNICATION_VALUE.name(), String.format("Phone Number/Email = %s for the customer is invalid", communicationValue));
			errors.add(apiError);
			response.setErrors(errors);
			return ;
		}
		String communicationType = null;
		if (sendMessageRequest.getMessageAttributes().getProtocol().equals(MessageProtocol.EMAIL)) {
			communicationType = APIConstants.EMAIL;
		} else if (sendMessageRequest.getMessageAttributes().getProtocol().equals(MessageProtocol.TEXT)) {
			communicationType = APIConstants.TEXT;
		}
		if (sendMessageRequest.getMessageSendingAttributes()!=null &&
				sendMessageRequest.getMessageSendingAttributes().getOverrideOptoutRules() != null && sendMessageRequest.getMessageSendingAttributes().getOverrideOptoutRules()) {

			// only avoiding opt-out for messaging-api and communications-api
			if(!(APIConstants.MESSAGING_API_SUBSRIBER_NAME.equalsIgnoreCase(serviceSubscriberName) || APIConstants.COMMUNICATIONS_API_SUBSRIBER_NAME.equalsIgnoreCase(serviceSubscriberName))) {
				sendMessageRequest.getMessageSendingAttributes().setOverrideOptoutRules(false);
				ApiWarning apiWarning = new ApiWarning(WarningCode.CANT_OVERRIDE_OPTOUT.name(), "You dont have the ability to override optout.");
				warnings.add(apiWarning);
				response.setWarnings(warnings);
			}
		}
		if(sendMessageRequest.getMessageSendingAttributes() != null && Boolean.TRUE == sendMessageRequest.getMessageSendingAttributes().getQueueIfOptedOut()) {
			String dsoForDoubleOptIn = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerAssociate.getDepartmentExtendedDTO().getDealerMinimalDTO().getUuid(),
				DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_ENABLE.getOptionKey());
			if(!"true".equalsIgnoreCase(dsoForDoubleOptIn)) {
				ApiWarning apiWarning = new ApiWarning(WarningCode.CANT_QUEUE_MESSAGE_IF_OPTED_OUT.name(), "You do not have authority to queue message if opted out");
				warnings.add(apiWarning);
			}
		}
		if(sendMessageRequest.getMessageAttributes().getProtocol().equals(MessageProtocol.TEXT) && sendMessageRequest.getMessageAttributes().getIsManual()) {
			if(!KManageApiHelper.checkDealerAssociateAuthority(Authority.MESSAGING_TEXT_SEND_MANUAL.getAuthority(), userUUID, dealerAssociate.getDepartmentExtendedDTO().getUuid())) {
				ApiError apiError = new ApiError(ErrorCode.USER_DOES_NOT_HAVE_AUTHORITY.name(), String.format("User does not have the authority=%s ", 
						Authority.MESSAGING_TEXT_SEND_MANUAL.getAuthority()));
				errors.add(apiError);
				response.setErrors(errors);
				return ;
			}
		} else if(sendMessageRequest.getMessageAttributes().getProtocol().equals(MessageProtocol.TEXT)) {
			if(!KManageApiHelper.checkDealerAssociateAuthority(Authority.MESSAGING_TEXT_SEND_AUTOMATIC.getAuthority(), userUUID, dealerAssociate.getDepartmentExtendedDTO().getUuid())) {
				ApiError apiError = new ApiError(ErrorCode.USER_DOES_NOT_HAVE_AUTHORITY.name(), String.format("User does not have the authority=%s ", 
						Authority.MESSAGING_TEXT_SEND_AUTOMATIC.getAuthority()));
				errors.add(apiError);
				response.setErrors(errors);
				return ;
			}
		} 
		
	}
	void applyManualFollowUpEventRules(String departmentUUID, Response response, Message message, Long departmentID, String messageUUID) throws Exception {
		List<ApiError> errors = response.getErrors();
		if(message==null) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_MESSAGE.name(), String.format("Invalid message for message_uuid=%s ", 
					messageUUID));
			errors.add(apiError);
			response.setErrors(errors);
			return ;
		}
		if(!message.getDealerDepartmentId().equals(departmentID)) {
			ApiError apiError = new ApiError(ErrorCode.MISMATCH_DEPARTMENT_MESSAGE.name(), String.format("Department of the message does not match with the department passed for message_uuid=%s ", 
					messageUUID));
			errors.add(apiError);
			response.setErrors(errors);
			return ;
		}
		if(message.getIsManual()==null || !message.getIsManual()) {
			ApiError apiError = new ApiError(ErrorCode.FOLLOW_UP_RULES_FAILED.name(), String.format("Message to be marked as followup is non-manual message_uuid=%s ", 
					messageUUID));
			errors.add(apiError);
			response.setErrors(errors);
			return ;
		}
		if(message.getMessagePurpose()==null || !MessagePurpose.FOLLOWUP.name().equalsIgnoreCase(message.getMessagePurpose())) {
			ApiError apiError = new ApiError(ErrorCode.FOLLOW_UP_RULES_FAILED.name(), String.format("Message to be marked as followup is not a follow up message for message_uuid=%s ", 
					messageUUID));
			errors.add(apiError);
			response.setErrors(errors);
			return ;
		}

        String dealerUUID = kCommunicationsUtils.getDealerUUIDFromDepartmentUUID(departmentUUID); 
		String dsoValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.COMMUNICATIONS_MANUAL_FOLLOWUP_ENABLE.getOptionKey());
		if(!"true".equalsIgnoreCase(dsoValue)) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_DSO.name(), String.format("DSO COMMUNICATIONS_MANUAL_FOLLOWUP_ENABLE is invalid for message_uuid=%s ", 
					messageUUID));
			errors.add(apiError);
			response.setErrors(errors);
			return ;
		}
	}
	
	public Response validateRecordingUrlUpdateRequest(String messageUUID) {
		
		Response response = new Response();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		
		if(messageUUID==null || messageUUID.isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_UUID.name(), ErrorCodes.MISSING_MESSAGE_UUID.getErrorDescription());
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		return response;
	}

	public Response validateRecordingUrlUpdateRequestForDealers(List<Long> dealerIDs) {
		
		Response response = new Response();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		
		if(dealerIDs==null || dealerIDs.isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_DEALER_IDS.name(), ErrorCodes.MISSING_DEALER_IDS.getErrorMessage());
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		return response;
	}
	
	public Response validatePostMessageReceived(String messageUUID, String departmentUUID){
		Response response = new Response();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		
		if(departmentUUID==null || departmentUUID.isEmpty()){
			response = getErrorResponse(ErrorCodes.INVALID_DDEPT_UUID.getErrorTitle(), ErrorCodes.INVALID_DDEPT_UUID.getErrorDescription(), errors, response);
			return response;
		}
		
		if(messageUUID==null || messageUUID.isEmpty()){
			response = getErrorResponse(ErrorCodes.MISSING_MESSAGE_UUID.getErrorTitle(), ErrorCodes.MISSING_MESSAGE_UUID.getErrorDescription(), errors, response);
			return response;
		}
		
		return response;
	}
	
	private Response getErrorResponse(String errorName, String errorDescription, List<ApiError> errors, Response response) {
		
		ApiError apiError = new ApiError(errorName, errorDescription);
		errors.add(apiError);
		response.setErrors(errors);
		
		return response;
	}
	
	public SendMultipleMessageResponse validateMultipleMessageRequest(MultipleMessageRequest multipleMessageRequest, Integer dayLimit,
			Integer minuteLimit) {
		
        SendMultipleMessageResponse response = new SendMultipleMessageResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
        if(multipleMessageRequest.getMessageAttributes() == null || multipleMessageRequest.getMultipleMessageSendingAttributes() == null) {
            ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), 
					"Missing either message or message sending attributes attributes");
			errors.add(apiError);
			response.setErrors(errors);
        }
        MultipleMessageSendingAttributes multipleMessageSendingAttributes = multipleMessageRequest.getMultipleMessageSendingAttributes();
        if((multipleMessageSendingAttributes.getCustomerUUIDList() == null || multipleMessageSendingAttributes.getCustomerUUIDList().isEmpty()) 
            && (multipleMessageSendingAttributes.getCustomerUUIDToCommunicationValues() == null || multipleMessageSendingAttributes.getCustomerUUIDToCommunicationValues().isEmpty())) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), 
					"Missing Customers to send message");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
        }
        if(multipleMessageSendingAttributes.getBulkText() == null || multipleMessageSendingAttributes.getBulkText()) {
            if(multipleMessageSendingAttributes.getStartTimeOfSendingMessages() == null || multipleMessageSendingAttributes.getEndTimeOfSendingMessages() == null) {
                ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), 
					"Missing start/end times for sending bulk messages.");
                errors.add(apiError);
                response.setErrors(errors);
                return response;
            }
            Date pstStartDate = utils.getPstDateFromIsoDate(multipleMessageSendingAttributes.getStartTimeOfSendingMessages());
            Date pstEndDate = utils.getPstDateFromIsoDate(multipleMessageSendingAttributes.getEndTimeOfSendingMessages());
            Date currentDate = new Date();
            LOGGER.info("in validateMultipleMessageRequest pstEndDate={} \n pstStartDate={} \n currentDate={} \n",pstEndDate, pstStartDate, new Date());
            if(pstStartDate == null || pstEndDate == null || !pstEndDate.after(pstStartDate) || pstStartDate.before(currentDate)) {
                ApiError apiError = new ApiError(ErrorCode.INVALID_DATES.name(), 
                        "Start/End date are incorrect.");
                errors.add(apiError);
                response.setErrors(errors);
                return response;
            }
            if(dayLimit != null && multipleMessageSendingAttributes.getCustomerUUIDList().size() > dayLimit) {
                ApiError apiError = new ApiError(ErrorCode.MESSAGE_SENDING_LIMIT_EXCEEDED.name(), 
                        String.format("Message can not be sent to more than %s customers.", dayLimit));
                errors.add(apiError);
                response.setErrors(errors);
                return response;
            }
            Long millis = pstEndDate.getTime()-pstStartDate.getTime();
            Long minutes = millis/(1000*60);
            if(minuteLimit != null && multipleMessageSendingAttributes.getCustomerUUIDList().size() > (minutes * minuteLimit)) {
                ApiError apiError = new ApiError(ErrorCode.MESSAGE_SENDING_LIMIT_EXCEEDED.name(), 
                        String.format("Message can not be sent to more than %s customers in the given time.",
                                (minutes * minuteLimit)));
                errors.add(apiError);
                response.setErrors(errors);
                return response;
            }
        }
		MessageAttributes messageAttributes = multipleMessageRequest.getMessageAttributes();
		if(messageAttributes==null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "Message Attributes are missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(messageAttributes.getBody()==null || messageAttributes.getBody().isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_BODY.name(), "Message body is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		
		if(messageAttributes.getType()==null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_TYPE.name(), "Message type is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		
		if(messageAttributes.getProtocol()==null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_PROTOCOL.name(), "Message protocol is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(messageAttributes.getIsManual()==null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_IS_MANUAL.name(), "Is manual attribute is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		return response;
	}

	public OptOutResponse validateOptOutRequest(String messageUUID, String departmentUUID) {
		
		OptOutResponse response = new OptOutResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		if(messageUUID == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_UUID.name(), "Message UUID missing in request");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(departmentUUID == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Department UUID missing in request");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		return response;
	}

	public MessageRedactResponse validateMessageRedactRequest(String messageUUID) {
		
		MessageRedactResponse response = new MessageRedactResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		if(messageUUID == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_UUID.name(), "Message UUID missing in request");
			errors.add(apiError);
			response.setErrors(errors);
		}
		return response;
	}

	public GetDepartmentUUIDResponse validateGetDepartmentUUIDRequest(String brokerNumber) {
		GetDepartmentUUIDResponse response = new GetDepartmentUUIDResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		
		if(!brokerNumber.matches("\\d{10}")) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_BROKER_NUMBER.name(), "Broker Number is invalid");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		return response;
	}

	 public SendMessageResponse validateEditDraftRequest(SendMessageRequest sendMessageRequest, String messageUUID) {
		 
	    SendMessageResponse response = new SendMessageResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		if(messageUUID == null || messageUUID.isEmpty()) {
	        ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_UUID.name(), "Message UUID is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
	     }
	        return validateSendMessageRequest(sendMessageRequest);
	  }

	public SubscriptionsUpdateResponse validateSubscriptionUpdateRequestForDealers(
				SubscriptionUpdateRequest subscriptionUpdateRequest) {
			
		SubscriptionsUpdateResponse subscriptionsUpdateResponse = new SubscriptionsUpdateResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		subscriptionsUpdateResponse.setErrors(errors);
		subscriptionsUpdateResponse.setWarnings(warnings);
			
		if(subscriptionUpdateRequest==null || subscriptionUpdateRequest.getFromDealerId()==null || subscriptionUpdateRequest.getToDealerId()==null || 
					subscriptionUpdateRequest.getFromDealerId() > subscriptionUpdateRequest.getToDealerId()) {
				
			ApiError apiError = new ApiError(ErrorCode.INVALID_DEALER_ID.name(), "DealerIDs mentioned not correct");
			errors.add(apiError);
			subscriptionsUpdateResponse.setErrors(errors);
			return subscriptionsUpdateResponse;
		}
			return subscriptionsUpdateResponse;
	}



	public Response validateSentimentRequest(String messageUUID) {
		
		Response response = new Response();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		if(messageUUID == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_UUID.name(), "Message UUID missing in request");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		return response;
	}

	public Response validateUpdateMessagePredictionRequest(UpdateMessagePredictionRequest updateMessagePredictionRequest) {	
		
		Response response = new Response();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		if(updateMessagePredictionRequest.getMessageID() == null) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_MESSAGE_ID.name(), "Message ID missing in request");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		if(updateMessagePredictionRequest.getPredictionFeature() == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_PREDICTION_FEATURE.name(), "Prediction Feature ID missing in request");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		if(updateMessagePredictionRequest.getPrediction() == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_PREDICTION.name(), "Prediction missing in request");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		return response;
	}
	
	SendNotificationWithoutCustomerResponse validateNotificationWithoutCustomerRequest(SendNotificationWithoutCustomerRequest notificationRequest) {
		SendNotificationWithoutCustomerResponse response = new SendNotificationWithoutCustomerResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		response.setErrors(errors);
		response.setWarnings(warnings);
		
		if(notificationRequest == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "Request body is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		
		MessageAttributes messageAttributes = notificationRequest.getMessageAttributes();
		if(messageAttributes == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "Message Attributes are missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(messageAttributes.getBody()==null || messageAttributes.getBody().isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_BODY.name(), "Message body is missing");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		// Setting these values explicitly since that's what they should be in this case
		messageAttributes.setType(MessageType.NOTE);
		messageAttributes.setProtocol(MessageProtocol.NONE);
		messageAttributes.setIsManual(false);
		
		return response;
	}

	public PredictPreferredCommunicationModeResponse validatePredictPreferredCommunicationModeRequest(String departmentUUID,
        String customerUUID, PredictPreferredCommunicationModeRequest predictPreferredCommunicationModeRequest) {
		
        PredictPreferredCommunicationModeResponse response = new PredictPreferredCommunicationModeResponse();
        List<ApiError> errors = new ArrayList<ApiError>();
        
        if(departmentUUID == null) {
            ApiError apiError = new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Department UUID missing in request");
            errors.add(apiError);
            response.setErrors(errors);
            return response;
        }
        if(customerUUID == null) {
            ApiError apiError = new ApiError(ErrorCode.MISSING_CUSTOMER_UUID.name(), "Customer UUID missing in request");
            errors.add(apiError);
            response.setErrors(errors);
            return response;
        }
        if(predictPreferredCommunicationModeRequest.getMessageUUID() == null) {
            ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_UUID.name(), "Message UUID missing in request");
            errors.add(apiError);
            response.setErrors(errors);
            return response;
        }
        
		return response;
	}

    public Response validateUpdatePreferredCommunicationModeRequest(String departmentUUID, String customerUUID,
            UpdatePreferredCommunicationModeRequest updatePreferredCommunicationModeRequest) {
        
        Response response = new Response();
        List<ApiError> errors = new ArrayList<ApiError>();
        
        if(departmentUUID == null) {
            ApiError apiError = new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Department UUID missing in request");
            errors.add(apiError);
            response.setErrors(errors);
            return response;
        }
        if(customerUUID == null) {
            ApiError apiError = new ApiError(ErrorCode.MISSING_CUSTOMER_UUID.name(), "Customer UUID missing in request");
            errors.add(apiError);
            response.setErrors(errors);
            return response;
        }
        if(updatePreferredCommunicationModeRequest == null || 
            updatePreferredCommunicationModeRequest.getPreferredCommunicationMode() == null || 
            updatePreferredCommunicationModeRequest.getPreferredCommunicationMode().isEmpty() || 
            updatePreferredCommunicationModeRequest.getPreferredCommunicationModeMetaData() == null || 
            updatePreferredCommunicationModeRequest.getPreferredCommunicationModeMetaData().isEmpty()) {
            ApiError apiError = new ApiError(ErrorCode.INSUFFICIENT_INFORMATION.name(), "Preferred Communication Mode data provided is insufficient");
            errors.add(apiError);
            response.setErrors(errors);
            return response;
        }
        
        return response;
    }

    public GetPreferredCommunicationModeResponse validateGetPreferredCommunicationModeRequest(String departmentUUID, String customerUUID) {
        
        GetPreferredCommunicationModeResponse response = new GetPreferredCommunicationModeResponse();
        List<ApiError> errors = new ArrayList<ApiError>();
        
        if(departmentUUID == null) {
            ApiError apiError = new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Department UUID missing in request");
            errors.add(apiError);
            response.setErrors(errors);
            return response;
        }
        if(customerUUID == null) {
            ApiError apiError = new ApiError(ErrorCode.MISSING_CUSTOMER_UUID.name(), "Customer UUID missing in request");
            errors.add(apiError);
            response.setErrors(errors);
            return response;
        }
        return response;
    }

    public MultipleCustomersPreferredCommunicationModeResponse validateGetMultipleCustomersPreferredCommunicationMode(
            String departmentUUID, MultipleCustomersPreferredCommunicationModeRequest multipleCustomersPreferredCommunicationModeRequest) {

		MultipleCustomersPreferredCommunicationModeResponse response = new MultipleCustomersPreferredCommunicationModeResponse();
		List<ApiError> errors = new ArrayList<ApiError>();

		if (departmentUUID == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Department UUID missing in request");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		if (multipleCustomersPreferredCommunicationModeRequest == null ||
			multipleCustomersPreferredCommunicationModeRequest.getCustomerUUIDList() == null ||
			multipleCustomersPreferredCommunicationModeRequest.getCustomerUUIDList().isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.INSUFFICIENT_INFORMATION.name(), "CustomerUUID List for getting Preferred Communication Mode information is empty");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}

		return response;
	}

	public OptOutStatusResponse validateGetOptOutStatusRequest(String departmentUUID, String communicationType, String communicationValue) {
		OptOutStatusResponse response = new OptOutStatusResponse();
		if(departmentUUID == null || departmentUUID.isEmpty()) {
			ApiError error = new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Department UUID missing in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		if(!validateCommunicationTypeAndCommunicationValue(communicationType, communicationValue)) {
			ApiError error = new ApiError(ErrorCode.INVALID_COMMUNICATION_ATTRIBUTES.name(), String.format("Invalid Communication Attributes for communication_type=%s communication_value=%s", communicationType, communicationValue));
			response.setErrors(Arrays.asList(error));
			return response;
		}
		return response;
		
	}

    public OptOutStatusListResponse validateCommunicationsOptOutStatusListRequest(String departmentUUID,
            CommunicationsOptOutStatusListRequest request) {
		
		OptOutStatusListResponse response = new OptOutStatusListResponse();
		if(departmentUUID == null || departmentUUID.isEmpty()) {
			ApiError error = new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Department UUID missing in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		if(request == null || request.getCommunicationList() == null || request.getCommunicationList().isEmpty()) {
			ApiError error = new ApiError(ErrorCode.INSUFFICIENT_INFORMATION.name(), "Communications Attributes information missing in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		for(CommunicationAttributes communicationAttributes : request.getCommunicationList()) {
			String communicationType = communicationAttributes.getCommunicationType();
			String communicationValue = communicationAttributes.getCommunicationValue();
			List<ApiError> errors = new ArrayList<>();
			if(!validateCommunicationTypeAndCommunicationValue(communicationType, communicationValue)) {
				ApiError error = new ApiError(ErrorCode.INVALID_COMMUNICATION_ATTRIBUTES.name(), String.format("Invalid Communication Attributes for communication_type=%s communication_value=%s", communicationType, communicationValue));
				errors.add(error);
			}
			response.setErrors(errors);
			return response;
		}
		return response;		
    }

	public boolean validateCommunicationTypeAndCommunicationValue(String communicationType, String communicationValue) {
		MessageProtocol messageProtocol = MessageProtocol.fromString(communicationType);
		if(MessageProtocol.TEXT.equals(messageProtocol) || MessageProtocol.VOICE_CALL.equals(messageProtocol)) {
			return utils.validatePhoneNumber(communicationValue);
		} else if(MessageProtocol.EMAIL.equals(messageProtocol)) {
			return utils.validateEmail(communicationValue);
		} else {
			return false;
		}
	}

    public OptOutStatusListResponse validateCustomersOptOutStatusListRequest(String departmentUUID,
            CustomersOptOutStatusListRequest request) {
		
		OptOutStatusListResponse response = new OptOutStatusListResponse();
		if(departmentUUID == null || departmentUUID.isEmpty()) {
			ApiError error = new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Department UUID missing in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		if(request == null || request.getCustomerUUIDList() == null || request.getCustomerUUIDList().isEmpty()) {
			ApiError error = new ApiError(ErrorCode.INSUFFICIENT_INFORMATION.name(), "Customers information missing in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		return response;
    }

    public Response validateUpdateOptOutStatusRequest(String departmentUUID, String serviceSubscriberName, UpdateOptOutStatusRequest request) {
        Response response = new Response();
		
		if(departmentUUID == null || departmentUUID.isEmpty()) {
			ApiError error = new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Department UUID missing in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		if(request == null || request.getEvent() == null || request.getUpdateType() == null) {
			ApiError error = new ApiError(ErrorCode.INSUFFICIENT_INFORMATION.name(), "Required update optout status information missing in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		if(UpdateOptOutStatusRequestType.CUSTOMER == request.getUpdateType()) {
			if(request.getCustomerUUID() == null || request.getCustomerUUID().isEmpty()) {
				ApiError error = new ApiError(ErrorCode.MISSING_CUSTOMER_UUID.name(), "Customer UUID missing in request");
				response.setErrors(Arrays.asList(error));
				return response;
			}
			if(request.getCommunicationAttributesList() == null || request.getCommunicationAttributesList().isEmpty()) {
				ApiError error = new ApiError(ErrorCode.INSUFFICIENT_INFORMATION.name(), "Communications Attributes information missing in request");
				response.setErrors(Arrays.asList(error));
				return response;
			}
			for(CommunicationAttributes communicationAttributes : request.getCommunicationAttributesList()) {
				String communicationType = communicationAttributes.getCommunicationType();
				String communicationValue = communicationAttributes.getCommunicationValue();
				List<ApiError> errors = new ArrayList<>();
				if(!validateCommunicationTypeAndCommunicationValue(communicationType, communicationValue)) {
					ApiError error = new ApiError(ErrorCode.INVALID_COMMUNICATION_ATTRIBUTES.name(), String.format("Invalid Communication Attributes for communication_type=%s communication_value=%s", communicationType, communicationValue));
					errors.add(error);
				}
				response.setErrors(errors);
				return response;
			}
		} else if(UpdateOptOutStatusRequestType.MESSAGE == request.getUpdateType()) {
			if(request.getMessageUUID() == null || request.getMessageUUID().isEmpty()) {
				ApiError error = new ApiError(ErrorCode.MISSING_MESSAGE_UUID.name(), "Message UUID missing in request");
				response.setErrors(Arrays.asList(error));
				return response;
			}
		}
		if(APIConstants.INTEGRATION.equalsIgnoreCase(request.getApiCallSource()) &&
			!(APIConstants.APPOINTMENT_API_SUBSRIBER_NAME.equalsIgnoreCase(serviceSubscriberName) ||
				APIConstants.KAARMA_DATA_CONTROLLER_SUBSRIBER_NAME.equalsIgnoreCase(serviceSubscriberName))) {
			LOGGER.warn("incorrect validateUpdateOptOutStatusRequest api_call_source={} for service_subscriber_name={}", request.getApiCallSource(), serviceSubscriberName);
			ApiWarning warning = new ApiWarning(WarningCode.UNAUTHORIZED_API_CALL_SOURCE.name(), String.format("You are not authorized to use this API Call Source. Defaulting API Call Source to %s", APIConstants.MYKAARMA));
			response.setWarnings(Arrays.asList(warning));
			request.setApiCallSource(APIConstants.MYKAARMA);
		}
		return response;
    }

	public Boolean applyOptOutStatusUpdateRules(CommunicationStatus communicationStatus, OptOutStatusUpdateEvent event, Boolean doubleOptInEnabled) {
		if(doubleOptInEnabled == null || event == null) {
			return false;
		}
		switch (event) {
			case COMMUNICATION_VALUE_CREATION:
			case COMMUNICATION_STATUS_NOT_FOUND:
				return doubleOptInEnabled && communicationStatus.getId() == null;
			case USER_REQUESTED_OPT_OUT:
				return doubleOptInEnabled && OptOutState.OPTED_IN.name().equalsIgnoreCase(communicationStatus.getOptOutState());
			case USER_REQUESTED_SEND_OPTIN_REQUEST:
				return doubleOptInEnabled && communicationStatus.getCanSendOptinRequest() == Boolean.TRUE
					&& OptOutState.OPTED_OUT.name().equalsIgnoreCase(communicationStatus.getOptOutState());
			case GENERIC_MESSAGE_RECEIVED:
			case OPTIN_MESSAGE_RECEIVED:
			case STOP_MESSAGE_RECEIVED:
			case STOP_SUSPECTED_MESSAGE_RECEIVED:
				return true;
			case DOUBLE_OPTIN_ROLLOUT:
				return doubleOptInEnabled;
			case DOUBLE_OPTIN_ROLLBACK:
				return !doubleOptInEnabled;
			default:
				return false;
		}
	}

    public Response validateDoubleOptInDeploymentRequest(String dealerUUID, DoubleOptInDeploymentRequest request) {
        Response response = new Response();
		if(dealerUUID == null || dealerUUID.isEmpty()) {
			ApiError error = new ApiError(ErrorCode.MISSING_DEALER_UUID.name(), 
				"missing dealer_uuid in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		if(request == null || request.getEvent() == null || request.getMaxEntriesToBeFetched() == null || request.getMaxEntriesToBeFetched() == 0l) {
			ApiError error = new ApiError(ErrorCodes.INSUFFICIENT_DETAILS.name(), 
				String.format("insufficient information in request for event=%s max_entries_to_be_fetched=%s", request.getEvent(), request.getMaxEntriesToBeFetched()));
			response.setErrors(Arrays.asList(error));
			return response;
		}
		return response;
    }

	public Response validatePredictOptOutStatusCallbackRequest(String departmentUUID, String messageUUID, PredictOptOutStatusCallbackRequest request) {
		Response response = new Response();
		if(departmentUUID == null || departmentUUID.isEmpty()) {
			ApiError error = new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "missing department_uuid in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		if(messageUUID == null || messageUUID.isEmpty()) {
			ApiError error = new ApiError(ErrorCode.MISSING_MESSAGE_UUID.name(), "missing message_uuid in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		if (request == null || (request.getOptOutV2Score() == null &&
			(request.getMessageKeyword() == null || request.getMessageKeyword().isEmpty()))) {
			ApiError error = new ApiError(ErrorCode.INSUFFICIENT_INFORMATION.name(), "opt_out_v2_score and message_keyword missing in request");
			response.setErrors(Arrays.asList(error));
			return response;
		}
		return response;
	}

	public SaveMessageListResponse validateBulkMessageSaveRequest(List<SaveMessageRequest> listSaveMessageRequest) {

		SaveMessageListResponse saveMessageListResponse = new SaveMessageListResponse();
		List<ApiError> errors = new ArrayList<ApiError>();

		if (listSaveMessageRequest == null || listSaveMessageRequest.isEmpty()) {

			ApiError apiError = new ApiError(ErrorCodes.REQUEST_NULL.getErrorTitle(), ErrorCodes.REQUEST_NULL.getErrorMessage());
			errors.add(apiError);
			saveMessageListResponse.setErrors(errors);
			return saveMessageListResponse;
		}
		if (listSaveMessageRequest != null && listSaveMessageRequest.size() > MAX_REQUESTS_LIMIT) {

			ApiError apiError = new ApiError(ErrorCode.MAX_COUNT_EXCEEDED.name(), String.format("Maximum message requests allowed are %s", MAX_REQUESTS_LIMIT));
			errors.add(apiError);
			saveMessageListResponse.setErrors(errors);
			return saveMessageListResponse;
		}

		return saveMessageListResponse;
	}

	public SaveMessageResponse validateSaveMessageRequest(SaveMessageRequest saveMessageRequest) {

		SaveMessageResponse saveMessageResponse = new SaveMessageResponse();

		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();

		if (saveMessageRequest == null) {

			ApiError apiError = new ApiError(ErrorCodes.REQUEST_NULL.getErrorTitle(), "no request object present");
			errors.add(apiError);
			saveMessageResponse.setErrors(errors);
			return saveMessageResponse;
		}

		if (saveMessageRequest.getSourceUuid() == null || saveMessageRequest.getSourceUuid().isEmpty()) {

			ApiError apiError = new ApiError(ErrorCode.NO_SOURCE_UUID.name(), "source uuid not present for the message to be pushed in mykaarma");
			errors.add(apiError);
			saveMessageResponse.setErrors(errors);
			return saveMessageResponse;

		}

		saveMessageResponse.setSourceUuid(saveMessageRequest.getSourceUuid());


		if (saveMessageRequest.getUserUuid() == null || saveMessageRequest.getUserUuid().isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.EMPTY_USER_UUID.name(), "user uuiid not present in request");
			errors.add(apiError);
			saveMessageResponse.setErrors(errors);
			return saveMessageResponse;
		}

		MessageAttributes messageAttributes = saveMessageRequest.getMessageAttributes();
		ApiError apiError = validateMessageAttributes(messageAttributes);

		if (apiError != null) {
			errors.add(apiError);
			saveMessageResponse.setErrors(errors);
			return saveMessageResponse;
		}

		if (messageAttributes.getPurpose() != null && MessagePurpose.F.equals(messageAttributes.getPurpose())) {
			LOGGER.info("message is draft type");

			apiError = validateDraftRequest(messageAttributes.getDraftAttributes(), messageAttributes.getType());
			if (apiError != null) {
				errors.add(apiError);
				saveMessageResponse.setErrors(errors);
				return saveMessageResponse;
			}

		}

		if (messageAttributes.getAttachments() != null && !messageAttributes.getAttachments().isEmpty()) {

			ArrayList<AttachmentAttributes> finalAttachments = validateAttachments(messageAttributes.getAttachments(), warnings, errors);
			messageAttributes.setAttachments(finalAttachments);

		}

		if (MessageType.NOTE.equals(messageAttributes.getType()) && MessageProtocol.NONE.equals(messageAttributes.getProtocol())) {

			if (saveMessageRequest.getNotificationAttributes() == null) {
				apiError = new ApiError(ErrorCode.MISSING_NOTIFIER_ATTRIBUTES.name(), "notification attributes not present");
				errors.add(apiError);
				saveMessageResponse.setErrors(errors);
				return saveMessageResponse;
			}
		}

		if (saveMessageRequest.getReceivedOn() == null) {

			if (saveMessageRequest.getNotificationAttributes() == null) {
				apiError = new ApiError(ErrorCode.MISSING_TIMESTAMP.name(), "time stamp missing for message");
				errors.add(apiError);
				saveMessageResponse.setErrors(errors);
				return saveMessageResponse;
			}
		}
		saveMessageResponse.setErrors(errors);
		saveMessageResponse.setWarnings(warnings);
		return saveMessageResponse;
	}

	private ArrayList<AttachmentAttributes> validateAttachments(List<AttachmentAttributes> attachmentAttributes, List<ApiWarning> apiWarnings, List<ApiError> apiErrors) {

		ArrayList<AttachmentAttributes> finalAttachments = new ArrayList<AttachmentAttributes>();
		for (AttachmentAttributes attachment : attachmentAttributes) {
			if (attachment.getAttachmentExtension() == null || attachment.getDocSize() == null || attachment.getFileURL() == null || attachment.getOriginalFileName() == null
				|| attachment.getFileURL().isEmpty() || attachment.getDocSize().isEmpty()) {
				ApiWarning apiWarning = new ApiWarning(WarningCode.MISSING_ATTACHMENT_DETAILS.name(), String.format("Missing attachment details for file_url=%s ", attachment.getFileURL()));
				apiWarnings.add(apiWarning);
			} else {
				finalAttachments.add(attachment);
			}
		}

		return finalAttachments;
	}

	private ApiError validateDraftRequest(DraftAttributes draftAttributes, MessageType messageType) {

		ApiError apiError = null;
		if (draftAttributes == null || draftAttributes.getDraftStatus() == null) {

			apiError = new ApiError(ErrorCode.MISSING_DRAFT_ATTRIBUTES.name(), "Draft attributes are missing.");
			return apiError;
		}

		if (DraftStatus.SENT.equals(draftAttributes.getDraftStatus()) && !MessageType.OUTGOING.equals(messageType)) {
			LOGGER.info("Draft is in sent state but still message Type is not outgoing(S)");
			apiError = new ApiError(ErrorCode.MISMATCH_DRAFT_STATUS.name(), "Draft is in sent state but still message Type is not outgoing(S).");
			return apiError;
		}
		if (DraftStatus.SCHEDULED.equals(draftAttributes.getDraftStatus()) && !MessageType.DRAFT.equals(messageType)) {
			LOGGER.info("Draft is in scheduled state but still message Type is not Draft(F).");
			apiError = new ApiError(ErrorCode.MISMATCH_DRAFT_STATUS.name(), "Draft is in scheduled state but still message Type is not Draft(F).");
			return apiError;
		}
		if (draftAttributes.getScheduledOn() != null && !draftAttributes.getScheduledOn().isEmpty()) {
			Date pstDate = utils.getPstDateFromIsoDate(draftAttributes.getScheduledOn());
			LOGGER.info("pstDate={} is in scheduled state but still message Type is not Draft(F).", pstDate);
			if (pstDate == null) {
				LOGGER.info("pstDate={} Draft date time is invalid. Please use ISO format for sending draft dates.", pstDate);
				apiError = new ApiError(ErrorCode.INVALID_DRAFT_DATE.name(), "Draft date time is invalid. Please use ISO format for sending draft dates.");
				return apiError;
			}
			if (MessageType.DRAFT.equals(messageType)) {
				if (pstDate.before(new Date())) {
					LOGGER.info("pstDate={} Draft date time is behind the current time", pstDate);
					apiError = new ApiError(ErrorCode.INVALID_DRAFT_DATE.name(), "Draft date time is behind the current time");
					return apiError;
				}
			} else if (MessageType.OUTGOING.equals(messageType)) {
				if (pstDate.after(new Date())) {
					LOGGER.info("pstDate={} Draft date time is ahead of current time, and the draft is already sent out", pstDate);
					apiError = new ApiError(ErrorCode.INVALID_DRAFT_DATE.name(), "Draft date time is ahead of current time, and the draft is already sent out");
					return apiError;
				}
			}
		} else if (draftAttributes.getScheduledOn() == null || draftAttributes.getScheduledOn().isEmpty()) {

			apiError = new ApiError(ErrorCode.INVALID_DRAFT_DATE.name(), "Draft date time not present");
			return apiError;
		}
		return null;
	}

	private ApiError validateMessageAttributes(MessageAttributes messageAttributes) {

		if (messageAttributes == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "Message Attributes are missing");
			return apiError;
		}

		if (messageAttributes.getBody() == null || messageAttributes.getBody().isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_BODY.name(), "Message body is missing");
			return apiError;
		}

		if (messageAttributes.getType() == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_TYPE.name(), "Message type is missing");
			return apiError;
		}

		if (messageAttributes.getProtocol() == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_PROTOCOL.name(), "Message protocol is missing");
			return apiError;
		}

		if (messageAttributes.getIsManual() == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_IS_MANUAL.name(), "Is manual attribute is missing");
			return apiError;
		}

		if (MessageType.INCOMING.getMessageType().equalsIgnoreCase(messageAttributes.getType().getMessageType())
			&& (MessageProtocol.TEXT.getMessageProtocol().equalsIgnoreCase(messageAttributes.getProtocol().getMessageProtocol())) && !messageAttributes.getIsManual()) {
			ApiError apiError = new ApiError(ErrorCode.MISMATCH_TYPE_AND_MANUAL.name(), "is manual is false for incoming message type");
			return apiError;
		}

		return null;
	}

	public SubscriptionSaveResponse validateSubscriptionSaveRequest(SubscriptionSaveRequest subscriptionSaveRequest) {

		SubscriptionSaveResponse saveSubscriptionResponse = new SubscriptionSaveResponse();
		List<ApiError> apiErrors = new ArrayList<ApiError>();
		List<ApiWarning> apiWarnings = new ArrayList<ApiWarning>();

		if (subscriptionSaveRequest == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_PROTOCOL.name(), "Message protocol is missing");
			apiErrors.add(apiError);
			saveSubscriptionResponse.setErrors(apiErrors);
			return saveSubscriptionResponse;
		}

		return saveSubscriptionResponse;
	}

	public VoiceCallResponse validateVoiceCallRequest(String departmentUUID, String customerUUID, VoiceCallRequest voiceCallRequest) {

		VoiceCallResponse response = new VoiceCallResponse();

		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();

		response.setErrors(errors);
		response.setWarnings(warnings);

		if (departmentUUID == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_UUIDS.name(), "passed departmentUUID is null");
			errors.add(apiError);
			response.setErrors(errors);
			response.setStatus(Status.FAILURE);
			return response;
		}

		if (voiceCallRequest == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_VOICECALL_BODY.name(), "VoiceCall Request body is null");
			errors.add(apiError);
			response.setErrors(errors);
			response.setStatus(Status.FAILURE);
			return response;
		}

		if (voiceCallRequest.isSupportCall() == false && customerUUID == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_CUSTOMER_UUID.name(), "passed customerUUID is null");
			errors.add(apiError);
			response.setErrors(errors);
			response.setStatus(Status.FAILURE);
			return response;
		}

		if (voiceCallRequest.getParty1Number() == null || (voiceCallRequest.isSupportCall() != true && voiceCallRequest.getParty2Number() == null)) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_PHONE_NUMBERS.name(), "PhoneNumber is null");
			errors.add(apiError);
			response.setErrors(errors);
			response.setStatus(Status.FAILURE);
			return response;
		}

		return response;
	}

	public Response validateCancelCallRequest(Long dealerID, String callSID, Long deptID) {
		Response response = new Response();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();

		response.setErrors(errors);
		response.setWarnings(warnings);

		if (dealerID == null || deptID == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_DEALER_IDS.name(), "dealerID or deptID is null");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}

		if (callSID == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_CALLSID.name(), "CallSID is null");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}

		return response;
	}

	public void validateUsersToNotify(SendMessageRequest request, SendMessageResponse response) {
		if(request.getInternalCommentAttributes() == null || request.getInternalCommentAttributes().getUsersToNotify() == null ||
				request.getInternalCommentAttributes().getUsersToNotify().isEmpty()) {
			return;
		}

		List<ApiError> errors = response.getErrors();
		for(User user: request.getInternalCommentAttributes().getUsersToNotify()) {
			if(!(StringUtils.hasLength(user.getUuid()) && StringUtils.hasLength(user.getName()) && user.getType() != null)) {
				ApiError apiError = new ApiError(ErrorCode.INVALID_USER.name(), String.format("One or more EditorReceiver Fields are invalid for uuid=%s", user.getUuid()));
				errors.add(apiError);
				response.setErrors(errors);
				return;
			}

			if(EditorType.USER.getEditorType().equalsIgnoreCase(user.getType().getEditorType()) && !StringUtils.hasLength(user.getDepartmentUuid())) {
				ApiError apiError = new ApiError(ErrorCode.INVALID_USER.name(), String.format("One or more EditorReceiver Fields are invalid for uuid=%s", user.getUuid()));
				errors.add(apiError);
				response.setErrors(errors);
				return;
			}
		}
	}

	public ThreadFollowResponse validateThreadFollowRequest(ThreadFollowRequest threadFollowRequest) {
		
		ThreadFollowResponse response = new ThreadFollowResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<ApiWarning> warnings = new ArrayList<ApiWarning>();
		Set<Event> events = new HashSet<Event>();
		
		if(threadFollowRequest == null || threadFollowRequest.getUserEvents()==null || threadFollowRequest.getUserEvents().isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_REQUEST.name(), String.format("Thread Follow Request is empty"));
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		if(threadFollowRequest.getCustomerUuid()==null || threadFollowRequest.getCustomerUuid().isEmpty() || threadFollowRequest.getDepartmentUuid()==null
				|| threadFollowRequest.getDepartmentUuid().isEmpty()) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_REQUEST.name(), String.format("Customer and Department Info is required"));
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		for(UserEvent userEvent: threadFollowRequest.getUserEvents()) {
			if(userEvent.getAddedEvents()!=null) {
				events.addAll(userEvent.getAddedEvents());
			}
			if(userEvent.getRevokedEvents()!=null) {
				events.addAll(userEvent.getRevokedEvents());
			}
		}
		
		//validateUsers(users, departmentUUID); to do through acl engine or write here only
		if(!validateUserEvents(events)) {
			
			ApiError apiError = new ApiError(ErrorCode.INVALID_REQUEST.name(), String.format("Notification Type INTERNAL, Category MANUAL and Event INTERNAL NOTE is supported currently"));
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		response.setErrors(errors);
		response.setWarnings(warnings);
		return response;
	}

	public Boolean validateUserEvents(Set<Event> events) {
		
		Set<CategoryEvent> eventNames = new HashSet<CategoryEvent>();
		
		for(Event event: events) {
			
			
			if(!NotificationType.INTERNAL.equals(event.getType()) || !CommunicationCategoryName.MANUAL.equals(event.getCategory().getName())) {
				return false;
			}
			
			for(CategoryEvent categoryEvent: event.getCategory().getEvents()) {
				if(!CategoryEvent.INTERNAL_NOTE.equals(categoryEvent)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public SendMessageWithoutCustomerResponse validateSendMessageWithoutCustomerRequest(SendMessageWithoutCustomerRequest sendMessageRequest) {
		SendMessageWithoutCustomerResponse response = new SendMessageResponse();
		List<ApiError> errors = new ArrayList<>();
		List<ApiWarning> warnings = new ArrayList<>();

		if (sendMessageRequest == null) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_REQUEST.name(), "SendMessageWithoutCustomer request body is null");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(ObjectUtils.isEmpty(sendMessageRequest.getMessageBody())) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_BODY.name(), "Message body is null or empty");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(ObjectUtils.isEmpty(sendMessageRequest.getToNumber())) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_PHONE_NUMBERS.name(), "the number to which sending message is null or empty");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(ObjectUtils.isEmpty(sendMessageRequest.getFromNumber())) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_PHONE_NUMBERS.name(), "the number from which sending message is null or empty");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(ObjectUtils.isEmpty(sendMessageRequest.getMessagePurposeUuid())) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "the purpose of the message is null or empty");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		if(sendMessageRequest.getSendSynchronously() != null && !sendMessageRequest.getSendSynchronously() && sendMessageRequest.getDelayInSeconds() == null) {
			ApiError apiError = new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "message needs to be sent with a delay but the delay value isn't passed");
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		response.setErrors(errors);
		response.setWarnings(warnings);
		return response;
	}

	public ForwardMessageResponse validateForwardMessageRequest(String departmentUuid, String userUuid, String messageUuid, ForwardMessageRequest request) {
		ForwardMessageResponse response = new ForwardMessageResponse();
		if(!StringUtils.hasText(departmentUuid)) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Dealer Department Uuid is missing in request")
			));
			return response;
		}
		if(!StringUtils.hasText(userUuid)) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_USER_UUID.name(), "User Uuid is missing in request")
			));
			return response;
		}
		if(!StringUtils.hasText(messageUuid)) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_UUID.name(), "Message Uuid is missing in request")
			));
			return response;
		}
		if(!StringUtils.hasText(request.getCommunicationValue())) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.INVALID_COMMUNICATION_VALUE.name(), "Communication Value is missing in request")
			));
			return response;
		}
		return response;
	}

    public BotMessageResponse validateSendBotMessageRequest(String departmentUuid, String userUuid, SendBotMessageRequest request) {
		BotMessageResponse response = new BotMessageResponse();
		if(!StringUtils.hasText(departmentUuid)) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Dealer Department Uuid is missing in request")
			));
			return response;
		}
		if(!StringUtils.hasText(userUuid)) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_USER_UUID.name(), "User Uuid is missing in request")
			));
			return response;
		}
		if(request.getMessageAttributes() == null) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "Message attributes are missing in request")
			));
			return response;
		}
		if(!StringUtils.hasText(request.getMessageAttributes().getBody())) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_BODY.name(), "Message body is missing in request")
			));
			return response;
		}
		if(request.getMessageAttributes().getType() == null) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_TYPE.name(), "Message type is missing in request")
			));
			return response;
		}
		if(MessageType.OUTGOING != request.getMessageAttributes().getType()) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.INVALID_MESSAGE_TYPE.name(), String.format("Message type is invalid. Supported Message Types are: %s", Collections.singletonList(MessageType.OUTGOING)))
			));
			return response;
		}
		if(request.getMessageAttributes().getProtocol() == null) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_PROTOCOL.name(), "Message protocol is missing in request")
			));
			return response;
		}
		if(MessageProtocol.TEXT != request.getMessageAttributes().getProtocol()) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.INVALID_MESSAGE_PROTOCOL.name(), String.format("Message protocol is invalid. Supported Message Protocols are: %s", Collections.singletonList(MessageProtocol.TEXT)))
			));
			return response;
		}
		if(request.getMessageSendingAttributes() == null) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_SENDING_ATTRIBUTES.name(), "Message sending attributes are missing in request")
			));
			return response;
		}
		if(!StringUtils.hasText(request.getMessageSendingAttributes().getRecipientCommunicationValue())) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_COMMUNICATION_VALUE.name(), "Communication value is missing in request")
			));
			return response;
		}
		if(!StringUtils.hasText(request.getMessageSendingAttributes().getSenderName())) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_SENDER_NAME.name(), "Bot Name is missing in request")
			));
			return response;
		}
		if(!validateCommunicationTypeAndCommunicationValue(request.getMessageAttributes().getProtocol().name(), request.getMessageSendingAttributes().getRecipientCommunicationValue())) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.INVALID_COMMUNICATION_VALUE.name(), String.format("invalid communication_value=%s for protocol=%s", request.getMessageSendingAttributes().getRecipientCommunicationValue(), request.getMessageAttributes().getProtocol()))
			));
		}
		return response;
    }

	public BotMessageResponse validateSaveBotMessageRequest(String departmentUuid, String userUuid, SaveBotMessageRequest request) {
		BotMessageResponse response = new BotMessageResponse();
		if(!StringUtils.hasText(departmentUuid)) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_DEALER_DEPARTMENT_UUID.name(), "Dealer Department Uuid is missing in request")
			));
			return response;
		}
		if(!StringUtils.hasText(userUuid)) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_USER_UUID.name(), "User Uuid is missing in request")
			));
			return response;
		}
		if(request.getMessageAttributes() == null) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_ATTRIBUTES.name(), "Message attributes are missing in request")
			));
			return response;
		}
		if(request.getMessageAttributes().getType() == null) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_TYPE.name(), "Message type is missing in request")
			));
			return response;
		}
		if(request.getMessageAttributes().getProtocol() == null) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_PROTOCOL.name(), "Message protocol is missing in request")
			));
			return response;
		}
		if(request.getMessageSendingAttributes() == null) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_SENDING_ATTRIBUTES.name(), "Message sending attributes are missing in request")
			));
			return response;
		}
		if(!StringUtils.hasText(request.getMessageSendingAttributes().getRecipientCommunicationValue())) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_COMMUNICATION_VALUE.name(), "Recipient communication value is missing in request")
			));
			return response;
		}
		if(!StringUtils.hasText(request.getMessageSendingAttributes().getSenderCommunicationValue())) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_COMMUNICATION_VALUE.name(), "Sender communication value is missing in request")
			));
			return response;
		}
		if(request.getMessageDeliveryAttributes() == null) {
			response.setErrors(Collections.singletonList(
				new ApiError(ErrorCode.MISSING_MESSAGE_DELIVERY_ATTRIBUTES.name(), "Message Delivery Attributes missing in request")
			));
			return response;
		}
		return response;
	}
}

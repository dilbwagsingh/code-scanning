package com.mykaarma.kcommunications.controller.impl;

import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.Twilio.TwilioGatewayServiceBO;
import com.mykaarma.kcommunications.communications.repository.UIElementTranslationRepository;
import com.mykaarma.kcommunications.jpa.repository.BotMessageRepository;
import com.mykaarma.kcommunications.jpa.repository.ForwardedMessageRepository;
import com.mykaarma.kcommunications.jpa.repository.ForwardingBrokerNumberMappingHelper;
import com.mykaarma.kcommunications.jpa.repository.ForwardingBrokerNumberPoolRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.jpa.BotMessage;
import com.mykaarma.kcommunications.model.jpa.DocFile;
import com.mykaarma.kcommunications.model.jpa.ForwardedMessage;
import com.mykaarma.kcommunications.model.jpa.ForwardingBrokerNumberMapping;
import com.mykaarma.kcommunications.model.jpa.ForwardingBrokerNumberPool;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.rabbit.PostIncomingBotMessageSave;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.utils.DealerRestHelper;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KCustomerApiHelperV2;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.OutOfOfficeHelper;
import com.mykaarma.kcommunications.utils.OutOfOfficeV2Helper;
import com.mykaarma.kcommunications_model.common.BotMessageAttributes;
import com.mykaarma.kcommunications_model.common.BotMessageDeliveryAttributes;
import com.mykaarma.kcommunications_model.common.BotMessageSendingAttributesExtended;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.request.ForwardMessageRequest;
import com.mykaarma.kcommunications_model.request.SaveBotMessageRequest;
import com.mykaarma.kcommunications_model.request.SendBotMessageRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.BotMessageResponse;
import com.mykaarma.kcommunications_model.response.ForwardMessageResponse;
import com.mykaarma.kcustomer_model.dto.Customer;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.twilio.exception.TwilioException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ForwardedAndBotMessageImpl {

    @Value("${twilio.botaccount.sid}")
    private String botTwilioAccountSid;

    @Value("${twilio.botaccount.authToken}")
    private String botTwilioAuthToken;

    @Value("${twilio.botaccount.number}")
    private String botTwilioAccountNumber;

    @Autowired
    private ValidateRequest validateRequest;

    @Autowired
    private GeneralRepository generalRepository;

    @Autowired
    private Helper helper;

    @Autowired
    private KCustomerApiHelperV2 kCustomerApiHelper;

    @Autowired
    private DealerRestHelper dealerRestHelper;

    @Autowired
    private UIElementTranslationRepository uiElementTranslationRepository;

    @Autowired
    private KManageApiHelper kManageApiHelper;

    @Autowired
    private OutOfOfficeHelper outOfOfficeHelper;

    @Autowired
    private ForwardingBrokerNumberPoolRepository forwardingBrokerNumberPoolRepository;

    @Autowired
    private ForwardingBrokerNumberMappingHelper forwardingBrokerNumberMappingHelper;

    @Autowired
    private TwilioGatewayServiceBO twilioGatewayServiceBO;

    @Autowired
    private ForwardedMessageRepository forwardedMessageRepository;

    @Autowired
    private BotMessageRepository botMessageRepository;

    @Autowired
    private RabbitHelper rabbitHelper;

    @Autowired
    private OutOfOfficeV2Helper outOfOfficeV2Helper;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String VIEW_ATTACHED_FILES_KEY = "view.attached.files";
    private static final String DA_NOT_FOUND_TURN_OFF_MESSAGE_SUBJECT = "Unknown request To OutOfOffice-TurnOff-Service";
    private static final String OOO_TURN_OFF_MESSAGE_SUBJECT = "Request To OutOfOffice-TurnOff-Service";
    public static final String OUT_OF_OFFICE_BOT = "OutOfOffice_Bot";


    public ResponseEntity<ForwardMessageResponse> forwardMessage(String departmentUuid, String userUuid, String messageUuid, ForwardMessageRequest request) throws Exception {
        ForwardMessageResponse response;

        log.info("in forwardMessage for department_uuid={} user_uuid={} message_uuid={} request={}", departmentUuid, userUuid, messageUuid, objectMapper.writeValueAsString(request));
        response = validateRequest.validateForwardMessageRequest(departmentUuid, userUuid, messageUuid, request);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Long departmentId;
        try {
            departmentId = generalRepository.getDepartmentIDForUUID(departmentUuid);
        } catch (Exception e) {
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INVALID_DEALER_DEPARTMENT_ID.name(), String.format("department_id not found for department_uuid=%s", departmentUuid))
            ));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Long dealerAssociateId = generalRepository.getDealerAssociateIDForUserUUID(userUuid, departmentId);
        if(dealerAssociateId == null) {
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INVALID_USER.name(), String.format("dealer_associate_id not found for department_uuid=%s user_uuid=%s", departmentUuid, userUuid))
            ));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Message message = helper.getMessageObjectWithDocFiles(messageUuid);
        if(message == null) {
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INVALID_MESSAGE_ID.name(), String.format("message_id not found for message_uuid=%s", messageUuid))
            ));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        MessageProtocol messageProtocol = MessageProtocol.getMessageProtocolForString(message.getProtocol());
        if(!MessageProtocol.TEXT.equals(messageProtocol)) {
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.UNSUPPORTED_MESSAGE_PROTOCOL.name(), String.format("forwarding not supported for protocol=%s", messageProtocol))
            ));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        MessageType messageType = MessageType.getMessageTypeForString(message.getMessageType());
        if(!MessageType.INCOMING.equals(messageType)) {
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.UNSUPPORTED_MESSAGE_TYPE.name(), String.format("forwarding not supported for message_type=%s", messageType))
            ));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if(!validateRequest.validateCommunicationTypeAndCommunicationValue(messageProtocol.name(), request.getCommunicationValue())) {
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INVALID_COMMUNICATION_VALUE.name(), String.format("invalid communication_value=%s for protocol=%s", request.getCommunicationValue(), messageProtocol))
            ));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        request.setCommunicationValue(KCommunicationsUtils.removeCountryCodeIfPresent(request.getCommunicationValue()));
        ForwardedMessage forwardedMessage = getForwardedMessageForMessage(departmentUuid, userUuid, message);
        ForwardingBrokerNumberMapping forwardingBrokerNumberMapping = outOfOfficeHelper.getForwardingBrokerNumberMapping(message.getDealerID(), dealerAssociateId, request.getCommunicationValue(), message.getCustomerID(), message.getFromNumber());
        sendAndSaveForwardedMessage(forwardedMessage, forwardingBrokerNumberMapping, response);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<BotMessageResponse> sendBotMessage(String departmentUuid, String userUuid, SendBotMessageRequest request) throws Exception {
        BotMessageResponse response;
        log.info("in sendBotMessage for department_uuid={} user_uuid={} request={}", departmentUuid, userUuid, objectMapper.writeValueAsString(request));
        response = validateRequest.validateSendBotMessageRequest(departmentUuid, userUuid, request);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Long departmentId;
        try {
            departmentId = generalRepository.getDepartmentIDForUUID(departmentUuid);
        } catch (Exception e) {
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INVALID_DEALER_DEPARTMENT_ID.name(), String.format("department_id not found for department_uuid=%s", departmentUuid))
            ));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Long dealerAssociateId = generalRepository.getDealerAssociateIDForUserUUID(userUuid, departmentId);
        if(dealerAssociateId == null) {
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INVALID_USER.name(), String.format("dealer_associate_id not found for department_uuid=%s user_uuid=%s", departmentUuid, userUuid))
            ));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        DealerAssociateExtendedDTO dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUuid, userUuid).getDealerAssociate();
        BotMessage botMessage = getBotMessageFromSendRequest(dealerAssociate, request);
        sendAndSaveBotMessage(botMessage, response);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<BotMessageResponse> saveBotMessage(String departmentUuid, String userUuid, SaveBotMessageRequest request) throws Exception {
        BotMessageResponse response;
        log.info("in saveBotMessage for request={}", objectMapper.writeValueAsString(request));
        response = validateRequest.validateSaveBotMessageRequest(departmentUuid, userUuid, request);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Long departmentId;
        try {
            departmentId = generalRepository.getDepartmentIDForUUID(departmentUuid);
        } catch (Exception e) {
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INVALID_DEALER_DEPARTMENT_ID.name(), String.format("department_id not found for department_uuid=%s", departmentUuid))
            ));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Long dealerAssociateId = generalRepository.getDealerAssociateIDForUserUUID(userUuid, departmentId);
        if(dealerAssociateId == null) {
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.INVALID_USER.name(), String.format("dealer_associate_id not found for department_uuid=%s user_uuid=%s", departmentUuid, userUuid))
            ));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        BotMessage savedBotMessage = null;
        if(StringUtils.hasText(request.getUuid())) {
            savedBotMessage = botMessageRepository.findByUuid(request.getUuid());
            if(savedBotMessage == null) {
                response.setErrors(Collections.singletonList(
                    new ApiError(ErrorCode.INVALID_MESSAGE_UUID.name(), String.format("Bot Message not found for bot_message_uuid=%s", request.getUuid()))
                ));
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        }
        DealerAssociateExtendedDTO dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUuid, userUuid).getDealerAssociate();
        BotMessage botMessage = getBotMessageFromSaveRequest(dealerAssociate, request, savedBotMessage);
        botMessageRepository.saveAndFlush(botMessage);
        response.setStatus(Status.SUCCESS);
        response.setMessageUuid(botMessage.getUuid());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void sendAndSaveForwardedMessage(ForwardedMessage forwardedMessage, ForwardingBrokerNumberMapping forwardingBrokerNumberMapping, ForwardMessageResponse response) throws Exception {
        ForwardingBrokerNumberPool forwardingBrokerNumberPool = forwardingBrokerNumberPoolRepository.findByBrokerNumber(forwardingBrokerNumberMapping.getBrokerNumber());
        com.twilio.rest.api.v2010.account.Message twilioMessage = null;
        try{
            String[] credentials = forwardingBrokerNumberPool.getCredentials().split("~");
            twilioMessage =  twilioGatewayServiceBO.sendText(
            credentials[0],
            credentials[1],
            forwardedMessage.getMessageBody(),
            forwardingBrokerNumberMapping.getBrokerNumber(),
            forwardingBrokerNumberMapping.getDealerAssociatePhoneNumber(),
            null, null);
        }
        catch (TwilioException e) {
            log.warn(String.format("Twilio exception received for forwarded_message_uuid=%s dealer_id=%s number=%s dealer_associate_id=%s exception=%s",
                forwardedMessage.getUuid(), forwardedMessage.getDealerId(), forwardingBrokerNumberMapping.getDealerAssociatePhoneNumber(),
                forwardingBrokerNumberMapping.getDealerAssociateID(), e.getMessage()), e);
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.TWILIO_SENDING_FAILURE.name(), String.format("Twilio sending failure."
                    + " error_description=%s",e.getMessage()))
            ));
            response.setStatus(Status.FAILURE);
            return;
        }
        if(twilioMessage.getErrorCode() != null) {
            log.warn(String.format("Twilio error received for forwarded_message_uuid=%s dealer_id=%s number=%s dealer_associate_id=%s exception=%s",
                forwardedMessage.getUuid(), forwardedMessage.getDealerId(), forwardingBrokerNumberMapping.getDealerAssociatePhoneNumber(),
                forwardingBrokerNumberMapping.getDealerAssociateID(), objectMapper.writeValueAsString(twilioMessage)));
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.TWILIO_SENDING_FAILURE.name(), String.format("Twilio sending failure. error_code=%s",
                    twilioMessage.getErrorCode()))
            ));
            response.setStatus(Status.FAILURE);
            return;
        }
        forwardingBrokerNumberMapping.setLastMessageOn(new Date());
        forwardingBrokerNumberMappingHelper.saveAndFlush(forwardingBrokerNumberMapping);
        forwardedMessageRepository.saveAndFlush(forwardedMessage);
        response.setForwardedMessageUuid(forwardedMessage.getUuid());
        response.setStatus(Status.SUCCESS);
    }

    public void saveIncomingBotMessageFromTwilio(String fromNumber, String toNumber, String messageBody) throws Exception {
        log.info("in saveIncomingBotMessageFromTwilio for from_number={} to_number={} message_body={}",
            fromNumber, toNumber, messageBody);
        SaveBotMessageRequest request = new SaveBotMessageRequest();
        BotMessageAttributes botMessageAttributes = new BotMessageAttributes();
        BotMessageSendingAttributesExtended botMessageSendingAttributes = new BotMessageSendingAttributesExtended();
        BotMessageDeliveryAttributes botMessageDeliveryAttributes = new BotMessageDeliveryAttributes();

        request.setMessageAttributes(botMessageAttributes);
        request.setMessageSendingAttributes(botMessageSendingAttributes);
        request.setMessageDeliveryAttributes(botMessageDeliveryAttributes);
        botMessageAttributes.setIsManual(true);
        botMessageAttributes.setBody(messageBody);
        botMessageAttributes.setType(MessageType.INCOMING);
        botMessageAttributes.setProtocol(MessageProtocol.TEXT);
        botMessageSendingAttributes.setSenderCommunicationValue(fromNumber);
        botMessageSendingAttributes.setRecipientCommunicationValue(toNumber);
        botMessageSendingAttributes.setRecipientName(OUT_OF_OFFICE_BOT);
        botMessageDeliveryAttributes.setReceivedTimestamp(new Date());
        botMessageDeliveryAttributes.setSentTimestamp(new Date());
        botMessageDeliveryAttributes.setDeliveryStatus("1");

        BotMessage botMessage = botMessageRepository.getLatestTurnOffBotMessageForNumber(
            KCommunicationsUtils.addCountryCodeIfNotPresent(fromNumber)
        );
        DealerAssociateExtendedDTO dealerAssociate = null;
        if(botMessage == null || botMessage.getDealerAssociateId() == null) {
            request.getMessageAttributes().setSubject(DA_NOT_FOUND_TURN_OFF_MESSAGE_SUBJECT);
            log.warn("in processTurnOffMessage could not find da details for communication_value={} bot_message={}",
                fromNumber, objectMapper.writeValueAsString(botMessage));
        } else {
            dealerAssociate = kManageApiHelper.getDealerAssociate(
                generalRepository.getDepartmentUUIDForDepartmentID(botMessage.getDealerDepartmentId()),
                generalRepository.getUserUUIDForDealerAssociateID(botMessage.getDealerAssociateId())
            ).getDealerAssociate();
            botMessageSendingAttributes.setSenderName(dealerAssociate.getFirstName() + " " + dealerAssociate.getLastName());
            request.getMessageAttributes().setSubject(OOO_TURN_OFF_MESSAGE_SUBJECT);
        }

        BotMessage incomingBotMessage = getBotMessageFromSaveRequest(dealerAssociate, request, null);
        incomingBotMessage = botMessageRepository.save(incomingBotMessage);
        rabbitHelper.pushToPostIncomingBotMessageSaveQueue(incomingBotMessage);
    }

    private void sendAndSaveBotMessage(BotMessage botMessage, BotMessageResponse response) throws Exception {
        com.twilio.rest.api.v2010.account.Message twilioMessage = null;
        try{
            twilioMessage =  twilioGatewayServiceBO.sendText(
                botTwilioAccountSid,
                botTwilioAuthToken,
                botMessage.getMessageBody(),
                botTwilioAccountNumber,
                botMessage.getToNumber(),
                null, null);
        }
        catch (TwilioException e) {
            log.warn(String.format("Twilio exception received for bot_message_uuid=%s dealer_id=%s dealer_associate_id=%s exception=%s",
                botMessage.getUuid() , botMessage.getDealerId(), botMessage.getDealerAssociateId(), e.getMessage()), e);
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.TWILIO_SENDING_FAILURE.name(), String.format("Twilio sending failure."
                    + " error_description=%s",e.getMessage()))
            ));
            response.setStatus(Status.FAILURE);
            return;
        }
        if(twilioMessage.getErrorCode() != null) {
            log.warn(String.format("Twilio exception received for bot_message_uuid=%s dealer_id=%s dealer_associate_id=%s twilio_message=%s",
                botMessage.getUuid() , botMessage.getDealerId(), botMessage.getDealerAssociateId(), objectMapper.writeValueAsString(twilioMessage)));
            response.setErrors(Collections.singletonList(
                new ApiError(ErrorCode.TWILIO_SENDING_FAILURE.name(), String.format("Twilio sending failure. error_code=%s",
                    twilioMessage.getErrorCode()))
            ));
            response.setStatus(Status.FAILURE);
            return;
        }
        botMessageRepository.saveAndFlush(botMessage);
        response.setMessageUuid(botMessage.getUuid());
        response.setStatus(Status.SUCCESS);
    }

    private ForwardedMessage getForwardedMessageForMessage(String departmentUuid, String userUuid, Message message) throws JsonProcessingException {
        ForwardedMessage forwardedMessage = new ForwardedMessage();
        forwardedMessage.setDealerId(message.getDealerID());
        forwardedMessage.setDealerAssociateId(message.getDealerAssociateID());
        forwardedMessage.setMessageType(message.getMessageType());
        forwardedMessage.setProtocol(message.getProtocol());
        forwardedMessage.setMessageBody(getMessageBodyForForwardedMessage(departmentUuid, userUuid, message));
        forwardedMessage.setMessageSize(forwardedMessage.getMessageBody().length());
        forwardedMessage.setUuid(helper.getBase64EncodedSHA256UUID());
        forwardedMessage.setTimeStamp(new Date());
        log.info("in getBotMessageFromSendRequest message={} forwarded_message={}", objectMapper.writeValueAsString(message), objectMapper.writeValueAsString(forwardedMessage));
        return forwardedMessage;
    }

    private BotMessage getBotMessageFromSaveRequest(DealerAssociateExtendedDTO dealerAssociate, SaveBotMessageRequest request, BotMessage savedBotMessage) throws JsonProcessingException {
        BotMessage botMessage = new BotMessage();
        if(savedBotMessage != null) {
            botMessage.setVersion(savedBotMessage.getVersion());
            botMessage.setId(savedBotMessage.getId());
            botMessage.setUuid(savedBotMessage.getUuid());
        } else {
            botMessage.setUuid(helper.getBase64EncodedSHA256UUID());
        }
        botMessage.setMessageBody(request.getMessageAttributes().getBody());
        botMessage.setMessageSize(request.getMessageAttributes().getBody().length());
        botMessage.setSubject(request.getMessageAttributes().getSubject());
        botMessage.setMessageType(request.getMessageAttributes().getType().getMessageType());
        botMessage.setProtocol(request.getMessageAttributes().getProtocol().getMessageProtocol());
        if(dealerAssociate != null) {
            botMessage.setDealerId(dealerAssociate.getDepartmentExtendedDTO().getDealerMinimalDTO().getId());
            botMessage.setDealerDepartmentId(dealerAssociate.getDepartmentExtendedDTO().getId());
            botMessage.setDealerAssociateId(dealerAssociate.getId());
        }
        if(request.getMessageAttributes().getPurpose() != null) {
            botMessage.setMessagePurpose(request.getMessageAttributes().getPurpose().getMessagePurpose());
        }
        botMessage.setIsManual(request.getMessageAttributes().getIsManual());
        botMessage.setNumberOfMessageAttachments(request.getMessageAttributes().getNumberOfMessageAttachments());
        botMessage.setToName(request.getMessageSendingAttributes().getRecipientName());
        botMessage.setToNumber(KCommunicationsUtils.addCountryCodeIfNotPresent(request.getMessageSendingAttributes().getRecipientCommunicationValue()));
        botMessage.setFromName(request.getMessageSendingAttributes().getSenderName());
        botMessage.setFromNumber(KCommunicationsUtils.addCountryCodeIfNotPresent(request.getMessageSendingAttributes().getSenderCommunicationValue()));
        botMessage.setDeliveryStatus(request.getMessageDeliveryAttributes().getDeliveryStatus());
        botMessage.setSentOn(request.getMessageDeliveryAttributes().getSentTimestamp());
        botMessage.setReceivedOn(request.getMessageDeliveryAttributes().getReceivedTimestamp());
        log.info("in getBotMessageFromSaveRequest new_bot_message={}", objectMapper.writeValueAsString(botMessage));
        return botMessage;

    }

    private BotMessage getBotMessageFromSendRequest(DealerAssociateExtendedDTO dealerAssociate, SendBotMessageRequest request) throws JsonProcessingException {
        BotMessage botMessage = new BotMessage();
        botMessage.setMessageBody(request.getMessageAttributes().getBody());
        botMessage.setMessageSize(request.getMessageAttributes().getBody().length());
        botMessage.setSubject(request.getMessageAttributes().getSubject());
        botMessage.setUuid(helper.getBase64EncodedSHA256UUID());
        botMessage.setMessageType(request.getMessageAttributes().getType().getMessageType());
        botMessage.setProtocol(request.getMessageAttributes().getProtocol().getMessageProtocol());
        botMessage.setDealerId(dealerAssociate.getDepartmentExtendedDTO().getDealerMinimalDTO().getId());
        botMessage.setDealerDepartmentId(dealerAssociate.getDepartmentExtendedDTO().getId());
        botMessage.setDealerAssociateId(dealerAssociate.getId());
        if(request.getMessageAttributes().getPurpose() != null) {
            botMessage.setMessagePurpose(request.getMessageAttributes().getPurpose().getMessagePurpose());
        }
        botMessage.setIsManual(request.getMessageAttributes().getIsManual());
        botMessage.setNumberOfMessageAttachments(request.getMessageAttributes().getNumberOfMessageAttachments());
        botMessage.setToName(dealerAssociate.getFirstName() + " " + dealerAssociate.getLastName());
        botMessage.setToNumber(KCommunicationsUtils.addCountryCodeIfNotPresent(request.getMessageSendingAttributes().getRecipientCommunicationValue()));
        botMessage.setFromName(request.getMessageSendingAttributes().getSenderName());
        botMessage.setFromNumber(KCommunicationsUtils.addCountryCodeIfNotPresent(botTwilioAccountNumber));
        botMessage.setDeliveryStatus("1");
        botMessage.setSentOn(new Date());
        botMessage.setReceivedOn(new Date());
        log.info("in getBotMessageFromSendRequest bot_message={}", objectMapper.writeValueAsString(botMessage));
        return botMessage;
    }

    private String getMessageBodyForForwardedMessage(String departmentUuid, String userUuid, Message message) throws JsonProcessingException {
        String messageBody = "";
        Customer customer = kCustomerApiHelper.getCustomerWithoutVehicle(
            departmentUuid,
            generalRepository.getCustomerUUIDFromCustomerID(message.getCustomerID())
        ).getCustomerWithVehicles().getCustomer();
        messageBody += customer.getFirstName() + " " + customer.getLastName();
        String customerPhoneNumber = message.getFromNumber();
        if(StringUtils.hasText(customerPhoneNumber)) {
            customerPhoneNumber = KCommunicationsUtils.removeCountryCodeIfPresent(customerPhoneNumber);
            if(customerPhoneNumber.length() == 10) {
                messageBody += " " + "(XXX)-XXX-" + customerPhoneNumber.substring(customerPhoneNumber.length() - 4);
            } else {
                messageBody += " " + "(" + new String(new char[customerPhoneNumber.length() - 4]).replace("\0", "X") + ")-" +
                    customerPhoneNumber.substring(customerPhoneNumber.length() - 4);
            }
        }
        messageBody += " : ";
        messageBody += message.getMessageExtn().getMessageBody() + " ";
        if(message.getDocFiles() != null && !message.getDocFiles().isEmpty()) {
            log.info("in getMessageBodyForForwardedMessage doc files present for message={}", objectMapper.writeValueAsString(message));
            String daPreferredLocale = helper.getDealerAssociatePreferredLocale(departmentUuid, userUuid);
            String[] attachmentUrls = dealerRestHelper.getShortUrlFromKaarmaDealer(
                generalRepository.getDealerUUIDFromDealerId(message.getDealerID()),
                departmentUuid,
                message.getDocFiles().stream().map(DocFile::getDocFileName).collect(Collectors.toList())
            );
            messageBody += uiElementTranslationRepository.getTranslatedText(VIEW_ATTACHED_FILES_KEY, daPreferredLocale) + " "
                + StringUtils.arrayToDelimitedString(attachmentUrls, ", ");
        }
        return messageBody;
    }

    public void postIncomingBotMessageSave(PostIncomingBotMessageSave postIncomingBotMessageSave) throws Exception {
        log.info("in postIncomingBotMessageSave for request={}", objectMapper.writeValueAsString(postIncomingBotMessageSave));
        BotMessage botMessage = postIncomingBotMessageSave.getBotMessage();
        if(botMessage.getDealerAssociateId() != null) {
            outOfOfficeV2Helper.processTurnOffMessage(
                generalRepository.getDepartmentUUIDForDepartmentID(botMessage.getDealerDepartmentId()),
                generalRepository.getUserUUIDForDealerAssociateID(botMessage.getDealerAssociateId()),
                OutOfOfficeV2Helper.createTurnOffMessageRequest(
                    botMessage.getFromNumber(),
                    botMessage.getMessageBody(),
                    botMessage.getProtocol())
            );
        }
    }
}

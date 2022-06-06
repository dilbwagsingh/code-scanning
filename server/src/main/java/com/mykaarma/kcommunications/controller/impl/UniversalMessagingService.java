package com.mykaarma.kcommunications.controller.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.authenticationutilsmodel.model.v2.request.AuthorizationRequest;
import com.mykaarma.authenticationutilsmodel.model.v2.response.AuthorizationResponse;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.AuthenticationUtilsApiWrapper;
import com.mykaarma.kcommunications.utils.ConvertToJpaEntity;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsException;
import com.mykaarma.kcommunications.utils.KCustomerApiHelperV2;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.User;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;
import com.mykaarma.kcustomer_model.lombokresponse.CustomerWithVehiclesResponse;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class UniversalMessagingService {

    private final static Logger LOGGER = LoggerFactory.getLogger(UniversalMessagingService.class);
    private static final String LOG_MESSAGE_API_SUPPORT = "Internal error - %s while processing request! Please contact Communications API support";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ValidateRequest validateRequest;

    @Autowired
    private ConvertToJpaEntity convertToJpaEntity;

    @Autowired
    private SaveMessageHelper saveMessageHelper;

    @Autowired
    private RabbitHelper rabbitHelper;

    @Autowired
    private KManageApiHelper kManageApiHelper;

    @Autowired
    private AuthenticationUtilsApiWrapper authenticationUtilsApiWrapper;

    @Autowired
    private Helper helper;

    @Autowired
    private SendCallback sendCallback;

    public ResponseEntity<SendMessageResponse> createMessage(String customerUUID, String departmentUUID,
            SendMessageRequest sendMessageRequest, String serviceSubscriberName) throws Exception {
        LOGGER.info("in createMessage received request for creating message for department_uuid={} customer_uuid={} send_message_request={}",
                departmentUUID, customerUUID, objectMapper.writeValueAsString(sendMessageRequest));

        try {
            SendMessageResponse universalMessageResponse = new SendMessageResponse();

            universalMessageResponse = validateRequest.validateSendMessageRequestEditor(sendMessageRequest);
            if(universalMessageResponse.getErrors()!=null && !universalMessageResponse.getErrors().isEmpty()) {
                LOGGER.error("Send Message Request Validation Failed for send_message_request={}", sendMessageRequest);
                universalMessageResponse.setStatus(Status.FAILURE);
                return new ResponseEntity<>(universalMessageResponse, HttpStatus.BAD_REQUEST);
            }

            LOGGER.info("Validating Send Message Request request={}", objectMapper.writeValueAsString(universalMessageResponse));
            universalMessageResponse = validateRequest.validateSendMessageRequest(sendMessageRequest);
            if(universalMessageResponse.getErrors()!=null && !universalMessageResponse.getErrors().isEmpty()) {
                universalMessageResponse.setStatus(Status.FAILURE);
                return new ResponseEntity<>(universalMessageResponse, HttpStatus.BAD_REQUEST);
            }

            // TODO : Move this under ACL Implementation TODO
            User editor = sendMessageRequest.getEditor();
            if(MessageType.NOTE.getMessageType().equalsIgnoreCase(sendMessageRequest.getMessageAttributes().getType().getMessageType())
                    && sendMessageRequest.getMessageAttributes().getIsManual()) {
                AuthorizationRequest authorizationRequest = helper.getAuthorizationRequestForCreatingMessage(editor, departmentUUID, sendMessageRequest);
                AuthorizationResponse authorizationResponse = authenticationUtilsApiWrapper.authorize(editor.getUuid(), authorizationRequest);

                if(authorizationResponse == null || !authorizationResponse.getIsAuthorized()) {
                    LOGGER.info("Editor is not authorized to send message for departmentUuid={} editor={} sendMessageRequest={}",
                            departmentUUID, Helper.toString(editor), Helper.toString(sendMessageRequest));
                    universalMessageResponse.setStatus(Status.FAILURE);
                    return new ResponseEntity<>(universalMessageResponse, HttpStatus.UNAUTHORIZED);
                }
            }

            if(MessageType.NOTE.getMessageType().equalsIgnoreCase(sendMessageRequest.getMessageAttributes().getType().getMessageType()) &&
                    MessageProtocol.NONE.getMessageProtocol().equalsIgnoreCase(sendMessageRequest.getMessageAttributes().getProtocol().getMessageProtocol())) {

                return processUniversalMessageNote(customerUUID, departmentUUID, editor.getUuid(),
                        sendMessageRequest, serviceSubscriberName, universalMessageResponse);

            } else {
                LOGGER.error("This Functionality has not been implemented yet. Only Manual Notes are supported. Request={}", objectMapper.writeValueAsString(sendMessageRequest));
                List<ApiError> errors = new ArrayList<>();
                errors.add(new ApiError(ErrorCode.NOT_IMPLEMENTED.name(), "Functionality Not Implemented. Only Manual Notes are supported"));

                universalMessageResponse.setErrors(errors);
                universalMessageResponse.setStatus(Status.FAILURE);
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(universalMessageResponse);
            }

        } catch (Exception e) {
            LOGGER.error("Exception in creating universal message", e);
            throw new KCommunicationsException(ErrorCode.INTERNAL_SERVER_ERROR, String.format(LOG_MESSAGE_API_SUPPORT, e.getMessage()));
        }

    }

    public ResponseEntity<SendMessageResponse> processUniversalMessageNote(String customerUUID, String departmentUUID, String userUUID,
            SendMessageRequest universalMessageRequest, String serviceSubscriberName, SendMessageResponse universalMessageResponse) throws Exception {

        LOGGER.info("In ProcessUniversalMessage received request for department_uuid={} customer_uuid={} user_uuid={} universal_message_request={}",
                departmentUUID, customerUUID, userUUID, objectMapper.writeValueAsString(universalMessageRequest));

        try {
            CustomerWithVehiclesResponse customer = KCustomerApiHelperV2.getCustomer(departmentUUID, customerUUID);

            validateRequest.validateCustomerBasic(universalMessageResponse, customer, customerUUID);
            if(universalMessageResponse.getErrors() != null && !universalMessageResponse.getErrors().isEmpty()) {
                universalMessageResponse.setStatus(Status.FAILURE);
                LOGGER.error("Error in validating customer request error={}", objectMapper.writeValueAsString(universalMessageResponse.getErrors()));
                return new ResponseEntity<>(universalMessageResponse, HttpStatus.BAD_REQUEST);
            }

            GetDealerAssociateResponseDTO dealerAssociate = null;
            if(APIConstants.DEFAULT.equalsIgnoreCase(userUUID)) {
                dealerAssociate = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);
            } else {
                dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUUID, userUUID);
            }

            validateRequest.validateDealerAssociate(userUUID, universalMessageRequest, universalMessageResponse, dealerAssociate);
            if(universalMessageResponse.getErrors() != null && !universalMessageResponse.getErrors().isEmpty()) {
                universalMessageResponse.setStatus(Status.FAILURE);
                LOGGER.info("Error in validating dealer_associate request error={}", objectMapper.writeValueAsString(universalMessageResponse.getErrors()));
                return new ResponseEntity<>(universalMessageResponse, HttpStatus.BAD_REQUEST);
            }

            validateRequest.validateUsersToNotify(universalMessageRequest, universalMessageResponse);
            if(universalMessageResponse.getErrors() != null && !universalMessageResponse.getErrors().isEmpty()) {
                universalMessageResponse.setStatus(Status.FAILURE);
                LOGGER.info("Error in validating users to notify in request error={}", objectMapper.writeValueAsString(universalMessageResponse.getErrors()));
                return new ResponseEntity<>(universalMessageResponse, HttpStatus.BAD_REQUEST);
            }

            // TODO : ACL Implementation

            Message message = convertToJpaEntity.getMessageJpaEntity(universalMessageRequest, customer.getCustomerWithVehicles().getCustomer(),
                    dealerAssociate.getDealerAssociate(), null);
            LOGGER.info("universalMessageRequest={} message_object={}", objectMapper.writeValueAsString(universalMessageRequest), objectMapper.writeValueAsString(message));

            List<User> usersToNotify = null;
            if(universalMessageRequest.getInternalCommentAttributes() != null) {
                usersToNotify = universalMessageRequest.getInternalCommentAttributes().getUsersToNotify();
            }

            Boolean sendSynchronously = false;
            if(universalMessageRequest.getMessageSendingAttributes() != null && universalMessageRequest.getMessageSendingAttributes().getSendSynchronously() != null &&
                    universalMessageRequest.getMessageSendingAttributes().getSendSynchronously()) {
                sendSynchronously = true;
            }

            Boolean updateThreadTimestamp = true;
            if(universalMessageRequest.getMessageAttributes().getUpdateThreadTimestamp() != null && !universalMessageRequest.getMessageAttributes().getUpdateThreadTimestamp()) {
                updateThreadTimestamp = false;
            }

            universalMessageResponse = saveAndPushUniversalMessage(customerUUID, departmentUUID, message, updateThreadTimestamp,
                    dealerAssociate.getDealerAssociate(), usersToNotify, sendSynchronously);

            sendCallback(universalMessageRequest.getMessageSendingAttributes(), universalMessageResponse);

            MDC.put(APIConstants.MESSAGE_ID, message.getId());
            return new ResponseEntity<>(universalMessageResponse, HttpStatus.OK);

        } catch (Exception e) {
            LOGGER.error("Exception in processUniversalMessage", e);
            throw new KCommunicationsException(ErrorCode.INTERNAL_SERVER_ERROR, String.format(LOG_MESSAGE_API_SUPPORT, e.getMessage()));
        }
    }

    private SendMessageResponse saveAndPushUniversalMessage(String customerUUID, String departmentUUID, Message message, Boolean updateThreadTimestamp,
            DealerAssociateExtendedDTO dealerAssociate, List<User> usersToNotify, Boolean sendSynchronously) throws Exception {

        SendMessageResponse universalMessageResponse = new SendMessageResponse();
        universalMessageResponse.setCustomerUUID(customerUUID);

        if(message == null) {
            LOGGER.error("in saveAndPushUniversalMessage trying to send a null message object");
            universalMessageResponse.setStatus(Status.FAILURE);
            return universalMessageResponse;
        }

        LOGGER.info("in saveAndPushUniversalMessage for message_object={} updateThreadTimestamp={} ", objectMapper.writeValueAsString(message), updateThreadTimestamp);

        if(sendSynchronously) {
            saveMessageHelper.saveMessage(message);
            try {
                if(message.getIsManual()) {
                    rabbitHelper.pushToPostUniversalMessageSendQueue(message, usersToNotify, updateThreadTimestamp);
                } else {
                    rabbitHelper.pushToMessagePostSendingQueue(message, null, false, false, false, updateThreadTimestamp);
                }
            } catch (Exception e) {
                LOGGER.error("Error in Pushing message for Post Message Send Processing for message_uuid={}", message.getUuid(), e);
            }
        }
        else {
            rabbitHelper.pushToMessageSavingQueue(message, updateThreadTimestamp, usersToNotify);
        }

        universalMessageResponse.setMessageUUID(message.getUuid());
        universalMessageResponse.setStatus(Status.SUCCESS);
        return universalMessageResponse;

    }

    private void sendCallback(MessageSendingAttributes messageSendingAttributes, SendMessageResponse universalMessageResponse) {
        if(messageSendingAttributes != null && !StringUtils.isEmpty(messageSendingAttributes.getCallbackURL())) {
            universalMessageResponse.setMetaData(messageSendingAttributes.getCallbackMetaData());

            try {
                LOGGER.info("Hitting callback for callbackUrl={} response={}", messageSendingAttributes.getCallbackURL(), Helper.toString(universalMessageResponse));
                sendCallback.sendCallback(messageSendingAttributes.getCallbackURL(), HttpMethod.POST, universalMessageResponse, true);
            }
            catch (Exception e) {
                LOGGER.error("Error in hitting callBack for callbackUrl={} response={}",
                    messageSendingAttributes.getCallbackURL(), Helper.toString(universalMessageResponse), e);
            }
        }
    }

}

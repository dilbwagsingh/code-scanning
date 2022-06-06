package com.mykaarma.kcommunications.controller.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.redis.RedisService;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.ConvertToJpaEntity;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsException;
import com.mykaarma.kcommunications.utils.KCustomerApiHelperV2;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;
import com.mykaarma.kcustomer_model.lombokresponse.CustomerWithVehiclesResponse;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class IncomingMessageService {

    private static final String LOG_MESSAGE_API_SUPPORT = "Internal error - %s while processing request! Please contact Communications API support";

    private final static Logger LOGGER = LoggerFactory.getLogger(IncomingMessageService.class);

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
    private RedisService redisService;

    @Autowired
    private KManageApiHelper kManageApiHelper;

    @Autowired
    Helper helper;

    public ResponseEntity<SendMessageResponse> processIncomingMessage(String customerUUID, String departmentUUID, String userUUID,
            SendMessageRequest incomingMessageRequest, String serviceSubscriberName,  SendMessageResponse incomingMessageResponse) throws Exception {
        LOGGER.info("in processIncomingMessage received request for department_uuid={} customer_uuid={} user_uuid={} incoming_message_request={}",
                departmentUUID, customerUUID, userUUID, objectMapper.writeValueAsString(incomingMessageRequest));

        try {
            CustomerWithVehiclesResponse customer = null;
            try {
                customer = KCustomerApiHelperV2.getCustomerWithoutVehicle(departmentUUID, customerUUID);
            } catch (Exception e) {
                LOGGER.error(String.format("Error in fetching customer for customer_uuid=%s dealer_department_uuid=%s ", customerUUID, departmentUUID), e);
            }

            validateRequest.validateCustomerBasic(incomingMessageResponse, customer, customerUUID);
            if(incomingMessageResponse.getErrors() != null && !incomingMessageResponse.getErrors().isEmpty()) {
                incomingMessageResponse.setStatus(Status.FAILURE);
                LOGGER.info("Error in validating customer request error={}", objectMapper.writeValueAsString(incomingMessageResponse.getErrors()));
                return new ResponseEntity<>(incomingMessageResponse, HttpStatus.BAD_REQUEST);
            }

            GetDealerAssociateResponseDTO dealerAssociate = null;
            if(APIConstants.DEFAULT.equalsIgnoreCase(userUUID)) {
                dealerAssociate = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);
            } else {
                dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUUID, userUUID);
            }

            validateRequest.validateDealerAssociate(userUUID, incomingMessageRequest, incomingMessageResponse, dealerAssociate);
            if(incomingMessageResponse.getErrors() != null && !incomingMessageResponse.getErrors().isEmpty()) {
                incomingMessageResponse.setStatus(Status.FAILURE);
                LOGGER.info("Error in validating dealer_associate request error={}", objectMapper.writeValueAsString(incomingMessageResponse.getErrors()));
                return new ResponseEntity<>(incomingMessageResponse, HttpStatus.BAD_REQUEST);
            }

            validateRequest.validateIncomingMessageAttributes(incomingMessageRequest, incomingMessageResponse);
            if(incomingMessageResponse.getErrors() !=null && !incomingMessageResponse.getErrors().isEmpty()) {
                incomingMessageResponse.setStatus(Status.FAILURE);
                LOGGER.info("Error in validating incoming message attributes request error={}", objectMapper.writeValueAsString(incomingMessageResponse.getErrors()));
                return new ResponseEntity<>(incomingMessageResponse, HttpStatus.BAD_REQUEST);
            }

            MDC.put(APIConstants.DEALER_ASSOCIATE_ID, dealerAssociate.getDealerAssociate().getId());
            MDC.put(APIConstants.CUSTOMER_ID, customer.getCustomerWithVehicles().getCustomer().getId());

            Message message = convertToJpaEntity.getMessageJpaEntity(incomingMessageRequest, customer.getCustomerWithVehicles().getCustomer(),
                    dealerAssociate.getDealerAssociate(), null);
            LOGGER.info("incomingMessageRequest={} message_object={}", objectMapper.writeValueAsString(incomingMessageRequest), objectMapper.writeValueAsString(message));

            boolean forwardText = false;
            if(incomingMessageRequest.getIncomingMessageAttributes() != null && incomingMessageRequest.getIncomingMessageAttributes().getForwardText() != null
                    && incomingMessageRequest.getIncomingMessageAttributes().getForwardText()) {
                forwardText = true;
            }

            incomingMessageResponse = saveAndPushIncomingMessage(customerUUID, departmentUUID, userUUID, message,
                    incomingMessageRequest.getMessageAttributes().getUpdateThreadTimestamp(), dealerAssociate.getDealerAssociate(), forwardText);

            MDC.put(APIConstants.MESSAGE_ID, message.getId());
            return new ResponseEntity<>(incomingMessageResponse, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error("Exception in processIncomingMessage", e);
            throw new KCommunicationsException(ErrorCode.INTERNAL_SERVER_ERROR, String.format(LOG_MESSAGE_API_SUPPORT, e.getMessage()));
        }
    }

    private SendMessageResponse saveAndPushIncomingMessage(String customerUUID, String departmentUUID, String userUUID,Message message, Boolean updateThreadTimestamp,
            DealerAssociateExtendedDTO dealerAssociate, boolean forwardText) throws Exception {
        SendMessageResponse incomingMessageResponse = new SendMessageResponse();

        if(message == null) {
            LOGGER.error("in saveAndPushIncomingMessage trying to send a null message object");
            incomingMessageResponse.setStatus(Status.FAILURE);
            return incomingMessageResponse;
        }

        LOGGER.info("in saveAndPushIncomingMessage for message_object={} updateThreadTimestamp={} forwardText={}",
                objectMapper.writeValueAsString(message), updateThreadTimestamp, forwardText);
        saveMessageHelper.saveMessage(message);

        Long threadDelegatee = message.getDealerAssociateID();
        if(dealerAssociate != null && dealerAssociate.getRoleDTO() != null &&
                !helper.checkIfDealerAssociateCanOwnThread(message, dealerAssociate.getRoleDTO().getRoleName(), departmentUUID, userUUID)) {
            threadDelegatee = null;
        }

        try {
            rabbitHelper.pushToPostIncomingMessageSaveQueue(message, threadDelegatee, updateThreadTimestamp);
            if (forwardText) {
                redisService.addToMessageSet(message.getId(), message.getReceivedOn().getTime(), message.getDealerID(),
                        Helper.zeroTime(message.getReceivedOn()).toString());
            }
        } catch (Exception e) {
            LOGGER.error("Error in processing post incoming message save for message_uuid={}", message.getUuid());
        }

        incomingMessageResponse.setCustomerUUID(customerUUID);
        incomingMessageResponse.setMessageUUID(message.getUuid());
        incomingMessageResponse.setStatus(Status.SUCCESS);
        return incomingMessageResponse;
    }

}

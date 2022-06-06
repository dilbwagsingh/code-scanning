package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mykaarma.global.MessagePredictionFeedbackTypes;
import com.mykaarma.global.PredictionFeature;
import com.mykaarma.kcommunications.communications.model.jpa.CustomerPreferredCommunicationMode;
import com.mykaarma.kcommunications.communications.repository.CustomerPreferredCommunicationModeRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessagePredictionRepository;
import com.mykaarma.kcommunications.jpa.repository.PredictionFeatureRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.kne.KNotificationMessage;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.rabbit.PreferredCommunicationModePrediction;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KNotificationApiHelper;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessagePurpose;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.WarningCode;
import com.mykaarma.kcommunications_model.request.MultipleCustomersPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.PredictPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionFeedbackRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionRequest;
import com.mykaarma.kcommunications_model.request.UpdatePreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.GetPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.MultipleCustomersPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.PredictPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.Response;

@Service
public class PreferredCommunicationModeImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(PreferredCommunicationModeImpl.class);

	@Autowired
    private ValidateRequest validateRequest;
    
    @Autowired
    private RabbitHelper rabbitHelper;
	
	@Autowired
    private Helper helper;
    
    @Autowired
    private CommunicationsApiImpl communicationsApiImpl;
    
    @Autowired
    private GeneralRepository generalRepository;

    @Autowired
    private PredictionFeatureRepository predictionFeatureRepository;

    @Autowired
    private MessagePredictionRepository messagePredictionRepository;

    @Autowired
    private KNotificationApiHelper kNotificationApiHelper;

    @Autowired
    private CustomerPreferredCommunicationModeRepository customerPreferredCommunicationModeRepository;

    private static final List<MessageProtocol> ALLOWED_PROTOCOLS = Arrays.asList(MessageProtocol.TEXT, MessageProtocol.EMAIL, MessageProtocol.VOICE_CALL);
    private static final String MESSAGE_PROTOCOL_SLIDING_WINDOW_ATTRIBUTE = "MESSAGE_PROTOCOL_SLIDING_WINDOW";
    private static final String PREDICTION_MESSAGE_ID_ATTRIBUTE = "PREDICTION_MESSAGE_ID";
    private static final Integer MESSAGE_PROTOCOL_SLIDING_WINDOW_SIZE = 4;
    private static final String FEEDBACK_MESSAGE_PROTOCOL_ATTRIBUTE = "FEEDBACK_MESSAGE_PROTOCOL";
    private static final String FEEDBACK_MESSAGE_ID_ATTRIBUTE = "FEEDBACK_MESSAGE_ID";

	public ResponseEntity<Response> updatePreferredCommunicationMode(String departmentUUID, String customerUUID,
            UpdatePreferredCommunicationModeRequest updatePreferredCommunicationModeRequest) throws Exception {
        ObjectMapper om = new ObjectMapper();
        Response response;

        LOGGER.info("received request in updatePreferredCommunicationMode with department_uuid={} customer_uuid={} request={}", 
            departmentUUID, customerUUID, om.writeValueAsString(updatePreferredCommunicationModeRequest));
        response = validateRequest.validateUpdatePreferredCommunicationModeRequest(departmentUUID, customerUUID, updatePreferredCommunicationModeRequest);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
        }
        customerPreferredCommunicationModeRepository.upsertPreferredCommunicationMode(customerUUID, updatePreferredCommunicationModeRequest.getPreferredCommunicationMode(), updatePreferredCommunicationModeRequest.getPreferredCommunicationModeMetaData());
        response = new Response();
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    public ResponseEntity<GetPreferredCommunicationModeResponse> getPreferredCommunicationMode(String departmentUUID, String customerUUID) throws Exception {
        
        GetPreferredCommunicationModeResponse response;
        LOGGER.info("received request in getPreferredCommunicationMode with department_uuid={} customer_uuid={}", 
            departmentUUID, customerUUID);
        response = validateRequest.validateGetPreferredCommunicationModeRequest(departmentUUID, customerUUID);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<GetPreferredCommunicationModeResponse>(response, HttpStatus.BAD_REQUEST);
        }
        CustomerPreferredCommunicationMode customerPreferredCommunicationMode = customerPreferredCommunicationModeRepository.findByCustomerUUID(customerUUID);
        response = new GetPreferredCommunicationModeResponse();
        if(customerPreferredCommunicationMode == null) {
            LOGGER.warn("No customer found in getCustomerPreferredCommunicationMode for customer_uuid={} department_uuid={} ", customerUUID, departmentUUID);
            ApiWarning warning = new ApiWarning(WarningCode.NO_CUSTOMER_FOUND.name(), "No entry exists for given UUID.");
            List<ApiWarning> warnings = new ArrayList<ApiWarning> ();
            warnings.add(warning);
            response.setWarnings(warnings);
        } else {
            response.setPreferredCommunicationMode(customerPreferredCommunicationMode.getProtocol());
            response.setPreferredCommunicationModeMetaData(customerPreferredCommunicationMode.getMetaData());
        }
        return new ResponseEntity<GetPreferredCommunicationModeResponse>(response, HttpStatus.OK);
    }

    public ResponseEntity<MultipleCustomersPreferredCommunicationModeResponse> getMultipleCustomersPreferredCommunicationModeProtocol(String departmentUUID, MultipleCustomersPreferredCommunicationModeRequest multipleCustomersPreferredCommunicationModeRequest) throws Exception {
        
        MultipleCustomersPreferredCommunicationModeResponse response;
        LOGGER.info("received request in getMultipleCustomersPreferredCommunicationMode with department_uuid={} customer_uuid_list={}", 
            departmentUUID, multipleCustomersPreferredCommunicationModeRequest.getCustomerUUIDList());
        response = validateRequest.validateGetMultipleCustomersPreferredCommunicationMode(departmentUUID, multipleCustomersPreferredCommunicationModeRequest);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<MultipleCustomersPreferredCommunicationModeResponse>(response, HttpStatus.BAD_REQUEST);
        }
        response = new MultipleCustomersPreferredCommunicationModeResponse();
        Map<String, String> customerPreferredCommunicationModeMap =  new HashMap<String, String>();
        List<Object[]> dbResponse = customerPreferredCommunicationModeRepository.getCustomersPreferredCommunicationProtocol(multipleCustomersPreferredCommunicationModeRequest.getCustomerUUIDList());
        if(dbResponse != null) {
            for(Object[] object : dbResponse) {
                String customerUUID = (String)object[0];
                String protocol = (String)object[1];
                customerPreferredCommunicationModeMap.put(customerUUID, protocol);
            }
        }
        response.setCustomerPreferredCommunicationModeMap(customerPreferredCommunicationModeMap);
        return new ResponseEntity<MultipleCustomersPreferredCommunicationModeResponse>(response, HttpStatus.OK);
    }

	public ResponseEntity<PredictPreferredCommunicationModeResponse> predictPreferredCommunicationMode(String departmentUUID, String customerUUID, PredictPreferredCommunicationModeRequest predictPreferredCommunicationModeRequest) 
		throws Exception {
        
        ObjectMapper om = new ObjectMapper();
        PredictPreferredCommunicationModeResponse response;

        LOGGER.info("received request in predictPreferredCommunicationMode with department_uuid={} customer_uuid={} request={}", 
            departmentUUID, customerUUID, om.writeValueAsString(predictPreferredCommunicationModeRequest));
        response = validateRequest.validatePredictPreferredCommunicationModeRequest(departmentUUID, customerUUID, predictPreferredCommunicationModeRequest);
		if(response.getErrors() != null && !response.getErrors().isEmpty()) {
			return new ResponseEntity<PredictPreferredCommunicationModeResponse>(response, HttpStatus.BAD_REQUEST);
		}
		
		response = new PredictPreferredCommunicationModeResponse();
		Message message = helper.getMessageObject(predictPreferredCommunicationModeRequest.getMessageUUID());
		if(message == null) {
			return new ResponseEntity<PredictPreferredCommunicationModeResponse>(response, HttpStatus.BAD_REQUEST);
		}
        
        Boolean processMessageForPrediction = isMessageRelevantForPrediction(message);
        if(processMessageForPrediction) {
            PreferredCommunicationModePrediction preferredCommunicationModePrediction = new PreferredCommunicationModePrediction();
            preferredCommunicationModePrediction.setCustomerUUID(customerUUID);
            preferredCommunicationModePrediction.setDepartmentUUID(departmentUUID);
            preferredCommunicationModePrediction.setMessage(message);
            rabbitHelper.pushToPreferredCommunicationModePredictionQueue(preferredCommunicationModePrediction);
        }
        response.setIsProcessed(processMessageForPrediction);
		return new ResponseEntity<PredictPreferredCommunicationModeResponse>(response, HttpStatus.OK);
    }

    private Boolean isMessageRelevantForPrediction(Message message) {
        if(message != null &&
            message.getIsManual() != null && message.getIsManual() && 
            "1".equalsIgnoreCase(message.getDeliveryStatus()) &&
            Arrays.asList(MessageType.INCOMING, MessageType.OUTGOING).contains(MessageType.getMessageTypeForString(message.getMessageType())) && 
            !Arrays.asList(MessagePurpose.PR, MessagePurpose.F).contains(MessagePurpose.getMessagePurposeForString(message.getMessagePurpose())) && 
            ALLOWED_PROTOCOLS.contains(MessageProtocol.getMessageProtocolForString(message.getProtocol()))) {
            return true;
        }
        return false;
    }

    private Boolean isMessageRelevantForSavingFeedback(Message message) {
        if(isMessageRelevantForPrediction(message) && 
            MessageType.OUTGOING.getMessageType().equalsIgnoreCase(message.getMessageType())) {
            return true;
        }
        return false;
    }

    public void predictPreferredCommunicationMode(String departmentUUID, String customerUUID, Message message) throws Exception {
        CustomerPreferredCommunicationMode customerPreferredCommunicationMode = customerPreferredCommunicationModeRepository.findByCustomerUUID(customerUUID);
        CustomerPreferredCommunicationMode newCustomerPreferredCommunicationMode = createPreferredCommunicationModePrediction(customerPreferredCommunicationMode, message);
        if(customerPreferredCommunicationMode != null) {
            newCustomerPreferredCommunicationMode.setId(customerPreferredCommunicationMode.getId());
        }
        newCustomerPreferredCommunicationMode.setCustomerUUID(customerUUID);
        customerPreferredCommunicationModeRepository.saveAndFlush(newCustomerPreferredCommunicationMode);
        if(customerPreferredCommunicationMode == null || !newCustomerPreferredCommunicationMode.getProtocol().equalsIgnoreCase(customerPreferredCommunicationMode.getProtocol())) {
            sendPreferredCommunicationModeUpdateNotification(departmentUUID, customerUUID, message);
        }
        Boolean saveFeedback = isMessageRelevantForSavingFeedback(message);
        if(saveFeedback) {
            saveFeedback(departmentUUID, message, customerPreferredCommunicationMode);
        }
        savePrediction(departmentUUID, message, newCustomerPreferredCommunicationMode);
    }

    private void sendPreferredCommunicationModeUpdateNotification(String departmentUUID, String customerUUID, Message message) {
        try {
            Set<Long> daIDSet = new HashSet<Long>(Arrays.asList(message.getDealerAssociateID()));
            KNotificationMessage kNotificationMessage = kNotificationApiHelper.getKNotificationMessage(message.getId(), message.getCustomerID(), message.getDealerAssociateID(), message.getDealerID(), message.getDealerDepartmentId(), daIDSet, daIDSet, null, null, null, EventName.UPDATE_CUSTOMER_PREFERRED_COMMUNICATION_MODE.name(), true, null);
            kNotificationApiHelper.pushToPubnub(kNotificationMessage);
        } catch(Exception e) {
            LOGGER.error("Error in sendPreferredCommunicationModeUpdateNotification for department_uuid={} customer_uuid={} message_uuid={}", departmentUUID, customerUUID, message.getUuid(), e);
        }
    }

    private Long extractPredictionMessageIDFromMetaData(CustomerPreferredCommunicationMode customerPreferredCommunicationMode) throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonObject jsonObject;
        try {
            if(customerPreferredCommunicationMode != null && customerPreferredCommunicationMode.getMetaData() != null) {
                jsonObject = new JsonParser().parse(customerPreferredCommunicationMode.getMetaData()).getAsJsonObject();
                if(jsonObject.has(PREDICTION_MESSAGE_ID_ATTRIBUTE)) {
                    return Long.valueOf(jsonObject.getAsJsonPrimitive(PREDICTION_MESSAGE_ID_ATTRIBUTE).getAsLong());
                }
            }   
            return null;
        } catch(Exception e) {
            LOGGER.error("Error while extracting prediction message id from the preferred_communication_mode={}", om.writeValueAsString(customerPreferredCommunicationMode), e);
            return null;
        }
    }

    private void saveFeedback(String departmentUUID, Message message, CustomerPreferredCommunicationMode customerPreferredCommunicationMode) {
        ObjectMapper om = new ObjectMapper();
        try {
            Long feedbackMessageID = extractPredictionMessageIDFromMetaData(customerPreferredCommunicationMode);
            String actualProtocol = message.getProtocol();
            String predictedProtocol = customerPreferredCommunicationMode == null ? null : customerPreferredCommunicationMode.getProtocol();
            if(feedbackMessageID == null || actualProtocol == null || predictedProtocol == null) {
                LOGGER.warn("could not save feedback as at least one of required data was missing among feedback_message_id={} prediction={} actual={} for department_uuid={} message_uuid={} preferred_communication_mode={}", feedbackMessageID, actualProtocol, predictedProtocol, departmentUUID, message.getUuid(), om.writeValueAsString(customerPreferredCommunicationMode));
                return;
            }
            LOGGER.info("Saving preferred communication mode feedback for department_uuid={} message_uuid={} feedback_message_id={} prediction={} actual={}", departmentUUID, message.getUuid(), feedbackMessageID, actualProtocol, predictedProtocol);
            UpdateMessagePredictionFeedbackRequest request = new UpdateMessagePredictionFeedbackRequest();
            String userUUID = generalRepository.getUserUUIDForDealerAssociateID(message.getDealerAssociateID());
            Long predictionFeatureID = predictionFeatureRepository.findIdByPredictionFeature(PredictionFeature.PREFERRED_COMMUNICATION_MODE.getFeatureKey());
            Long messagePredictionID = messagePredictionRepository.getMessagePredictionIDForMessageIDAndPredictionFeatureID(feedbackMessageID, predictionFeatureID);
            String userFeedback = actualProtocol.equalsIgnoreCase(predictedProtocol) ? MessagePredictionFeedbackTypes.POSITIVE.getType() : MessagePredictionFeedbackTypes.NEGATIVE.getType();
            request.setMessagePredictionID(messagePredictionID);
            request.setUserFeedback(userFeedback);
            JsonObject jsonObject = new JsonObject();
            jsonObject.add(FEEDBACK_MESSAGE_PROTOCOL_ATTRIBUTE, new Gson().toJsonTree(actualProtocol).getAsJsonPrimitive());
            jsonObject.add(FEEDBACK_MESSAGE_ID_ATTRIBUTE, new Gson().toJsonTree(message.getId()).getAsJsonPrimitive());
            request.setReason(jsonObject.toString());
            ResponseEntity<Response> response =  communicationsApiImpl.updateMessagePredictionFeedback(departmentUUID, userUUID, request);
            if(response == null || response.getBody() == null || (response.getBody().getErrors() != null && !response.getBody().getErrors().isEmpty())) {
                LOGGER.error("Error while saving preferred communication mode prediction feedback for department_uuid={} message_uuid={} with response={}", departmentUUID, message.getUuid(), om.writeValueAsString(response));
            }
        } catch(Exception e) {
            LOGGER.error("Error while saving preferred communication mode prediction feedback for department_uuid={} message_uuid={}", departmentUUID, message.getUuid(), e);
        }    
    }

    private void savePrediction(String departmentUUID, Message message, CustomerPreferredCommunicationMode customerPreferredCommunicationMode) {
        ObjectMapper om = new ObjectMapper();
        try {
            LOGGER.info("Saving preferred communication mode prediction for department_uuid={} message_uuid={} prediction={}", departmentUUID, message.getUuid(), om.writeValueAsString(customerPreferredCommunicationMode));
            UpdateMessagePredictionRequest request = new UpdateMessagePredictionRequest();
            request.setMessageID(message.getId());
            request.setMetadata(customerPreferredCommunicationMode.getMetaData());
            request.setPrediction(customerPreferredCommunicationMode.getProtocol());
            request.setPredictionFeature(PredictionFeature.PREFERRED_COMMUNICATION_MODE.getFeatureKey());
            ResponseEntity<Response> response =  communicationsApiImpl.updateMessagePrediction(request, departmentUUID);
            if(response == null || response.getBody() == null || (response.getBody().getErrors() != null && !response.getBody().getErrors().isEmpty())) {
                LOGGER.error("Error while saving preferred communication mode prediction for department_uuid={} message_uuid={} with response={}", departmentUUID, message.getUuid(), om.writeValueAsString(response));
            }
        } catch(Exception e) {
            LOGGER.error("Error while saving preferred communication mode prediction for department_uuid={} message_uuid={} prediction={}", departmentUUID, message.getUuid(), e);
        }    
    }

    private CustomerPreferredCommunicationMode createPreferredCommunicationModePrediction(CustomerPreferredCommunicationMode customerPreferredCommunicationMode, Message message) throws Exception {
        CustomerPreferredCommunicationMode result = new CustomerPreferredCommunicationMode();
        ObjectMapper om = new ObjectMapper();
        List<String> messageProtocolSlidingWindow = new ArrayList<String>();
        List<String> newMessageProtocolSlidingWindow = new ArrayList<String>();
        JsonObject jsonObject;
        try {
            if(customerPreferredCommunicationMode != null && customerPreferredCommunicationMode.getMetaData() != null) {
                jsonObject = new JsonParser().parse(customerPreferredCommunicationMode.getMetaData()).getAsJsonObject();
                if(jsonObject.has(MESSAGE_PROTOCOL_SLIDING_WINDOW_ATTRIBUTE)) {
                    JsonArray jsonMessageProtocolSlidingWindow = jsonObject.getAsJsonArray(MESSAGE_PROTOCOL_SLIDING_WINDOW_ATTRIBUTE);
                    for(JsonElement element : jsonMessageProtocolSlidingWindow) {
                        messageProtocolSlidingWindow.add(element.getAsString());
                    }
                }
            }
        } catch(Exception e) {
            LOGGER.error("Error while extracting data from the customer_preferred_communication_mode={} for message_uuid={}. Now using default values", om.writeValueAsString(customerPreferredCommunicationMode), message.getUuid());
        }
        
        newMessageProtocolSlidingWindow.add(message.getProtocol());
        newMessageProtocolSlidingWindow.addAll(messageProtocolSlidingWindow);
        if(newMessageProtocolSlidingWindow.size() > MESSAGE_PROTOCOL_SLIDING_WINDOW_SIZE) {
            newMessageProtocolSlidingWindow = newMessageProtocolSlidingWindow.subList(0, MESSAGE_PROTOCOL_SLIDING_WINDOW_SIZE);
        }
        jsonObject = new JsonObject();
        jsonObject.add(MESSAGE_PROTOCOL_SLIDING_WINDOW_ATTRIBUTE, new Gson().toJsonTree(newMessageProtocolSlidingWindow).getAsJsonArray());
        jsonObject.add(PREDICTION_MESSAGE_ID_ATTRIBUTE, new Gson().toJsonTree(message.getId()).getAsJsonPrimitive());
        result.setMetaData(jsonObject.toString());


        Map<MessageProtocol, Integer> messageProtocolCounts = new HashMap<MessageProtocol, Integer>();
        for(MessageProtocol messageProtocol : ALLOWED_PROTOCOLS) {
            messageProtocolCounts.put(messageProtocol, 0);
        }
        Integer maxProtocolCount = 0;
        for(String messageProtocolStr : newMessageProtocolSlidingWindow) {
            MessageProtocol messageProtocol = MessageProtocol.getMessageProtocolForString(messageProtocolStr);
            if(messageProtocolCounts.containsKey(messageProtocol)) {
                Integer value = messageProtocolCounts.get(messageProtocol) + 1;
                messageProtocolCounts.put(messageProtocol, value);
                if(value > maxProtocolCount) {
                    maxProtocolCount = value;
                }
            }
        }
        Set<MessageProtocol> predictionCandidateMessageProtocols = new HashSet<MessageProtocol>();
        for(MessageProtocol messageProtocol : ALLOWED_PROTOCOLS) {
            if(messageProtocolCounts.get(messageProtocol) == maxProtocolCount) {
                predictionCandidateMessageProtocols.add(messageProtocol);
            }
        }
        for(String messageProtocolStr : newMessageProtocolSlidingWindow) {
            if(predictionCandidateMessageProtocols.contains(MessageProtocol.getMessageProtocolForString(messageProtocolStr))) {
                result.setProtocol(messageProtocolStr);
                break;
            }
        }
        return result;
    }

    
}
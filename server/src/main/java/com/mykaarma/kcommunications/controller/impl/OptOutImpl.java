package com.mykaarma.kcommunications.controller.impl;

import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.kcommunications.jpa.repository.CommunicationStatusHistoryRepository;
import com.mykaarma.kcommunications.jpa.repository.CommunicationStatusRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.api.CommunicationStatusRequest;
import com.mykaarma.kcommunications.model.jpa.CommunicationStatus;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.mongo.DoubleOptInDepartmentGroupConfiguration;
import com.mykaarma.kcommunications.model.rabbit.DoubleOptInDeployment;
import com.mykaarma.kcommunications.model.rabbit.OptOutStatusUpdate;
import com.mykaarma.kcommunications.model.rabbit.PostOptOutStatusUpdate;
import com.mykaarma.kcommunications.model.rabbit.PostOptOutStatusUpdate.PostOptOutStatusUpdateEvent;
import com.mykaarma.kcommunications.model.redis.DoubleOptInDeploymentStatus;
import com.mykaarma.kcommunications.model.redis.DoubleOptInDeploymentStatus.DepartmentDeploymentStatus;
import com.mykaarma.kcommunications.mongo.repository.DoubleOptInDepartmentGroupConfigurationRepository;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.redis.OptOutRedisService;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.KMessagingApiHelper;
import com.mykaarma.kcommunications.utils.OptOutStatusHelper;
import com.mykaarma.kcommunications.utils.OrakleApiHelper;
import com.mykaarma.kcommunications.utils.TemplateConstants;
import com.mykaarma.kcommunications_model.common.CommunicationAttributes;
import com.mykaarma.kcommunications_model.common.CommunicationOptOutStatusAttributes;
import com.mykaarma.kcommunications_model.common.OptOutStatusAttributes;
import com.mykaarma.kcommunications_model.enums.DeploymentEvent;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.Feature;
import com.mykaarma.kcommunications_model.enums.MessageKeyword;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.OptOutState;
import com.mykaarma.kcommunications_model.enums.OptOutStatusUpdateEvent;
import com.mykaarma.kcommunications_model.enums.UpdateOptOutStatusRequestType;
import com.mykaarma.kcommunications_model.request.CommunicationsOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.CustomersOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.DoubleOptInDeploymentRequest;
import com.mykaarma.kcommunications_model.request.PredictOptOutStatusCallbackRequest;
import com.mykaarma.kcommunications_model.request.UpdateOptOutStatusRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.OptOutResponse;
import com.mykaarma.kcommunications_model.response.OptOutStatusListResponse;
import com.mykaarma.kcommunications_model.response.OptOutStatusResponse;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kmanage.model.dto.json.DepartmentGroupExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.DepartmentMinimalDTO;
import com.mykaarma.kmanage.model.dto.json.FeatureDTO;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;

@Service
public class OptOutImpl {

	@Value("${kmessaging_api_url}")
	private String kmessagingBaseUrl;

	@Value("${kcommunications_api_url}")
    private String kCommunicationsBaseUrl;
	
	@Autowired
	private OrakleApiHelper orakleApiHelper;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OptOutImpl.class);

	@Autowired
	private ValidateRequest validateRequest;
	
	@Autowired
	private GeneralRepository generalRepository;

	@Autowired
	private Helper helper;

	@Autowired
	private KCommunicationsUtils kCommunicationsUtils;
	
    @Autowired
    private KManageApiHelper kManageApiHelper;

    @Autowired
    private KMessagingApiHelper kMessagingApiHelper;

    @Autowired
    private CommunicationStatusRepository communicationStatusRepository;

    @Autowired
    private OptOutStatusHelper optOutStatusHelper;

    @Autowired
    private RabbitHelper rabbitHelper;

    @Autowired
    private CommunicationStatusHistoryRepository communicationStatusHistoryRepository;

    @Autowired
    private DoubleOptInDepartmentGroupConfigurationRepository doubleOptInDepartmentGroupConfigurationRepository;

    @Autowired
    private OptOutRedisService optOutRedisService;

    @Autowired
    private SendMessageHelper sendMessageHelper;

	private static final Double DEFAULT_SCORE_WARNING = 0.62;
	private Double warningThresholdValue;
	private String dealerFooter;
    private static final String OPT_OUT_DEFAULT_PREDICTIONS_FILE = "https://files.mykaarma.com/opt_out_default_predictions.json"; 
    private static Map<MessageKeyword, Set<String>> defaultPredictions = null;

    private static final String INTEGRATION_PULL_IMPLIES_DIRECT_OPTIN_CONFIGURATION_KEY = "integration.pull.implies.directoptin";
    private static final String INTEGRATION_PULL_IMPLIES_SEND_OPTIN_REQUEST_CONFIGURATION_KEY = "integration.pull.implies.send.optinrequest";
    private static final String INTEGRATION_CREATION_IMPLIES_OPTIN_DURING_ROLLOUT_CONFIGURATION_KEY = "rollout.integration.creation.implies.directoptin";

    private static final Double OPT_OUT_V2_PREDICTION_DEFAULT_CRITICAL_THRESHOLD = 0.40;
    private static final Double OPT_OUT_V2_PREDICTION_DEFAULT_SOFT_THRESHOLD = 0.20;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public ResponseEntity<OptOutResponse> predictOptOutForMessage(String messageUUID, String departmentUUID) 
		throws Exception {
		
		OptOutResponse optOutResponse;
		
		optOutResponse = validateRequest.validateOptOutRequest(messageUUID, departmentUUID);
		if(optOutResponse.getErrors() != null && !optOutResponse.getErrors().isEmpty()) {
			return new ResponseEntity<OptOutResponse>(optOutResponse, HttpStatus.BAD_REQUEST);
		}
		
		optOutResponse = new OptOutResponse();
		Message message = helper.getMessageObject(messageUUID);
		if(message == null) {
		    ApiError apiError = new ApiError(ErrorCode.INVALID_MESSAGE_UUID.name(),
                String.format("invalid message_uuid=%s in request", messageUUID));
		    optOutResponse.setErrors(Arrays.asList(apiError));
			return new ResponseEntity<OptOutResponse>(optOutResponse, HttpStatus.BAD_REQUEST);
		}
        if(!(MessageProtocol.TEXT.getMessageProtocol().equalsIgnoreCase(message.getProtocol()) &&
            MessageType.INCOMING.getMessageType().equalsIgnoreCase(message.getMessageType()))) {
            ApiError apiError = new ApiError(ErrorCode.INVALID_MESSAGE.name(),
                String.format("message prediction not supported for message_type=%s message_protocol=%s of message_uuid=%s",
                    message.getProtocol(), message.getMessageType(), message.getUuid()));
            optOutResponse.setErrors(Arrays.asList(apiError));
            return new ResponseEntity<OptOutResponse>(optOutResponse, HttpStatus.BAD_REQUEST);
        }
        // Get values from dealer
		fetchDealerFooterAndWarningThreshold(messageUUID, departmentUUID, message.getDealerID());
		
        String messageBody = message.getMessageExtn().getMessageBody();
		String contactNumber = message.getFromNumber();
        MessageKeyword messageKeyword = getDefaultPrediction(messageUUID, messageBody, optOutResponse);
		Boolean doubleOptInEnabled = doubleOptInEnabled(message.getDealerID());
        if(messageKeyword != null) {
            LOGGER.info("in predictOptOutForMessage bypassing orakle_api with prediction={} for message_uuid={} message_body={} ", messageKeyword.name(), messageUUID, messageBody);
            if(doubleOptInEnabled) {
                PredictOptOutStatusCallbackRequest request = new PredictOptOutStatusCallbackRequest();
                request.setMessageKeyword(messageKeyword.name());
                predictOptOutStatusCallback(departmentUUID, message, request);
            } else {
                CommunicationStatusRequest communicationStatusRequest = new CommunicationStatusRequest();
                communicationStatusRequest.setMessageUuid(messageUUID);
                communicationStatusRequest.setMessageKeyword(messageKeyword);
                kMessagingApiHelper.updateCommunicationStatus(generalRepository.getDealerUUIDFromDealerId(message.getDealerID()), departmentUUID, APIConstants.TEXT, message.getFromNumber(), communicationStatusRequest);
            }
			optOutResponse.setHitOrakleApi(false);
			return new ResponseEntity<OptOutResponse>(optOutResponse, HttpStatus.OK);
		}
        String callbackUrl = doubleOptInEnabled ? String.format("%sdepartment/%s/message/%s/optoutstatus/predict/callback", kCommunicationsBaseUrl, departmentUUID, messageUUID)
            : String.format("%smessage/properties", kmessagingBaseUrl);
		LOGGER.info(String.format("calling orakle api for message_uuid=%s message_body=%s contact_number=%s", messageUUID, messageBody, contactNumber));
		Boolean result = orakleApiHelper.hitOrakleApi(messageUUID, messageBody, contactNumber, callbackUrl, warningThresholdValue, optOutV2Enabled(message.getDealerID()));
		optOutResponse.setMessageReactionDealerType(false);
		optOutResponse.setHitOrakleApi(result);
		return new ResponseEntity<OptOutResponse>(optOutResponse, HttpStatus.OK);
		
	}

    private MessageKeyword getDefaultPrediction(String messageUUID, String messageBody, OptOutResponse optOutResponse) {
        MessageKeyword messageKeyword = null;
        try {
            if(kCommunicationsUtils.isMessageReactionType(messageBody) && kCommunicationsUtils.isMessageDealerType(messageBody, dealerFooter)) {
                messageKeyword = MessageKeyword.GENERIC;
                optOutResponse.setMessageReactionDealerType(true);
            } else {
                String strippedMessageBody = kCommunicationsUtils.lowerCaseAndStripReactionAndPunctuations(messageBody);
                if(defaultPredictions == null) {
                    fillOptOutDefaultPredictions();
                }
                for(MessageKeyword predictionCandidate : defaultPredictions.keySet()) {
                    if(defaultPredictions.get(predictionCandidate).contains(strippedMessageBody)) {
                        messageKeyword = predictionCandidate;
                        break;
                    }
                }
            }
        } catch(Exception e) {
            LOGGER.error("error while fetching default prediction for message_uuid={} message_body={}", messageUUID, messageBody, e);
        }
        return messageKeyword;
    }

	private void fetchDealerFooterAndWarningThreshold(String messageUUID, String departmentUUID, Long dealerID) {
        String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
        dealerFooter = null;
        warningThresholdValue = DEFAULT_SCORE_WARNING;
        String optionValue;
        try {
            optionValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, 
                DealerSetupOption.COMMUNICATIONS_OPT_OUT_FOOTER_TEXT.getOptionKey());
            if(optionValue == null) {
                LOGGER.info(String.format("No dealer opt out footer set, will have to use default for message_uuid=%s department_uuid=%s", messageUUID, departmentUUID));
            } else {
                dealerFooter = optionValue;
            }
        } catch(Exception e) {
            LOGGER.error(String.format("Error in fetching dso=%s for message_uuid=%s department_uuid=%s", 
                DealerSetupOption.COMMUNICATIONS_OPT_OUT_FOOTER_TEXT.getOptionKey(), messageUUID, departmentUUID), e);
        }
        try {
            optionValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, 
                DealerSetupOption.MESSAGING_INBOUND_TEXT_OPTOUT_CRITICAL_THRESHOLD.getOptionKey());
            if(optionValue == null) {
                LOGGER.info(String.format("No warningThreshold set, using warningThreshold=%s as default for message_uuid=%s and department_uuid=%s", DEFAULT_SCORE_WARNING.toString(), messageUUID, departmentUUID));
            } else {
                warningThresholdValue = Double.parseDouble(optionValue);
            }
        } catch(Exception e) {
            LOGGER.error(String.format("Error in fetching dso=%s for message_uuid=%s department_uuid=%s", 
                DealerSetupOption.MESSAGING_INBOUND_TEXT_OPTOUT_CRITICAL_THRESHOLD.getOptionKey(), messageUUID, departmentUUID), e);
        }		
    }

    private void fillOptOutDefaultPredictions() {
        try 
		{
            defaultPredictions = new EnumMap<MessageKeyword, Set<String>>(MessageKeyword.class);
            URL url = new URL(OPT_OUT_DEFAULT_PREDICTIONS_FILE);
			String jsonStr = IOUtils.toString(url.openStream(), "UTF-8").toString();
            LOGGER.info("in fillOptOutDefaultPredictions trying to extract defaultPredictions from opt_out_default_predictions_json={}", jsonStr);
			JSONObject jsonObject = new JSONObject(jsonStr);
			for(String messageKeywordStr : jsonObject.keySet()) {
                MessageKeyword messageKeyword = MessageKeyword.fromString(messageKeywordStr);
                if(messageKeyword == null) {
                    continue;
                }
                JSONArray jsonArray = jsonObject.getJSONArray(messageKeywordStr);
                TreeSet<String> predictionMessages = new TreeSet<String>();
                for(int i = 0; i < jsonArray.length(); i++) {
                    String predictionMessage = jsonArray.getString(i);
                    if(predictionMessage != null) {
                        predictionMessages.add(predictionMessage); 
                    }
                }
                LOGGER.info("in fillOptOutDefaultPredictions with prediction={} and messages={}", messageKeyword.name(), predictionMessages);
                if(!predictionMessages.isEmpty()) {
                    defaultPredictions.put(messageKeyword, predictionMessages);
                }
            }
            if(defaultPredictions.isEmpty()) {
                throw new Exception("could not extract predictions from json");
            }
		}
		catch(Exception e) {
            LOGGER.warn("exception in fillOptOutDefaultPredictions filling static default predictions", e);
            defaultPredictions = new EnumMap<MessageKeyword, Set<String>>(MessageKeyword.class);
            defaultPredictions.put(MessageKeyword.GENERIC, new TreeSet<String>(
                Arrays.asList("wrong", "wrong address", "wrong email", "please text me", "please call me", "text me", "call me", "off", "quite", "wrong estimate"))
            );
		}
        LOGGER.info("in fillOptOutDefaultPredictions with default_predictions={}", defaultPredictions);
	}

    private Boolean optOutV2Enabled(Long dealerID) {
        String optionValue = null;
        try {
            String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
            optionValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, 
                DealerSetupOption.MESSAGING_OPTOUT_AI_V2_ENABLED.getOptionKey());    
        } catch(Exception e) {
            LOGGER.error(String.format("Error in fetching dso=%s for dealer_id=%s ", 
                DealerSetupOption.MESSAGING_OPTOUT_AI_V2_ENABLED.getOptionKey(), dealerID), e);
        }
        return Boolean.TRUE.toString().equalsIgnoreCase(optionValue);
    }

    private boolean doubleOptInEnabled(Long dealerID) {
        String optionValue = null;
        try {
            String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
            optionValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, 
                DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_ENABLE.getOptionKey());    
        } catch(Exception e) {
            LOGGER.error(String.format("Error in fetching dso=%s for dealer_id=%s ", 
                DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_ENABLE.getOptionKey(), dealerID), e);
        }
        return Boolean.TRUE.toString().equalsIgnoreCase(optionValue);
    }

    public ResponseEntity<OptOutStatusResponse> getOptOutStatus(String departmentUUID, String communicationType,
            String communicationValue) throws Exception {
        
        LOGGER.info("in getOptOutStatus received request for department_uuid={} communication_type={} communication_value={}", departmentUUID, communicationType, communicationValue);
        OptOutStatusResponse response = validateRequest.validateGetOptOutStatusRequest(departmentUUID, communicationType, communicationValue);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<OptOutStatusResponse>(response, HttpStatus.BAD_REQUEST);
        }

        MessageProtocol messageProtocol = MessageProtocol.fromString(communicationType);
        Long departmentID = null;
        try {
			departmentID = generalRepository.getDepartmentIDForUUID(departmentUUID);
		}  catch(Exception e) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_DEALER_DEPARTMENT_ID.name(), String.format("department_id not found for department_uuid=%s",departmentUUID));
            response.setErrors(Arrays.asList(apiError));
            return new ResponseEntity<OptOutStatusResponse>(response, HttpStatus.BAD_REQUEST);
		}
        
        Long dealerID = generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
        Boolean doubleOptInEnabled = doubleOptInEnabled(dealerID);
        CommunicationStatus communicationStatus = optOutStatusHelper.getOptOutStatusFromDB(dealerID, departmentID, messageProtocol.name(), communicationValue, doubleOptInEnabled);
        findAndPushUnavailableCommunicationStatusToOptOutStatusUpdateQueue(dealerID, departmentID, doubleOptInEnabled, Collections.singletonList(communicationStatus));
        OptOutStatusAttributes optOutStatus = new OptOutStatusAttributes();
        optOutStatus.setOptOutState(communicationStatus.getOptOutState());
        optOutStatus.setCanSendOptinRequest(communicationStatus.getCanSendOptinRequest());
        response.setOptOutStatus(optOutStatus);
        return new ResponseEntity<OptOutStatusResponse>(response, HttpStatus.OK);
    }

    public ResponseEntity<OptOutStatusListResponse> getCommunicationsOptOutStatusList(String departmentUUID,
            CommunicationsOptOutStatusListRequest request) throws Exception {

	    LOGGER.info("in getCommunicationsOptOutStatusList received request for department_uuid={} request={}", departmentUUID, OBJECT_MAPPER.writeValueAsString(request));
        OptOutStatusListResponse response = validateRequest.validateCommunicationsOptOutStatusListRequest(departmentUUID, request);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<OptOutStatusListResponse>(response, HttpStatus.BAD_REQUEST);
        }
        Long departmentID = null;
        try {
            departmentID = generalRepository.getDepartmentIDForUUID(departmentUUID);
        }  catch(Exception e) {
            ApiError apiError = new ApiError(ErrorCode.INVALID_DEALER_DEPARTMENT_ID.name(), String.format("department_id not found for department_uuid=%s",departmentUUID));
            response.setErrors(Arrays.asList(apiError));
            return new ResponseEntity<OptOutStatusListResponse>(response, HttpStatus.BAD_REQUEST);
        }
        
        Long dealerID = generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
        Boolean doubleOptInEnabled = doubleOptInEnabled(dealerID);
        List<CommunicationStatus> communicationStatusList = optOutStatusHelper.getOptOutStatusListFromDB(dealerID, departmentID, request.getCommunicationList(), doubleOptInEnabled);
        List<CommunicationOptOutStatusAttributes> optOutStatusList = buildOptOutStatusListFromCommunicationStatusList(communicationStatusList);
        findAndPushUnavailableCommunicationStatusToOptOutStatusUpdateQueue(dealerID, departmentID, doubleOptInEnabled, communicationStatusList);
        response.setOptOutStatusList(optOutStatusList);
        return new ResponseEntity<OptOutStatusListResponse>(response, HttpStatus.OK);
    }

    public ResponseEntity<OptOutStatusListResponse> getCustomersOptOutStatusList(String departmentUUID,
            CustomersOptOutStatusListRequest request) throws Exception {

	    LOGGER.info("in getCustomersOptOutStatusList received request for department_uuid={} request={}", departmentUUID, OBJECT_MAPPER.writeValueAsString(request));
        OptOutStatusListResponse response = validateRequest.validateCustomersOptOutStatusListRequest(departmentUUID, request);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<OptOutStatusListResponse>(response, HttpStatus.BAD_REQUEST);
        }
        Long departmentID = null;
        try {
            departmentID = generalRepository.getDepartmentIDForUUID(departmentUUID);
        }  catch(Exception e) {
            ApiError apiError = new ApiError(ErrorCode.INVALID_DEALER_DEPARTMENT_ID.name(), String.format("department_id not found for department_uuid=%s",departmentUUID));
            response.setErrors(Arrays.asList(apiError));
            return new ResponseEntity<OptOutStatusListResponse>(response, HttpStatus.BAD_REQUEST);
        }
        
        Long dealerID = generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
        Boolean doubleOptInEnabled = doubleOptInEnabled(dealerID);

        List<Object[]> customerCommunications = generalRepository.getCustomerCommunicationAttributesForUUIDs(request.getCustomerUUIDList());
        if(customerCommunications != null && !customerCommunications.isEmpty()) {

            List<CommunicationAttributes> communicationAttributesList = customerCommunications.stream().map(customerCommunication -> {
                CommunicationAttributes communicationAttributes =  new CommunicationAttributes();
                communicationAttributes.setCommunicationValue((String) customerCommunication[1]);
                communicationAttributes.setCommunicationType(APIConstants.PHONE_COMMUNICATION_TYPE_ID.equalsIgnoreCase((String) customerCommunication[0])
                    ? MessageProtocol.TEXT.name() : MessageProtocol.EMAIL.name());
                return communicationAttributes;
            }).collect(Collectors.toList());

            List<CommunicationStatus> communicationStatusList = optOutStatusHelper.getOptOutStatusListFromDB(dealerID, departmentID, communicationAttributesList, doubleOptInEnabled);
            List<CommunicationOptOutStatusAttributes> optOutStatusList = buildOptOutStatusListFromCommunicationStatusList(communicationStatusList);
            findAndPushUnavailableCommunicationStatusToOptOutStatusUpdateQueue(dealerID, departmentID, doubleOptInEnabled, communicationStatusList);
            response.setOptOutStatusList(optOutStatusList);
            
        }

        return new ResponseEntity<OptOutStatusListResponse>(response, HttpStatus.OK);
    }

    public ResponseEntity<Response> updateOptOutStatus(String departmentUUID, UpdateOptOutStatusRequest request, String serviceSubscriberName) throws Exception {

        LOGGER.info("in updateOptOutStatus received request for department_uuid={} request={}", departmentUUID, OBJECT_MAPPER.writeValueAsString(request));
        Response response = validateRequest.validateUpdateOptOutStatusRequest(departmentUUID, serviceSubscriberName, request);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
        }
        Long dealerID;
        Long departmentID;
        Long dealerAssociateID;
        Long customerID;
        Long messageID = null;
        List<CommunicationAttributes> communicationAttributesList;
        String apiCallSource = request.getApiCallSource() == null || request.getApiCallSource().isEmpty() 
            ? APIConstants.MYKAARMA : request.getApiCallSource();
        if(UpdateOptOutStatusRequestType.MESSAGE == request.getUpdateType()) {
            Message message = helper.getMessageObject(request.getMessageUUID());
            if(message == null) {
                ApiError apiError = new ApiError(ErrorCode.INVALID_MESSAGE_UUID.name(), String.format("message_id not found for message_uuid=%s", request.getMessageUUID()));
                response.setErrors(Arrays.asList(apiError));
                return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
            }
            departmentID = message.getDealerDepartmentId();
            dealerAssociateID = message.getDealerAssociateID();
            dealerID = message.getDealerID();
            customerID = message.getCustomerID();
            messageID = message.getId();
            CommunicationAttributes communicationAttributes = new CommunicationAttributes();
            communicationAttributes.setCommunicationValue(message.getFromNumber());
            if(MessageProtocol.TEXT.getMessageProtocol().equalsIgnoreCase(message.getProtocol())) {
                communicationAttributes.setCommunicationType(MessageProtocol.TEXT.name());
            } else {
                ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.name(), String.format("unsupported message_protocol=%s", message.getProtocol()));
                response.setErrors(Arrays.asList(apiError));
                return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
            }
            communicationAttributesList = Arrays.asList(communicationAttributes);
        }
        else {
            try {
                departmentID = generalRepository.getDepartmentIDForUUID(departmentUUID);
            }  catch(Exception e) {
                ApiError apiError = new ApiError(ErrorCode.INVALID_DEALER_DEPARTMENT_ID.name(), String.format("department_id not found for department_uuid=%s", departmentUUID));
                response.setErrors(Arrays.asList(apiError));
                return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
            }
            if(request.getUserUUID() == null || request.getUserUUID().isEmpty()) {
                dealerAssociateID = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID).getDealerAssociate().getId();
            } else {
                dealerAssociateID = kManageApiHelper.getDealerAssociate(departmentUUID, request.getUserUUID()).getDealerAssociate().getId();
            }
            dealerID = generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
            customerID = generalRepository.getCustomerIDForUUID(request.getCustomerUUID());
            communicationAttributesList = request.getCommunicationAttributesList();
        }
        
        Boolean doubleOptInEnabled = doubleOptInEnabled(dealerID);
        OptOutStatusUpdate optoutStatusUpdate = new OptOutStatusUpdate();
        optoutStatusUpdate.setApiCallSource(apiCallSource);
        optoutStatusUpdate.setCustomerID(customerID);
        optoutStatusUpdate.setDealerAssociateID(dealerAssociateID);
        optoutStatusUpdate.setDealerDepartmentID(departmentID);
        optoutStatusUpdate.setDealerID(dealerID);
        optoutStatusUpdate.setDoubleOptInEnabled(doubleOptInEnabled);
        optoutStatusUpdate.setEvent(request.getEvent());
        optoutStatusUpdate.setMessageID(messageID);
        optoutStatusUpdate.setUpdateType(request.getUpdateType());
        for(CommunicationAttributes communicationAttributes : communicationAttributesList) {
            MessageProtocol messageProtocol = MessageProtocol.fromString(communicationAttributes.getCommunicationType());
            optoutStatusUpdate.setMessageProtocol(messageProtocol);
            optoutStatusUpdate.setCommunicationValue(communicationAttributes.getCommunicationValue());
            optoutStatusUpdate.setOkToCommunicate(communicationAttributes.getOkToCommunicate());
            rabbitHelper.pushToOptOutStatusUpdateQueue(optoutStatusUpdate);
        }
        return new ResponseEntity<Response>(response, HttpStatus.OK);

    }

    public void updateOptOutStatus(OptOutStatusUpdate optOutStatusUpdate) throws Exception {
        LOGGER.info("in updateOptOutStatus for request={}", OBJECT_MAPPER.writeValueAsString(optOutStatusUpdate));
        String apiCallSource = optOutStatusUpdate.getApiCallSource();
        Long departmentID = optOutStatusUpdate.getDealerDepartmentID();
        Long dealerID = optOutStatusUpdate.getDealerID();
        String communicationValue = optOutStatusUpdate.getCommunicationValue();
        MessageProtocol messageProtocol = optOutStatusUpdate.getMessageProtocol();
        Boolean doubleOptInEnabled = optOutStatusUpdate.getDoubleOptInEnabled();
        OptOutStatusUpdateEvent event = optOutStatusUpdate.getEvent();
        UpdateOptOutStatusRequestType updateType = optOutStatusUpdate.getUpdateType();

        if(doubleOptInEnabled == null) {
            doubleOptInEnabled = doubleOptInEnabled(dealerID);
            optOutStatusUpdate.setDoubleOptInEnabled(doubleOptInEnabled);
        }
        CommunicationStatus communicationStatus = optOutStatusHelper.getOptOutStatusFromDB(dealerID, departmentID, messageProtocol.name(), communicationValue, doubleOptInEnabled);
        
        if(!validateRequest.applyOptOutStatusUpdateRules(communicationStatus, event, doubleOptInEnabled) || !MessageProtocol.TEXT.equals(messageProtocol)) {
            LOGGER.warn("in updateOptOutStatus discarding invalid_request={} for communication_status={} double_optin_enabled={} message_protocol={}", 
                OBJECT_MAPPER.writeValueAsString(optOutStatusUpdate), OBJECT_MAPPER.writeValueAsString(communicationStatus), doubleOptInEnabled, messageProtocol.name());
            return;
        }
        
        String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(departmentID);
        DepartmentGroupExtendedDTO requestedDepartmentGroup = kManageApiHelper.getDepartmentGroupByFeature(
            departmentUUID, Feature.OPT_IN_ADVANCED.getKey());

        // Fill Common Template Parameters
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put(APIConstants.API_CALL_SOURCE, apiCallSource);
        templateParams.put(APIConstants.COMMUNICATION_VALUE, communicationValue);
        templateParams.put(APIConstants.COMMUNICATION_TYPE, messageProtocol.name());

        if(UpdateOptOutStatusRequestType.CUSTOMER == updateType) {
            handleCustomerOptOutStatusUpdateEvent(optOutStatusUpdate, requestedDepartmentGroup, communicationStatus, templateParams);
        } else if(UpdateOptOutStatusRequestType.MESSAGE == updateType) {
            handleMessageOptOutStatusUpdateEvent(optOutStatusUpdate, requestedDepartmentGroup, communicationStatus, templateParams);
        } else if(UpdateOptOutStatusRequestType.DEPLOYMENT == updateType) {
            handleDeploymentOptOutStatusUpdateEvent(optOutStatusUpdate, requestedDepartmentGroup, communicationStatus, templateParams);
        }
    }

    private void handleCustomerOptOutStatusUpdateEvent(OptOutStatusUpdate optOutStatusUpdate, DepartmentGroupExtendedDTO requestedDepartmentGroup, CommunicationStatus communicationStatus, Map<String, Object> templateParams) throws Exception {
        LOGGER.info("in handleCustomerOptOutStatusUpdateEvent for request={} department_group={} communication_status={}", OBJECT_MAPPER.writeValueAsString(optOutStatusUpdate), OBJECT_MAPPER.writeValueAsString(requestedDepartmentGroup), communicationStatus);
        String apiCallSource = optOutStatusUpdate.getApiCallSource();
        Long customerID = optOutStatusUpdate.getCustomerID();
        Long departmentID = optOutStatusUpdate.getDealerDepartmentID();
        Long dealerID = optOutStatusUpdate.getDealerID();
        Long dealerAssociateID = optOutStatusUpdate.getDealerAssociateID();
        String communicationValue = optOutStatusUpdate.getCommunicationValue();
        MessageProtocol messageProtocol = optOutStatusUpdate.getMessageProtocol();
        OptOutStatusUpdateEvent event = optOutStatusUpdate.getEvent();
        UpdateOptOutStatusRequestType updateType = optOutStatusUpdate.getUpdateType();
        OptOutState currentStateRequestedDepartmentGroup = OptOutState.fromString(communicationStatus.getOptOutState());
        OptOutState newStateRequestedDepartmentGroup = null;
        Boolean canSendOptinRequestRequestedDepartmentGroup;
        List<Long> requestedDepartmentGroupDepartments = Arrays.asList(departmentID);
        if(requestedDepartmentGroup != null) {
            requestedDepartmentGroupDepartments = requestedDepartmentGroup.getDepartmentMinimalDTOList()
                    .stream().map(DepartmentMinimalDTO::getId)
                    .collect(Collectors.toList());
        }

        Map<Long, String> allDepartmentIDNameMap = new HashMap<>();
        List<Long> otherDepartments = new ArrayList<>();
        List<String> requestedDepartmentGroupDepartmentNames = new ArrayList<>();
        List<String> otherDepartmentNames = new ArrayList<>();
        List<String> allDepartmentNames = new ArrayList<>();

        // Fill Department Names.
        allDepartmentIDNameMap = generalRepository.getAllDepartmentIDAndNameForDealerId(dealerID)
                .stream().collect(Collectors.toMap(department -> ((BigInteger) department[0]).longValue(), department -> (String) department[1]));
        otherDepartments = new ArrayList<>(allDepartmentIDNameMap.keySet());
        otherDepartments.removeAll(requestedDepartmentGroupDepartments);
        for(Entry<Long, String> e : allDepartmentIDNameMap.entrySet()) {
            if(requestedDepartmentGroupDepartments.contains(e.getKey())) {
                requestedDepartmentGroupDepartmentNames.add(e.getValue());
            } else {
                otherDepartmentNames.add(e.getValue());
            }
            allDepartmentNames.add(e.getValue());
        }
        // Fill DealerAssociate Name
        templateParams.put(APIConstants.DEALER_ASSOCIATE_NAME, generalRepository.getDealerAssociateName(dealerAssociateID));

        switch (event) {
            case COMMUNICATION_STATUS_NOT_FOUND:

                newStateRequestedDepartmentGroup = OptOutState.OPTED_OUT;
                canSendOptinRequestRequestedDepartmentGroup = true;
                if (requestedDepartmentGroup != null) {
                    List<CommunicationStatus> communicationStatusList = communicationStatusRepository.
                            findAllByDealerIDAndDealerDepartmentIDListAndMessageProtocolAndCommunicationValue(dealerID, requestedDepartmentGroupDepartments, MessageProtocol.TEXT.name(), communicationValue);
                    if (communicationStatusList != null && !communicationStatusList.isEmpty()) {
                        LOGGER.info("in handleCustomerOptOutStatusUpdateEvent found existing_communication_status_list={} for department_id={} communication_value={} message_protocol={} event={}",
                                OBJECT_MAPPER.writeValueAsString(communicationStatusList), departmentID, communicationValue, messageProtocol.name(), event.name());
                        // * Check if any department is OPTED_OUT_CONSENT_SENT OR OPTED_OUT_CONSENT_NOT_SENT
                        // * If all departments are OPTED_IN this department will also get OPTED_IN
                        Boolean optedOutConsentSentPresent = false;
                        Boolean optedOutConsentNotSentPresent = false;
                        for (CommunicationStatus cs : communicationStatusList) {
                            if (OptOutState.OPTED_OUT.equals(OptOutState.fromString(cs.getOptOutState()))) {
                                if (cs.getCanSendOptinRequest()) {
                                    optedOutConsentNotSentPresent = true;
                                } else {
                                    optedOutConsentSentPresent = true;
                                }
                            }
                        }
                        if (optedOutConsentSentPresent) {
                            newStateRequestedDepartmentGroup = OptOutState.OPTED_OUT;
                            canSendOptinRequestRequestedDepartmentGroup = false;
                        } else if (optedOutConsentNotSentPresent) {
                            newStateRequestedDepartmentGroup = OptOutState.OPTED_OUT;
                            canSendOptinRequestRequestedDepartmentGroup = true;
                        } else {
                            newStateRequestedDepartmentGroup = OptOutState.OPTED_IN;
                            canSendOptinRequestRequestedDepartmentGroup = false;
                        }
                    }
                }

                // Update Opt Out Status
                communicationStatusRepository.upsertCommunicationStatusForDepartments(requestedDepartmentGroupDepartments, messageProtocol.name(), communicationValue, newStateRequestedDepartmentGroup.name(), canSendOptinRequestRequestedDepartmentGroup);
                // Send KNotification Message
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup, null, null, PostOptOutStatusUpdateEvent.SEND_OPTOUT_STATUS_UPDATE_KNOTIFICATION_MESSAGE);
                break;
            case COMMUNICATION_VALUE_CREATION:
                newStateRequestedDepartmentGroup = OptOutState.OPTED_OUT;
                canSendOptinRequestRequestedDepartmentGroup = true;
                if (APIConstants.INTEGRATION.equalsIgnoreCase(apiCallSource)) {
                    Boolean okToCommunicate = Boolean.TRUE == optOutStatusUpdate.getOkToCommunicate();
                    LOGGER.info("in handleCustomerOptOutStatusUpdateEvent communication_value={} with ok_to_communicate={} created by trusted_api_call_source={} for department_id={}", communicationValue, okToCommunicate, apiCallSource, departmentID);
                    if (!okToCommunicate) {
                        LOGGER.info("in handleCustomerOptOutStatusUpdateEvent communication_value={} is not ok to communicate department_id={}. Not checking for direct opt-in or automatic opt-in request send", communicationValue, departmentID);
                    } else if (requestedDepartmentGroup == null) {
                        LOGGER.warn("in handleCustomerOptOutStatusUpdateEvent department_id={} does not have a department group set up. Not checking for direct opt-in or automatic opt-in request send for communication_value={}", departmentID, communicationValue);
                    } else {
                        // Check for Direct Optin
                        FeatureDTO featureDTO = kManageApiHelper.getFeatureByKey(Feature.OPT_IN_ADVANCED.getKey());
                        DoubleOptInDepartmentGroupConfiguration isDirectOptInAllowed = doubleOptInDepartmentGroupConfigurationRepository
                                .findFirstByFeatureUUIDAndDepartmentGroupUUIDAndKey(featureDTO.getUuid(), requestedDepartmentGroup.getUuid(),
                                        INTEGRATION_PULL_IMPLIES_DIRECT_OPTIN_CONFIGURATION_KEY);
                        if (isDirectOptInAllowed != null && Boolean.TRUE.toString().equalsIgnoreCase(isDirectOptInAllowed.getValue())) {
                            LOGGER.info("in handleCustomerOptOutStatusUpdateEvent communication_value={} is approved for direct opt-in department_id={}", communicationValue, departmentID);
                            newStateRequestedDepartmentGroup = OptOutState.OPTED_IN;
                            canSendOptinRequestRequestedDepartmentGroup = false;
                        } else {
                            // Check if we can automatically Send Optin request
                            DoubleOptInDepartmentGroupConfiguration isAutomaticOptinRequestAllowed = doubleOptInDepartmentGroupConfigurationRepository
                                    .findFirstByFeatureUUIDAndDepartmentGroupUUIDAndKey(featureDTO.getUuid(), requestedDepartmentGroup.getUuid(),
                                            INTEGRATION_PULL_IMPLIES_SEND_OPTIN_REQUEST_CONFIGURATION_KEY);
                            if (isAutomaticOptinRequestAllowed != null && Boolean.TRUE.toString().equalsIgnoreCase(isAutomaticOptinRequestAllowed.getValue())) {
                                LOGGER.info("in handleCustomerOptOutStatusUpdateEvent communication_value={} will be sent an automatic opt-in consent request for department_id={}", communicationValue, departmentID);
                                canSendOptinRequestRequestedDepartmentGroup = false;
                            }
                        }

                    }
                }

                // Update Opt Out Status
                communicationStatusRepository.upsertCommunicationStatusForDepartments(requestedDepartmentGroupDepartments, messageProtocol.name(), communicationValue, newStateRequestedDepartmentGroup.name(), canSendOptinRequestRequestedDepartmentGroup);
                communicationStatusRepository.upsertCommunicationStatusForDepartments(otherDepartments, messageProtocol.name(), communicationValue, OptOutState.OPTED_OUT.name(), true);

                // Send KNotification Message
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup, null, null, PostOptOutStatusUpdateEvent.SEND_OPTOUT_STATUS_UPDATE_KNOTIFICATION_MESSAGE);

                // Create System Notification
                String template = null;
                if (newStateRequestedDepartmentGroup == OptOutState.OPTED_IN) {
                    template = TemplateConstants.NEW_COMMUNICATION_VALUE_DIRECT_OPTIN_SYSTEM_NOTE_TEMPLATE;
                    templateParams.put(APIConstants.OPTED_IN_DEPARTMENTS, requestedDepartmentGroupDepartmentNames);
                    templateParams.put(APIConstants.OPTED_OUT_DEPARTMENTS, otherDepartmentNames);
                } else if (!canSendOptinRequestRequestedDepartmentGroup) {
                    template = TemplateConstants.NEW_COMMUNICATION_VALUE_OPTIN_CONSENT_SENT_SYSTEM_NOTE_TEMPLATE;
                    templateParams.put(APIConstants.OPTED_OUT_DEPARTMENTS, allDepartmentNames);
                } else {
                    template = TemplateConstants.NEW_COMMUNICATION_VALUE_SYSTEM_NOTE_TEMPLATE;
                    templateParams.put(APIConstants.OPTED_OUT_DEPARTMENTS, allDepartmentNames);
                }

                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup,
                        template, templateParams, PostOptOutStatusUpdateEvent.SEND_SYSTEM_NOTIFICATION);

                // Send Optin Request
                if (OptOutState.OPTED_OUT == newStateRequestedDepartmentGroup && !canSendOptinRequestRequestedDepartmentGroup) {
                    fillTemplateParamsForOptinConsentRequestTemplate(templateParams, customerID, departmentID, dealerID);
                    sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup,
                            TemplateConstants.USER_REQUESTED_SEND_OPTIN_REQUEST_AUTORESPONDER_TEMPLATE, templateParams, PostOptOutStatusUpdateEvent.SEND_AUTORESPONDER);
                }
                break;
            case USER_REQUESTED_SEND_OPTIN_REQUEST:
                fillTemplateParamsForOptinConsentRequestTemplate(templateParams, customerID, departmentID, dealerID);
                newStateRequestedDepartmentGroup = OptOutState.OPTED_OUT;
                canSendOptinRequestRequestedDepartmentGroup = false;
                communicationStatusRepository.upsertCommunicationStatusForDepartments(requestedDepartmentGroupDepartments, messageProtocol.name(), communicationValue, newStateRequestedDepartmentGroup.name(), canSendOptinRequestRequestedDepartmentGroup);
                // Send KNotification Message
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, null, null, null, null, PostOptOutStatusUpdateEvent.SEND_OPTOUT_STATUS_UPDATE_KNOTIFICATION_MESSAGE);
                // Send Optin Request
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup,
                        TemplateConstants.USER_REQUESTED_SEND_OPTIN_REQUEST_AUTORESPONDER_TEMPLATE, templateParams, PostOptOutStatusUpdateEvent.SEND_AUTORESPONDER);
                // Create System Notification
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup,
                        TemplateConstants.USER_REQUESTED_SEND_OPTIN_REQUEST_SYSTEM_NOTE_TEMPLATE, templateParams, PostOptOutStatusUpdateEvent.SEND_SYSTEM_NOTIFICATION);
                break;
            case USER_REQUESTED_OPT_OUT:
                newStateRequestedDepartmentGroup = OptOutState.OPTED_OUT;
                canSendOptinRequestRequestedDepartmentGroup = false;
                communicationStatusRepository.upsertCommunicationStatusForDepartments(requestedDepartmentGroupDepartments, messageProtocol.name(), communicationValue, newStateRequestedDepartmentGroup.name(), canSendOptinRequestRequestedDepartmentGroup);
                // Send KNotification Message
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup, null, null, PostOptOutStatusUpdateEvent.SEND_OPTOUT_STATUS_UPDATE_KNOTIFICATION_MESSAGE);
                // Send Manual OptOut Confirmation
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup,
                        TemplateConstants.USER_REQUESTED_OPTOUT_AUTORESPONDER_TEMPLATE, templateParams, PostOptOutStatusUpdateEvent.SEND_AUTORESPONDER);
                // Create System Notification
                templateParams.put(APIConstants.OPTED_OUT_DEPARTMENTS, requestedDepartmentGroupDepartmentNames);
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup,
                        TemplateConstants.USER_REQUESTED_OPTOUT_SYSTEM_NOTE_TEMPLATE, templateParams, PostOptOutStatusUpdateEvent.SEND_SYSTEM_NOTIFICATION);
                break;
            default:
                LOGGER.warn("in handleCustomerOptOutStatusUpdateEvent unsupported_event={} for optoutstatus_update_type={}. Discarding request for department_id={} customer_id={}",
                        event.name(), updateType.name(), departmentID, customerID);
                break;
        }
        LOGGER.info("in handleCustomerOptOutStatusUpdateEvent event={} completed with new_opt_out_state={} for departments={} communication_value={}",
            event.name(), newStateRequestedDepartmentGroup, requestedDepartmentGroupDepartments, communicationValue);
    }

    private void fillTemplateParamsForOptinConsentRequestTemplate(Map<String, Object> templateParams, Long customerID, Long departmentID, Long dealerID) {
        templateParams.put(APIConstants.CUSTOMER_NAME, generalRepository.getCustomerNameFromId(customerID));
        templateParams.put(APIConstants.DEPARTMENT_NAME, generalRepository.getDepartmentNameFromId(departmentID));
        templateParams.put(APIConstants.DEALER_NAME, generalRepository.getDealerNameFromId(dealerID));
    }

    private void handleMessageOptOutStatusUpdateEvent(OptOutStatusUpdate optOutStatusUpdate, DepartmentGroupExtendedDTO requestedDepartmentGroup, CommunicationStatus communicationStatus, Map<String, Object> templateParams) throws Exception {
        LOGGER.info("in handleMessageOptOutStatusUpdateEvent for request={} department_group={} current_state={}", OBJECT_MAPPER.writeValueAsString(optOutStatusUpdate), OBJECT_MAPPER.writeValueAsString(requestedDepartmentGroup), communicationStatus);
        Long customerID = optOutStatusUpdate.getCustomerID();
        Long departmentID = optOutStatusUpdate.getDealerDepartmentID();
        String communicationValue = optOutStatusUpdate.getCommunicationValue();
        MessageProtocol messageProtocol = optOutStatusUpdate.getMessageProtocol();
        Boolean doubleOptInEnabled = optOutStatusUpdate.getDoubleOptInEnabled();
        OptOutStatusUpdateEvent event = optOutStatusUpdate.getEvent();
        UpdateOptOutStatusRequestType updateType = optOutStatusUpdate.getUpdateType();
        OptOutState currentStateRequestedDepartmentGroup = OptOutState.fromString(communicationStatus.getOptOutState());
        OptOutState newStateRequestedDepartmentGroup = null;
        Boolean canSendOptinRequestRequestedDepartmentGroup;

        List<Long> requestedDepartmentGroupDepartments = Arrays.asList(departmentID);
        if(requestedDepartmentGroup != null) {
            requestedDepartmentGroupDepartments = requestedDepartmentGroup.getDepartmentMinimalDTOList()
                    .stream().map(DepartmentMinimalDTO::getId)
                    .collect(Collectors.toList());
        }

        Long messageID = optOutStatusUpdate.getMessageID();
        switch (event) {
            case STOP_MESSAGE_RECEIVED:
            case STOP_SUSPECTED_MESSAGE_RECEIVED:
            case GENERIC_MESSAGE_RECEIVED:
            case OPTIN_MESSAGE_RECEIVED:
                newStateRequestedDepartmentGroup = optOutStatusHelper.getNewOptOutStateForMessageRelatedEvents(currentStateRequestedDepartmentGroup, event);
                canSendOptinRequestRequestedDepartmentGroup = false; // * If commValue is currently OPTED_OUT he will get autoresponder and hence no optin consent request can be sent further
                if((doubleOptInEnabled && communicationStatus.getId() == null) || !(currentStateRequestedDepartmentGroup.equals(newStateRequestedDepartmentGroup) && canSendOptinRequestRequestedDepartmentGroup == communicationStatus.getCanSendOptinRequest())) {
                    communicationStatusRepository.upsertCommunicationStatusForDepartments(requestedDepartmentGroupDepartments, messageProtocol.name(), communicationValue, newStateRequestedDepartmentGroup.name(), canSendOptinRequestRequestedDepartmentGroup);
                    communicationStatusHistoryRepository.upsertCommunicationStatusForDepartments(requestedDepartmentGroupDepartments, messageID, messageProtocol.name(), communicationValue, newStateRequestedDepartmentGroup.name());
                    // Send KNotification Message
                    sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup, null, null, PostOptOutStatusUpdateEvent.SEND_OPTOUT_STATUS_UPDATE_KNOTIFICATION_MESSAGE);
                    // Send MVC message for populating Stop Requested Filter
                    if(OptOutStatusUpdateEvent.STOP_MESSAGE_RECEIVED.equals(event)) {
                        sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup, null, null, PostOptOutStatusUpdateEvent.SEND_MVC_UPDATE);
                    }
                    // Update Message MetaData and Message Prediction
                    if(optOutStatusUpdate.getMessageKeyword() != null) {
                        sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup, null, null, PostOptOutStatusUpdateEvent.UPDATE_MESSAGE_META_DATA_AND_MESSAGE_PREDICTION);
                    }
                }
                String emailTemplate = getEmailTemplateTypeForMessageRelatedEvents(currentStateRequestedDepartmentGroup, event);
                if(emailTemplate != null) {
                    sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup,
                            emailTemplate, templateParams, PostOptOutStatusUpdateEvent.SEND_AUTORESPONDER);
                }
                break;
            default:
                LOGGER.warn("in handleMessageOptOutStatusUpdateEvent unsupported_event={} for optoutstatus_update_type={}. Discarding request for department_id={} customer_id={}",
                        event.name(), updateType.name(), departmentID, customerID);
                break;
        }
        LOGGER.info("in handleMessageOptOutStatusUpdateEvent event={} completed with new_opt_out_state={} for departments={} communication_value={}",
                event.name(), newStateRequestedDepartmentGroup, requestedDepartmentGroupDepartments, communicationValue);
    }

    private void handleDeploymentOptOutStatusUpdateEvent(OptOutStatusUpdate optOutStatusUpdate, DepartmentGroupExtendedDTO requestedDepartmentGroup, CommunicationStatus communicationStatus, Map<String, Object> templateParams) throws Exception {
        LOGGER.info("in handleDeploymentOptOutStatusUpdateEvent for request={} department_group={} current_state={}", OBJECT_MAPPER.writeValueAsString(optOutStatusUpdate), OBJECT_MAPPER.writeValueAsString(requestedDepartmentGroup), communicationStatus);
        Long customerID = optOutStatusUpdate.getCustomerID();
        Long departmentID = optOutStatusUpdate.getDealerDepartmentID();
        Long dealerID = optOutStatusUpdate.getDealerID();
        Long dealerAssociateID = optOutStatusUpdate.getDealerAssociateID();
        String communicationValue = optOutStatusUpdate.getCommunicationValue();
        MessageProtocol messageProtocol = optOutStatusUpdate.getMessageProtocol();
        OptOutStatusUpdateEvent event = optOutStatusUpdate.getEvent();
        UpdateOptOutStatusRequestType updateType = optOutStatusUpdate.getUpdateType();
        OptOutState currentStateRequestedDepartmentGroup = OptOutState.fromString(communicationStatus.getOptOutState());
        OptOutState newStateRequestedDepartmentGroup = null;
        Boolean canSendOptinRequestRequestedDepartmentGroup;

        List<Long> requestedDepartmentGroupDepartments = Arrays.asList(departmentID);
        if (requestedDepartmentGroup != null) {
            requestedDepartmentGroupDepartments = requestedDepartmentGroup.getDepartmentMinimalDTOList()
                    .stream().map(DepartmentMinimalDTO::getId)
                    .collect(Collectors.toList());
        }

        Map<Long, String> allDepartmentIDNameMap = new HashMap<>();
        allDepartmentIDNameMap = generalRepository.getAllDepartmentIDAndNameForDealerId(dealerID)
                .stream().collect(Collectors.toMap(department -> ((BigInteger) department[0]).longValue(), department -> (String) department[1]));

        // Fill DealerAssociate Name
        templateParams.put(APIConstants.DEALER_ASSOCIATE_NAME, generalRepository.getDealerAssociateName(dealerAssociateID));

        switch (event) {
            case DOUBLE_OPTIN_ROLLOUT:
                newStateRequestedDepartmentGroup = OptOutState.OPTED_OUT;
                canSendOptinRequestRequestedDepartmentGroup = true;
                Boolean incomingMessageFound = optOutStatusUpdate.getIncomingMessageFound();
                String customerCommunicationCreatedBy = optOutStatusUpdate.getCustomerCommunicationCreatedBy();
                if(communicationStatus.getId() != null) {
                    newStateRequestedDepartmentGroup = currentStateRequestedDepartmentGroup;
                    canSendOptinRequestRequestedDepartmentGroup = communicationStatus.getCanSendOptinRequest();
                } else if(requestedDepartmentGroup != null && Boolean.TRUE == incomingMessageFound
                        && APIConstants.EXTERNAL_CONTROLLER.equalsIgnoreCase(customerCommunicationCreatedBy)) {

                    // Check for optin candidature
                    DoubleOptInDepartmentGroupConfiguration isRolloutOptInAllowed = doubleOptInDepartmentGroupConfigurationRepository
                            .findFirstByFeatureUUIDAndDepartmentGroupUUIDAndKey(Feature.OPT_IN_ADVANCED.getKey(), requestedDepartmentGroup.getUuid(),
                                    INTEGRATION_CREATION_IMPLIES_OPTIN_DURING_ROLLOUT_CONFIGURATION_KEY);
                    if(isRolloutOptInAllowed != null && Boolean.TRUE.toString().equalsIgnoreCase(isRolloutOptInAllowed.getValue())) {
                        LOGGER.info("in handleDeploymentOptOutStatusUpdateEvent communication_value={} is approved for rollout opt-in for department_id={}", communicationValue, departmentID);
                        newStateRequestedDepartmentGroup = OptOutState.OPTED_IN;
                        canSendOptinRequestRequestedDepartmentGroup = false;
                    }
                }
                communicationStatusRepository.upsertCommunicationStatusForDepartments(requestedDepartmentGroupDepartments, messageProtocol.name(), communicationValue, newStateRequestedDepartmentGroup.name(), canSendOptinRequestRequestedDepartmentGroup);
                // Send KNotification Message
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments,currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup, null, null, PostOptOutStatusUpdateEvent.SEND_OPTOUT_STATUS_UPDATE_KNOTIFICATION_MESSAGE);
                doubleOptInDeploymentPostOptOutStatusUpdateProcessing(dealerID, customerID, communicationValue, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup, optOutStatusUpdate, TemplateConstants.DOUBLE_OPTIN_ROLLOUT_SYSTEM_NOTE_TEMPLATE, templateParams, requestedDepartmentGroupDepartments, allDepartmentIDNameMap);
                break;
            case DOUBLE_OPTIN_ROLLBACK:
                newStateRequestedDepartmentGroup = currentStateRequestedDepartmentGroup;
                canSendOptinRequestRequestedDepartmentGroup = communicationStatus.getCanSendOptinRequest();
                if(communicationStatus.getId() == null ||
                        (OptOutState.OPTED_OUT.equals(currentStateRequestedDepartmentGroup) && communicationStatus.getCanSendOptinRequest())) {
                    newStateRequestedDepartmentGroup = OptOutState.OPTED_IN;
                    canSendOptinRequestRequestedDepartmentGroup = false;
                }
                communicationStatusRepository.upsertCommunicationStatusForDepartments(requestedDepartmentGroupDepartments, messageProtocol.name(), communicationValue, newStateRequestedDepartmentGroup.name(), canSendOptinRequestRequestedDepartmentGroup);
                // Send KNotification Message
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, requestedDepartmentGroupDepartments,null, null, null, null, PostOptOutStatusUpdateEvent.SEND_OPTOUT_STATUS_UPDATE_KNOTIFICATION_MESSAGE);
                doubleOptInDeploymentPostOptOutStatusUpdateProcessing(dealerID, customerID, communicationValue, currentStateRequestedDepartmentGroup, newStateRequestedDepartmentGroup, optOutStatusUpdate, TemplateConstants.DOUBLE_OPTIN_ROLLBACK_SYSTEM_NOTE_TEMPLATE, templateParams, requestedDepartmentGroupDepartments, allDepartmentIDNameMap);
                break;
            default:
                LOGGER.warn("in handleDeploymentOptOutStatusUpdateEvent unsupported_event={} for optoutstatus_update_type={}. Discarding request for department_id={} customer_id={}",
                        event.name(), updateType.name(), departmentID, customerID);
                break;
        }

        LOGGER.info("in handleDeploymentOptOutStatusUpdateEvent event={} completed with new_opt_out_state={} for departments={} communication_value={}",
                event.name(), newStateRequestedDepartmentGroup, requestedDepartmentGroupDepartments, communicationValue);
    }

    private void sendToPostOptOutStatusUpdateQueue(OptOutStatusUpdate optOutStatusUpdate, List<Long> requestedDepartmentGroupDepartments, OptOutState currentOptOutState, OptOutState newOptOutState,
        String template, Map<String, Object> templateParams, PostOptOutStatusUpdateEvent event) throws Exception {
        try {
            PostOptOutStatusUpdate postOptOutStatusUpdate = new PostOptOutStatusUpdate();
            postOptOutStatusUpdate.setCurrentOptOutState(currentOptOutState);
            postOptOutStatusUpdate.setEvent(event);
            postOptOutStatusUpdate.setNewOptOutState(newOptOutState);
            postOptOutStatusUpdate.setOptOutStatusUpdate(optOutStatusUpdate);
            postOptOutStatusUpdate.setRequestedDepartmentGroupDepartments(requestedDepartmentGroupDepartments);
            postOptOutStatusUpdate.setTemplate(template);
            postOptOutStatusUpdate.setTemplateParams(templateParams);
            rabbitHelper.pushToPostOptOutStatusUpdateQueue(postOptOutStatusUpdate);
        } catch(Exception e) {
            LOGGER.error("Exception in sendToPostOptOutUpdateQueue for optout_status_update={} current_optout_state={} new_optout_state={} template={} template_params={}",
                OBJECT_MAPPER.writeValueAsString(optOutStatusUpdate), currentOptOutState.name(), newOptOutState.name(), template, templateParams, e);
        }
    }

    private String getEmailTemplateTypeForMessageRelatedEvents(OptOutState currentState, OptOutStatusUpdateEvent event) {
        switch (currentState) {
            case OPTED_IN:
                switch (event) {
                    case STOP_SUSPECTED_MESSAGE_RECEIVED:
                        return TemplateConstants.STOP_SUSPECTED_TEXT_RECEIVED_AUTORESPONDER_TEMPLATE;
                    case STOP_MESSAGE_RECEIVED:
                        return TemplateConstants.STOP_TEXT_RECEIVED_AUTORESPONDER_TEMPLATE;
                    default:
                        return null;
                }
            case OPTED_OUT:
                switch (event) {
                    case GENERIC_MESSAGE_RECEIVED:
                    case STOP_SUSPECTED_MESSAGE_RECEIVED:
                    case STOP_MESSAGE_RECEIVED:
                        return TemplateConstants.OPTED_OUT_NON_OPTIN_TEXT_RECEIVED_AUTORESPONDER_TEMPLATE;
                    case OPTIN_MESSAGE_RECEIVED:
                        return TemplateConstants.OPTED_OUT_OPTIN_TEXT_RECEIVED_AUTORESPONDER_TEMPLATE;
                    default:
                        return null;
                }
            default:
                return null;

        }
    }

    public ResponseEntity<Response> deployDoubleOptIn(String dealerUUID, DoubleOptInDeploymentRequest request) throws Exception {
        LOGGER.info("in deployDoubleOptIn for dealer_uuid={} request={}", dealerUUID, OBJECT_MAPPER.writeValueAsString(request));
        Response response = validateRequest.validateDoubleOptInDeploymentRequest(dealerUUID, request);
        if(response.getErrors() != null && !response.getErrors().isEmpty()) {
            return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
        }
        Long dealerID;
        try {
            dealerID = generalRepository.getDealerIdFromDealerUUID(dealerUUID);
        } catch(Exception e) {
            ApiError error = new ApiError(HttpStatus.BAD_REQUEST.name(), String.format("No dealer_id found for dealer_uuid=%s", dealerUUID));
            response.setErrors(Arrays.asList(error));
            return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
        }
        KManageApiHelper.updateDealerSetupOption(dealerUUID, DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_ENABLE.getOptionKey(), 
            DeploymentEvent.ROLLOUT.equals(request.getEvent()) ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
        DoubleOptInDeployment doubleOptInDeployment = new DoubleOptInDeployment();
        Boolean doubleOptinEnabled = Boolean.TRUE.toString().equalsIgnoreCase(kManageApiHelper.getAndUpdateDealerSetupOptionValueInCache(dealerUUID, DealerSetupOption.COMMUNICATIONS_DOUBLE_OPTIN_ENABLE.getOptionKey()));
        if(!(
            (doubleOptinEnabled && DeploymentEvent.ROLLOUT.equals(request.getEvent())) ||
                !doubleOptinEnabled && DeploymentEvent.ROLLBACK.equals(request.getEvent()))) {
            ApiError error = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), "DoubleOptin DSO value did not get updated");
            response.setErrors(Arrays.asList(error));
            return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        List<Long> allDepartmentIDList = generalRepository.getAllDepartemntIDsForDealerId(dealerID).stream().map(BigInteger::longValue).collect(Collectors.toList());
        
        // Splitting department into department groups for processing
        List<List<Long>> departmentGroups = new ArrayList<>();
        Set<Long> processedDepartments = new TreeSet<>();        
        for(Long departmentID : allDepartmentIDList) {
            if(!processedDepartments.contains(departmentID)) {
                String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(departmentID);
                List<Long> departmentGroupDepartments = Arrays.asList(departmentID);
                DepartmentGroupExtendedDTO departmentGroup = kManageApiHelper.getDepartmentGroupByFeature(departmentUUID, Feature.OPT_IN_ADVANCED.getKey());
                if(departmentGroup != null) {
                    departmentGroupDepartments = departmentGroup.getDepartmentMinimalDTOList()
                        .stream().map(DepartmentMinimalDTO::getId)
                        .collect(Collectors.toList());
                }
                processedDepartments.addAll(departmentGroupDepartments);
                departmentGroups.add(departmentGroupDepartments);
            }
        }

        doubleOptInDeployment.setDealerID(dealerID);
        doubleOptInDeployment.setDepartmentGroups(departmentGroups);
        doubleOptInDeployment.setEvent(request.getEvent());
        doubleOptInDeployment.setMinCustomerCommunicationID(0L);
        doubleOptInDeployment.setMaxEntriesToBeFetched(request.getMaxEntriesToBeFetched());
        rabbitHelper.pushToDoubleOptInDeploymentQueue(doubleOptInDeployment);
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    public void deployDoubleOptIn(DoubleOptInDeployment doubleOptInDeployment) throws Exception {
        LOGGER.info("in deployDoubleOptIn for request={}", OBJECT_MAPPER.writeValueAsString(doubleOptInDeployment));
        Long dealerID = doubleOptInDeployment.getDealerID();
        List<List<Long>> departmentGroups = doubleOptInDeployment.getDepartmentGroups();
        DeploymentEvent event = doubleOptInDeployment.getEvent();
        Long maxEntriesToBeFetched = doubleOptInDeployment.getMaxEntriesToBeFetched();
        Long minCustomerCommunicationID = doubleOptInDeployment.getMinCustomerCommunicationID();
        if(event == null || maxEntriesToBeFetched == null || maxEntriesToBeFetched.equals(0L) || minCustomerCommunicationID == null
            || !Arrays.asList(DeploymentEvent.ROLLOUT, DeploymentEvent.ROLLBACK).contains(event) ) {
            LOGGER.warn("in deployDoubleOptIn received invalid_request={}. Discarding request", OBJECT_MAPPER.writeValueAsString(doubleOptInDeployment));
        }
        List<Object[]> customerCommunications = generalRepository.getCustomerCommunicationAttributesForDealer(dealerID, Arrays.asList(APIConstants.PHONE_COMMUNICATION_TYPE_ID), minCustomerCommunicationID, maxEntriesToBeFetched);
        if(customerCommunications != null && !customerCommunications.isEmpty()) {
            minCustomerCommunicationID = ((BigInteger) customerCommunications.get(customerCommunications.size() - 1)[0]).longValue();
            LOGGER.info("in deployDoubleOptin found customer_communication_count={} max_customer_communication_id={} for request={}",
                    customerCommunications.size(), minCustomerCommunicationID, OBJECT_MAPPER.writeValueAsString(doubleOptInDeployment));
            for(Object[] customerCommunication : customerCommunications) {
                Long customerID = ((BigInteger) customerCommunication[1]).longValue();
                String communicationValue = (String) customerCommunication[3];
                String createdBy = (String) customerCommunication[4];

                DoubleOptInDeploymentStatus doubleOptInDeploymentStatus = new DoubleOptInDeploymentStatus();
                List<DepartmentDeploymentStatus> departmentDeploymentStatuses = new ArrayList<>();
                for(List<Long> departmentGroupDepartments : departmentGroups) {
                    for(Long departmentID : departmentGroupDepartments) {
                        String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(departmentID);
                        GetDealerAssociateResponseDTO responseDTO = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);
                        if(responseDTO != null && responseDTO.getDealerAssociate() != null) {
                            DepartmentDeploymentStatus departmentDeploymentStatus = new DepartmentDeploymentStatus();
                            departmentDeploymentStatus.setDepartmentID(departmentID);
                            departmentDeploymentStatuses.add(departmentDeploymentStatus);
                        }
                    }
                }
                // No need to synchronise here since this is pushing the status.
                doubleOptInDeploymentStatus.setDepartmentDeploymentStatusList(departmentDeploymentStatuses);
                optOutRedisService.setDoubleOptInDeploymentStatus(dealerID, customerID, communicationValue, doubleOptInDeploymentStatus);

                for(List<Long> departmentGroupDepartments : departmentGroups) {
                    OptOutStatusUpdate optOutStatusUpdate = new OptOutStatusUpdate();
                    Long deploymentRequesterDepartmentID = departmentGroupDepartments.get(0);
                    List<CommunicationStatus> communicationStatusList = communicationStatusRepository.
                            findAllByDealerIDAndDealerDepartmentIDListAndMessageProtocolAndCommunicationValue(dealerID, departmentGroupDepartments, MessageProtocol.TEXT.name(), communicationValue);
                    if(DeploymentEvent.ROLLOUT.equals(event)) {
                        if(communicationStatusList != null && !communicationStatusList.isEmpty()) {
                            LOGGER.info("in deployDoubleOptin found existing_communication_status_list={} for department_group_departments={} communication_value={} message_protocol={} event={}",
                                OBJECT_MAPPER.writeValueAsString(communicationStatusList), departmentGroupDepartments, communicationValue, MessageProtocol.TEXT.name(), event.name());
                            // Check if any is OPTED_OUT, if so we will opt out for the complete department group
                            Optional<CommunicationStatus> newStateOptional = communicationStatusList.stream()
                                .filter(cs -> OptOutState.OPTED_OUT.name().equalsIgnoreCase(cs.getOptOutState()))
                                .findFirst();
                            CommunicationStatus newState = newStateOptional.orElseGet(() -> communicationStatusList.get(0));
                            deploymentRequesterDepartmentID = newState.getDealerDepartmentID();
                        } else {
                            Long latestIncomingMessageDepartmentID = generalRepository.getLatestMessageDepartmentIDForCustomerIDAndDepartmentIDListAndCommunicationValue(
                                customerID, departmentGroupDepartments, communicationValue, Collections.singletonList(MessageType.INCOMING.getMessageType()), Collections.singletonList(MessageProtocol.TEXT.getMessageProtocol()));
                            if(latestIncomingMessageDepartmentID != null) {
                                deploymentRequesterDepartmentID = latestIncomingMessageDepartmentID;
                                optOutStatusUpdate.setIncomingMessageFound(true);
                            } else {
                                optOutStatusUpdate.setIncomingMessageFound(false);
                            }
                        }
                    }
                    String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(deploymentRequesterDepartmentID);
                    GetDealerAssociateResponseDTO responseDTO = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);
                    if(responseDTO != null && responseDTO.getDealerAssociate() != null) {
                        optOutStatusUpdate.setApiCallSource(APIConstants.MYKAARMA);
                        optOutStatusUpdate.setCommunicationValue(communicationValue);
                        optOutStatusUpdate.setCustomerID(customerID);
                        optOutStatusUpdate.setCustomerCommunicationCreatedBy(createdBy);
                        optOutStatusUpdate.setDealerID(dealerID);
                        optOutStatusUpdate.setDealerDepartmentID(deploymentRequesterDepartmentID);
                        optOutStatusUpdate.setDealerAssociateID(kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID).getDealerAssociate().getId());
                        optOutStatusUpdate.setDoubleOptInEnabled(DeploymentEvent.ROLLOUT.equals(event));
                        optOutStatusUpdate.setEvent(DeploymentEvent.ROLLOUT.equals(event) ? OptOutStatusUpdateEvent.DOUBLE_OPTIN_ROLLOUT : OptOutStatusUpdateEvent.DOUBLE_OPTIN_ROLLBACK);
                        optOutStatusUpdate.setMessageProtocol(MessageProtocol.TEXT);
                        optOutStatusUpdate.setUpdateType(UpdateOptOutStatusRequestType.DEPLOYMENT);
                        rabbitHelper.pushToOptOutStatusUpdateQueue(optOutStatusUpdate);
                    }
                }
            }
            if(doubleOptInDeployment.getMaxEntriesToBeFetched().equals((long) customerCommunications.size())) {
               doubleOptInDeployment.setMinCustomerCommunicationID(minCustomerCommunicationID + 1);
               rabbitHelper.pushToDoubleOptInDeploymentQueue(doubleOptInDeployment);
            }
        }
    }

    private void doubleOptInDeploymentPostOptOutStatusUpdateProcessing(Long dealerID, Long customerID, String communicationValue, OptOutState currentOptOutState, 
        OptOutState newOptOutState, OptOutStatusUpdate optOutStatusUpdate, String template, Map<String, Object> templateParams, List<Long> requestedDepartmentGroupDepartments,
        Map<Long, String> allDepartmentIDNameMap) throws Exception {
        LOGGER.info("in doubleOptInDeploymentPostOptOutStatusUpdateProcessing for request={} department_group={} new_opt_out_state={}", OBJECT_MAPPER.writeValueAsString(optOutStatusUpdate), requestedDepartmentGroupDepartments, newOptOutState);
        Lock lock = null;
        try {
            lock = optOutRedisService.obtainLockOnDoubleOptInDeploymentStatus(dealerID, customerID, communicationValue);
            DoubleOptInDeploymentStatus doubleOptInDeploymentStatus = optOutRedisService.getDoubleOptInDeploymentStatus(dealerID, customerID, communicationValue);
            LOGGER.info("in doubleOptInDeploymentPostOptOutStatusUpdateProcessing double_optin_deployment_status={} for department_group={} request={}", OBJECT_MAPPER.writeValueAsString(doubleOptInDeploymentStatus), requestedDepartmentGroupDepartments, OBJECT_MAPPER.writeValueAsString(optOutStatusUpdate));
            for(DepartmentDeploymentStatus departmentDeploymentStatus : doubleOptInDeploymentStatus.getDepartmentDeploymentStatusList()) {
                if(requestedDepartmentGroupDepartments.contains(departmentDeploymentStatus.getDepartmentID())) {
                    departmentDeploymentStatus.setDepartmentName(allDepartmentIDNameMap.get(departmentDeploymentStatus.getDepartmentID()));
                    departmentDeploymentStatus.setNewOptOutState(newOptOutState);
                    departmentDeploymentStatus.setOptOutStateProcessingDone(true);
                }
            }
            optOutRedisService.setDoubleOptInDeploymentStatus(dealerID, customerID, communicationValue, doubleOptInDeploymentStatus);
            Boolean allProcessed = doubleOptInDeploymentStatus.getDepartmentDeploymentStatusList().stream().allMatch(DepartmentDeploymentStatus::getOptOutStateProcessingDone);
            LOGGER.info("in doubleOptInDeploymentPostOptOutStatusUpdateProcessing all_processed={} for department_group={} double_optin_deployment_status={} request={}", allProcessed, requestedDepartmentGroupDepartments, OBJECT_MAPPER.writeValueAsString(doubleOptInDeploymentStatus), OBJECT_MAPPER.writeValueAsString(optOutStatusUpdate));
            if(allProcessed) {
                // All are processed send to PostOptOutStatusUpdate Queue
                templateParams.put(APIConstants.OPTED_IN_DEPARTMENTS,
                        doubleOptInDeploymentStatus.getDepartmentDeploymentStatusList()
                            .stream().filter(dds -> OptOutState.OPTED_IN.equals(dds.getNewOptOutState()))
                            .map(DepartmentDeploymentStatus::getDepartmentName)
                            .collect(Collectors.toList())
                );
                templateParams.put(APIConstants.OPTED_OUT_DEPARTMENTS,
                        doubleOptInDeploymentStatus.getDepartmentDeploymentStatusList()
                                .stream().filter(dds -> OptOutState.OPTED_OUT.equals(dds.getNewOptOutState()))
                                .map(DepartmentDeploymentStatus::getDepartmentName)
                                .collect(Collectors.toList())
                );
                sendToPostOptOutStatusUpdateQueue(optOutStatusUpdate, null, currentOptOutState, newOptOutState, template, templateParams, PostOptOutStatusUpdateEvent.SEND_SYSTEM_NOTIFICATION);
            }
        } catch(Exception e) {
            LOGGER.error("Exception in sendToPostOptOutUpdateQueue for optout_status_update={} current_optout_state={} new_optout_state={} template={} template_params={}",
                OBJECT_MAPPER.writeValueAsString(optOutStatusUpdate), currentOptOutState.name(), newOptOutState.name(), template, templateParams, e);
        } finally {
            if(lock != null) {
                lock.unlock();
            }
        }            
    }

    public ResponseEntity<Response> predictOptOutStatusCallback(String departmentUUID, String messageUUID, PredictOptOutStatusCallbackRequest request) throws Exception {
	    LOGGER.info("in predictOptOutStatusCallback for department_uuid={} message_uuid={} request={}", departmentUUID, messageUUID, OBJECT_MAPPER.writeValueAsString(request));
	    Response response = validateRequest.validatePredictOptOutStatusCallbackRequest(departmentUUID, messageUUID, request);
	    if(response.getErrors() != null && !response.getErrors().isEmpty()) {
	        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Message message = helper.getMessageObject(messageUUID);
        if(message == null) {
            ApiError apiError = new ApiError(ErrorCode.INVALID_MESSAGE_UUID.name(),
                String.format("invalid message_uuid=%s in request", messageUUID));
            response.setErrors(Arrays.asList(apiError));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        return predictOptOutStatusCallback(departmentUUID, message, request);
    }

    public ResponseEntity<Response> predictOptOutStatusCallback(String departmentUUID, Message message, PredictOptOutStatusCallbackRequest request) throws Exception {
        Response response = new Response();
        String messageUUID = message.getUuid();
        if(!(MessageProtocol.TEXT.getMessageProtocol().equalsIgnoreCase(message.getProtocol()) &&
            MessageType.INCOMING.getMessageType().equalsIgnoreCase(message.getMessageType()))) {
            ApiError apiError = new ApiError(ErrorCode.INVALID_MESSAGE.name(),
                String.format("message prediction not supported for message_type=%s message_protocol=%s of message_uuid=%s",
                    message.getProtocol(), message.getMessageType()));
            response.setErrors(Arrays.asList(apiError));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        MessageKeyword messageKeyword = null;
        if(request.getMessageKeyword() != null) {
            messageKeyword = MessageKeyword.fromString(request.getMessageKeyword());
            if(messageKeyword == null) {
                ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.name(),
                    String.format("invalid message_keyword=%s in request", request.getMessageKeyword()));
                response.setErrors(Arrays.asList(apiError));
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        } else {
            Boolean optOutV2Enabled = optOutV2Enabled(message.getDealerID());
            if(!optOutV2Enabled) {
                ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.name(), "opt_out_v2 not enabled for this dealer");
                response.setErrors(Arrays.asList(apiError));
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            Set<String> dsoKeys = new TreeSet<>();
            dsoKeys.add(DealerSetupOption.MESSAGING_INBOUND_TEXT_OPTOUT_V2_CRITICAL_THRESHOLD.getOptionKey());
            dsoKeys.add(DealerSetupOption.MESSAGING_INBOUND_TEXT_OPTOUT_V2_SOFT_THRESHOLD.getOptionKey());
            Double softThreshold = OPT_OUT_V2_PREDICTION_DEFAULT_SOFT_THRESHOLD;
            Double criticalThreshold = OPT_OUT_V2_PREDICTION_DEFAULT_CRITICAL_THRESHOLD;
            Map<String, String> thresholdDSOs = helper.getDSOValuesFromKManage(message.getDealerID(), dsoKeys);
            if(thresholdDSOs != null) {
                try {
                    criticalThreshold = Double.parseDouble(thresholdDSOs.get(DealerSetupOption.MESSAGING_INBOUND_TEXT_OPTOUT_V2_CRITICAL_THRESHOLD.getOptionKey()));
                    softThreshold = Double.parseDouble(thresholdDSOs.get(DealerSetupOption.MESSAGING_INBOUND_TEXT_OPTOUT_V2_SOFT_THRESHOLD.getOptionKey()));
                } catch (Exception e) {
                    LOGGER.error("in predictOptOutStatusCallback error while paring threshold dsos", e);
                }
            }
            if(criticalThreshold.compareTo(request.getOptOutV2Score()) < 0) {
                messageKeyword = MessageKeyword.STOP;
            } else if(softThreshold.compareTo(request.getOptOutV2Score()) < 0) {
                messageKeyword = MessageKeyword.STOP_SUSPECTED;
            } else {
                messageKeyword = MessageKeyword.GENERIC;
            }
        }
        LOGGER.info("in predictOptOutStatusCallback message_keyword={} for department_uuid={} message_uuid={} opt_out_v2_score={}",
            messageKeyword.name(), departmentUUID, messageUUID, request.getOptOutV2Score());
        OptOutStatusUpdate optOutStatusUpdate = new OptOutStatusUpdate();
        optOutStatusUpdate.setApiCallSource(APIConstants.MYKAARMA);
        optOutStatusUpdate.setCustomerID(message.getCustomerID());
        optOutStatusUpdate.setCommunicationValue(message.getFromNumber());
        optOutStatusUpdate.setDealerID(message.getDealerID());
        optOutStatusUpdate.setDealerAssociateID(message.getDealerAssociateID());
        optOutStatusUpdate.setDoubleOptInEnabled(doubleOptInEnabled(message.getDealerID()));
        optOutStatusUpdate.setDealerDepartmentID(message.getDealerDepartmentId());
        optOutStatusUpdate.setEvent(getUpdateEventForMessageKeyword(messageKeyword));
        optOutStatusUpdate.setMessageID(message.getId());
        optOutStatusUpdate.setMessageKeyword(messageKeyword);
        optOutStatusUpdate.setMessageProtocol(MessageProtocol.getMessageProtocolForString(message.getProtocol()));
        optOutStatusUpdate.setOptOutV2Score(request.getOptOutV2Score());
        optOutStatusUpdate.setUpdateType(UpdateOptOutStatusRequestType.MESSAGE);
        rabbitHelper.pushToOptOutStatusUpdateQueue(optOutStatusUpdate);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private OptOutStatusUpdateEvent getUpdateEventForMessageKeyword(MessageKeyword messageKeyword) {
	    switch (messageKeyword) {
            case STOP:
                return OptOutStatusUpdateEvent.STOP_MESSAGE_RECEIVED;
            case STOP_SUSPECTED:
                return OptOutStatusUpdateEvent.STOP_SUSPECTED_MESSAGE_RECEIVED;
            case GENERIC:
                return OptOutStatusUpdateEvent.GENERIC_MESSAGE_RECEIVED;
            case OPTIN:
                return OptOutStatusUpdateEvent.OPTIN_MESSAGE_RECEIVED;
            default:
                return null;
        }
    }

    private void findAndPushUnavailableCommunicationStatusToOptOutStatusUpdateQueue(Long dealerID, Long departmentID, boolean doubleOptInEnabled, List<CommunicationStatus> communicationStatusList) {
        if(doubleOptInEnabled) {
            try {
                List<CommunicationStatus> notFoundCommunicationStatusList = communicationStatusList.stream()
                    .filter(cs -> cs.getId() == null && MessageProtocol.TEXT.name().equalsIgnoreCase(cs.getMessageProtocol())).collect(Collectors.toList());
                if(!notFoundCommunicationStatusList.isEmpty()) {
                    LOGGER.info("in pushUnavailableCommunicationStatusToOptOutStatusUpdateQueue found unavailable_communication_status_list={} for dealer_id={} department_id={}",
                        OBJECT_MAPPER.writeValueAsString(notFoundCommunicationStatusList), dealerID, departmentID);
                    String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(departmentID);
                    Long dealerAssociateID = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID).getDealerAssociate().getId();
                    for(CommunicationStatus communicationStatus : notFoundCommunicationStatusList) {
                        OptOutStatusUpdate optOutStatusUpdate = new OptOutStatusUpdate();
                        optOutStatusUpdate.setApiCallSource(APIConstants.MYKAARMA);
                        optOutStatusUpdate.setCommunicationValue(communicationStatus.getCommunicationValue());
                        optOutStatusUpdate.setDealerAssociateID(dealerAssociateID);
                        optOutStatusUpdate.setDealerDepartmentID(departmentID);
                        optOutStatusUpdate.setDealerID(dealerID);
                        optOutStatusUpdate.setDoubleOptInEnabled(doubleOptInEnabled);
                        optOutStatusUpdate.setEvent(OptOutStatusUpdateEvent.COMMUNICATION_STATUS_NOT_FOUND);
                        optOutStatusUpdate.setMessageProtocol(MessageProtocol.fromString(communicationStatus.getMessageProtocol()));
                        optOutStatusUpdate.setUpdateType(UpdateOptOutStatusRequestType.CUSTOMER);
                        rabbitHelper.pushToOptOutStatusUpdateQueue(optOutStatusUpdate);
                    }
                }
            } catch(Exception e) {
                LOGGER.error("Error in findAndPushUnavailableCommunicationStatusToOptOutStatusUpdateQueue for dealer_id={} department_id={}", dealerID, departmentID, e);
            }
        }
    }

    private List<CommunicationOptOutStatusAttributes> buildOptOutStatusListFromCommunicationStatusList(List<CommunicationStatus> communicationStatusList) {
        List<CommunicationOptOutStatusAttributes> optOutStatusList = new ArrayList<>();
        for(CommunicationStatus communicationStatus : communicationStatusList) {
            CommunicationOptOutStatusAttributes optOutStatus = new CommunicationOptOutStatusAttributes();
            CommunicationAttributes communicationAttributes = new CommunicationAttributes();
            communicationAttributes.setCommunicationType(communicationStatus.getMessageProtocol());
            communicationAttributes.setCommunicationValue(communicationStatus.getCommunicationValue());
            OptOutStatusAttributes optOutStatusAttributes = new OptOutStatusAttributes();
            optOutStatusAttributes.setOptOutState(communicationStatus.getOptOutState());
            optOutStatusAttributes.setCanSendOptinRequest(communicationStatus.getCanSendOptinRequest());
            optOutStatus.setCommunication(communicationAttributes);
            optOutStatus.setOptOutStatus(optOutStatusAttributes);
            optOutStatusList.add(optOutStatus);
        }
        return optOutStatusList;
    }
}
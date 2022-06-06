package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.CustomerSentiment;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.MessageType;
import com.mykaarma.global.PredictionFeature;
import com.mykaarma.global.SentimentAnalysisPredictionTypes;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageExtnRepository;
import com.mykaarma.kcommunications.jpa.repository.MessagePredictionRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageThreadRepository;
import com.mykaarma.kcommunications.jpa.repository.PredictionFeatureRepository;
import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.model.jpa.MessageThread;
import com.mykaarma.kcommunications.model.kne.KNotificationMessage;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.utils.AppConfigHelper;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KCustomerApiHelperV2;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.KNotificationApiHelper;
import com.mykaarma.kcommunications.utils.KSentimentAPIHelper;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications.utils.SystemNotificationHelper;
import com.mykaarma.kcommunications_model.enums.MessagePurpose;
import com.mykaarma.kcommunications_model.request.UpdateCustomerSentimentStatusRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.Response;

@Service
public class CustomerSentimentImpl {
	
	@Autowired 
	private ValidateRequest validateRequest;
	
	@Autowired
	private Helper helper;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	@Autowired
	MessageRepository messageRepository;

	@Autowired
	PredictionFeatureRepository predictionFeatureRepo;

	@Autowired
	MessagePredictionRepository messagePredictionRepo;
	
	@Autowired
	private ThreadRepository threadRepository;
	
	@Autowired
	private MessageThreadRepository messageThreadRepository;
	
	@Autowired
	private KSentimentAPIHelper kSentimentAPIHelper;
	
	@Autowired
	MessageExtnRepository messageExtnRepository;
	
	@Autowired
	private KManageApiHelper kManageApiHelper;
	
	@Autowired
	private MessagingViewControllerHelper messagingViewControllerHelper;

	@Autowired
	CommunicationsApiImpl communicationsApiImpl;
	
	@Autowired 
	private KCustomerApiHelperV2 kCustomerAPIHelperV2;
	
	@Autowired 
	private KCommunicationsUtils kCommunicationsUtils;
	
	@Autowired 
	private KNotificationApiHelper kNotificationApiHelper;
	
	@Autowired 
	private SystemNotificationHelper systemNotificationHelper;

	private final static Logger LOGGER = LoggerFactory.getLogger(CustomerSentimentImpl.class);	
	private static final String CUSTOMER_STATUS_EVENT = "%s has marked this customer as %s";
	private static final String UPSET_CUSTOMER = "upset";
	private static final String NOT_UPSET_CUSTOMER = "not upset";
	private static final String MYKAARMA = "myKaarma";
	private static final Float defaultNegativeSentimentThreshold = (float) -0.5;
	
	private static final String SENTIMENT_SCORE = "SENTIMENT_SCORE";

	public ResponseEntity<Response> sentimentPredictionForMessage(String departmentUUID, String messageUUID) throws Exception {
		
		Response response = new Response();
		
		try {
			
			response = validateRequest.validateSentimentRequest(messageUUID);
			if(response.getErrors() != null && !response.getErrors().isEmpty()) {
				return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
			}
			
			Message message = helper.getMessageObject(messageUUID);
			if(message == null) {
				List<ApiError> errors = new ArrayList<>();
				ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.name(), String.format("Message not found for message_uuid=%s",messageUUID));
				errors.add(apiError);
				response.setErrors(errors);
				return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
			}

			String messageBody = message.getMessageExtn().getMessageBody();
			response = new Response();

			LOGGER.info(String.format("calling sentiment api for department_uuid=%s message_uuid=%s message_body=%s", departmentUUID, messageUUID, messageBody));
			
			Boolean result = kSentimentAPIHelper.hitSentimentApi(departmentUUID, messageUUID, messageBody);
			if(result)
				return new ResponseEntity<Response>(response, HttpStatus.OK);
			else {
				List<ApiError> errors = new ArrayList<>();
				ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.name(), String.format("Error in predicting sentiment for message_uuid=%s",messageUUID));
				errors.add(apiError);
				response.setErrors(errors);
				return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		}
		catch(Exception e) {
			LOGGER.error("Error in predicting sentiment for message_uuid={} ", messageUUID, e);
			throw e;
		}
	}

	public ResponseEntity<Response> updateMessageSentimentPrediction(String departmentUUID, String messageUUID, Float sentimentScore) throws Exception {
		
		Response response = new Response();
		
		try {
			
			response = validateRequest.validateSentimentRequest(messageUUID);
			if(response.getErrors() != null && !response.getErrors().isEmpty()) {
				return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
			}
			
			Message message = helper.getMessageObject(messageUUID);
			if(message == null) {
				List<ApiError> errors = new ArrayList<>();
				ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.name(), String.format("Message not found for message_uuid=%s",messageUUID));
				errors.add(apiError);
				response.setErrors(errors);
				return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
			}
			
			JSONObject metadata = new JSONObject();
			metadata.put(SENTIMENT_SCORE, sentimentScore.toString());

			LOGGER.info(String.format("updating sentiment in message prediction table for message_uuid=%s sentiment_score=%s", 
					messageUUID, metadata.toString()));
			
			Float negativeSentimentThreshold = defaultNegativeSentimentThreshold;
			
            String dealerUUID = kCommunicationsUtils.getDealerUUIDFromDepartmentUUID(departmentUUID);
            HashMap<String, String> dsoMap = kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(dealerUUID, getDSOListForSentimentThresholds());
			
			if(dsoMap.get(DealerSetupOption.COMMUNICATIONS_SENTIMENTANALYSIS_NEGATIVE_THRESHOLD.getOptionKey())!=null) {

				try {
					LOGGER.info(String.format("dso sentiment analysis threshold for dealer_uuid=%s negative_threshold=%s ", 
							dealerUUID, dsoMap.get(DealerSetupOption.COMMUNICATIONS_SENTIMENTANALYSIS_NEGATIVE_THRESHOLD.getOptionKey())));
					negativeSentimentThreshold = Float.parseFloat(dsoMap.get(DealerSetupOption.COMMUNICATIONS_SENTIMENTANALYSIS_NEGATIVE_THRESHOLD.getOptionKey()));
				} catch (Exception e) {
					LOGGER.error("Error while parsing negativeSentimentThreshold for negativeSentimentThreshold_dso_value={} ", dsoMap.get(DealerSetupOption.COMMUNICATIONS_SENTIMENTANALYSIS_NEGATIVE_THRESHOLD.getOptionKey()), e);
					negativeSentimentThreshold = defaultNegativeSentimentThreshold;
				}
			}

			LOGGER.info(String.format("thresholds for sentiment analysis for dealer_uuid=%s negative_threshold=%s ", 
					dealerUUID, negativeSentimentThreshold.toString()));
			
			if(sentimentScore <= negativeSentimentThreshold) {
				
				communicationsApiImpl.updateMessagePrediction(message.getId(), SentimentAnalysisPredictionTypes.NEGATIVE.getType(), metadata.toString(), PredictionFeature.SENTIMENT_ANALYSIS.getFeatureKey());
				
				updateFiltersAndSendNotificationForCustomerSentimentStatus(message);
				
				UpdateCustomerSentimentStatusRequest updateCustomerSentimentStatusRequest = new UpdateCustomerSentimentStatusRequest();
				updateCustomerSentimentStatusRequest.setCustomerSentiment(CustomerSentiment.UPSET.name());
				updateCustomerSentimentStatusRequest.setDealerID(message.getDealerID()); 
				updateCustomerSentimentStatusRequest.setDealerAssociateID(null);

				com.mykaarma.kcommunications.model.jpa.Thread thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(), message.getDealerDepartmentId(), false);

				String customerUUID = generalRepository.getCustomerUUIDFromCustomerID(message.getCustomerID());
				Boolean isUpset = kCustomerAPIHelperV2.checkCustomerSentimentStatus(customerUUID, departmentUUID);
				
				if(isUpset==null || !isUpset) {
		    		saveInternalNotification(updateCustomerSentimentStatusRequest, message.getCustomerID(), thread.getId(), departmentUUID);
		    		
				} else {
					LOGGER.info(String.format("customer is already marked as upset customer_id=%s department_uuid=%s", 
							message.getCustomerID(), departmentUUID));
				}
				
				updateCustomerStatus(customerUUID, departmentUUID, CustomerSentiment.UPSET.name());
				
			} else {
				communicationsApiImpl.updateMessagePrediction(message.getId(), SentimentAnalysisPredictionTypes.NEUTRAL.getType(), metadata.toString(), PredictionFeature.SENTIMENT_ANALYSIS.getFeatureKey());
				
			}

			LOGGER.info(String.format("succesfully updated sentiment in message prediction table for message_uuid=%s sentiment_score=%s", 
					messageUUID, metadata.toString()));
			
			return new ResponseEntity<Response>(response, HttpStatus.OK);
			
		}
		catch(Exception e) {
			LOGGER.error("Error in updating sentiment prediction for message_uuid={} ", messageUUID, e);
			throw e;
		}
	}
	
	private Set<String> getDSOListForSentimentThresholds() {
		Set<String> dsoList = new HashSet<>();
		dsoList.add(DealerSetupOption.COMMUNICATIONS_SENTIMENTANALYSIS_NEGATIVE_THRESHOLD.getOptionKey());
		return dsoList;
	}
	
	private void updateCustomerStatus(String customerUUID, String departmentUUID, String customerSentiment) throws Exception {

		try {
			LOGGER.info(String.format("trying to update customer_status table for customer_uuid=%s, department_uuid=%s ,customer_sentiment=%s",
					customerUUID, departmentUUID, customerSentiment));
			
			com.mykaarma.kcustomer_model.request.UpdateCustomerSentimentStatusRequest updateCustomerSentimentStatusRequest = 
					new com.mykaarma.kcustomer_model.request.UpdateCustomerSentimentStatusRequest();
			updateCustomerSentimentStatusRequest.setCustomerSentiment(customerSentiment);
			
			kCustomerAPIHelperV2.updateCustomerSentimentStatus(customerUUID, departmentUUID, updateCustomerSentimentStatusRequest);
			
		} catch (Exception e) {
			LOGGER.error(String.format("Exception in updating customer_status table for customer_uuid=%s, department_uuid=%s, customer_sentiment=%s",
					customerUUID, departmentUUID, customerSentiment), e);
			throw e;
		}
	}
	
	private void updateFiltersForCustomerSentimentStatus(Long customerID, Long departmentID, Long dealerID, String sentiment) throws Exception {

		try {
			com.mykaarma.kcommunications.model.jpa.Thread thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(customerID, departmentID, false);
			messagingViewControllerHelper.publishUpsetCustomerEvent(thread, dealerID, sentiment);

		} catch (Exception e) {
			throw e;
		}
			
	}
	
	private void updateFiltersAndSendNotificationForCustomerSentimentStatus(Message message) throws Exception {

		try {
			com.mykaarma.kcommunications.model.jpa.Thread thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(message.getCustomerID(), message.getDealerDepartmentId(), false);
			messagingViewControllerHelper.publishMessageSaveEvent(message, EventName.PREDICTED_CUSTOMER_UPSET, thread, false);
			
		} catch (Exception e) {
			throw e;
		}
			
	}
	
	public ResponseEntity<Response> updateCustomerSentimentStatus(UpdateCustomerSentimentStatusRequest updateCustomerSentimentStatusRequest, String departmentUUID) throws Exception {
		
		Response response = new Response();
		Long departmentID;
		
		try {
			departmentID = generalRepository.getDepartmentIDForUUID(departmentUUID);
			
			if(departmentID == null) {
				List<ApiError> errors = new ArrayList<>();
				ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.name(), String.format("department_id not found for department_uuid=%s",departmentUUID));
				errors.add(apiError);
				response.setErrors(errors);
				return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
			}
			
		}  catch(Exception e) {
			List<ApiError> errors = new ArrayList<>();
			ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.name(), String.format("department_id not found for department_uuid=%s",departmentUUID));
			errors.add(apiError);
			response.setErrors(errors);
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		}
			
		try {	
		    HashMap<Long,Long> customerIDAndThreadID = updateCustomerSentimentStatusRequest.getCustomerIDAndThreadID();

		    for (Map.Entry<Long,Long> mapElement : customerIDAndThreadID.entrySet()) {

				Long customerID = mapElement.getKey();
				Long threadID = mapElement.getValue();

		    	updateFiltersForCustomerSentimentStatus(customerID, departmentID, 
		    			updateCustomerSentimentStatusRequest.getDealerID(), updateCustomerSentimentStatusRequest.getCustomerSentiment());
		    	
		    	String customerUUID = generalRepository.getCustomerUUIDFromCustomerID(customerID);
	    		updateCustomerStatus(customerUUID, departmentUUID, updateCustomerSentimentStatusRequest.getCustomerSentiment());
		    	saveInternalNotification(updateCustomerSentimentStatusRequest, customerID, threadID, departmentUUID);
		    }
			return new ResponseEntity<Response>(response, HttpStatus.OK);
		}
		catch(Exception e) {
			LOGGER.error("Error in updating customer sentiment status for department_uuid={} sentiment={} ", 
				departmentUUID, updateCustomerSentimentStatusRequest.getCustomerSentiment(), e);
			throw e;
		}
	}
	
	private void saveInternalNotification(UpdateCustomerSentimentStatusRequest updateCustomerSentimentStatusRequest, Long customerID, Long threadID, String departmentUUID) throws Exception {
		
		try {
			Long dealerAssociateID  = updateCustomerSentimentStatusRequest.getDealerAssociateID();
			
			Long systemUserDAID = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID).getDealerAssociate().getId();
	
			LOGGER.info(String.format("saving into message successful for dealer_id=%s customer_id=%s department_uuid=%s ", updateCustomerSentimentStatusRequest.getDealerID(), customerID, departmentUUID));

			String template = CUSTOMER_STATUS_EVENT;
			String dealerAssociateName = MYKAARMA;
			String event = "";
			
			if(updateCustomerSentimentStatusRequest.getDealerAssociateID() != null) {
				dealerAssociateName = generalRepository.getDealerAssociateName(dealerAssociateID);
			} else {
				dealerAssociateID = systemUserDAID;
			}
	
	    	if(CustomerSentiment.UPSET.name().equalsIgnoreCase(updateCustomerSentimentStatusRequest.getCustomerSentiment())) {
				event = UPSET_CUSTOMER;
	    	} else {
				event = NOT_UPSET_CUSTOMER;
	    	}
			
			String messageBody = String.format(template, dealerAssociateName, event);
			LOGGER.info(String.format("Internal comment message_body=%s for customer_id=%s ", messageBody, customerID));
	
			String subject = null;
	    	if(CustomerSentiment.UPSET.name().equalsIgnoreCase(updateCustomerSentimentStatusRequest.getCustomerSentiment())) {
	    		subject = EventName.MARK_CUSTOMER_UPSET.name();
	    	} else {
	    		subject = EventName.MARK_CUSTOMER_NOT_UPSET.name();
	    	}

			systemNotificationHelper.saveUpsetCustomerOrWFRNotification(departmentUUID, messageBody, customerID, dealerAssociateID, subject, MessagePurpose.CUSTOMER_SENTIMENT_STATUS);
		
		} catch (Exception e) {

			LOGGER.error(String.format("Error in saving into message table for dealer_id=%s customer_id=%s department_uuid=%s ", updateCustomerSentimentStatusRequest.getDealerID(), 
					customerID, departmentUUID), e);
			throw e;
		}

	}
}

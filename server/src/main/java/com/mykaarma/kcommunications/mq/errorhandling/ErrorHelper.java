package com.mykaarma.kcommunications.mq.errorhandling;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.MessageRejectedException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessage;
import com.mykaarma.kcommunications.model.api.DelayedFilterRemovalRequest;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.rabbit.BaseRmqMessage;
import com.mykaarma.kcommunications.model.rabbit.CustomerSubscriptionsUpdate;
import com.mykaarma.kcommunications.model.rabbit.DoubleOptInDeployment;
import com.mykaarma.kcommunications.model.rabbit.FetchCustomersDealer;
import com.mykaarma.kcommunications.model.rabbit.GlobalOrderTransitionUpdate;
import com.mykaarma.kcommunications.model.rabbit.MessageUpdateOnEvent;
import com.mykaarma.kcommunications.model.rabbit.MultipleMessageSending;
import com.mykaarma.kcommunications.model.rabbit.OptInAwaitingMessageExpire;
import com.mykaarma.kcommunications.model.rabbit.OptOutStatusUpdate;
import com.mykaarma.kcommunications.model.rabbit.PostIncomingBotMessageSave;
import com.mykaarma.kcommunications.model.rabbit.PostIncomingMessageSave;
import com.mykaarma.kcommunications.model.rabbit.PostMessageReceived;
import com.mykaarma.kcommunications.model.rabbit.PostMessageSent;
import com.mykaarma.kcommunications.model.rabbit.PostOptOutStatusUpdate;
import com.mykaarma.kcommunications.model.rabbit.PostUniversalMessageSendPayload;
import com.mykaarma.kcommunications.model.rabbit.PreferredCommunicationModePrediction;
import com.mykaarma.kcommunications.model.rabbit.SaveHistoricalMessageRequest;
import com.mykaarma.kcommunications.model.rabbit.TemplateIndexingRequest;
import com.mykaarma.kcommunications.model.utils.CommunicationsVerification;
import com.mykaarma.kcommunications_model.common.DealerMessagesFetchRequest;
import com.mykaarma.kcommunications_model.common.RecordingURLMessageUpdateRequest;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryMailRequest;

@Service("errorhelper")
public class ErrorHelper {

	private static final Logger logger=LoggerFactory.getLogger(ErrorHelper.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${initialexpiration:1}")
    private int initialexpiration;
	@Value("${multiplier:8}")
    private int multiplier;
	@Value("${maximumretries:6}")
    private int maximumretries;
	@Value("${intialexpirationrecording:1000}")
    private int intialexpirationrecording;
	
	@Value("${initialexpiration.customermerging}")
	private int initiaExpirationCustomerMerging;
	
	@Value("${multiplier.customermerging}")
	private int multiplierCustomerMerging;
	
	public String onErrorInKCommunicationsProcessing(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Message message = mapper.readValue((String)e.getFailedMessage().getPayload(), Message.class);
			logger.error(String.format("Error occurred in sending message. Re-queuing the message for dealer_associate_id=%s dealer_id=%s message_uuid=%s ", 
					message.getDealerAssociateID(), message.getDealerID(), message.getUuid()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e1) {
			logger.error("Error in onErrorInKCommunicationsProcessing for message={} ", e.getFailedMessage().getPayload(), e);
			return null;
		}
	}
	
	public String onErrorInKCommunicationsMessageWithoutCustomerProcessing(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			ExternalMessage message = mapper.readValue((String)e.getFailedMessage().getPayload(), ExternalMessage.class);
			logger.error(String.format("Error occurred in sending message. Re-queuing the message for message_uuid=%s ",  message.getUuid()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e1) {
			logger.error("Error in onErrorInKCommunicationsMessageWithoutCustomerProcessing for message={} ", e.getFailedMessage().getPayload(), e);
			return null;
		}
	}
	
	public String onErrorInKCommunicationsProcessing(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Message message = mapper.readValue((String)e.getFailedMessage().getPayload(), Message.class);
			logger.error(String.format("Error occurred in sending message. Re-queuing the message for dealer_associate_id=%s dealer_id=%s message_uuid=%s ", 
					message.getDealerAssociateID(), message.getDealerID(), message.getUuid()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e1) {
			logger.error("Error in onErrorInKCommunicationsProcessing for message={} ", e.getFailedMessage().getPayload(), e);
			return null;
		}
	}
	
	
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, Message message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		
		return mapper.writeValueAsString(message);
		
	}
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, ExternalMessage message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		
		return mapper.writeValueAsString(message);
		
	}
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, PostMessageReceived message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		
		return mapper.writeValueAsString(message);
		
	}
	
	public String updateExpirationForMessage(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Message m = mapper.readValue(message, Message.class);
		Integer newExpiration = null;
		if(m.getExpiration() == null ) {
			logger.info("updateExpirationForMessage initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {   
			logger.info("updateExpirationForMessage current_expiration={} multiplies={}", m.getExpiration(), this.multiplier);
			double x = Math.log((double)(m.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForMessage max retries reached for current_expiration={} message_uuid={} retries={} ", m.getExpiration(), m.getUuid(), this.maximumretries);
				newExpiration = null;
			}
			else {
				newExpiration = m.getExpiration()*this.multiplier;
				logger.info("updateExpirationForMessage current_expiration={} new_expiration={}", m.getExpiration(),newExpiration);
			}
		}
		logger.info(String.format("updateExpirationForMessageData for message_uuid=%s dealer_id=%s new_expiration=%s ", 
				m.getUuid(), m.getDealerID(), newExpiration));
		m.setExpiration(newExpiration);
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	public String updateExpirationForMessageWithoutCustomer(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		ExternalMessage m = mapper.readValue(message, ExternalMessage.class);
		Integer newExpiration = null;
		if(m.getExpiration() == null ) {
			logger.info("updateExpirationForMessage initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {   
			logger.info("updateExpirationForMessage current_expiration={} multiplies={}", m.getExpiration(), this.multiplier);
			double x = Math.log((double)(m.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForMessage max retries reached for current_expiration={} message_uuid={} retries={} ", m.getExpiration(), m.getUuid(), this.maximumretries);
				newExpiration = null;
			}
			else {
				newExpiration = m.getExpiration()*this.multiplier;
				logger.info("updateExpirationForMessage current_expiration={} new_expiration={}", m.getExpiration(),newExpiration);
			}
		}
		logger.info(String.format("updateExpirationForMessageData for message_uuid=%s new_expiration=%s ", 
				m.getUuid(), newExpiration));
		m.setExpiration(newExpiration);
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	
	public String onErrorInPostMessageProcessing(org.springframework.integration.MessageRejectedException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostMessageSent postMessageSent = mapper.readValue((String)e.getFailedMessage().getPayload(), PostMessageSent.class);
		logger.error(String.format("Error occurred in post message sent. Re-queuing the message for dealer_associate_id=%s dealer_id=%s message_uuid=%s ", 
				postMessageSent.getMessage().getDealerAssociateID(), postMessageSent.getMessage().getDealerID(), postMessageSent.getMessage().getUuid()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(),postMessageSent);
	}
	
	public String onErrorInPostMessageProcessing(org.springframework.messaging.MessageHandlingException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostMessageSent postMessageSent = mapper.readValue((String)e.getFailedMessage().getPayload(), PostMessageSent.class);
		logger.error(String.format("Error occurred in post message sent. Re-queuing the message for dealer_associate_id=%s dealer_id=%s message_uuid=%s ", 
				postMessageSent.getMessage().getDealerAssociateID(), postMessageSent.getMessage().getDealerID(), postMessageSent.getMessage().getUuid()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(),postMessageSent);
	}

	public String onErrorInPostIncomingMessageSaveProcessing(org.springframework.integration.MessageRejectedException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostIncomingMessageSave postIncomingMessageSave = mapper.readValue((String) e.getFailedMessage().getPayload(), PostIncomingMessageSave.class);
		logger.error(String.format("Error occurred in post incoming message save. Re-queuing the message for dealer_associate_id=%s dealer_id=%s message_uuid=%s ",
				postIncomingMessageSave.getMessage().getDealerAssociateID(), postIncomingMessageSave.getMessage().getDealerID(), postIncomingMessageSave.getMessage().getUuid()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(), postIncomingMessageSave);
	}

	public String onErrorInPostIncomingMessageSaveProcessing(org.springframework.messaging.MessageHandlingException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostIncomingMessageSave postIncomingMessageSave = mapper.readValue((String)e.getFailedMessage().getPayload(), PostIncomingMessageSave.class);
		logger.error(String.format("Error occurred in post incoming message save. Re-queuing the message for dealer_associate_id=%s dealer_id=%s message_uuid=%s ",
				postIncomingMessageSave.getMessage().getDealerAssociateID(), postIncomingMessageSave.getMessage().getDealerID(), postIncomingMessageSave.getMessage().getUuid()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(), postIncomingMessageSave);
	}

	public String onErrorInPostIncomingBotMessageSave(org.springframework.integration.MessageRejectedException e) throws Exception {
		String payload = (String) e.getFailedMessage().getPayload();
		logger.error("Error occurred in post incoming bot message save. Re-queuing the message for request={}", payload, e);
		return processFailedMessage(e.getFailedMessage().getHeaders(), payload, PostIncomingBotMessageSave.class);
	}

	public String onErrorInPostIncomingBotMessageSave(org.springframework.messaging.MessageHandlingException e) throws Exception {
		String payload = (String) e.getFailedMessage().getPayload();
		logger.error("Error occurred in post incoming bot message save. Re-queuing the message for request={}", payload, e);
		return processFailedMessage(e.getFailedMessage().getHeaders(), payload, PostIncomingBotMessageSave.class);
	}

	private <T extends BaseRmqMessage> String processFailedMessage(MessageHeaders messageHeaders, String payload, Class<T> messageType) throws Exception {
		T message = objectMapper.readValue(payload, messageType);
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
				.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				}
			}
		} else {
			message.setExpiration(null);
		}
		String newPayload = objectMapper.writeValueAsString(message);
		logger.info("processFailedMessage expiration={} for request={}", newPayload, message.getExpiration());
		return newPayload;
	}

	public String onErrorInPostUniversalMessageSendProcessing(org.springframework.integration.MessageRejectedException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostUniversalMessageSendPayload postUniversalMessageSendPayload = mapper.readValue((String) e.getFailedMessage().getPayload(), PostUniversalMessageSendPayload.class);
		logger.error(String.format("Error occurred in post universal message send. Re-queuing the message for dealer_associate_id=%s dealer_id=%s message_uuid=%s ",
				postUniversalMessageSendPayload.getMessage().getDealerAssociateID(), postUniversalMessageSendPayload.getMessage().getDealerID(), postUniversalMessageSendPayload.getMessage().getUuid()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(), postUniversalMessageSendPayload);
	}

	public String onErrorInPostUniversalMessageSendProcessing(org.springframework.messaging.MessageHandlingException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostUniversalMessageSendPayload postUniversalMessageSendPayload = mapper.readValue((String)e.getFailedMessage().getPayload(), PostUniversalMessageSendPayload.class);
		logger.error(String.format("Error occurred in post universal message send. Re-queuing the message for dealer_associate_id=%s dealer_id=%s message_uuid=%s ",
				postUniversalMessageSendPayload.getMessage().getDealerAssociateID(), postUniversalMessageSendPayload.getMessage().getDealerID(), postUniversalMessageSendPayload.getMessage().getUuid()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(), postUniversalMessageSendPayload);
	}
	
	public String onErrorInDelayedFilterUpdate(org.springframework.integration.MessageRejectedException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		DelayedFilterRemovalRequest delayedFilterRemovalRequest = mapper.readValue((String)e.getFailedMessage().getPayload(), DelayedFilterRemovalRequest.class);
		logger.error(String.format("Error occurred in delayed filter update. Re-queuing the message for from_dealer_id=%s to_dealer_id=%s offset=%s ", 
				delayedFilterRemovalRequest.getFromDealerID(),delayedFilterRemovalRequest.getToDealerID(),delayedFilterRemovalRequest.getOffset()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(),delayedFilterRemovalRequest);
	}
	
	public String onErrorInDelayedFilterUpdate(org.springframework.messaging.MessageHandlingException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		DelayedFilterRemovalRequest delayedFilterRemovalRequest = mapper.readValue((String)e.getFailedMessage().getPayload(), DelayedFilterRemovalRequest.class);
		logger.error(String.format("Error occurred in delayed filter update. Re-queuing the message for from_dealer_id=%s to_dealer_id=%s offset=%s ", 
				delayedFilterRemovalRequest.getFromDealerID(),delayedFilterRemovalRequest.getToDealerID(),delayedFilterRemovalRequest.getOffset()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(),delayedFilterRemovalRequest);
	}

	public String onErrorInOptinAwaitingMessageExpire(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			OptInAwaitingMessageExpire message = mapper.readValue((String)e.getFailedMessage().getPayload(), OptInAwaitingMessageExpire.class);
			logger.error(String.format("Error occurred in expiring optin awaiting message. Re-queuing the message for message_uuid=%s ",
				message.getMessageUUID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e1) {
			logger.error("Error in onErrorInOptinAwaitingMessageExpire for message={} ", e.getFailedMessage().getPayload(), e);
			return null;
		}
	}

	public String onErrorInOptinAwaitingMessageExpire(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			OptInAwaitingMessageExpire message = mapper.readValue((String)e.getFailedMessage().getPayload(), OptInAwaitingMessageExpire.class);
			logger.error(String.format("Error occurred in expiring optin awaiting message. Re-queuing the message for message_uuid=%s ",
				message.getMessageUUID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e1) {
			logger.error("Error in onErrorInOptinAwaitingMessageExpire for message={} ", e.getFailedMessage().getPayload(), e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private String processFailedMessage(MessageHeaders messageHeaders, OptInAwaitingMessageExpire optInAwaitingMessageExpire) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					optInAwaitingMessageExpire.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				}
			}
		} else {
			optInAwaitingMessageExpire.setExpiration(null);
		}

		return mapper.writeValueAsString(optInAwaitingMessageExpire);
	}


	public String onErrorInTemplateIndexing(org.springframework.integration.MessageRejectedException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		TemplateIndexingRequest templateIndexingRequest = mapper.readValue((String)e.getFailedMessage().getPayload(), TemplateIndexingRequest.class);
		logger.error(String.format("Error occurred in indexing template. Re-queuing the message for template_uuid=%s template_type=%s ", 
				templateIndexingRequest.getTemplateUuid(),templateIndexingRequest.getTemplateType()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(),templateIndexingRequest);
	}
	
	public String onErrorInTemplateIndexing(org.springframework.messaging.MessageHandlingException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		TemplateIndexingRequest templateIndexingRequest = mapper.readValue((String)e.getFailedMessage().getPayload(), TemplateIndexingRequest.class);
		logger.error(String.format("Error occurred in indexing template. Re-queuing the message for template_uuid=%s template_type=%s ", 
				templateIndexingRequest.getTemplateUuid(),templateIndexingRequest.getTemplateType()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(),templateIndexingRequest);
	}
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, PostMessageSent postMessageSent) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					postMessageSent.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			postMessageSent.setExpiration(null);
		}
		
		return mapper.writeValueAsString(postMessageSent);
		
	}

	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, PostIncomingMessageSave postIncomingMessageSave) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders.get("x-death");
			if (deathList != null && deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					postIncomingMessageSave.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				}
			}
		} else {
			postIncomingMessageSave.setExpiration(null);
		}

		return mapper.writeValueAsString(postIncomingMessageSave);
	}

	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, PostUniversalMessageSendPayload postUniversalMessageSendPayload) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders.get("x-death");
			if (deathList != null && deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					postUniversalMessageSendPayload.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				}
			}
		} else {
			postUniversalMessageSendPayload.setExpiration(null);
		}

		return mapper.writeValueAsString(postUniversalMessageSendPayload);
	}
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, TemplateIndexingRequest templateIndexingRequest) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders.get("x-death");
			if (deathList != null && deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					templateIndexingRequest.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				}
			}
		} else {
			templateIndexingRequest.setExpiration(null);
		}

		return mapper.writeValueAsString(templateIndexingRequest);
	}
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, DelayedFilterRemovalRequest delayedFilterRemovalRequest) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					delayedFilterRemovalRequest.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			delayedFilterRemovalRequest.setExpiration(null);
		}
		
		return mapper.writeValueAsString(delayedFilterRemovalRequest);
		
	}
	
	public String updateExpirationForPostMessageSent(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostMessageSent postMessageSent = mapper.readValue(message, PostMessageSent.class);
		Integer newExpiration = null;
		if(postMessageSent.getExpiration() == null ) {
            logger.info("updateExpirationForPostMessageSent initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {   
            logger.info("updateExpirationForPostMessageSent current_expiration={} multiplies={}", postMessageSent.getExpiration(), this.multiplier);
			double x = Math.log((double)(postMessageSent.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForPostMessageSent max retries reached for current_expiration={} message_uuid={} retries={} ", postMessageSent.getExpiration(), postMessageSent.getMessage().getUuid(), this.maximumretries);
				newExpiration = null;
			}
			else {
				newExpiration = postMessageSent.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForPostMessageSent for message_uuid=%s dealer_id=%s new_expiration=%s ", 
				postMessageSent.getMessage().getUuid(), postMessageSent.getMessage().getDealerID(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}

	public String updateExpirationForPostIncomingBotMessageSave(String message) throws Exception {
		logger.info("in updateExpirationForPostIncomingBotMessageSave for payload={}", message);
		return updateExpiration(message);
	}

	private String updateExpiration(String messagePayload) throws Exception {
		BaseRmqMessage message = objectMapper.readValue(messagePayload, BaseRmqMessage.class);
		Integer newExpiration = null;
		if(message.getExpiration() == null ) {
			logger.info("updateExpiration initial_expiration={} for payload={}", this.initialexpiration, messagePayload);
			newExpiration = this.initialexpiration;
		}
		else {
			logger.info("updateExpiration current_expiration={} multiplies={} for payload={}", message.getExpiration(), this.multiplier, messagePayload);
			double x = Math.log((double)(message.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpiration max retries reached for current_expiration={} payload={} retries={}",
					message.getExpiration(), messagePayload, this.maximumretries);
			}
			else {
				newExpiration = message.getExpiration()*this.multiplier;
			}
		}
		logger.info("updateExpiration for payload={} new_expiration={}",
			messagePayload, (newExpiration != null ? String.valueOf(newExpiration) : null));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}

	public String updateExpirationForPostIncomingMessageSave(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostIncomingMessageSave postIncomingMessageSave = mapper.readValue(message, PostIncomingMessageSave.class);
		Integer newExpiration = null;
		if(postIncomingMessageSave.getExpiration() == null ) {
			logger.info("updateExpirationForPostIncomingMessageSave initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {
			logger.info("updateExpirationForPostIncomingMessageSave current_expiration={} multiplies={}", postIncomingMessageSave.getExpiration(), this.multiplier);
			double x = Math.log((double)(postIncomingMessageSave.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForPostIncomingMessageSave max retries reached for current_expiration={} message_uuid={} retries={} ",
						postIncomingMessageSave.getExpiration(), postIncomingMessageSave.getMessage().getUuid(), this.maximumretries);
				newExpiration = null;
			}
			else {
				newExpiration = postIncomingMessageSave.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForPostIncomingMessageSave for message_uuid=%s dealer_id=%s new_expiration=%s ",
				postIncomingMessageSave.getMessage().getUuid(), postIncomingMessageSave.getMessage().getDealerID(), (newExpiration != null ? String.valueOf(newExpiration) : null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}

	public String updateExpirationForPostUniversalMessageSend(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostUniversalMessageSendPayload postUniversalMessageSendPayload = mapper.readValue(message, PostUniversalMessageSendPayload.class);
		Integer newExpiration = null;
		if(postUniversalMessageSendPayload.getExpiration() == null ) {
			logger.info("updateExpirationForPostUniversalMessageSend initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {
			logger.info("updateExpirationForPostUniversalMessageSend current_expiration={} multiplies={}", postUniversalMessageSendPayload.getExpiration(), this.multiplier);
			double x = Math.log((double)(postUniversalMessageSendPayload.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForPostUniversalMessageSend max retries reached for current_expiration={} message_uuid={} retries={} ",
						postUniversalMessageSendPayload.getExpiration(), postUniversalMessageSendPayload.getMessage().getUuid(), this.maximumretries);
				newExpiration = null;
			}
			else {
				newExpiration = postUniversalMessageSendPayload.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForPostUniversalMessageSend for message_uuid=%s dealer_id=%s new_expiration=%s ",
				postUniversalMessageSendPayload.getMessage().getUuid(), postUniversalMessageSendPayload.getMessage().getDealerID(), (newExpiration != null ? String.valueOf(newExpiration) : null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	public String updateExpirationForTemplateIndexing(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		TemplateIndexingRequest templateIndexingRequest = mapper.readValue(message, TemplateIndexingRequest.class);
		Integer newExpiration = null;
		if(templateIndexingRequest.getExpiration() == null ) {
			logger.info("updateExpirationForTemplateIndexing initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {
			logger.info("updateExpirationForTemplateIndexing current_expiration={} multiplies={}", templateIndexingRequest.getExpiration(), this.multiplier);
			double x = Math.log((double)(templateIndexingRequest.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForTemplateIndexing max retries reached for current_expiration={} template_uuid={} retries={} ",
						templateIndexingRequest.getExpiration(), templateIndexingRequest.getTemplateUuid(), this.maximumretries);
				newExpiration = null;
			}
			else {
				newExpiration = templateIndexingRequest.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForTemplateIndexing for message_uuid=%s dealer_id=%s new_expiration=%s ",
				templateIndexingRequest.getTemplateUuid(), templateIndexingRequest.getTemplateType(), (newExpiration != null ? String.valueOf(newExpiration) : null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	public String updateExpirationForDelayedFilterUpdate(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		DelayedFilterRemovalRequest delayedFilterRemovalRequest = mapper.readValue(message, DelayedFilterRemovalRequest.class);
		Integer newExpiration = null;
		if(delayedFilterRemovalRequest.getExpiration() == null ) {
			newExpiration = this.initialexpiration;
		}
		else {   
			double x = Math.log((double)(delayedFilterRemovalRequest.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			}
			else {
				newExpiration = delayedFilterRemovalRequest.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForDelayedFilterUpdate for from_dealer_id=%s to_dealer_id=%s offset=%s new_expiration=%s ", 
				delayedFilterRemovalRequest.getFromDealerID(),delayedFilterRemovalRequest.getToDealerID(),delayedFilterRemovalRequest.getOffset(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	public String onErrorInEventProcessing(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			MessageUpdateOnEvent message = mapper.readValue((String)e.getFailedMessage().getPayload(), MessageUpdateOnEvent.class);
			logger.error(String.format("Error occurred in onErrorInEventeProcessing1 message. Re-queuing the message for message_uuid=%s ", 
					message.getMessageUUID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e2) {
			logger.error("error in onErrorInEventProcessing1 ", e2);
			return null;
		}
	}
	
	public String onErrorInEventProcessing(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			MessageUpdateOnEvent message = mapper.readValue((String)e.getFailedMessage().getPayload(), MessageUpdateOnEvent.class);
			logger.info(String.format("Error occurred in onErrorInEventeProcessing2 message. Re-queuing the message for message_uuid=%s ", 
					message.getMessageUUID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e2) {
			logger.error("error in onErrorInEventProcessing2 ", e2);
			return null;
		}
	}
	
	
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, MessageUpdateOnEvent message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		logger.info("processFailedMessage expiration={}", message.getExpiration());
		return mapper.writeValueAsString(message);
		
	}
	
	public String updateExpirationForEventProcessor(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		MessageUpdateOnEvent messageUpdateOnEvent = mapper.readValue(message, MessageUpdateOnEvent.class);
		Integer newExpiration = null;
		if(messageUpdateOnEvent.getExpiration() == null ) {
			newExpiration = this.initialexpiration;
		}
		else {   
			double x = Math.log((double)(messageUpdateOnEvent.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			}
			else {
				newExpiration = messageUpdateOnEvent.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForEventProcessor for message_uuid=%s new_expiration=%s ", 
				messageUpdateOnEvent.getMessageUUID(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	
	public String onErrorInFetchingMessagesForDealer(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			DealerMessagesFetchRequest message = mapper.readValue((String)e.getFailedMessage().getPayload(), DealerMessagesFetchRequest.class);
			logger.error(String.format("Error occurred in onErrorInFetchingMessagesForDealer message. Re-queuing the message for dealer_id=%s ", 
					message.getDealerID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e2) {
			logger.error("error in onErrorInFetchingMessagesForDealer ", e2);
			return null;
		}
	}
	
	public String onErrorInFetchingMessagesForDealer(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			DealerMessagesFetchRequest message = mapper.readValue((String)e.getFailedMessage().getPayload(), DealerMessagesFetchRequest.class);
			logger.info(String.format("Error occurred in onErrorInFetchingMessagesForDealer message. Re-queuing the message for dealer_id=%s ", 
					message.getDealerID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e2) {
			logger.error("error in onErrorInFetchingMessagesForDealer ", e2);
			return null;
		}
	}
	
	public String onErrorInPostMessageReceived(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			PostMessageReceived message = mapper.readValue((String)e.getFailedMessage().getPayload(), PostMessageReceived.class);
			logger.error(String.format("Error occurred in onErrorInPostMessageReceived message. Re-queuing the message for dealer_id=%s message_uuid=%s ", 
					message.getDealerID(), message.getMessageUUID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e2) {
			logger.error("error in onErrorInPostMessageReceived ", e2);
			return null;
		}
	}
	
	public String onErrorInPostMessageReceived(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			PostMessageReceived message = mapper.readValue((String)e.getFailedMessage().getPayload(), PostMessageReceived.class);
			logger.info(String.format("Error occurred in onErrorInPostMessageReceived message. Re-queuing the message for dealer_id=%s message_uuid=%s ", 
					message.getDealerID(), message.getMessageUUID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e2) {
			logger.error("error in onErrorInPostMessageReceived ", e2);
			return null;
		}
	}
	
	public String onErrorInFetchingCustomersForDealer(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			FetchCustomersDealer message = mapper.readValue((String)e.getFailedMessage().getPayload(), FetchCustomersDealer.class);
			logger.error(String.format("Error occurred in onErrorInFetchingCustomersForDealer message. Re-queuing the message for dealer_id=%s ", 
					message.getDealerId()	),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e2) {
			logger.error("error in onErrorInFetchingMessagesForDealer ", e2);
			return null;
		}
	}
	
	public String onErrorInFetchingCustomersForDealer(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			FetchCustomersDealer message = mapper.readValue((String) e.getFailedMessage().getPayload(), FetchCustomersDealer.class);
			logger.info(String.format("Error occurred in onErrorInFetchingCustomersForDealer message. Re-queuing the message for dealer_id=%s ",
				message.getDealerId()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), message);
		} catch (Exception e2) {
			logger.error("error in onErrorInFetchingMessagesForDealer ", e2);
			return null;
		}
	}

	public String onErrorSavingHistoricalCommunications(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			SaveHistoricalMessageRequest message = mapper.readValue((String) e.getFailedMessage().getPayload(), SaveHistoricalMessageRequest.class);
			logger.error(String.format("Error occurred in onErrorSavingHistoricalCommunications message. Re-queuing the message for customer_uuid=%s source_uuid=%s",
				message.getCustomerUuid(), message.getSaveMessageRequest().getSourceUuid()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), message);
		} catch (Exception e2) {
			logger.error("error in onErrorSavingHistoricalCommunications ", e2);
			return null;
		}
	}

	public String onErrorSavingHistoricalCommunications(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			SaveHistoricalMessageRequest message = mapper.readValue((String) e.getFailedMessage().getPayload(), SaveHistoricalMessageRequest.class);
			logger.error(String.format("Error occurred in onErrorSavingHistoricalCommunications message. Re-queuing the message for customer_uuid=%s source_uuid=%s",
				message.getCustomerUuid(), message.getSaveMessageRequest().getSourceUuid()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), message);
		} catch (Exception e2) {
			logger.error("error in onErrorInFetchingMessagesForDealer ", e2);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, SaveHistoricalMessageRequest message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
				.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String) death.get("original-expiration")));
				}
			}
		} else {
			message.setExpiration(null);
		}
		logger.info("processFailedMessage expiration={}", message.getExpiration());
		return mapper.writeValueAsString(message);

	}

	public String onErrorChangingThreadOwnerChangeOnROCreation(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			GlobalOrderTransitionUpdate message = mapper.readValue((String) e.getFailedMessage().getPayload(), GlobalOrderTransitionUpdate.class);
			logger.error(String.format("Error occurred in onErrorChangingThreadOwnerChangeOnROCreation message. Re-queuing the message for customer_uuid=%s department_uuid=%s",
				message.getOrder().getCustomer().getUuid(), message.getDepartmentUuid()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), message);
		} catch (Exception e2) {
			logger.error("error in onErrorChangingThreadOwnerChangeOnROCreation ", e2);
			return null;
		}
	}

	public String onErrorChangingThreadOwnerChangeOnROCreation(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			GlobalOrderTransitionUpdate message = mapper.readValue((String) e.getFailedMessage().getPayload(), GlobalOrderTransitionUpdate.class);
			logger.error(String.format("Error occurred in onErrorChangingThreadOwnerChangeOnROCreation message. Re-queuing the message for customer_uuid=%s department_uuid=%s",
					message.getOrder().getCustomer().getUuid(), message.getDepartmentUuid()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), message);
		} catch (Exception e2) {
			logger.error("error in onErrorChangingThreadOwnerChangeOnROCreation ", e2);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, GlobalOrderTransitionUpdate message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
				.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String) death.get("original-expiration")));
				}
			}
		} else {
			message.setExpiration(null);
		}
		logger.info("processFailedMessage expiration={}", message.getExpiration());
		return mapper.writeValueAsString(message);

	}

	public String onErrorInUpdatingCustomerSubscriptions(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			CustomerSubscriptionsUpdate message = mapper.readValue((String) e.getFailedMessage().getPayload(), CustomerSubscriptionsUpdate.class);
			logger.error(String.format("Error occurred in onErrorInUpdatingCustomerSubscriptions message. Re-queuing the message for customer_id=%s ",
				message.getCustomerId()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), message);
		} catch (Exception e2) {
			logger.error("error in onErrorInFetchingMessagesForDealer ", e2);
			return null;
		}
	}
	
	public String onErrorInUpdatingCustomerSubscriptions(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			CustomerSubscriptionsUpdate message = mapper.readValue((String)e.getFailedMessage().getPayload(), CustomerSubscriptionsUpdate.class);
			logger.info(String.format("Error occurred in onErrorInUpdatingCustomerSubscriptions message. Re-queuing the message for customer_id=%s ", 
					message.getCustomerId()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),message);
		} catch (Exception e2) {
			logger.error("error in onErrorInFetchingMessagesForDealer ", e2);
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, CustomerSubscriptionsUpdate message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		logger.info("processFailedMessage expiration={}", message.getExpiration());
		return mapper.writeValueAsString(message);
		
	}
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, FetchCustomersDealer message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		logger.info("processFailedMessage expiration={}", message.getExpiration());
		return mapper.writeValueAsString(message);
		
	}
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, DealerMessagesFetchRequest message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		logger.info("processFailedMessage expiration={}", message.getExpiration());
		return mapper.writeValueAsString(message);
		
	}
	
	
	public String updateExpirationForFetchingCustomersForDealer(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		FetchCustomersDealer fetchCustomersDealer = mapper.readValue(message, FetchCustomersDealer.class);
		Integer newExpiration = null;
		if(fetchCustomersDealer.getExpiration() == null ) {
			newExpiration = this.intialexpirationrecording;
		}
		else {   
			double x = Math.log((double)(fetchCustomersDealer.getExpiration()/this.intialexpirationrecording));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			}
			else {
				newExpiration = fetchCustomersDealer.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForFetchingCustomersForDealer for dealer_id=%s new_expiration=%s ", 
				fetchCustomersDealer.getDealerId(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
		
	}
	
	public String updateExpirationForFetchingMessagesForDealer(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		DealerMessagesFetchRequest messageForDealer = mapper.readValue(message, DealerMessagesFetchRequest.class);
		Integer newExpiration = null;
		if(messageForDealer.getExpiration() == null ) {
			newExpiration = this.intialexpirationrecording;
		}
		else {   
			double x = Math.log((double)(messageForDealer.getExpiration()/this.intialexpirationrecording));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			}
			else {
				newExpiration = messageForDealer.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForFetchingMessagesForDealer for dealer_id=%s new_expiration=%s ", 
				messageForDealer.getDealerID(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	public String updateExpirationForPostMessageReceived(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostMessageReceived postMessageReceived = mapper.readValue(message, PostMessageReceived.class);
		Integer newExpiration = null;
		if(postMessageReceived.getExpiration() == null ) {
			newExpiration = this.intialexpirationrecording;
		}
		else {   
			double x = Math.log((double)(postMessageReceived.getExpiration()/this.intialexpirationrecording));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			}
			else {
				newExpiration = postMessageReceived.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForPostMessageReceived for dealer_id=%s message_uuid=%s new_expiration=%s ", 
				postMessageReceived.getDealerID(),postMessageReceived.getMessageUUID(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	public String onErrorInUpdatingRecordingURLForMessage(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			RecordingURLMessageUpdateRequest updateRecordingURLForMessage = mapper.readValue((String)e.getFailedMessage().getPayload(), RecordingURLMessageUpdateRequest.class);
			logger.error(String.format("Error occurred in onErrorInUpdatingRecordingURLForMessage message. Re-queuing the message for message_id=%s ", 
					updateRecordingURLForMessage.getMessageID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),updateRecordingURLForMessage);
		} catch (Exception e2) {
			logger.error("error in onErrorInUpdatingRecordingURLForMessage ", e2);
			return null;
		}
	}
	
	public String onErrorInUpdatingRecordingURLForMessage(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			RecordingURLMessageUpdateRequest updateRecordingURLForMessage = mapper.readValue((String)e.getFailedMessage().getPayload(), RecordingURLMessageUpdateRequest.class);
			logger.info(String.format("Error occurred in onErrorInUpdatingRecordingURLForMessage message. Re-queuing the message for dealer_id=%s ", 
					updateRecordingURLForMessage.getMessageID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),updateRecordingURLForMessage);
		} catch (Exception e2) {
			logger.error("error in onErrorInUpdatingRecordingURLForMessage ", e2);
			return null;
		}
	}
	
	public String onErrorInMailingCustomerThread(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			CommunicationHistoryMailRequest commHistoryMailRequest = mapper.readValue((String)e.getFailedMessage().getPayload(), CommunicationHistoryMailRequest.class);
			logger.error(String.format("Error occurred in onErrorInMailingCustomerThread message. Re-queuing the message for customer_uuid=%s department_uuid=%s", 
					commHistoryMailRequest.getCustomerUUID(), commHistoryMailRequest.getDepartmentUUID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),commHistoryMailRequest);
		} catch (Exception e2) {
			logger.error("error in onErrorInUpdatingRecordingURLForMessage ", e2);
			return null;
		}
	}
	
	public String onErrorInMailingCustomerThread(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			CommunicationHistoryMailRequest commHistoryMailRequest = mapper.readValue((String)e.getFailedMessage().getPayload(), CommunicationHistoryMailRequest.class);
			logger.error(String.format("Error occurred in onErrorInMailingCustomerThread message. Re-queuing the message for customer_uuid=%s department_uuid=%s", 
					commHistoryMailRequest.getCustomerUUID(), commHistoryMailRequest.getDepartmentUUID()),e);
			return processFailedMessage(e.getFailedMessage().getHeaders(),commHistoryMailRequest);
		} catch (Exception e2) {
			logger.error("error in onErrorInUpdatingRecordingURLForMessage ", e2);
			return null;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, RecordingURLMessageUpdateRequest message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		logger.info("processFailedMessage expiration={}", message.getExpiration());
		return mapper.writeValueAsString(message);
		
	}
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, CommunicationHistoryMailRequest message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		logger.info("processFailedMessage expiration={}", message.getExpiration());
		return mapper.writeValueAsString(message);
		
	}
	
	public String updateExpirationForUpdatingRecordingURLForMessage(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		RecordingURLMessageUpdateRequest updateRecordingURLForMessage = mapper.readValue(message, RecordingURLMessageUpdateRequest.class);
		Integer newExpiration = null;
		if(updateRecordingURLForMessage.getExpiration() == null ) {
			newExpiration = this.intialexpirationrecording;
		}
		else {   
			double x = Math.log((double)(updateRecordingURLForMessage.getExpiration()/this.intialexpirationrecording));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			}
			else {
				newExpiration = updateRecordingURLForMessage.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForUpdatingRecordingURLForMessage for message_id=%s new_expiration=%s ", 
				updateRecordingURLForMessage.getMessageID(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	public String updateExpirationForMailingCustomerThread(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		CommunicationHistoryMailRequest communicationHistoryMailRequest = mapper.readValue(message, CommunicationHistoryMailRequest.class);
		Integer newExpiration = null;
		if(communicationHistoryMailRequest.getExpiration() == null ) {
			newExpiration = this.intialexpirationrecording;
		}
		else {   
			double x = Math.log((double)(communicationHistoryMailRequest.getExpiration()/this.intialexpirationrecording));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			}
			else {
				newExpiration = communicationHistoryMailRequest.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForMailingCustomerThread for customer_uuid=%s department_uuid=%s new_expiration=%s", 
				communicationHistoryMailRequest.getCustomerUUID(), communicationHistoryMailRequest.getDepartmentUUID(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	public String onErrorInMultipleMessageProcessing(org.springframework.integration.MessageRejectedException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		MultipleMessageSending multipleMessageSending = mapper.readValue((String)e.getFailedMessage().getPayload(), MultipleMessageSending.class);
		logger.error(String.format("onErrorInMultipleMessageProcessing Re-queuing the message for user_uuid=%s dealer_id=%s dealer_department_uuid=%s ", 
				multipleMessageSending.getUserUUID(), multipleMessageSending.getDealerDepartmentUUID()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(),multipleMessageSending);
	}
	
	public String onErrorInMultipleMessageProcessing(org.springframework.messaging.MessageHandlingException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		MultipleMessageSending multipleMessageSending = mapper.readValue((String)e.getFailedMessage().getPayload(), MultipleMessageSending.class);
		logger.error(String.format("onErrorInMultipleMessageProcessing Re-queuing the message for user_uuid=%s dealer_id=%s dealer_department_uuid=%s ", 
				multipleMessageSending.getUserUUID(), multipleMessageSending.getDealerDepartmentUUID()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(),multipleMessageSending);
	}
	
	public String onErrorVerifyCommunications(org.springframework.integration.MessageRejectedException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		CommunicationsVerification communicationsVerification = mapper.readValue((String)e.getFailedMessage().getPayload(), CommunicationsVerification.class);
		logger.error(String.format("onErrorVerifyCommunications Re-queuing the message for dealer_department_id=%s ", 
				communicationsVerification.getDepartmentId()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(),communicationsVerification);
	}
	

	public String onErrorVerifyCommunications(org.springframework.messaging.MessageHandlingException e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		CommunicationsVerification communicationsVerification = mapper.readValue((String)e.getFailedMessage().getPayload(), CommunicationsVerification.class);
		logger.error(String.format("onErrorVerifyCommunications Re-queuing the message for dealer_department_id=%s ", 
				communicationsVerification.getDepartmentId()),e);
		return processFailedMessage(e.getFailedMessage().getHeaders(),communicationsVerification);
	}
	
	@SuppressWarnings("unchecked")
	private String processFailedMessage(MessageHeaders messageHeaders, CommunicationsVerification communicationsVerification) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					communicationsVerification.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			communicationsVerification.setExpiration(null);
		}
		logger.info("processFailedMessage communicationsVerification expiration={}", communicationsVerification.getExpiration());
		return mapper.writeValueAsString(communicationsVerification);
	}
	
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, MultipleMessageSending multipleMessageSending) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					multipleMessageSending.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			multipleMessageSending.setExpiration(null);
		}
		
		return mapper.writeValueAsString(multipleMessageSending);
		
	}
	
	public String updateExpirationForMultipleMessageSending(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		MultipleMessageSending multipleMessageSending = mapper.readValue(message, MultipleMessageSending.class);
		Integer newExpiration = null;
		if(multipleMessageSending.getExpiration() == null ) {
			newExpiration = this.initialexpiration;
		}
		else {   
			double x = Math.log((double)(multipleMessageSending.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			}
			else {
				newExpiration = multipleMessageSending.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForMultipleMessageSending for request_uuid=%s user_uuid=%s new_expiration=%s ", 
				multipleMessageSending.getRequestUUID(), multipleMessageSending.getUserUUID(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
	
	public String updateExpirationForUpdatingCustomerSubscriptions(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		CustomerSubscriptionsUpdate customerSubscriptionsDelete = mapper.readValue(message, CustomerSubscriptionsUpdate.class);
		Integer newExpiration = null;
		if(customerSubscriptionsDelete.getExpiration() == null ) {
			newExpiration = this.intialexpirationrecording;
		}
		else {   
			double x = Math.log((double)(customerSubscriptionsDelete.getExpiration()/this.intialexpirationrecording));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			}
			else {
				newExpiration = customerSubscriptionsDelete.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForDeletingCustomerSubscriptions for customer_id=%s new_expiration=%s ", 
				customerSubscriptionsDelete.getCustomerId(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
		
	}
	
    public String onErrorInPreferredCommunicationModePrediction(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
            PreferredCommunicationModePrediction preferredCommunicationModePrediction = mapper.readValue((String)e.getFailedMessage().getPayload(), PreferredCommunicationModePrediction.class);
            Message message = preferredCommunicationModePrediction.getMessage();
			logger.error(String.format("Error occurred in preferred communication mode prediction. Re-queuing the message for dealer_id=%s message_uuid=%s ", 
                message.getDealerID(), message.getUuid()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), preferredCommunicationModePrediction);
		} catch (Exception e1) {
            logger.error("Error in onErrorInPreferredCommunicationModePrediction", e1);
            return null;
		}
	}
	
	public String onErrorInPreferredCommunicationModePrediction(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			PreferredCommunicationModePrediction preferredCommunicationModePrediction = mapper.readValue((String)e.getFailedMessage().getPayload(), PreferredCommunicationModePrediction.class);
            Message message = preferredCommunicationModePrediction.getMessage();
			logger.error(String.format("Error occurred in preferred communication mode prediction. Re-queuing the message for dealer_id=%s message_uuid=%s ", 
                message.getDealerID(), message.getUuid()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), preferredCommunicationModePrediction);
		} catch (Exception e1) {
			logger.error("Error in onErrorInPreferredCommunicationModePrediction", e1);
			return null;
		}
    }
    
    
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, PreferredCommunicationModePrediction message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		
		return mapper.writeValueAsString(message);
		
	}
	
    public String updateExpirationForPreferredCommunicationModePrediction(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        PreferredCommunicationModePrediction preferredCommunicationModePrediction = mapper.readValue(message, PreferredCommunicationModePrediction.class);
		Integer newExpiration = null;
		if(preferredCommunicationModePrediction.getExpiration() == null ) {
			logger.info("updateExpirationForPreferredCommunicationModePrediction initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {   
			logger.info("updateExpirationForPreferredCommunicationModePrediction current_expiration={} multiplies={}", preferredCommunicationModePrediction.getExpiration(), this.multiplier);
			double x = Math.log((double)(preferredCommunicationModePrediction.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForPreferredCommunicationModePrediction max retries reached for current_expiration={} message_uuid={} retries={} ", preferredCommunicationModePrediction.getExpiration(), preferredCommunicationModePrediction.getMessage().getUuid(), this.maximumretries);
				newExpiration = null;
			}
			else {
				newExpiration = preferredCommunicationModePrediction.getExpiration()*this.multiplier;
				logger.info("updateExpirationForPreferredCommunicationModePrediction current_expiration={} new_expiration={}", preferredCommunicationModePrediction.getExpiration(), newExpiration);
			}
		}
		logger.info(String.format("updateExpirationForPreferredCommunicationModePrediction for message_uuid=%s dealer_id=%s new_expiration=%s ", 
            preferredCommunicationModePrediction.getMessage().getUuid(), preferredCommunicationModePrediction.getMessage().getDealerID(), newExpiration));
        preferredCommunicationModePrediction.setExpiration(newExpiration);
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}
    
	public String updateExpirationForVerifyCommunications(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		CommunicationsVerification communicationsVerification = mapper.readValue(message, CommunicationsVerification.class);
		Integer newExpiration = null;
		if(communicationsVerification.getExpiration() == null ) {
			newExpiration = this.intialexpirationrecording;
		}
		else {   
			double x = Math.log((double)(communicationsVerification.getExpiration()/this.intialexpirationrecording));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			}
			else {
				newExpiration = communicationsVerification.getExpiration()*this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForVerifyCommunications for department_id=%s new_expiration=%s ", 
				communicationsVerification.getDepartmentId(), (newExpiration!=null?String.valueOf(newExpiration):null)));
		return newExpiration!=null?String.valueOf(newExpiration):null;
		
	}

	public String onErrorInOptOutStatusUpdate(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
            OptOutStatusUpdate optOutStatusUpdate = mapper.readValue((String)e.getFailedMessage().getPayload(), OptOutStatusUpdate.class);
			logger.error(String.format("Error occurred in optoutstatus update. Re-queuing the message for department_id=%s customer_id=%s ", 
				optOutStatusUpdate.getDealerDepartmentID(), optOutStatusUpdate.getCustomerID()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), optOutStatusUpdate);
		} catch (Exception e1) {
            logger.error("Error in onErrorInOptOutStatusUpdate", e1);
            return null;
		}
	}
	
	public String onErrorInOptOutStatusUpdate(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			OptOutStatusUpdate optOutStatusUpdate = mapper.readValue((String)e.getFailedMessage().getPayload(), OptOutStatusUpdate.class);
			logger.error(String.format("Error occurred in optoutstatus update. Re-queuing the message for department_id=%s customer_id=%s ", 
				optOutStatusUpdate.getDealerDepartmentID(), optOutStatusUpdate.getCustomerID()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), optOutStatusUpdate);
		} catch (Exception e1) {
			logger.error("Error in onErrorInOptOutStatusUpdate", e1);
			return null;
		}
    }
    
    
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, OptOutStatusUpdate message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		
		return mapper.writeValueAsString(message);
		
	}
	
    public String updateExpirationForOptOutStatusUpdate(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        OptOutStatusUpdate optOutStatusUpdate = mapper.readValue(message, OptOutStatusUpdate.class);
		Integer newExpiration = null;
		if(optOutStatusUpdate.getExpiration() == null ) {
			logger.info("updateExpirationForOptOutStatusUpdate initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {   
			logger.info("updateExpirationForOptOutStatusUpdate current_expiration={} multiplies={}", optOutStatusUpdate.getExpiration(), this.multiplier);
			double x = Math.log((double)(optOutStatusUpdate.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForOptOutStatusUpdate max retries reached for current_expiration={} department_id={} customer_id={} retries={} ", optOutStatusUpdate.getExpiration(), optOutStatusUpdate.getDealerDepartmentID(), optOutStatusUpdate.getCustomerID(), this.maximumretries);
				newExpiration = null;
			}
			else {
				newExpiration = optOutStatusUpdate.getExpiration()*this.multiplier;
				logger.info("updateExpirationForOptOutStatusUpdate current_expiration={} new_expiration={}", optOutStatusUpdate.getExpiration(), newExpiration);
			}
		}
		logger.info("updateExpirationForOptOutStatusUpdate for department_id={} customer_id={} new_expiration={} ", 
            optOutStatusUpdate.getDealerDepartmentID(), optOutStatusUpdate.getCustomerID(), newExpiration);
        optOutStatusUpdate.setExpiration(newExpiration);
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}

	public String onErrorInPostOptOutStatusUpdate(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
            PostOptOutStatusUpdate postOptOutStatusUpdate = mapper.readValue((String)e.getFailedMessage().getPayload(), PostOptOutStatusUpdate.class);
			logger.error(String.format("Error occurred in post optoutstatus update. Re-queuing the message for department_id=%s customer_id=%s ", 
				postOptOutStatusUpdate.getOptOutStatusUpdate().getDealerDepartmentID(), postOptOutStatusUpdate.getOptOutStatusUpdate().getCustomerID()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), postOptOutStatusUpdate);
		} catch (Exception e1) {
            logger.error("Error in onErrorInPostOptOutStatusUpdate", e1);
            return null;
		}
	}
	
	public String onErrorInPostOptOutStatusUpdate(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			PostOptOutStatusUpdate postOptOutStatusUpdate = mapper.readValue((String)e.getFailedMessage().getPayload(), PostOptOutStatusUpdate.class);
			logger.error(String.format("Error occurred in post optoutstatus update. Re-queuing the message for department_id=%s customer_id=%s ", 
				postOptOutStatusUpdate.getOptOutStatusUpdate().getDealerDepartmentID(), postOptOutStatusUpdate.getOptOutStatusUpdate().getCustomerID()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), postOptOutStatusUpdate);
		} catch (Exception e1) {
			logger.error("Error in onErrorInPostOptOutStatusUpdate", e1);
			return null;
		}
    }
    
    
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, PostOptOutStatusUpdate message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		
		return mapper.writeValueAsString(message);
		
	}
	
    public String updateExpirationForPostOptOutStatusUpdate(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        PostOptOutStatusUpdate postOptOutStatusUpdate = mapper.readValue(message, PostOptOutStatusUpdate.class);
		Integer newExpiration = null;
		if(postOptOutStatusUpdate.getExpiration() == null ) {
			logger.info("updateExpirationForPostOptOutStatusUpdate initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {   
			logger.info("updateExpirationForPostOptOutStatusUpdate current_expiration={} multiplies={}", postOptOutStatusUpdate.getExpiration(), this.multiplier);
			double x = Math.log((double)(postOptOutStatusUpdate.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForPostOptOutStatusUpdate max retries reached for current_expiration={} dealer_id={} customer_id={} retries={} ", postOptOutStatusUpdate.getExpiration(), postOptOutStatusUpdate.getOptOutStatusUpdate().getDealerDepartmentID(), postOptOutStatusUpdate.getOptOutStatusUpdate().getCustomerID(), this.maximumretries);
				newExpiration = null;
			}
			else {
				newExpiration = postOptOutStatusUpdate.getExpiration()*this.multiplier;
				logger.info("updateExpirationForPostOptOutStatusUpdate current_expiration={} new_expiration={}", postOptOutStatusUpdate.getExpiration(), newExpiration);
			}
		}
		logger.info("updateExpirationForPostOptOutStatusUpdate for department_id={} customer_id={} new_expiration={} ", 
            postOptOutStatusUpdate.getOptOutStatusUpdate().getDealerDepartmentID(), postOptOutStatusUpdate.getOptOutStatusUpdate().getCustomerID(), newExpiration);
        postOptOutStatusUpdate.setExpiration(newExpiration);
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}

	public String onErrorInDoubleOptinDeployment(org.springframework.integration.MessageRejectedException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
            DoubleOptInDeployment doubleOptInDeployment = mapper.readValue((String)e.getFailedMessage().getPayload(), DoubleOptInDeployment.class);
			logger.error(String.format("Error occurred in double optin deployment. Re-queuing the message for dealer_id=%s ", 
				doubleOptInDeployment.getDealerID()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), doubleOptInDeployment);
		} catch (Exception e1) {
            logger.error("Error in onErrorInDoubleOptinDeployment", e1);
            return null;
		}
	}
	
	public String onErrorInDoubleOptinDeployment(org.springframework.messaging.MessageHandlingException e) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
            DoubleOptInDeployment doubleOptInDeployment = mapper.readValue((String)e.getFailedMessage().getPayload(), DoubleOptInDeployment.class);
			logger.error(String.format("Error occurred in double optin deployment. Re-queuing the message for dealer_id=%s ", 
				doubleOptInDeployment.getDealerID()), e);
			return processFailedMessage(e.getFailedMessage().getHeaders(), doubleOptInDeployment);
		} catch (Exception e1) {
            logger.error("Error in onErrorInDoubleOptinDeployment", e1);
            return null;
		}
    }
    
    
	@SuppressWarnings("unchecked")
	public String processFailedMessage(MessageHeaders messageHeaders, DoubleOptInDeployment message) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					message.setExpiration(Integer.parseInt((String)death.get("original-expiration")));
				} 
			} 
		} else {
			message.setExpiration(null);
		}
		
		return mapper.writeValueAsString(message);
		
	}
	
    public String updateExpirationForDoubleOptinDeployment(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        DoubleOptInDeployment doubleOptinDeployment = mapper.readValue(message, DoubleOptInDeployment.class);
		Integer newExpiration = null;
		if(doubleOptinDeployment.getExpiration() == null ) {
			logger.info("updateExpirationForDoubleOptinDeployment initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {   
			logger.info("updateExpirationForDoubleOptinDeployment current_expiration={} multiplies={}", doubleOptinDeployment.getExpiration(), this.multiplier);
			double x = Math.log((double)(doubleOptinDeployment.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForDoubleOptinDeployment max retries reached for current_expiration={} dealer_id={}retries={} ", doubleOptinDeployment.getExpiration(), doubleOptinDeployment.getDealerID(), this.maximumretries);
				newExpiration = null;
			}
			else {
				newExpiration = doubleOptinDeployment.getExpiration()*this.multiplier;
				logger.info("updateExpirationForDoubleOptinDeployment current_expiration={} new_expiration={}", doubleOptinDeployment.getExpiration(), newExpiration);
			}
		}
		logger.info("updateExpirationForDoubleOptinDeployment for dealer_id={} new_expiration={} ", 
            doubleOptinDeployment.getDealerID(), newExpiration);
        doubleOptinDeployment.setExpiration(newExpiration);
		return newExpiration!=null?String.valueOf(newExpiration):null;
	}

	public String updateExpirationForOptinAwaitingMessageExpire(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		OptInAwaitingMessageExpire optInAwaitingMessageExpire = mapper.readValue(message, OptInAwaitingMessageExpire.class);
		Integer newExpiration = null;
		if(optInAwaitingMessageExpire.getExpiration() == null ) {
			logger.info("updateExpirationForOptinAwaitingMessageExpire initial_expiration={}", this.initialexpiration);
			newExpiration = this.initialexpiration;
		}
		else {
			logger.info("updateExpirationForOptinAwaitingMessageExpire current_expiration={} multiplies={}", optInAwaitingMessageExpire.getExpiration(), this.multiplier);
			double x = Math.log((double)(optInAwaitingMessageExpire.getExpiration()/this.initialexpiration));
			double y = Math.log((double)this.multiplier);
			long retryCount = Math.round(x/y);
			if((retryCount+1) >= this.maximumretries) {
				logger.info("updateExpirationForOptinAwaitingMessageExpire max retries reached for current_expiration={} message_uuid={} retries={} ",
					optInAwaitingMessageExpire.getExpiration(), optInAwaitingMessageExpire.getMessageUUID(), this.maximumretries);
				newExpiration = null;
			} else {
				newExpiration = optInAwaitingMessageExpire.getExpiration() * this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForOptinAwaitingMessageExpire for message_uuid=%s new_expiration=%s ",
			optInAwaitingMessageExpire.getMessageUUID(), (newExpiration != null ? String.valueOf(newExpiration) : null)));
		return newExpiration != null ? String.valueOf(newExpiration) : null;
	}

	public String updateExpirationForSavingHistoricalCommunications(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		SaveHistoricalMessageRequest saveHistoricalMessageRequest = mapper.readValue(message, SaveHistoricalMessageRequest.class);
		Integer newExpiration = null;
		if (saveHistoricalMessageRequest.getExpiration() == null) {
			newExpiration = this.intialexpirationrecording;
		} else {
			double x = Math.log((double) (saveHistoricalMessageRequest.getExpiration() / this.intialexpirationrecording));
			double y = Math.log((double) this.multiplier);
			long retryCount = Math.round(x / y);
			if ((retryCount + 1) >= this.maximumretries) {
				newExpiration = null;
			} else {
				newExpiration = saveHistoricalMessageRequest.getExpiration() * this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForSavingHistoricalCommunications for customer_uuid=%s source_uuid=%s new_expiration=%s ",
			saveHistoricalMessageRequest.getCustomerUuid(), saveHistoricalMessageRequest.getSaveMessageRequest().getSourceUuid(), (newExpiration != null ? String.valueOf(newExpiration) : null)));
		return newExpiration != null ? String.valueOf(newExpiration) : null;

	}

	public String updateExpirationForChangingThreadOwnerChangeOnROCreation(String message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		GlobalOrderTransitionUpdate globalOrderTransitionUpdate = mapper.readValue(message, GlobalOrderTransitionUpdate.class);
		Integer newExpiration = null;
		if (globalOrderTransitionUpdate.getExpiration() == null) {
			newExpiration = this.intialexpirationrecording;
		} else {
			double x = Math.log((double) (globalOrderTransitionUpdate.getExpiration() / this.intialexpirationrecording));
			double y = Math.log((double) this.multiplier);
			long retryCount = Math.round(x / y);
			if ((retryCount + 1) >= this.maximumretries) {
				newExpiration = null;
			} else {
				newExpiration = globalOrderTransitionUpdate.getExpiration() * this.multiplier;
			}
		}
		logger.info(String.format("updateExpirationForChangingThreadOwnerChangeOnROCreation for customer_uuid=%s department_uuid=%s new_expiration=%s ",
				globalOrderTransitionUpdate.getOrder().getCustomer().getUuid(), globalOrderTransitionUpdate.getDepartmentUuid(), (newExpiration != null ? String.valueOf(newExpiration) : null)));
		return newExpiration != null ? String.valueOf(newExpiration) : null;

	}
	
	public String onErrorInCustomerMongoMerge(Exception e) throws Exception {
		String message = null;
		MessageHeaders messageHeaders = null;
		if (e instanceof MessageHandlingException) {
			message = (String)((MessageHandlingException)e).getFailedMessage().getPayload();
			messageHeaders = ((MessageHandlingException)e).getFailedMessage().getHeaders();
		} else if (e instanceof MessageRejectedException) {
			message = (String)((MessageRejectedException)e).getFailedMessage().getPayload();
			messageHeaders = ((MessageRejectedException)e).getFailedMessage().getHeaders();
		}
		if (message != null) {
			logger.error("Error occurred in Customer merge. Re queuing the message={}", message, e);
			return processCustomerMergeFailedMessage(messageHeaders, message);
		}
		logger.error("Error occurred in Customer merge. message={}", message, e);
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public String processCustomerMergeFailedMessage(MessageHeaders messageHeaders, String message) throws Exception {
		JSONObject jsonObject = new JSONObject(message);
		if (messageHeaders.containsKey("x-death")) {
			List<HashMap<String, Object>> deathList = (List<HashMap<String, Object>>) messageHeaders
					.get("x-death");
			if (deathList.size() > 0) {
				HashMap<String, Object> death = deathList.get(0);
				if (death.containsKey("original-expiration")) {
					jsonObject.put("expiration",Integer.parseInt((String)death.get("original-expiration")));
					logger.info("message",message," original-expiration = "+death.get("original-expiration"));
				} 
			} 
		} else {
			jsonObject.put("expiration", JSONObject.NULL);
		}
		return jsonObject.toString();
	}
	
	public String updateExpirationForCustomerMerging(String message) throws Exception {
		JSONObject jsonObject = new JSONObject(message);
		Integer newExpiration = null;
		Object exp = jsonObject.get("expiration");
		if (exp == JSONObject.NULL ) {
			newExpiration = this.initiaExpirationCustomerMerging;
		} else {   
			double x = Math.log((double)((int)exp/this.initiaExpirationCustomerMerging));
			double y = Math.log((double)this.multiplierCustomerMerging);
			long retryCount = Math.round(x/y);
			if ((retryCount+1) >= this.maximumretries) {
				newExpiration = null;
			} else {
				newExpiration = jsonObject.getInt("expiration")*this.multiplierCustomerMerging;
			}
		}
		logger.info("updateExpirationForCustomerMerging message={} NewExpiration={} ", message, newExpiration);
		return newExpiration != null ? String.valueOf(newExpiration) : null;
	}
}
	

package com.mykaarma.kcommunications.mq.errorhandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Router;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessage;
import com.mykaarma.kcommunications.model.api.DelayedFilterRemovalRequest;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.rabbit.CustomerSubscriptionsUpdate;
import com.mykaarma.kcommunications.model.rabbit.DoubleOptInDeployment;
import com.mykaarma.kcommunications.model.rabbit.FetchCustomersDealer;
import com.mykaarma.kcommunications.model.rabbit.GlobalOrderTransitionUpdate;
import com.mykaarma.kcommunications.model.rabbit.MessageUpdateOnEvent;
import com.mykaarma.kcommunications.model.rabbit.MultipleMessageSending;
import com.mykaarma.kcommunications.model.rabbit.OptInAwaitingMessageExpire;
import com.mykaarma.kcommunications.model.rabbit.OptOutStatusUpdate;
import com.mykaarma.kcommunications.model.rabbit.PostIncomingMessageSave;
import com.mykaarma.kcommunications.model.rabbit.PostMessageReceived;
import com.mykaarma.kcommunications.model.rabbit.PostMessageSent;
import com.mykaarma.kcommunications.model.rabbit.PostOptOutStatusUpdate;
import com.mykaarma.kcommunications.model.rabbit.PostUniversalMessageSendPayload;
import com.mykaarma.kcommunications.model.rabbit.PreferredCommunicationModePrediction;
import com.mykaarma.kcommunications.model.rabbit.SaveHistoricalMessageRequest;
import com.mykaarma.kcommunications.model.utils.CommunicationsVerification;
import com.mykaarma.kcommunications_model.common.DealerMessagesFetchRequest;
import com.mykaarma.kcommunications_model.common.RecordingURLMessageUpdateRequest;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryMailRequest;


@MessageEndpoint
public class FailedMessageRouter 
{
	private static final Logger logger = LoggerFactory.getLogger(FailedMessageRouter.class);
	private static final String FromKCommunicationsProcessingRouterWait = "fromKCommunicationsProcessingRouterWait";
	private static final String FromKCommunicationsProcessingRouterFailed = "fromKCommunicationsProcessingRouterFailed";
	private static final String FromKCommunicationsPostMessageProcessingRouterWait = "fromKCommunicationsPostMessageProcessingRouterWait";
	private static final String FromKCommunicationsPostMessageProcessingRouterFailed = "fromKCommunicationsPostMessageProcessingRouterFailed";
	private static final String FromKCommunicationsPostIncomingMessageSaveProcessingRouterWait = "fromKCommunicationsPostIncomingMessageSaveProcessingRouterWait";
	private static final String FromKCommunicationsPostIncomingMessageSaveProcessingRouterFailed = "fromKCommunicationsPostIncomingMessageSaveProcessingRouterFailed";
	private static final String FromKCommunicationsEventProcessingRouterWait = "fromKCommunicationsEventProcessingRouterWait";
	private static final String FromKCommunicationsEventProcessingRouterFailed = "fromKCommunicationsEventProcessingRouterFailed";
	private static final String FromFecthingMessagesForDealerErrorMessageRouterWait = "fromFecthingMessagesForDealerErrorMessageRouterWait";
	private static final String FromFecthingMessagesForDealerErrorMessageRouterFailed = "fromFecthingMessagesForDealerErrorMessageRouterFailed";
	private static final String FromUpdatingRecordingUrlForMessageErrorMessageRouterWait = "fromUpdatingRecordingUrlForMessageErrorMessageRouterWait";
	private static final String FromUpdatingRecordingUrlForMessageErrorMessageRouterFailed = "fromUpdatingRecordingUrlForMessageErrorMessageRouterFailed";
	private static final String PostMessageReceivedErrorMessageRouterWait = "postMessageReceivedErrorMessageRouterWait";
	private static final String PostMessageReceivedErrorMessageRouterFailed = "postMessageReceivedErrorMessageRouterFailed";
	private static final String FromMultipleMessageSendingErrorMessageRouterWait = "fromMultipleMessageSendingErrorMessageRouterWait";
	private static final String FromMultipleMessageSendingErrorMessageRouterFailed = "fromMultipleMessageSendingErrorMessageRouterFailed";
	private static final String FromMailCustomerThreadErrorMessageRouterWait = "fromMailCustomerThreadErrorMessageRouterWait";
	private static final String FromMailCustomerThreadErrorMessageRouterFailed = "fromMailCustomerThreadErrorMessageRouterFailed";
	private static final String FromDelayedFilterUpdateErrorMessageRouterWait = "fromDelayedFilterUpdateErrorMessageRouterWait";
	private static final String FromDelayedFilterUpdateErrorMessageRouterFailed = "fromDelayedFilterUpdateErrorMessageRouterFailed";
	private static final String FromFecthingCustomersForDealerErrorMessageRouterWait = "fromFecthingCustomersForDealerErrorMessageRouterWait";
	private static final String FromFecthingCustomersForDealerErrorMessageRouterFailed = "fromFecthingCustomersForDealerErrorMessageRouterFailed";
	private static final String FromUpdatingCustomerSubscriptionsErrorMessageRouterWait = "fromUpdatingCustomerSubscriptionsErrorMessageRouterWait";
	private static final String FromUpdatingCustomerSubscriptionsErrorMessageRouterFailed = "fromUpdatingCustomerSubscriptionsErrorMessageRouterFailed";
    private static final String FromPreferredCommunicationModePredictionErrorMessageRouterWait = "fromPreferredCommunicationModePredictionErrorMessageRouterWait";
    private static final String FromPreferredCommunicationModePredictionErrorMessageRouterFailed = "fromPreferredCommunicationModePredictionErrorMessageRouterFailed";
	private static final String FromVerifyCommunicationsErrorMessageRouterWait = "fromVerifyCommunicationsErrorMessageRouterWait";
	private static final String FromVerifyCommunicationsErrorMessageRouterFailed = "fromVerifyCommunicationsErrorMessageRouterFailed";
	private static final String FromOptOutStatusUpdateErrorMessageRouterWait = "fromOptOutStatusUpdateErrorMessageRouterWait";
	private static final String FromOptOutStatusUpdateErrorMessageRouterFailed = "fromOptOutStatusUpdateErrorMessageRouterFailed";
	private static final String FromPostOptOutStatusUpdateErrorMessageRouterWait = "fromPostOptOutStatusUpdateErrorMessageRouterWait";
	private static final String FromPostOptOutStatusUpdateErrorMessageRouterFailed = "fromPostOptOutStatusUpdateErrorMessageRouterFailed";
	private static final String FromDoubleOptinDeploymentErrorMessageRouterWait = "fromDoubleOptinDeploymentErrorMessageRouterWait";
	private static final String FromDoubleOptinDeploymentErrorMessageRouterFailed = "fromDoubleOptinDeploymentErrorMessageRouterFailed";
	private static final String FromOptinAwaitingMessageExpireErrorMessageRouterWait = "fromOptinAwaitingMessageExpireErrorMessageRouterWait";
	private static final String FromOptinAwaitingMessageExpireErrorMessageRouterFailed = "fromOptinAwaitingMessageExpireErrorMessageRouterFailed";
	private static final String FromHistoricalCommunicationsSaveErrorMessageRouterWait = "fromHistoricalCommunicationsSaveErrorMessageRouterWait";
	private static final String FromHistoricalCommunicationsSaveErrorMessageRouterFailed = "fromHistoricalCommunicationsSaveErrorMessageRouterFailed";
	private static final String FromTemplateIndexingErrorMessageRouterWait = "fromTemplateIndexingErrorMessageRouterWait";
	private static final String FromTemplateIndexingErrorMessageRouterFailed = "fromTemplateIndexingErrorMessageRouterFailed";

	private static final String FromKCommunicationsPostUniversalMessageSendProcessingRouterWait = "fromKCommunicationsPostUniversalMessageSendProcessingRouterWait";
	private static final String FromKCommunicationsPostUniversalMessageSendProcessingRouterFailed = "fromKCommunicationsPostUniversalMessageSendProcessingRouterFailed";

	private static final String FromThreadOwnerChangeOnROCreationChannelErrorMessageRouterWait = "fromThreadOwnerChangeOnROCreationChannelErrorMessageRouterWait";
	private static final String FromThreadOwnerChangeOnROCreationChannelErrorMessageRouterFailed = "fromThreadOwnerChangeOnROCreationChannelErrorMessageRouterFailed";
	
	private static final String FromKCommunicationsMessageWithoutCustomerProcessingRouterWait = "fromKCommunicationsMessageWithoutCustomerProcessingRouterWait";
	private static final String FromKCommunicationsMessageWithoutCustomerProcessingRouterFailed = "fromKCommunicationsMessageWithoutCustomerProcessingRouterFailed";

	private static final String FromKCommunicationsPostIncomingBotMessageSaveRouterWait = "fromKCommunicationsPostIncomingMessageSaveRouterWait";
	private static final String FromKCommunicationsPostIncomingBotMessageSaveRouterFailed = "fromKCommunicationsPostIncomingMessageSaveRouterFailed";
	
	private static final String fromCustomerMongoMergeErrorMessageRouterFailed = "fromCustomerMongoMergeErrorMessageRouterFailed";
	private static final String fromCustomerMongoMergeErrorMessageRouterWait = "fromCustomerMongoMergeErrorMessageRouterWait";
	
	@Router(inputChannel="toThreadOwnerChangeOnROCreationChannelErrorHandler")
	public String processThreadOwnerChangeOnROCreationFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		GlobalOrderTransitionUpdate m = mapper.readValue(message, GlobalOrderTransitionUpdate.class);
		logger.info("processThreadOwnerChangeOnROCreationFailedMessageExpiration {} {}", m.getUuid(), messageHeaders);
		if (messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processMessageSendFailedMessageExpiration {} {}", m.getUuid(), expiration);
			if (expiration == null || expiration.isEmpty() || expiration.equals("null")) {
				return FromThreadOwnerChangeOnROCreationChannelErrorMessageRouterFailed;
			}
			return FromThreadOwnerChangeOnROCreationChannelErrorMessageRouterWait;
		} else {
			return FromThreadOwnerChangeOnROCreationChannelErrorMessageRouterFailed;
		}
	}
	
	@Router(inputChannel="toKCommunicationsProcessingErrorMessageRouter")
	public String processKCommunicationsProcessingFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Message m = mapper.readValue(message, Message.class);
		logger.info("processMessageSendFailedMessageExpiration {} {}", m.getUuid(), messageHeaders);
		if (messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processMessageSendFailedMessageExpiration {} {}", m.getUuid(), expiration);
			if (expiration == null || expiration.isEmpty() || expiration.equals("null")) {
				return FromKCommunicationsProcessingRouterFailed;
			}
			return FromKCommunicationsProcessingRouterWait;
		} else {
			return FromKCommunicationsProcessingRouterFailed;
		}
	}
	
	@Router(inputChannel="toKCommunicationsTemplateIndexingErrorMessageRouter")
	public String processKCommunicationsTemplateIndexingFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Message m = mapper.readValue(message, Message.class);
		logger.info("processKCommunicationsTemplateIndexingFailedMessageExpiration {} {}",m.getUuid(),messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processMessageSendFailedMessageExpiration {} {}",m.getUuid(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromTemplateIndexingErrorMessageRouterFailed;
			}
			return FromTemplateIndexingErrorMessageRouterWait;		
        }
        else {
        	return FromTemplateIndexingErrorMessageRouterFailed;
        }
	}
	
	@Router(inputChannel="toKCommunicationsPostMessageProcessingErrorMessageRouter")
	public String processKCommunicationsPostMessageProcessingFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostMessageSent postMessageSent = mapper.readValue(message, PostMessageSent.class);
		logger.info("processKCommunicationsPostMessageProcessingFailedMessageExpiration {} {}",postMessageSent.getMessage().getUuid(),messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processKCommunicationsPostMessageProcessingFailedMessageExpiration {} {}",postMessageSent.getMessage().getUuid(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromKCommunicationsPostMessageProcessingRouterFailed;
			}
			return FromKCommunicationsPostMessageProcessingRouterWait;		
        }
        else {
        	return FromKCommunicationsPostMessageProcessingRouterFailed;
        }
	}

	@Router(inputChannel="toKCommunicationsPostIncomingMessageSaveProcessingErrorMessageRouter")
	public String processKCommunicationsPostIncomingMessageSaveProcessingFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostIncomingMessageSave postIncomingMessageSave = mapper.readValue(message, PostIncomingMessageSave.class);
		logger.info("processKCommunicationsPostIncomingMessageSaveProcessingFailedMessageExpiration {} {}",postIncomingMessageSave.getMessage().getUuid(),messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processKCommunicationsPostIncomingMessageSaveProcessingFailedMessageExpiration {} {}",postIncomingMessageSave.getMessage().getUuid(),expiration);

			if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromKCommunicationsPostIncomingMessageSaveProcessingRouterFailed;
			}
			return FromKCommunicationsPostIncomingMessageSaveProcessingRouterWait;
		}
		else {
			return FromKCommunicationsPostIncomingMessageSaveProcessingRouterFailed;
		}
	}

	@Router(inputChannel="toKCommunicationsPostIncomingBotMessageSaveErrorMessageRouter")
	public String processKCommunicationsPostIncomingBotMessageSaveFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		logger.info("processKCommunicationsPostIncomingBotMessageSaveFailedMessageExpiration {} {}", message,messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processKCommunicationsPostIncomingBotMessageSaveFailedMessageExpiration {} {}", message, expiration);

			if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromKCommunicationsPostIncomingBotMessageSaveRouterFailed;
			}
			return FromKCommunicationsPostIncomingBotMessageSaveRouterWait;
		}
		else {
			return FromKCommunicationsPostIncomingBotMessageSaveRouterFailed;
		}
	}

	@Router(inputChannel="toKCommunicationsPostUniversalMessageSendProcessingErrorMessageRouter")
	public String processKCommunicationsPostUniversalMessageSendProcessingFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostUniversalMessageSendPayload postUniversalMessageSendPayload = mapper.readValue(message, PostUniversalMessageSendPayload.class);
		logger.info("processKCommunicationsPostUniversalMessageSendProcessingFailedMessageExpiration {} {}",postUniversalMessageSendPayload.getMessage().getUuid(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processKCommunicationsPostUniversalMessageSendProcessingFailedMessageExpiration {} {}", postUniversalMessageSendPayload.getMessage().getUuid(), expiration);

			if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromKCommunicationsPostUniversalMessageSendProcessingRouterFailed;
			}
			return FromKCommunicationsPostUniversalMessageSendProcessingRouterWait;
		}
		else {
			return FromKCommunicationsPostUniversalMessageSendProcessingRouterFailed;
		}
	}
	
	@Router(inputChannel="toKCommunicationsEventProcessingErrorMessageRouter")
	public String processKCommunicationsEventProcessingFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		MessageUpdateOnEvent messageUpdateOnEvent = mapper.readValue(message, MessageUpdateOnEvent.class);
		logger.info("processKCommunicationsEventProcessingFailedMessageExpiration message_uuid={} header={}",messageUpdateOnEvent.getMessageUUID(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.info("processKCommunicationsFetchMessagesForDealerFailedMessageExpiration message_uuid={} expiration={}",messageUpdateOnEvent.getMessageUUID(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromKCommunicationsEventProcessingRouterFailed;
			}
			return FromKCommunicationsEventProcessingRouterWait;		
        }
        else {
        	return FromKCommunicationsEventProcessingRouterFailed;
        }
	}
	
	@Router(inputChannel="toFecthingMessagesForDealerErrorMessageRouter")
	public String processKCommunicationsFetchMessagesForDealerFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		DealerMessagesFetchRequest messagesForDealer = mapper.readValue(message, DealerMessagesFetchRequest.class);
		logger.info("processFecthingMessagesForDealerErrorMessageExpiration dealer_id={} header={}",messagesForDealer.getDealerID(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.info("processFecthingMessagesForDealerErrorMessageExpiration dealer_id={} expiration={}",messagesForDealer.getDealerID(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromFecthingMessagesForDealerErrorMessageRouterFailed;
			}
			return FromFecthingMessagesForDealerErrorMessageRouterWait;		
        }
        else {
        	return FromFecthingMessagesForDealerErrorMessageRouterFailed;
        }
	}
	
	@Router(inputChannel="toUpdatingRecordingUrlForMessageErrorMessageRouter")
	public String processKCommunicationsUpdatingRecordingURLForMessageFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		RecordingURLMessageUpdateRequest updateRecordingURLForMessage = mapper.readValue(message, RecordingURLMessageUpdateRequest.class);
		logger.info("processUpdatingRecordingUrlForMessageExpiration message_id={} header={}",updateRecordingURLForMessage.getMessageID(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.info("processUpdatingRecordingUrlForMessageExpiration message_id={} expiration={}",updateRecordingURLForMessage.getMessageID(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromUpdatingRecordingUrlForMessageErrorMessageRouterFailed;
			}
			return FromUpdatingRecordingUrlForMessageErrorMessageRouterWait;		
        }
        else {
        	return FromUpdatingRecordingUrlForMessageErrorMessageRouterFailed;
        }
	}
	
	@Router(inputChannel="toMailCustomerThreadErrorMessageRouter")
	public String processMailingCustomerThreadFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		CommunicationHistoryMailRequest communicationHistoryMailRequest = mapper.readValue(message, CommunicationHistoryMailRequest.class);
		logger.info("processMailingCustomerThreadFailedMessageExpiration customer_uuid={} department_uuid={} header={}",communicationHistoryMailRequest.getCustomerUUID(), communicationHistoryMailRequest.getDepartmentUUID(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.info("processMailingCustomerThreadFailedMessageExpiration customer_uuid={} department_uuid={} expiration={}",communicationHistoryMailRequest.getCustomerUUID(), communicationHistoryMailRequest.getDepartmentUUID(), expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromMailCustomerThreadErrorMessageRouterFailed;
			}
			return FromMailCustomerThreadErrorMessageRouterWait;		
        }
        else {
        	return FromMailCustomerThreadErrorMessageRouterFailed;
        }
	}
	
	@Router(inputChannel="postMessageReceivedErrorMessageRouter")
	public String processPostMessageReceivedFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PostMessageReceived postMessageReceived = mapper.readValue(message, PostMessageReceived.class);
		logger.info("processPostMessageReceivedFailedMessageExpiration dealer_id={} message_uuid={} header={}",postMessageReceived.getDealerID(),postMessageReceived.getMessageUUID(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.info("processPostMessageReceivedFailedMessageExpiration dealer_id={} message_uuid={} expiration={}",postMessageReceived.getDealerID(),postMessageReceived.getMessageUUID(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return PostMessageReceivedErrorMessageRouterFailed;
			}
			return PostMessageReceivedErrorMessageRouterWait;		
        }
        else {
        	return PostMessageReceivedErrorMessageRouterFailed;
        }
	}

	@Router(inputChannel="toMultipleMessageSendingErrorMessageRouter")
	public String processMultipleMessageSendingFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		MultipleMessageSending m = mapper.readValue(message, MultipleMessageSending.class);
		logger.info("processMultipleMessageSendingFailedMessageExpiration request_uuid={} {}",m.getRequestUUID(),messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processMultipleMessageSendingFailedMessageExpiration request_uuid={} {}",m.getRequestUUID(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromMultipleMessageSendingErrorMessageRouterFailed;
			}
			return FromMultipleMessageSendingErrorMessageRouterWait;		
        }
        else {
        	return FromMultipleMessageSendingErrorMessageRouterFailed;
        }
	}
	
	@Router(inputChannel="toDelayedFilterUpdateChannelErrorHandlerErrorMessageRouter")
	public String processDelayedFilterUpdateMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		DelayedFilterRemovalRequest delayedFilterRemovalRequest = mapper.readValue(message, DelayedFilterRemovalRequest.class);
		logger.info("processDelayedFilterUpdateMessageExpiration from_dealer_id={} to_dealer_id={} offset={} {} ", 
				delayedFilterRemovalRequest.getFromDealerID(),delayedFilterRemovalRequest.getToDealerID(),delayedFilterRemovalRequest.getOffset(),messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processDelayedFilterUpdateMessageExpiration from_dealer_id={} to_dealer_id={} offset={} {}",
					delayedFilterRemovalRequest.getFromDealerID(),delayedFilterRemovalRequest.getToDealerID(),delayedFilterRemovalRequest.getOffset(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromDelayedFilterUpdateErrorMessageRouterFailed;
			}
			return FromDelayedFilterUpdateErrorMessageRouterWait;		
        }
        else {
        	return FromDelayedFilterUpdateErrorMessageRouterFailed;
        }
	}
	
	@Router(inputChannel="toFecthingCustomersForDealerErrorMessageRouter")
	public String processKCommunicationsFetchCustomersForDealerFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		FetchCustomersDealer fetchCustomersDealer = mapper.readValue(message, FetchCustomersDealer.class);
		logger.info("processFecthingCustomersForDealerErrorMessageExpiration dealer_id={} header={}",fetchCustomersDealer.getDealerId(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.info("processFecthingCustomersForDealerErrorMessageExpiration dealer_id={} expiration={}",fetchCustomersDealer.getDealerId(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromFecthingCustomersForDealerErrorMessageRouterFailed;
			}
			return FromFecthingCustomersForDealerErrorMessageRouterWait;		
        }
        else {
        	return FromFecthingCustomersForDealerErrorMessageRouterFailed;
        }
	}

	@Router(inputChannel="toUpdatingCustomerSubscriptionsErrorMessageRouter")
	public String processKCommunicationsDeletingCustomerSubscriptionsFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		CustomerSubscriptionsUpdate customerSubscriptionsDelete = mapper.readValue(message, CustomerSubscriptionsUpdate.class);
		logger.info("processDeletingCustomerSubscriptionsErrorMessageExpiration customer_id={} header={}",customerSubscriptionsDelete.getCustomerId(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.info("processDeletingCustomerSubscriptionsErrorMessageExpiration customer_id={} expiration={}",customerSubscriptionsDelete.getCustomerId(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromUpdatingCustomerSubscriptionsErrorMessageRouterFailed;
			}
			return FromUpdatingCustomerSubscriptionsErrorMessageRouterWait;		
        }
        else {
        	return FromUpdatingCustomerSubscriptionsErrorMessageRouterFailed;
        }
	}

    @Router(inputChannel="toPreferredCommunicationModePredictionErrorMessageRouter")
	public String processPreferredCommunicationModePredictionFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        PreferredCommunicationModePrediction preferredCommunicationModePrediction = mapper.readValue(message, PreferredCommunicationModePrediction.class);
        Message m = preferredCommunicationModePrediction.getMessage();
		logger.info("processPreferredCommunicationModePredictionFailedMessageExpiration {} {}",m.getUuid(),messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processPreferredCommunicationModePredictionFailedMessageExpiration {} {}",m.getUuid(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromPreferredCommunicationModePredictionErrorMessageRouterFailed;
			}
			return FromPreferredCommunicationModePredictionErrorMessageRouterWait;		
        }
        else {
        	return FromPreferredCommunicationModePredictionErrorMessageRouterFailed;
        }
    }
	
	@Router(inputChannel="toVerifyCommunicationsErrorMessageRouter")
	public String processKCommunicationsVerificationFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		CommunicationsVerification communicationsVerification = mapper.readValue(message, CommunicationsVerification.class);
		logger.info("processKCommunicationsVerificationFailedMessageExpiration department_id={} header={}",communicationsVerification.getDepartmentId(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.info("processKCommunicationsVerificationFailedMessageExpiration department_id={} expiration={}",communicationsVerification.getDepartmentId(),expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromVerifyCommunicationsErrorMessageRouterFailed;
			}
			return FromVerifyCommunicationsErrorMessageRouterWait;		
        }
        else {
        	return FromVerifyCommunicationsErrorMessageRouterFailed;
        }
	}

	@Router(inputChannel="toOptOutStatusUpdateErrorMessageRouter")
	public String processOptOutStatusUpdateFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        OptOutStatusUpdate optOutStatusUpdate = mapper.readValue(message, OptOutStatusUpdate.class);
        logger.info("processOptOutStatusUpdateFailedMessageExpiration {} {} {}", optOutStatusUpdate.getDealerDepartmentID(), optOutStatusUpdate.getCustomerID(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processOptOutStatusUpdateFailedMessageExpiration {} {} {}", optOutStatusUpdate.getDealerDepartmentID(), optOutStatusUpdate.getCustomerID(), expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromOptOutStatusUpdateErrorMessageRouterFailed;
			}
			return FromOptOutStatusUpdateErrorMessageRouterWait;		
        }
        else {
        	return FromOptOutStatusUpdateErrorMessageRouterFailed;
        }
    }

	@Router(inputChannel="toPostOptOutStatusUpdateErrorMessageRouter")
	public String processPostOptOutStatusUpdateFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        PostOptOutStatusUpdate postOptOutStatusUpdate = mapper.readValue(message, PostOptOutStatusUpdate.class);
        logger.info("processPostOptOutStatusUpdateFailedMessageExpiration {} {} {}", postOptOutStatusUpdate.getOptOutStatusUpdate().getDealerDepartmentID(), postOptOutStatusUpdate.getOptOutStatusUpdate().getCustomerID(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processPostOptOutStatusUpdateFailedMessageExpiration {} {} {}", postOptOutStatusUpdate.getOptOutStatusUpdate().getDealerDepartmentID(), postOptOutStatusUpdate.getOptOutStatusUpdate().getCustomerID(), expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromPostOptOutStatusUpdateErrorMessageRouterFailed;
			}
			return FromPostOptOutStatusUpdateErrorMessageRouterWait;		
        }
        else {
        	return FromPostOptOutStatusUpdateErrorMessageRouterFailed;
        }
    }

	@Router(inputChannel="toDoubleOptinDeploymentErrorMessageRouter")
	public String processDoubleOptinDeploymentFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        DoubleOptInDeployment doubleOptInDeployment = mapper.readValue(message, DoubleOptInDeployment.class);
        logger.info("processDoubleOptinDeploymentFailedMessageExpiration {} {}", doubleOptInDeployment.getDealerID(), messageHeaders);
		if(messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processDoubleOptinDeploymentFailedMessageExpiration {} {}", doubleOptInDeployment.getDealerID(), expiration);
		 	if(expiration==null || expiration.isEmpty() || expiration.equals("null")) {
				return FromDoubleOptinDeploymentErrorMessageRouterFailed;
			}
			return FromDoubleOptinDeploymentErrorMessageRouterWait;		
        }
        else {
        	return FromDoubleOptinDeploymentErrorMessageRouterFailed;
        }
    }

	@Router(inputChannel="toOptinAwaitingMessageExpireErrorMessageRouter")
	public String processOptinAwaitingMessageExpireMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		OptInAwaitingMessageExpire optInAwaitingMessageExpire = mapper.readValue(message, OptInAwaitingMessageExpire.class);
		logger.info("processDoubleOptinDeploymentFailedMessageExpiration {} {}", optInAwaitingMessageExpire.getMessageUUID(), messageHeaders);
		if (messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processDoubleOptinDeploymentFailedMessageExpiration {} {}", optInAwaitingMessageExpire.getMessageUUID(), expiration);
			if (expiration == null || expiration.isEmpty() || expiration.equals("null")) {
				return FromOptinAwaitingMessageExpireErrorMessageRouterFailed;
			}
			return FromOptinAwaitingMessageExpireErrorMessageRouterWait;
		} else {
			return FromOptinAwaitingMessageExpireErrorMessageRouterFailed;
		}
	}

	@Router(inputChannel = "toHistoricalCommunicationsSaveErrorMessageRouter")
	public String processHistoricalMessageFailedMessageExpiration(@Payload String message, @Headers MessageHeaders
		messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		SaveHistoricalMessageRequest saveHistoricalMessageRequest = mapper.readValue(message, SaveHistoricalMessageRequest.class);
		logger.info("processHistoricalMessageFailedMessageExpiration customer_uuid={} source_uuid={} header={}", saveHistoricalMessageRequest.getCustomerUuid(),
			saveHistoricalMessageRequest.getSaveMessageRequest().getSourceUuid(), messageHeaders);
		if (messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.info("processHistoricalMessageFailedMessageExpiration customer_uuid={} source_uuid={} expiration={}", saveHistoricalMessageRequest.getCustomerUuid(),
				saveHistoricalMessageRequest.getSaveMessageRequest().getSourceUuid(), expiration);
			if (expiration == null || expiration.isEmpty() || expiration.equals("null")) {
				return FromHistoricalCommunicationsSaveErrorMessageRouterFailed;
			}
			return FromHistoricalCommunicationsSaveErrorMessageRouterWait;
		} else {
			return FromHistoricalCommunicationsSaveErrorMessageRouterFailed;
		}
	}
	
	@Router(inputChannel="toKCommunicationsMessageWithoutCustomerProcessingErrorMessageRouter")
	public String processKCommunicationsMessageWithoutCustomerProcessingFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		ExternalMessage m = mapper.readValue(message, ExternalMessage.class);
		logger.info("processKCommunicationsMessageWithoutCustomerProcessingFailedMessageExpiration {} {}", m.getUuid(), messageHeaders);
		if (messageHeaders.containsKey("amqp_expiration")) {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			logger.debug("processKCommunicationsMessageWithoutCustomerProcessingFailedMessageExpiration {} {}", m.getUuid(), expiration);
			if (expiration == null || expiration.isEmpty() || expiration.equals("null")) {
				return FromKCommunicationsMessageWithoutCustomerProcessingRouterFailed;
			}
			return FromKCommunicationsMessageWithoutCustomerProcessingRouterWait;
		} else {
			return FromKCommunicationsMessageWithoutCustomerProcessingRouterFailed;
		}
	}
	
	@Router(inputChannel="toCustomerMongoMergeErrorMessageRouter")
	public String processCustomerMongoMergeFailedMessageExpiration(@Payload String message, @Headers MessageHeaders messageHeaders) throws Exception
	{
		if(messageHeaders.containsKey("amqp_expiration"))
        {
			String expiration = (String) messageHeaders.get("amqp_expiration");
			if(expiration==null || expiration.isEmpty() || expiration.equals("null"))
			{
				return fromCustomerMongoMergeErrorMessageRouterFailed;
			}
			return fromCustomerMongoMergeErrorMessageRouterWait;
			
        }
        else
        {
        	return fromCustomerMongoMergeErrorMessageRouterFailed;
        }
	}
}

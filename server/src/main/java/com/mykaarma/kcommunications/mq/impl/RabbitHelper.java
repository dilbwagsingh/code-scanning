package com.mykaarma.kcommunications.mq.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessage;
import com.mykaarma.kcommunications.model.api.DelayedFilterRemovalRequest;
import com.mykaarma.kcommunications.model.jpa.BotMessage;
import com.mykaarma.kcommunications.model.rabbit.CustomerSubscriptionsUpdate;
import com.mykaarma.kcommunications.model.rabbit.DoubleOptInDeployment;
import com.mykaarma.kcommunications.model.rabbit.FetchCustomersDealer;
import com.mykaarma.kcommunications.model.rabbit.MessageSavingQueueData;
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
import com.mykaarma.kcommunications.utils.RabbitQueueInfo;
import com.mykaarma.kcommunications.utils.TemplateType;
import com.mykaarma.kcommunications_model.common.DealerMessagesFetchRequest;
import com.mykaarma.kcommunications_model.common.RecordingURLMessageUpdateRequest;
import com.mykaarma.kcommunications_model.common.User;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryMailRequest;

@Service
public class RabbitHelper {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(RabbitHelper.class);
	private final static ObjectMapper objectMapper = new ObjectMapper();
	
	@Autowired
	RabbitTemplate rabbitTemplate;

	public void pushToMessageSendingQueue(com.mykaarma.kcommunications.model.jpa.Message message, Integer delay) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(message);
		LOGGER.info(String.format("push message to sending queue for message_uuid=%s message_json=%s ", message.getUuid(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.MESSAGE_SENDING_QUEUE.getExchangeName(),RabbitQueueInfo.MESSAGE_SENDING_QUEUE.getQueueKey(), requestJSON,
				new MessagePostProcessor() {

			public Message postProcessMessage(Message message) throws AmqpException {
				message.getMessageProperties().setDelay(delay);
				return message;
			}


		});		
		LOGGER.info(String.format("successfuly pushed message to sending queue fors message_uuid=%s", message.getUuid()));
	}
	
	public void pushToTemplateIndexingQueue(TemplateType templateType, String templateUuid) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		TemplateIndexingRequest templateIndexingRequest=new TemplateIndexingRequest();
		templateIndexingRequest.setTemplateType(templateType);
		templateIndexingRequest.setTemplateUuid(templateUuid);
		String requestJSON = mapper.writeValueAsString(templateIndexingRequest);
		LOGGER.info(String.format("push message to template indexing queue for template_uuid=%s message_json=%s ", templateUuid, requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.TEMPLATE_INDEXING_QUEUE.getExchangeName(),RabbitQueueInfo.TEMPLATE_INDEXING_QUEUE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to template indexing queue fors template_uuid=%s", templateUuid));
	}
	
	public void pushToMessageSavingQueue(com.mykaarma.kcommunications.model.jpa.Message message, Boolean updateThreadTimestamp, List<User> usersToNotify) throws Exception {
		MessageSavingQueueData data = new MessageSavingQueueData();
		data.setMessage(message);
		data.setUpdateThreadTimestamp(updateThreadTimestamp);
		data.setUsersToNotify(usersToNotify);
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(data);
		LOGGER.info(String.format("push message to saving queue for message_uuid=%s message_json=%s ", message.getUuid(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.MESSAGE_SAVING_QUEUE.getExchangeName(),RabbitQueueInfo.MESSAGE_SAVING_QUEUE.getQueueKey(), data);
		LOGGER.info(String.format("successfuly pushed message to saving queue for message_uuid=%s", message.getUuid()));
	}
	
	public void pushToMessagePostSendingQueue(com.mykaarma.kcommunications.model.jpa.Message message, Long threadDelegatee,
  			Boolean postMessageProcessingToBeDone, Boolean isEditedDraft, Boolean isFailedMessage, Boolean updateThreadTimestamp) throws Exception {
		PostMessageSent postMessageSent = new PostMessageSent();
		postMessageSent.setMessage(message);
		postMessageSent.setThreadDelegatee(threadDelegatee);
        postMessageSent.setPostMessageProcessingToBeDone(postMessageProcessingToBeDone);
        postMessageSent.setIsEditedDraft(isEditedDraft);
        postMessageSent.setIsFailedMessage(isFailedMessage);
        postMessageSent.setUpdateThreadTimestamp(updateThreadTimestamp);
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(postMessageSent);
		LOGGER.info(String.format("push message to post message sending queue for message_uuid=%s message_json=%s ", message.getUuid(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.POST_MESSAGE_SENDING_QUEUE.getExchangeName(),RabbitQueueInfo.POST_MESSAGE_SENDING_QUEUE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to post message sending queue for message_uuid=%s", message.getUuid()));
	}
	
	public void pushMessageToEventProcessingQueue(MessageUpdateOnEvent messageUpdateOnEvent) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(messageUpdateOnEvent);
		LOGGER.info(String.format("push message to event processing queue for message_uuid=%s message_json=%s ", messageUpdateOnEvent.getMessageUUID(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.EVENT_PROCESSING_QUEUE.getExchangeName(),RabbitQueueInfo.EVENT_PROCESSING_QUEUE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to event processing queue for message_uuid=%s", messageUpdateOnEvent.getMessageUUID()));
	}
	
	public void pushDataToFetchMessageForDealerQueue(DealerMessagesFetchRequest messagesForDealer) throws Exception{
		
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(messagesForDealer);
		LOGGER.info(String.format("push message to queue=%s for message_json=%s ", RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_DEALER.getQueueName(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_DEALER.getExchangeName(),RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_DEALER.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to queue=%s for message_json=%s ", RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_DEALER.getQueueName(), requestJSON));
		
	}
	
	public void pushDataToTakePostMessageReceivedActions(PostMessageReceived postMessageReceived) throws Exception{
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(postMessageReceived);
		LOGGER.info(String.format("push message to queue=%s for message_json=%s ", RabbitQueueInfo.POST_MESSAGE_RECEIVED_QUEUE.getQueueName(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.POST_MESSAGE_RECEIVED_QUEUE.getExchangeName(),RabbitQueueInfo.POST_MESSAGE_RECEIVED_QUEUE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to queue=%s for message_json=%s ", RabbitQueueInfo.POST_MESSAGE_RECEIVED_QUEUE.getQueueName(), requestJSON));
	}
	
	public void pushDataToUpdateURLForMessage(RecordingURLMessageUpdateRequest updateRecordingUrl) throws Exception{
		
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(updateRecordingUrl);
		LOGGER.info(String.format("push message to queue=%s for message_json=%s ", RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE.getQueueName(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE.getExchangeName(),RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to queue=%s for message_json=%s ", RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE.getQueueName(), requestJSON));
		
	}
	
	public void pushDataToUpdateURLForMessageDelayedQueue(RecordingURLMessageUpdateRequest updateRecordingUrl) throws Exception{
		
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(updateRecordingUrl);
		LOGGER.info(String.format("push message to queue=%s for message_json=%s ", RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE_DELAYED.getQueueName(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE_DELAYED.getExchangeName(),RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE_DELAYED.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to queue=%s for message_json=%s ", RabbitQueueInfo.UPDATE_RECORDING_URL_FOR_MESSAGE_DELAYED.getQueueName(), requestJSON));
		
	}
	
	public void pushDataToMultipleMessageSendingRequestDelayedQueue(MultipleMessageSending multipleMessageSending, Integer delay) throws Exception{
		
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(multipleMessageSending);
		LOGGER.info(String.format("pushDataToMultipleMessageSendingRequestDelayedQueue message_json=%s delay=%s ", requestJSON, delay));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.MULTIPLE_MESSAGE_SENDING_QUEUE.getExchangeName(),RabbitQueueInfo.MULTIPLE_MESSAGE_SENDING_QUEUE.getQueueKey(), requestJSON,
				new MessagePostProcessor() {

			public Message postProcessMessage(Message message) throws AmqpException {
				message.getMessageProperties().setDelay(delay);
				return message;
			}


		});		
		LOGGER.info(String.format("pushDataToMultipleMessageSendingRequestDelayedQueue successfuly pushed message to sending queue for"
				+ " request_uuid=%s", multipleMessageSending.getRequestUUID()));
	
	}

	public void pushDataToMailCustomerThreadQueue(CommunicationHistoryMailRequest communicationHistoryMailRequest) throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(communicationHistoryMailRequest);
		LOGGER.info(String.format("push message to queue=%s for message_json=%s ", RabbitQueueInfo.MAILT_CUSTOMER_THREAD.getQueueName(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.MAILT_CUSTOMER_THREAD.getExchangeName(),RabbitQueueInfo.MAILT_CUSTOMER_THREAD.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to queue=%s for message_json=%s ", RabbitQueueInfo.MAILT_CUSTOMER_THREAD.getQueueName(), requestJSON));
		
	}
	
	public void pushDataForDelayedFilterRemoval(DelayedFilterRemovalRequest delayedFilterRemovalRequest) throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(delayedFilterRemovalRequest);
		LOGGER.info(String.format("push message to queue=%s for message_json=%s ", RabbitQueueInfo.MAILT_CUSTOMER_THREAD.getQueueName(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.DELAYED_FILTER_UPDATE.getExchangeName(),RabbitQueueInfo.DELAYED_FILTER_UPDATE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to queue=%s for message_json=%s ", RabbitQueueInfo.DELAYED_FILTER_UPDATE.getQueueName(), requestJSON));
		
	}

	public void pushDatatoSubscriptionUpdateForDealer(FetchCustomersDealer fetchCustomersDealer) throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(fetchCustomersDealer);
		LOGGER.info(String.format("push message to queue=%s for message_json=%s ", RabbitQueueInfo.SUBSCRIPTION_DEALER_UPDATE.getQueueName(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.SUBSCRIPTION_DEALER_UPDATE.getExchangeName(),RabbitQueueInfo.SUBSCRIPTION_DEALER_UPDATE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to queue=%s for message_json=%s ", RabbitQueueInfo.SUBSCRIPTION_DEALER_UPDATE.getQueueName(), requestJSON));
	}

	public void pushDataToDeleteCustomerSubscriptionsQueue(CustomerSubscriptionsUpdate customerSubscriptionsDelete) throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(customerSubscriptionsDelete);
		LOGGER.info(String.format("push message to queue=%s for message_json=%s ", RabbitQueueInfo.SUBSCRIPTION_DEALER_UPDATE.getQueueName(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.SUBSCRIPTION_CUSTOMER_UPDATE.getExchangeName(),RabbitQueueInfo.SUBSCRIPTION_CUSTOMER_UPDATE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to queue=%s for message_json=%s ", RabbitQueueInfo.SUBSCRIPTION_CUSTOMER_UPDATE.getQueueName(), requestJSON));
	}

	public void pushDataForVerifyCommunicationsBilling(CommunicationsVerification communicationsVerification) throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(communicationsVerification);
		LOGGER.info(String.format("push message to queue=%s for message_json=%s ", RabbitQueueInfo.VERIIFY_COMMUNICATIONS.getQueueName(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.VERIIFY_COMMUNICATIONS.getExchangeName(),RabbitQueueInfo.VERIIFY_COMMUNICATIONS.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to queue=%s for message_json=%s ", RabbitQueueInfo.VERIIFY_COMMUNICATIONS.getQueueName(), requestJSON));
		
	}
	
	public void pushToPreferredCommunicationModePredictionQueue(PreferredCommunicationModePrediction preferredCommunicationModePrediction) throws Exception {    
    
        ObjectMapper mapper = new ObjectMapper();
        String requestJSON = mapper.writeValueAsString(preferredCommunicationModePrediction);
        LOGGER.info(String.format("push message to preferred communication mode prediction queue for message_uuid=%s message_json=%s ", preferredCommunicationModePrediction.getMessage().getUuid(), requestJSON));
        rabbitTemplate.convertAndSend(RabbitQueueInfo.PREFERRED_COMMUNICATION_MODE_PREDICT.getExchangeName(), RabbitQueueInfo.PREFERRED_COMMUNICATION_MODE_PREDICT.getQueueKey(), requestJSON);
        LOGGER.info(String.format("successfuly pushed message to preferred communication mode prediction queue for message_uuid=%s", preferredCommunicationModePrediction.getMessage().getUuid()));
	}

    public void pushToOptOutStatusUpdateQueue(OptOutStatusUpdate optOutStatusUpdate) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        String requestJSON = mapper.writeValueAsString(optOutStatusUpdate);
        rabbitTemplate.convertAndSend(RabbitQueueInfo.OPT_OUT_STATUS_UPDATE.getExchangeName(), RabbitQueueInfo.OPT_OUT_STATUS_UPDATE.getQueueKey(), requestJSON);
        LOGGER.info(String.format("successfully pushed message to optout status update queue for request=%s ", requestJSON));
    }

    public void pushToPostOptOutStatusUpdateQueue(PostOptOutStatusUpdate postOptOutStatusUpdate) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        String requestJSON = mapper.writeValueAsString(postOptOutStatusUpdate);
		rabbitTemplate.convertAndSend(RabbitQueueInfo.POST_OPT_OUT_STATUS_UPDATE.getExchangeName(), RabbitQueueInfo.POST_OPT_OUT_STATUS_UPDATE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfully pushed message to post optout status update queue for request=%s ", requestJSON));
	}

	public void pushToDoubleOptInDeploymentQueue(DoubleOptInDeployment doubleOptInDeployment) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(doubleOptInDeployment);
		rabbitTemplate.convertAndSend(RabbitQueueInfo.DOUBLE_OPTIN_DEPLOYMENT.getExchangeName(), RabbitQueueInfo.DOUBLE_OPTIN_DEPLOYMENT.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfully pushed message to double optin deployment update queue for request=%s ", requestJSON));
	}

	public void pushDataForHistoricalMessageProcessingQueue(SaveHistoricalMessageRequest saveHistoricalMessageRequest) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(saveHistoricalMessageRequest);
		LOGGER.info(String.format("push message to save historical message request customer_uuid=%s message_json=%s ", saveHistoricalMessageRequest.getCustomerUuid(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.SAVE_HISTORICAL_MESSAGES.getExchangeName(), RabbitQueueInfo.SAVE_HISTORICAL_MESSAGES.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to save historical message request customer_uuid=%s", saveHistoricalMessageRequest.getCustomerUuid()));

	}

	public void pushToPostIncomingMessageSaveQueue(com.mykaarma.kcommunications.model.jpa.Message message, Long threadDelegatee, Boolean updateThreadTimestamp) throws Exception {
		PostIncomingMessageSave postIncomingMessageSave = new PostIncomingMessageSave();
		postIncomingMessageSave.setMessage(message);
		postIncomingMessageSave.setThreadDelegatee(threadDelegatee);
		postIncomingMessageSave.setUpdateThreadTimestamp(updateThreadTimestamp);

		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(postIncomingMessageSave);

		LOGGER.info(String.format("push message to PostIncomingMessageSaveQueue for message_uuid=%s message_json=%s ", message.getUuid(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.POST_INCOMING_MESSAGE_SAVE_QUEUE.getExchangeName(), RabbitQueueInfo.POST_INCOMING_MESSAGE_SAVE_QUEUE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfuly pushed message to PostIncomingMessageSaveQueue for message_uuid=%s", message.getUuid()));
	}

	public void pushToPostIncomingBotMessageSaveQueue(BotMessage message) throws Exception {
		PostIncomingBotMessageSave postIncomingBotMessageSave = new PostIncomingBotMessageSave();
		postIncomingBotMessageSave.setBotMessage(message);
		String requestJSON = objectMapper.writeValueAsString(postIncomingBotMessageSave);
		LOGGER.info("in pushToPostIncomingBotMessageSaveQueue for request={}", requestJSON);
		rabbitTemplate.convertAndSend(RabbitQueueInfo.POST_INCOMING_BOT_MESSAGE_SAVE_QUEUE.getExchangeName(), RabbitQueueInfo.POST_INCOMING_BOT_MESSAGE_SAVE_QUEUE.getQueueKey(), requestJSON);
	}

	public void pushToOptInAwaitingMessageExpireQueue(OptInAwaitingMessageExpire optInAwaitingMessageExpire, Integer delay) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(optInAwaitingMessageExpire);
		LOGGER.info(String.format("push message to sending queue for message_uuid=%s message_json=%s ", optInAwaitingMessageExpire.getMessageUUID(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.OPTIN_AWAITING_MESSAGE_EXPIRE_QUEUE.getExchangeName(),RabbitQueueInfo.OPTIN_AWAITING_MESSAGE_EXPIRE_QUEUE.getQueueKey(), requestJSON,
			new MessagePostProcessor() {

				public Message postProcessMessage(Message message) throws AmqpException {
					message.getMessageProperties().setDelay(delay);
					return message;
				}


			});
		LOGGER.info(String.format("successfully pushed message to sending queue for message_uuid=%s", optInAwaitingMessageExpire.getMessageUUID()));
	}

	public void pushToPostUniversalMessageSendQueue(com.mykaarma.kcommunications.model.jpa.Message message, List<User> usersToNotify, Boolean updateThreadTimestamp) throws Exception {
		PostUniversalMessageSendPayload postUniversalMessageSendPayload = new PostUniversalMessageSendPayload();
		postUniversalMessageSendPayload.setMessage(message);
		postUniversalMessageSendPayload.setUsersToNotify(usersToNotify);
		postUniversalMessageSendPayload.setUpdateThreadTimestamp(updateThreadTimestamp);

		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(postUniversalMessageSendPayload);

		LOGGER.info(String.format("pushing message to PostUniversalMessageSendQueue for message_uuid=%s message_json=%s ", message.getUuid(), requestJSON));
		rabbitTemplate.convertAndSend(RabbitQueueInfo.POST_UNIVERSAL_MESSAGE_SEND_QUEUE.getExchangeName(), RabbitQueueInfo.POST_UNIVERSAL_MESSAGE_SEND_QUEUE.getQueueKey(), requestJSON);
		LOGGER.info(String.format("successfully pushed message to PostUniversalMessageSendQueue for message_uuid=%s", message.getUuid()));
	}
	
	public void pushToMessageWithoutCustomerSendingQueue(ExternalMessage message, Integer delay) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(message);
		LOGGER.info("push message without customer to sending queue for message_uuid={} message_json={} ", message.getUuid(), requestJSON);
		try {
			rabbitTemplate.convertAndSend(RabbitQueueInfo.MESSAGE_WITHOUT_CUSTOMER_SENDING_QUEUE.getExchangeName(),RabbitQueueInfo.MESSAGE_WITHOUT_CUSTOMER_SENDING_QUEUE.getQueueKey(), requestJSON,
					new MessagePostProcessor() {

				public Message postProcessMessage(Message message) throws AmqpException {
					message.getMessageProperties().setDelay(delay);
					return message;
				}

			});	
		} catch(AmqpException e) {
			LOGGER.info("error occured while pushing event = {} to queue with delay = {} ", requestJSON, delay, e);
		}
		LOGGER.info("successfuly pushed message to sending queue fors message_uuid={}", message.getUuid());
	}

}

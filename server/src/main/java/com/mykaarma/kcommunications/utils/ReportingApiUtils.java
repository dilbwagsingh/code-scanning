package com.mykaarma.kcommunications.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.ModuleLogCodes;
import com.mykaarma.kcommunications.controller.impl.CommunicationsApiImpl;
import com.mykaarma.kcommunications.events.AutoCsiLogEvent;
import com.mykaarma.kcommunications.jpa.repository.DealerOrderMessageRepository;
import com.mykaarma.kcommunications.jpa.repository.DealerOrderRepository;
import com.mykaarma.kcommunications.model.jpa.DealerOrderMessage;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications_model.request.AutoCsiLogEventRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;

@Service
public class ReportingApiUtils {
	

	@Autowired
	private DealerOrderMessageRepository dealerOrderMessageRepository;
	
	@Autowired
	private DealerOrderRepository dealerOrderRepository;	
	
	@Autowired
	private AwsEventsUtil awsEvents;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(ReportingApiUtils.class);
	private static final String LOG_MESSAGE_API_SUPPORT = "Internal error - %s while processing request! Please contact Communications API support";
	public Boolean isAutoCsiMessage(Message m) {
		List<DealerOrderMessage> dealerOrderMessages = dealerOrderMessageRepository.findBymessageID(m.getId());
		Boolean output = false;
		if(m != null && !dealerOrderMessages.isEmpty()){
			for(DealerOrderMessage dom: dealerOrderMessages) {
				if("auto".equalsIgnoreCase(dom.getMessageOrigin()) && "CEP".equalsIgnoreCase(dom.getRelationType())){
					output = true;
				}
			}
		}
		LOGGER.info(String.format(ModuleLogCodes.MESSAGING_INFO_CODE.getLogMessage()+ "Message with message_id=%s isAutoCSICommunication=%s ", m.getId(),output));
		return output;
	}
	
	private String getDealerOrderUUIDFromMessage(Message m) {
		List<DealerOrderMessage> dealerOrderMessages = dealerOrderMessageRepository.findBymessageID(m.getId());
		String dealerOrderUUID= null;
		if(m != null && !dealerOrderMessages.isEmpty()){
			for(DealerOrderMessage dom: dealerOrderMessages) {
				Long dealerOrderID=dom.getDealerOrderID();
				dealerOrderUUID = dealerOrderRepository.findUUIDByID(dealerOrderID);
			}
		}
		return dealerOrderUUID;
	}
	
	public void sendAutoCsiMessageErrorsToReporting(SendMessageResponse response, Message m) {
		
		String dealerOrderUUID = getDealerOrderUUIDFromMessage(m);
		String messageUUID = response.getMessageUUID();
		String messageProtocol = m.getProtocol();
		
		if(response.getErrors()!=null&& !response.getErrors().isEmpty()) {
			String errors = "Kcommunication-api Errors : ";
			for(ApiError e:response.getErrors()) {
				errors = errors + e.getErrorDescription() + ", ";
			}
			LOGGER.info(String.format("error inside sendAutoCsiMessageErrorsToReporting for messageType : %s", messageProtocol));
			logAutoCsiStatus(dealerOrderUUID,messageUUID, false, errors, messageProtocol);
		}else {
			LOGGER.info(String.format("successfully sent message inside sendAutoCsiMessageErrorsToReporting for messageType : %s", messageProtocol));
			logAutoCsiStatus(dealerOrderUUID,messageUUID, true,null,messageProtocol);
		}
		
	}
	
	private void logAutoCsiStatus(String dealerOrderUUID, String messageUUID, Boolean isSent,String sentFailureReason, String messageProtocol) {
		AutoCsiLogEventRequest logAutoCsiRequest = getLogAutoCsiRequest(dealerOrderUUID, messageUUID, true, null, isSent, sentFailureReason, messageProtocol);
		try{
			sendAutoCsiRequestToReporting(logAutoCsiRequest);
		}catch(Exception e) {
			LOGGER.error("Exception in sending aws Event,",e);
		}
	}
	
	private AutoCsiLogEventRequest getLogAutoCsiRequest(String dealerOrderUUID, String messageUUID, Boolean isScheduled, String scheduledFailureReason,Boolean isSent, String sentFailureReason, String messageProtocol) {
		AutoCsiLogEventRequest logAutoCsiRequest = new AutoCsiLogEventRequest();
		
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));
		Instant instant = Instant.now();
		
		logAutoCsiRequest.setDealerOrderUUID(dealerOrderUUID);
		logAutoCsiRequest.setMessageUUID(messageUUID);
		logAutoCsiRequest.setIsScheduled(isScheduled);
		logAutoCsiRequest.setScheduledFailureReason(scheduledFailureReason);
		logAutoCsiRequest.setIsSent(isSent);
		logAutoCsiRequest.setSentFailureReason(sentFailureReason);
		logAutoCsiRequest.setMessageProtocol(messageProtocol);
		logAutoCsiRequest.setTsUTC(dtf.format(instant));
		
		return logAutoCsiRequest;
	}
	
	public void sendAutoCsiRequestToReporting(AutoCsiLogEventRequest logAutoCsiRequest) throws Exception{
		try {
			AutoCsiLogEvent logAutoCsiEvent = new AutoCsiLogEvent();
			logAutoCsiEvent.addDataItem(logAutoCsiRequest);
			String detailJson = new ObjectMapper().writeValueAsString(logAutoCsiEvent);
			String 	eventSource = "reporting-kcommunication-events";
			String detailType = "autocsi-log-event";
			awsEvents.putEvents(eventSource, detailType, detailJson);
		}catch(Exception e) {
			LOGGER.error("Exception in sending awsEvent dealer_order={}",logAutoCsiRequest.getDealerOrderUUID(),e);
			throw new KCommunicationsException(com.mykaarma.kcommunications_model.enums.ErrorCode.FAILED_AWS_EVENT,
					String.format(LOG_MESSAGE_API_SUPPORT, e.getMessage()));
		}
	}
}

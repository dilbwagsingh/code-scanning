package com.mykaarma.kcommunications.utils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

@Service
public class AwsEventsUtil {
	@Value("${aws.kcommunication-server.mykaarma-global-events-bus-name}")
	private String myKaarmaGlobalEventsBusName;

	@Value("${aws.kcommunication-server.enabled}")
	private Boolean enabled;

	@Autowired
	private EventBridgeClient awsEventBridgeClient;
	
	private static Logger LOGGER = LoggerFactory.getLogger(AwsEventsUtil.class);

	public void putEvents(String eventSource, String detailType, String detail) throws Exception {
		if (enabled == false) {
			return;
		}
		
		PutEventsRequestEntry requestEntry = PutEventsRequestEntry.builder()
			.eventBusName(myKaarmaGlobalEventsBusName)
		    .source(eventSource)
		    .detailType(detailType)
		    .detail(detail)
		    .build();	

		List<PutEventsRequestEntry> requestEntries = new ArrayList<PutEventsRequestEntry>();
		requestEntries.add(requestEntry);

		PutEventsRequest eventsRequest = PutEventsRequest.builder()
		    .entries(requestEntries)
		    .build();

		PutEventsResponse result = awsEventBridgeClient.putEvents(eventsRequest);
		
		for (PutEventsResultEntry resultEntry: result.entries()) {
		    if (resultEntry.eventId() != null) {
		    	LOGGER.info("Event Id: " + resultEntry.eventId());
		    } 
		    else {
		    	throw new Exception("AWS PutEvents failed. errorCode=" + resultEntry.errorCode() + " detail=" + detail + " eventSource=" + eventSource);
		    }
		}                

	}
}

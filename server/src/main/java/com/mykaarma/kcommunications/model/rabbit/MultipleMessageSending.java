package com.mykaarma.kcommunications.model.rabbit;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.mykaarma.kcommunications_model.request.SendMessageRequest;

import lombok.Data;

@Data
public class MultipleMessageSending implements Serializable {
	private SendMessageRequest sendMessageRequest;
	private List<String> customerUUIDList;
	private Integer gapInSendingMessagesInSeconds;
	private String dealerDepartmentUUID;
	private String userUUID;
	private Integer expiration;
	private String requestUUID;
    private Map<String, String> customerUUIDToCommunicationValues;
}

package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchMessagesForCommunicationIdentifierListRequest {

	List<String> communicationIdentifierList;
}

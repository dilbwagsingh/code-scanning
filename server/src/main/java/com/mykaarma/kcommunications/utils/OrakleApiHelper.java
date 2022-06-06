package com.mykaarma.kcommunications.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.ModuleLogCodes;
import com.mykaarma.orakle.client.OrakleApiClientService;
import com.mykaarma.orakle.client.utils.OrakleClientUtils;
import com.mykaarma.orakle.model.json.MessageDTO;
import com.mykaarma.orakle.model.json.request.SemanticPropertiesRequestDTO;
import com.mykaarma.orakle.model.json.response.StatusResponseDTO;
import com.mykaarma.orakle.model.proto.response.StatusResponseProto.StatusResponse;
import com.mykaarma.orakle.model.proto.request.SemanticPropertiesRequestProto.SemanticPropertiesRequest;

@Service
public class OrakleApiHelper {

	@Value("${orakle_api_url}")
	private String orakle_api_url;
	
	@Value("${kcommunications_basic_auth_user}")
	private String username;
	
	@Value("${kcommunications_basic_auth_pass}")
	private String password;
	
	private final Logger LOGGER = Logger.getLogger(OrakleApiHelper.class);

	private OrakleApiClientService clientService;
	
	public Boolean hitOrakleApi(String messageUUID, String messageBody, String contactNumber, String callbackUrl, Double weightThreshold, Boolean useOnlyOptOutV2)
	{	
		if(clientService == null) {
			clientService = new OrakleApiClientService(orakle_api_url, username, password);
		}
		SemanticPropertiesRequestDTO semanticPropertiesRequest = new SemanticPropertiesRequestDTO();
		MessageDTO messageInfo = new MessageDTO();
		messageInfo.setBody(messageBody);
		messageInfo.setUuid(messageUUID);
		messageInfo.setContactNumber(contactNumber);
		Set<MessageDTO> messageSet = new HashSet<MessageDTO>();
		messageSet.add(messageInfo);
		semanticPropertiesRequest.setMessages(messageSet);
		LOGGER.info(String.format("callback_url=%s orakle_api_url=%s user_name=%s ", callbackUrl, orakle_api_url, username));
		semanticPropertiesRequest.setCallBackUrl(callbackUrl);
		semanticPropertiesRequest.setWeightThreshold(weightThreshold);
        semanticPropertiesRequest.setUseOnlyOptOutV2(useOnlyOptOutV2);
		SemanticPropertiesRequest semanticPropertiesRequestProto = null;
		try {
			semanticPropertiesRequestProto = (SemanticPropertiesRequest)OrakleClientUtils.getProto(semanticPropertiesRequest, SemanticPropertiesRequest.class);
		}
		catch (Exception e) {
			LOGGER.error(String.format("could not create semantic properties request object in orakleApi for message_uuid=%s and message_body=%s", messageUUID, messageBody), e);
			return false;
		}
		return getSematicProperties(messageUUID, messageBody, semanticPropertiesRequestProto);
	}

	private Boolean getSematicProperties(String messageUUID, String messageBody, SemanticPropertiesRequest semanticPropertiesRequestProto) {
		int retryCount = 0;
		while(retryCount<3) {
			try {
				StatusResponse statusResponse = clientService.getSemanticProperties(semanticPropertiesRequestProto);
				StatusResponseDTO statusResponseDTO = (StatusResponseDTO) OrakleClientUtils.getDTO(statusResponse.toByteArray(), StatusResponse.class, StatusResponseDTO.class);
				if(statusResponseDTO.getErrors()!=null && !statusResponseDTO.getErrors().isEmpty()) {
					LOGGER.error(String.format("%s checkIfStopMessage error in orakleApi for message_uuid=%s retry_count=%s error=%s  ", 
							ModuleLogCodes.MESSAGING_ERROR_CODE.getLogMessage(), messageUUID, retryCount, new ObjectMapper().writeValueAsString(statusResponseDTO.getErrors())));
					retryCount++;
				} else {
					LOGGER.info(String.format("%s checkIfStopMessage successful message_uuid=%s response=%s retry_count=%s  warnings=%s ", ModuleLogCodes.MESSAGING_INFO_CODE.getLogMessage(), 
							messageUUID, statusResponseDTO.getStatus(), retryCount, new ObjectMapper().writeValueAsString(statusResponseDTO.getWarnings())));
					return true;
				}
			} catch (Exception e) {
				LOGGER.error(String.format("%s error in orakleApi for message_uuid=%s message_body=%s ", 
						ModuleLogCodes.MESSAGING_ERROR_CODE.getLogMessage(), messageUUID, messageBody), e);
				retryCount++;
			}
		}
		return false;
	}
}
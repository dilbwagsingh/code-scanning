package com.mykaarma.kcommunications.utils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.ModuleLogCodes;
import com.mykaarma.kcommunications.model.api.CommunicationListRequest;
import com.mykaarma.kcommunications.model.api.CommunicationStatusRequest;
import com.mykaarma.kcommunications.model.api.CommunicationStatusResponse;
import com.mykaarma.kcommunications.model.api.DelegateeResponse;
import com.mykaarma.kcommunications.model.api.OptedOutCommunicationsResponse;
import com.mykaarma.kcommunications_model.response.Response;

@Service
public class KMessagingApiHelper {
	
	@Value("${kmessaging_api_url}")
	private String kmessagingBaseUrl;
	
	@Value("${kcommunications_basic_auth_user}")
	private String apiUser;
	
	@Value("${kcommunications_basic_auth_pass}")
	private String apiPass;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(Helper.class);
	
	@Autowired
	RestTemplate restTemplate;
	

	public CommunicationStatusResponse getCommunicationStatus(String dealerUUID, String departmentUUID, String communicationType, String communicationValue) 
			throws Exception {
		
		if(communicationValue!=null && communicationValue.startsWith(APIConstants.COUNTRY_CODE)){
			communicationValue=communicationValue.substring(2);
		}
		String serviceUrl = String.format("%sdealer/%s/department/%s/commType/%s/commValue/%s/status", kmessagingBaseUrl, 
				dealerUUID, departmentUUID, communicationType, communicationValue);
		LOGGER.info(ModuleLogCodes.MESSAGING_INFO_CODE.getLogMessage()+ "calling messaging_api_url=" +serviceUrl );
		
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serviceUrl);		

		ParameterizedTypeReference<CommunicationStatusResponse> responseTypeRef = new ParameterizedTypeReference<CommunicationStatusResponse>() {};

		HttpHeaders httpHeaders = createHeadersWithBasicAuth();
		HttpEntity<Void> requestEntity = new HttpEntity<Void>(httpHeaders);

		httpHeaders.add("Content-Type", "application/json");

		
		HttpHeaders headers = createHeadersWithBasicAuth();
		headers.add("Content-Type", "application/json");
		ResponseEntity<CommunicationStatusResponse> response =  restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, requestEntity, responseTypeRef);
		LOGGER.info("response="+response.getStatusCode());

		return response.getBody();
		
	}
	
    public CommunicationStatusResponse updateCommunicationStatus(String dealerUUID, String departmentUUID, String communicationType, String communicationValue, CommunicationStatusRequest communicationStatusRequest) 
			throws Exception {
		
		if(communicationValue!=null && communicationValue.startsWith(APIConstants.COUNTRY_CODE)){
			communicationValue=communicationValue.substring(2);
		}
		String serviceUrl = String.format("%sdealer/%s/department/%s/commType/%s/commValue/%s/status", kmessagingBaseUrl, 
				dealerUUID, departmentUUID, communicationType, communicationValue);
		LOGGER.info(ModuleLogCodes.MESSAGING_INFO_CODE.getLogMessage()+ "in updateCommunicationStatus calling messaging_api_url=" +serviceUrl );
		
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serviceUrl);		

		ParameterizedTypeReference<CommunicationStatusResponse> responseTypeRef = new ParameterizedTypeReference<CommunicationStatusResponse>() {};

		HttpHeaders httpHeaders = createHeadersWithBasicAuth();
		httpHeaders.add("Content-Type", "application/json");
		HttpEntity<CommunicationStatusRequest> requestEntity = new HttpEntity<CommunicationStatusRequest>(communicationStatusRequest, httpHeaders);

		ResponseEntity<CommunicationStatusResponse> response =  restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, requestEntity, responseTypeRef);
		LOGGER.info("in updateCommunicationStatus response={}", new ObjectMapper().writeValueAsString(response));

		return response == null ? null : response.getBody();
		
	}
	
	public void onMessageSending(com.mykaarma.kcommunications.model.jpa.Message message, String departmentUUID, String dealerUUID) throws Exception{
	
		String serviceUrl = String.format("%sdealer/%s/department/%s/message/%s/sent", kmessagingBaseUrl, 
				dealerUUID, departmentUUID,message.getUuid());
		LOGGER.info("calling messaging_api_url=" +serviceUrl );
	
		HttpHeaders headers = createHeadersWithBasicAuth();
		headers.add("Content-Type", "application/json");
		HttpEntity<Void> requestEntity = new HttpEntity<Void>(headers);
		ResponseEntity<com.mykaarma.kcommunications.model.api.Response> response = restTemplate.exchange(serviceUrl, HttpMethod.PUT, requestEntity, com.mykaarma.kcommunications.model.api.Response.class);
		LOGGER.info("response="+response.getStatusCode());
	}
	
	public Long getDelegateeForDA(String dealerAssociateUUID,String departmentUUID){
		
		if(dealerAssociateUUID==null || dealerAssociateUUID.isEmpty()
				|| departmentUUID==null || departmentUUID.isEmpty()){
			LOGGER.info(String.format( " in getDelegateeForDA invalid parameters being passed for dealer_associate_uuid=%s department_uuid=%s" ,
					dealerAssociateUUID,departmentUUID) );
			return null;
		}
		try {
			String serviceUrl = String.format("%sdepartment/%s/user/%s/delegatee", kmessagingBaseUrl, 
				 departmentUUID, dealerAssociateUUID);
			LOGGER.info(String.format( " in getDelegateeForDA calling messaging_api_url=%s for dealer_associate_uuid=%s department_uuid=%s" ,
				serviceUrl,dealerAssociateUUID,departmentUUID) );
		
			HttpHeaders headers = createHeadersWithBasicAuth();
			headers.add("Content-Type", "application/json");
			HttpEntity<Void> requestEntity = new HttpEntity<Void>(headers);
			ResponseEntity<DelegateeResponse> response = restTemplate.exchange(serviceUrl, HttpMethod.GET, requestEntity, DelegateeResponse.class);
		
			LOGGER.info(String.format( " in getDelegateeForDA response=%s for dealer_associate_uuid=%s department_uuid=%s" ,
					new ObjectMapper().writeValueAsString(response),dealerAssociateUUID,departmentUUID) );
		
			if(response==null || response.getBody()==null){
				return null;
			}
			return response.getBody().getDelegateeDAID();
		} catch (Exception e) {
			LOGGER.error(String.format( "error_message=%s in getDelegateeForDA for dealer_associate_uuid=%s department_uuid=%s" ,
					e.getMessage(),dealerAssociateUUID,departmentUUID),e);
		}
		return null;
		
	}
	
	public boolean updateFilterTable(FilterHistory filterHistory, String filterName, String departmentUUID) {
		
		String serviceUrl = String.format("%sdepartment/%s/filter/%s", kmessagingBaseUrl, departmentUUID, filterName);
		HttpHeaders headers = createHeadersWithBasicAuth();
		headers.add("Content-Type", "application/json");
		boolean result = false;
		HttpEntity<FilterHistory> requestEntity = new HttpEntity<FilterHistory>(filterHistory, headers);
		try {
			LOGGER.info(" Requesting kmessaging-api for event={} for dealer_id={} and department_uuid={} " ,filterHistory.getActionSource(),filterHistory.getDealerID(), departmentUUID);
			ResponseEntity<Response> response = restTemplate.exchange(serviceUrl, HttpMethod.POST, requestEntity, Response.class);
			
			if(response != null) {
				if(response.getStatusCode().is2xxSuccessful()) {
					result = true;
					LOGGER.info(" Request to kmessaging api for event={} is successful for dealer_id={} and department_uuid= {} " ,filterHistory.getActionSource(),filterHistory.getDealerID(), departmentUUID);
				} 
			}
		}
			catch(Exception e) {
				LOGGER.error("Error in requesting kmessaging api for event={} for dealer_id={} and department_uuid={} " ,filterHistory.getActionSource(),filterHistory.getDealerID(), departmentUUID, e);
			}	
		return result;
	} 
	
	public List<String> getOptedOutCommunicationList(String dealerUuid, String commType, List<String> communicationList) {
		
		List<String> optedOutCommList = new ArrayList<String>();
		if(communicationList == null || communicationList.isEmpty()) {
			return optedOutCommList;
		}
		
		try {
			String url = String.format("%s/dealer/%s/department/%s/commType/%s/getOptoutCommunicationList", kmessagingBaseUrl, dealerUuid, null, commType);
			HttpHeaders headers = createHeadersWithBasicAuth();
			headers.add("Content-Type", "application/json");
			CommunicationListRequest commRequest = new CommunicationListRequest();
			commRequest.setCommunicationValueList(communicationList);
			HttpEntity<CommunicationListRequest> requestEntity = new HttpEntity<CommunicationListRequest>(commRequest, headers);
			OptedOutCommunicationsResponse response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, OptedOutCommunicationsResponse.class).getBody();
			
			if(response.getOptoutCommunicationList() != null) {
				optedOutCommList = response.getOptoutCommunicationList();
			}
		} catch(Exception e) {
			LOGGER.error("Exception in filtering out opted out emails for dealer_uuid="+dealerUuid+"comm_type="+commType);
		}
		return optedOutCommList;
	}
	
	
	public HttpHeaders createHeadersWithBasicAuth(){
		   return new HttpHeaders(){
		      {
		         String auth = apiUser + ":" + apiPass;
		         byte[] encodedAuth = Base64.encodeBase64( 
		            auth.getBytes(Charset.forName("US-ASCII")) );
		         String authHeader = "Basic " + new String( encodedAuth );
		         set( "Authorization", authHeader );
		      }
		   };
	}
}

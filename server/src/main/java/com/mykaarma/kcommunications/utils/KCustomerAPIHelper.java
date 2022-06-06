/**
 * 
 */
package com.mykaarma.kcommunications.utils;

import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.mykaarma.global.FeatureKeys;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;


@Service
public class KCustomerAPIHelper {
	
	@Value("${kcustomer_api_url_v2}")
	private String kcustomerApiUrl;
	
	@Value("${kcommunications_basic_auth_user}")
	private String apiUser;
	
	@Value("${kcommunications_basic_auth_pass}")
	private String apiPass;
	
	@Value("${appconfig.webservice.url}")
	private String appConfigUrl;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	@Qualifier("kCommunicationsRestTemplate")
	private RestTemplate restTemplate;
	
	public static final String CUSTOMER_ID = "customer_id";
	public static final String CUSTOMER_UUID = "customer_uuid";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(KCustomerAPIHelper.class);
	
	public boolean customerMergingEnabled(Long dealerId) {
		Set<String> features = getDealerFeatures(dealerId);
		if(features.contains(FeatureKeys.CUSTOMER_MERGING.getKey()))
			return true;
		return false;
	}//
	
	@SuppressWarnings("unchecked")
	public Set<String> getDealerFeatures(Long dealerID)
	{
		
		String url = appConfigUrl+"/dealerfeatures";
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParam("dealerid", dealerID);
		Set<String> features = (Set<String>) restTemplate.getForObject(builder.build().encode().toUri(), Set.class);
		if(features != null)
			return features;
		else
			return null;
	}

	public String getPrimaryCustomerGuid(Long secondaryCustomerId, Long dealerId, String ddUUID) {
		try {
			String url = kcustomerApiUrl + "/department/" + ddUUID + "/customer/primary";
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParam("q", secondaryCustomerId+"").queryParam("searchType", CUSTOMER_ID);
			HttpEntity<String> entity = new HttpEntity<String>(getHeaders());
			ResponseEntity<String> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, String.class);
			JSONObject object = new JSONObject(response.getBody());
			return object.getString("primaryCustomerGUID");
		} catch (Exception e) {
			return null;
		}
	}
	
	private HttpHeaders getHeaders()
	{
		String apiKey = apiUser+":"+apiPass;
		String plainCreds = apiKey;
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + base64Creds);
		headers.add("content-type", "application/json");
		return headers;
	}

	public String getPrimaryCustomerGUIDForCustomerGUID(String customerUUID, Long dealerID) {
		
		String result = customerUUID;
		try {
			String dealerDepartmentUUID = generalRepository.getDepartmentUUIDForDealerID(dealerID, "V");
			String primaryCustomerGUID  = getPrimaryCustomerGUIDForCustomerIdentifier(customerUUID, dealerDepartmentUUID, CUSTOMER_UUID);
			if(primaryCustomerGUID != null)
				result= primaryCustomerGUID;
		} catch (Exception e) {
			LOGGER.warn("Error finding primary_customer_guid for customer_guid=\"{}\" dealer_id=\"{}\" ",customerUUID,dealerID, e);
		}
		return result;
		
	}
	
	public String getPrimaryCustomerGUIDForCustomerGUID(String customerGUID, String dealerDepartmentUUID) {
		
		String result =  customerGUID;
		try {
			 String primaryCustomerGUID = getPrimaryCustomerGUIDForCustomerIdentifier(customerGUID, dealerDepartmentUUID, CUSTOMER_UUID);
			 if(primaryCustomerGUID != null)
				 result = primaryCustomerGUID;
		} catch (Exception e) {
			LOGGER.warn("Error finding primary_customer_guid for customer_guid=\"{}\" dealer_department_uuid=\"{}\" ",customerGUID,dealerDepartmentUUID, e);
		}
		return result;
		
	}
	
	private String getPrimaryCustomerGUIDForCustomerIdentifier(String customerIdentifier, String dealerDepartmentUUID, String searchType) throws Exception {
		
		String url = kcustomerApiUrl+"/department/"+dealerDepartmentUUID+"/customer/primary/";
		url = UriComponentsBuilder.fromHttpUrl(url).queryParam("q", customerIdentifier).queryParam("searchType", searchType).build().encode().toUriString();
		LOGGER.info("getPrimaryCustomer Requested URL is {}", url);
		HttpEntity<String> entity = new HttpEntity<String>(getHeaders());
		ResponseEntity<String> response  = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		JSONObject jsonObject = new JSONObject(response.getBody());
		String customerGUID =  jsonObject.getString("primaryCustomerGUID");
		return customerGUID;
	
	}
}

package com.mykaarma.kcommunications.utils;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.cache.CacheConfig;


@Component
public class AppConfigHelper {
	
	RestTemplate restTemplate = new RestTemplate();
	
	@Value("${appconfig.webservice.url}")
	private String kaarmaConfigBaseUrl;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AppConfigHelper.class);
	
	@Cacheable(value=CacheConfig.DEALER_UUID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public String getDealerUUIDForID(Long dealerID) {
		try {
		     String url = kaarmaConfigBaseUrl+"/dealerUuid";
		     UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
				        .queryParam("dealerID", dealerID);
			 ResponseEntity<String> responseGet = restTemplate.getForEntity(builder.build().encode().toUri(), String.class);
			 return responseGet.getBody();
			 
		} catch (Exception e) {
			LOGGER.error("Unable to get UUID for dealer_id="+dealerID, e);
		}
		return null;
	}
	
	@Cacheable(value=CacheConfig.DEALER_DEPARTMENT_UUID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public String getDealerDepartmentUUIDForID(Long dealerDepartmentID) {
		try {
		     String url = kaarmaConfigBaseUrl+"/dealerDepartmentUuid";
		     UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
				        .queryParam("dealerDepartmentID", dealerDepartmentID);
			 ResponseEntity<String> responseGet = restTemplate.getForEntity(builder.build().encode().toUri(), String.class);
			 return responseGet.getBody();
			 
		} catch (Exception e) {
			LOGGER.error("Unable to get UUID for dealer_department_id="+dealerDepartmentID, e);
		}
		return null;
	}
	
	@Cacheable(value=CacheConfig.DEALER_ASSOCIATE_UUID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public String getDealerAssociateUUIDForID(Long dealerAssociateID) {
		try {
		     String url = kaarmaConfigBaseUrl+"/dealerAssociateUuid";
		     UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
				        .queryParam("dealerAssociateID", dealerAssociateID);
			 ResponseEntity<String> responseGet = restTemplate.getForEntity(builder.build().encode().toUri(), String.class);
			 return responseGet.getBody();
			 
		} catch (Exception e) {
			LOGGER.error("Unable to get UUID for dealer_associate_id="+dealerAssociateID, e);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Cacheable(value=CacheConfig.DSO_LIST_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public HashMap<String, String> getDealerSetupOptionsFromConfigService(Long dealerID, List<String> keyList) {
		String url = kaarmaConfigBaseUrl +"/getdsolist";
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParam("dealerid", dealerID) ;
		HashMap<String, String> dsoMap = restTemplate.postForObject(builder.build().encode().toUri(), keyList, HashMap.class);
		try {
			LOGGER.info("getDealerSetupOptionsFromConfigService for dealer_id={} map={}",dealerID, new ObjectMapper().writeValueAsString(dsoMap));
		} catch (JsonProcessingException e) {
			LOGGER.error("Error in printing getDealerSetupOptionsFromConfigService ", e);
		}
		return dsoMap;
	}
	
	@Cacheable(value=CacheConfig.DSO_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public String getDealerSetupOptionValueFromConfigService(Long dealerID, String optionKey) {
		String url = kaarmaConfigBaseUrl+"/getdso";
		LOGGER.info("getDealerSetupOptionsFromConfigService for dealer_id={} option_key={}",dealerID, optionKey);
		UriComponentsBuilder builder =  UriComponentsBuilder.fromHttpUrl(url).queryParam("dealerid", dealerID).queryParam("key", optionKey);
		return restTemplate.getForObject(builder.build().encode().toUri(), String.class);
	}

	@Cacheable(value = CacheConfig.TEXT_TRANSLATION_CACHE, keyGenerator = "customKeyGenerator", unless="#result == null")
	public String getTranslatedText(String widgetGroup, String textKey, String locale) {
		if (textKey == null) {
			return textKey;
		}
		String translatedText = textKey;
		String skin = "mykaarma";
		if (locale == null || locale.trim().isEmpty()) {
			locale = "en-us";
		}
		String url = kaarmaConfigBaseUrl + "/getTranslatedMessage";
		try {
			UriComponentsBuilder builder =  UriComponentsBuilder.fromHttpUrl(url).queryParam("widgetGroup", widgetGroup).queryParam("textEnumName", textKey).queryParam("skin", skin).queryParam("locale", locale);
			translatedText = restTemplate.getForObject(builder.build().encode().toUri(), String.class);
		} catch (Exception e) {
			LOGGER.warn("Error while fetching translated text from appConfig. widget_group={} textKey={} locale={} module=backend ", widgetGroup, textKey, locale);
		}
		if (translatedText == null) {
			translatedText = textKey;
		}
		return translatedText;
	}
	
}

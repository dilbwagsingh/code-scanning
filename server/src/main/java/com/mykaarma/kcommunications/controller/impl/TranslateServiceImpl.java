package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.TranslateRequestInitializer;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.google.api.services.translate.model.TranslationsResource;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.request.TranslateTextRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.TranslateLanguagesResponse;
import com.mykaarma.kcommunications_model.response.TranslateTextResponse;
import com.google.api.services.translate.Translate.Builder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TranslateServiceImpl {
	
	@Autowired
	GeneralRepository generalRepository;
	
	private static final String API_KEY = "AIzaSyCVNDbgCONmB3cG9oI8e--vUUWac1jAEqA";

	public ResponseEntity<TranslateLanguagesResponse> getSupportedLanguages() {
		
		try {
			Map<String, String> languages = generalRepository.getTranslateLanguages();
			TranslateLanguagesResponse translateLanguagesResponse = new TranslateLanguagesResponse();
			translateLanguagesResponse.setLanguages(languages);
			
			return new ResponseEntity<TranslateLanguagesResponse>(translateLanguagesResponse, HttpStatus.OK);
		} catch(Exception e) {
			log.error("Error while fetching translate languages {}", e);
			ApiError error = new ApiError();
			error.setErrorDescription(e.getMessage());
			List<ApiError> errors = new ArrayList<ApiError>();
			errors.add(error);
			TranslateLanguagesResponse translateLanguagesResponse = new TranslateLanguagesResponse();
			translateLanguagesResponse.setErrors(errors);
			return new ResponseEntity<TranslateLanguagesResponse>(translateLanguagesResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}

	public ResponseEntity<TranslateTextResponse> translateText(TranslateTextRequest translateTextRequest) {
		
		TranslateTextResponse translateTextResponse =translateString(translateTextRequest.getText(), translateTextRequest.getLangCode());

		return new ResponseEntity<TranslateTextResponse>(translateTextResponse, HttpStatus.OK);
		
	}
	
	
 	public TranslateTextResponse translateString(String source, String langCode) {
		
 		TranslateTextResponse translateTextResponse = new TranslateTextResponse();
		Translate  translate=buildTranslate();
    	List<String> list=new ArrayList<String>();
    	list.add(source);
    	try {
			com.google.api.services.translate.Translate.Translations.List q = translate.translations().list(list, langCode);
			TranslationsListResponse response = q.execute();
			List<TranslationsResource> resource = response.getTranslations();
			for (TranslationsResource translationsResource : resource) {
				
				translateTextResponse.setTranslatedText(translationsResource.getTranslatedText());
				translateTextResponse.setDetectedSourceLanguage( translationsResource.getDetectedSourceLanguage());
				log.info("Language Detected: "+ translationsResource.getDetectedSourceLanguage());
				log.info("Translation: "+ translationsResource.getTranslatedText());
			}
			
		} catch (Exception e) {
			log.error("Failed to translate string source=\"{}\" langCode={} ",source,langCode, e);
			ApiError error = new ApiError(ErrorCode.TRANSLATION_REQUEST_FAILED.name(), e.getMessage());
			translateTextResponse.setErrors(Arrays.asList(error));
			return translateTextResponse;
		}

		return translateTextResponse;
	}
	
	private Translate buildTranslate() {
		
		CloseableHttpClient httpClient=HttpClients.createMinimal();
		HttpTransport httpTransport = new ApacheHttpTransport(httpClient);
		JsonFactory jsonFactory=new JacksonFactory();
		
		GoogleClientRequestInitializer translateRequestInitializer= new TranslateRequestInitializer(API_KEY);
	
		Builder builder=new Builder(httpTransport, jsonFactory, null);
		
    	builder.setApplicationName("Kaarma Google Translate Service");
    	builder.setGoogleClientRequestInitializer(translateRequestInitializer);
    	Translate  translate=builder.build(); 
    	return translate;
	 }
	 
	
}

package com.mykaarma.kcommunications.utils.TranscriptionHandler.impl;

import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.TranscribeService;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.dto.JobStatusDto;

@Service
public class TranscribeServiceImpl implements TranscribeService {


	@Autowired
	private StorageFactory storageFactory;
	 
    private static final Logger LOGGER=Logger.getLogger(TranscribeServiceImpl.class);

    @Value("${api.key}")
	String API_KEY;
    
    @Value("${status.url}")
	String STATUS_URL;
    
    @Value("${transcribe.url}")
    String TRANSCRIBE_URL;

    @Override
    public String transcribe(String recordingUrl, int sampleRate, String languageCode) {

    	String transcibeUrl = TRANSCRIBE_URL;
        String postPayload = getRequestJson(recordingUrl, sampleRate, languageCode);
        String accessToken = null;
        try {
            GoogleCredential credential = storageFactory.getCredential();
            boolean isTokenAvailable = credential.refreshToken();
            if(isTokenAvailable){
                accessToken = credential.getAccessToken();
            }
        } catch (Exception e) {
            LOGGER.trace("Unable to get google cloud auth credentials", e);
        }

        if(accessToken == null){
            LOGGER.error("Unable to get google service auth access token");
            return null;
        }
        
        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(postPayload, headers);
        RestTemplate restTemplate = new RestTemplate();

        LOGGER.info("Transcription begin recording_url="+recordingUrl);
        ResponseEntity<Map> exchange = restTemplate.exchange(transcibeUrl, HttpMethod.POST, entity, Map.class);

        // return the job name
        return exchange.getBody().get("name").toString();
    }

    @Override
    public JobStatusDto getJobStatus(String transcribeJobName) {

    	String apiKey = API_KEY;
        String statusUrl = STATUS_URL+transcribeJobName+"?key="+apiKey;
        
        RestTemplate restTemplate = new RestTemplate();
        JobStatusDto status = restTemplate.getForObject(statusUrl, JobStatusDto.class);


        return status;
    }

    private String getRequestJson(String recordingUrl, int sampleRate, String languageCode){
        JSONObject requestJsonObject = new JSONObject();
        JSONObject configJsonObject = new JSONObject();
        JSONObject audioJsonObject = new JSONObject();

        configJsonObject.put("encoding","LINEAR16");
        configJsonObject.put("profanityFilter",true);
        configJsonObject.put("sampleRateHertz", sampleRate);
        configJsonObject.put("languageCode", languageCode);

        audioJsonObject.put("uri",recordingUrl);

        requestJsonObject.put("config",configJsonObject);
        requestJsonObject.put("audio",audioJsonObject);

        return requestJsonObject.toString();
    }
}
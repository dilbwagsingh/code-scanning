package com.mykaarma.kcommunications.utils.TranscriptionHandler.impl;


import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * This class manages the details of creating a Storage service, including auth.
 */


@Service
public class StorageFactory {

    private static Storage storageService = null;

    private static GoogleCredential credential = null;

	@Value("${google.cloud.auth}")
	private String authPath;
	
    public synchronized Storage getService() throws IOException, GeneralSecurityException {
        if(credential == null){
            buildCredential();
        }
        if(storageService == null) {
            storageService = buildService();
        }
        return storageService;
    }

    public synchronized GoogleCredential getCredential() throws GeneralSecurityException, IOException {
        if(credential == null){
            buildCredential();
        }
        return credential;
    }

    private void buildCredential() throws GeneralSecurityException, IOException {
    	
//    	credential = GoogleCredential
//        		  .fromStream(new FileInputStream(authPath));
    	
    	credential = GoogleCredential.fromStream(
                new ByteArrayInputStream(authPath.getBytes()));

        if (credential.createScopedRequired()) {
            Collection<String> scopes = StorageScopes.all();
            credential = credential.createScoped(scopes);
        }
    }

    private Storage buildService() throws IOException, GeneralSecurityException {
        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        return new Storage.Builder(transport, jsonFactory, credential)
                .setApplicationName("Mykaarma")
                .build();
    }
}
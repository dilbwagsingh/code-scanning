package com.mykaarma.kcommunications.utils.TranscriptionHandler.impl;

import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.ObjectAccessControl;
import com.google.api.services.storage.model.StorageObject;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.StorageService;


@Service
public class StorageServiceImpl implements StorageService {

    /**
     * The logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(StorageService.class);

    /**
     * Back off period time before next retry.
     */
    private static final long BACK_OFF_PERIOD = 500;

    /**
     * Maximum upload attempt.
     */
    private static final int MAX_UPLOAD_ATTEMPT = 30;

    /**
     * The {@link StorageFactory} instance.
     */
    @Autowired
    private StorageFactory storageFactory;
    
    

    @Override
    public boolean uploadToCloud(String contentUrl, String bucketName, String contentName, String mime) throws Exception{

    	LOGGER.info("contentUrl " + contentUrl);
    	LOGGER.info("bucketName = " + bucketName);
    	LOGGER.info("ContentName = " + contentName);
    	LOGGER.info("mime = " + mime);
   
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_UPLOAD_ATTEMPT);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(BACK_OFF_PERIOD);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        Boolean isUploaded = retryTemplate.execute(new RetryCallback<Boolean, Exception>() {
            @Override
            public Boolean doWithRetry(RetryContext context) throws Exception {
                LOGGER.info("trying to upload recording to google cloud, attempt = " + context.getRetryCount() + " recording_url = " + contentUrl);
                return tryToUpload(contentUrl, bucketName, contentName, mime);
            }
        }, new RecoveryCallback<Boolean>() {
            @Override
            public Boolean recover(RetryContext context) throws Exception {
                LOGGER.error("unable to upload file on google cloud recording_url = " + contentUrl, context.getLastThrowable());
                return Boolean.FALSE;
            }
        });

        return isUploaded.booleanValue();
    }

    /**
     * Helper function which tries to upload recording to google cloud.
     *
     * @param contentUrl remote recording url
     * @param bucketName google cloud bucket name
     * @param contentName content name
     * @param mime media type
     * @return <code>true</code> is successfully upload to the cloud, <code>false</code> otherwise.
     */
    private boolean tryToUpload(String contentUrl, String bucketName, String contentName, String mime) throws Exception{
    	
        URL url = new URL(contentUrl);
        URLConnection urlConnection = url.openConnection();
        InputStreamContent contentStream = new InputStreamContent(mime, urlConnection.getInputStream());
        // Setting the length improves upload performance
        contentStream.setLength(urlConnection.getContentLength());
        StorageObject objectMetadata = new StorageObject()
                // Set the destination object name
                .setName(contentName)
                // Set the access control list to publicly read-only
                .setAcl(Arrays.asList(
                        new ObjectAccessControl().setEntity("allUsers").setRole("READER")));

        // Do the insert
        Storage client = storageFactory.getService();
        Storage.Objects.Insert insertRequest = client.objects().insert(bucketName, objectMetadata, contentStream);
        insertRequest.execute();
        return true;
    }

    @Override
    public boolean deleteFromCloud(String bucketName, String contentName) {
        try {
            Storage client = storageFactory.getService();
            Storage.Objects.Delete deleteRequest = client.objects().delete(bucketName, contentName);
            deleteRequest.execute();
            LOGGER.info("Successfully deleted file from cloud");
            return true;
        } catch (Exception e) {
            LOGGER.error("Error deleting file = "+contentName, e);
        }
        return false;
    }
}
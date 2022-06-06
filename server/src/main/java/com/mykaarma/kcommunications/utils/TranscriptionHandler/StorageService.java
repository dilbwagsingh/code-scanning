package com.mykaarma.kcommunications.utils.TranscriptionHandler;

import org.springframework.stereotype.Service;

/**
 * Provide google cloud storage related services.
 */
@Service
public interface StorageService {

    /**
     * Upload a object present at given url to google cloud bucket
     *
     * @param contentUrl url of the content
     * @param bucketName name of the bucket
     * @param contentName name of the content
     * @param mime content type
     */
    boolean uploadToCloud(String contentUrl, String bucketName, String contentName, String mime) throws Exception;

    /**
     * Delete file from google cloud.
     *
     * @param bucketName name of the bucket.
     * @param contentName name of the content.
     * @return <code>true</code> if deleted successfully, <code>false</code> otherwise.
     */
    boolean deleteFromCloud(String bucketName, String contentName);
}
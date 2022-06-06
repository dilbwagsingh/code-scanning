package com.mykaarma.kcommunications.controller.impl;

import com.mykaarma.kcommunications.jpa.repository.DocFileRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.model.jpa.DocFile;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.utils.AWSClientUtil;
import com.mykaarma.kcommunications.utils.AttachmentUtil;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications_model.dto.DocFileDTO;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.request.UploadAttachmentsToS3Request;
import com.mykaarma.kcommunications_model.response.MediaPreviewURLFetchResponse;
import com.mykaarma.kcommunications_model.response.UploadAttachmentResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class AttachmentService {

    private final static Logger LOGGER = LoggerFactory.getLogger(AttachmentService.class);

    private static final String HTML_CONTENT_TYPE = "text/html";

    @Autowired
    private AttachmentUtil attachmentUtil;

    @Autowired
    private AWSClientUtil awsClientUtil;

    @Autowired
    private KCommunicationsUtils kCommunicationsUtils;
    
    @Autowired
    private MessageRepository messageRepo;
    
    @Autowired
    private DocFileRepository docFileRepository;

    @Value("${awsVideoOutputBucketUrl}")
    private String awsVideoOutputBucketUrl;
    
	@Value("${base_url}")
	private String baseURL;

    public ResponseEntity<UploadAttachmentResponse> uploadAttachmentUsingUrl(UploadAttachmentsToS3Request uploadAttachmentsToS3Request,
             Long dealerID, String folderPrefix) {
        UploadAttachmentResponse response = new UploadAttachmentResponse();

        if(uploadAttachmentsToS3Request.getMediaUrl() != null && !uploadAttachmentsToS3Request.getMediaUrl().isEmpty()) {
            HttpURLConnection conn = null;

            try {
                List<String> videoExtensionList = attachmentUtil.fetchValidVideoExtensionsList();
                List<String> imageExtensionList = attachmentUtil.fetchValidImageExtensionsList();

                String attachmentUrl = uploadAttachmentsToS3Request.getMediaUrl();
                LOGGER.info(String.format("Attachment_url=%s", attachmentUrl));

                URL source = new URL(attachmentUrl);
                conn = (HttpURLConnection) source.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(60000);
                conn.connect();

                int status = conn.getResponseCode();
                LOGGER.info(String.format("attachmentUrl=%s Response Code=%s", attachmentUrl, status));

                boolean redirect = false;
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                        redirect = true;
                    }
                }

                if (redirect) {
                    // Get redirect URL from "location" header field and cookie for login
                    String newUrl = conn.getHeaderField("Location");
                    String cookies = conn.getHeaderField("Set-Cookie");

                    // open the new connection again
                    conn = (HttpURLConnection) new URL(newUrl).openConnection();
                    conn.setRequestProperty("Cookie", cookies);
                    conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
                    conn.addRequestProperty("User-Agent", "Mozilla");
                    conn.addRequestProperty("Referer", "google.com");

                    LOGGER.info("Redirect to URL : " + newUrl);
                }

                URI uri = new URI(attachmentUrl);
                String path = uri.getPath();
                String name = path.substring(path.lastIndexOf('/') + 1);

                String contentType = uploadAttachmentsToS3Request.getContentType();
                String fileName = new Date().getTime() + "_" + name;
                String extension = null;
                String mediaPreviewURL = null;
                String docSize = null;
                String thumbnailURL = null;

                if(attachmentUtil.isVideo(contentType)){
                    awsClientUtil.uploadVideoToS3(conn.getInputStream(), fileName);

                    String outputKey = fileName + "_converted.mp4";
                    LOGGER.info(String.format("outputKey=%s", outputKey));

                    awsClientUtil.elasticTranscodeVideo(fileName, outputKey, true);
                    mediaPreviewURL = awsVideoOutputBucketUrl + "/" + outputKey;
                    LOGGER.info(String.format("Video uploaded to S3 Video Bucket dealer_id=%s uploaded_video_url=%s", dealerID, mediaPreviewURL));

                    String htmlString = attachmentUtil.getHtmlToRenderInIframe(mediaPreviewURL);
                    extension = "html";
                    fileName += "." + extension;
                    String fileUrl = awsClientUtil.uploadMediaToAWSS3(IOUtils.toInputStream(htmlString), fileName, HTML_CONTENT_TYPE, new Date().getTime(), null, dealerID, false, folderPrefix);
                    LOGGER.info(String.format("File uploaded to S3 dealer_id=%s uploaded_file_url=%s", dealerID, fileUrl));
                    fileName = fileUrl;

                    try {
                        long contentLength = 0L;
                        contentLength = htmlString.length();
                        docSize = contentLength / 1024 + "KB";
                    } catch (Exception e) {
                        LOGGER.warn("Error in determining content_length for attachment_Url={} dealer_id={}", attachmentUrl, dealerID);
                    }

                    if(videoExtensionList != null && videoExtensionList.contains(extension)) {
                        thumbnailURL = attachmentUtil.getThumbNailFileName(fileName, extension, videoExtensionList);
                    }
                } else {
                    extension = attachmentUtil.getExtensionFromMIME(contentType);
                    fileName += "." + extension;

                    String fileUrl = awsClientUtil.uploadMediaToAWSS3(conn.getInputStream(), fileName, contentType, new Date().getTime(), null, dealerID, false, folderPrefix);
                    LOGGER.info(String.format("File uploaded to S3 dealer_id=%s uploaded_file_url=%s", dealerID, fileUrl));

                    try {
                        long contentLength = 0L;
                        contentLength = conn.getInputStream().available();
                        docSize = contentLength / 1024 + "KB";
                    } catch (Exception e) {
                        LOGGER.warn("Error in determining content_length for attachment_Url={} dealer_id={}", attachmentUrl, dealerID);
                    }

                    fileName = fileUrl;

                    if(extension != null && imageExtensionList != null && imageExtensionList.contains(extension)) {
                        thumbnailURL = attachmentUtil.getThumbNailFileName(fileName, extension, imageExtensionList);
                    }
                }

                response.setAttachmentURL(fileName);
                response.setMediaPreviewURL(mediaPreviewURL);
                response.setThumbnailURL(thumbnailURL);
                response.setExtension(extension);
                response.setContentType(contentType);
                response.setDocSize(docSize);
                return new ResponseEntity<UploadAttachmentResponse>(response, HttpStatus.OK);

            } catch (Exception e) {
                LOGGER.error("exception in uploading file ", e);
                response.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.UPLOAD_ATTACHMENT_FAILED.name())));
                return new ResponseEntity<UploadAttachmentResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                if(conn != null) {
                    conn.disconnect();
                }
            }
        }

        LOGGER.error("Attachment Url Received is null or empty for dealer_id={}", dealerID);
        response.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.INVALID_ATTACHMENT_URL.name())));
        return new ResponseEntity<UploadAttachmentResponse>(response, HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<UploadAttachmentResponse> uploadAttachmentUsingByteFile(UploadAttachmentsToS3Request uploadAttachmentsToS3Request,
            Long dealerID, String folderPrefix) {
        UploadAttachmentResponse response = new UploadAttachmentResponse();

        if(uploadAttachmentsToS3Request.getFileItems() != null && !uploadAttachmentsToS3Request.getFileItems().isEmpty()) {
            for (byte[] item : uploadAttachmentsToS3Request.getFileItems()) {
                try {
                    InputStream stream = new ByteArrayInputStream(item);
                    String contentType = uploadAttachmentsToS3Request.getContentType();
                    String fileName = uploadAttachmentsToS3Request.getFileName();
                    LOGGER.info("fileItem received with file_Name={} content_Type={} dealer_id={}", fileName, uploadAttachmentsToS3Request.getContentType(), dealerID);
                    fileName = fileName.replaceAll("\\s", "");
                    fileName = fileName.replaceAll("\\+", "");

                    String fileUrl = awsClientUtil.uploadMediaToAWSS3(stream, fileName, contentType, new Date().getTime(), null, dealerID, false, folderPrefix);
                    LOGGER.info("fileItem after modification file_Name={} content_Type={} dealer_id={}", fileName, uploadAttachmentsToS3Request.getContentType(), dealerID);
                    if (fileUrl != null && !fileUrl.isEmpty()) {
                        response.setAttachmentURL(fileUrl);
                    }
                    return new ResponseEntity<UploadAttachmentResponse>(response, HttpStatus.OK);
                } catch (Exception e) {
                    LOGGER.error("exception in uploading file ", e);
                    response.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.UPLOAD_ATTACHMENT_FAILED.name())));
                    return new ResponseEntity<UploadAttachmentResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        LOGGER.error("file items null or empty for dealer_id={}", dealerID);
        response.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.UPLOAD_ATTACHMENT_FAILED.name())));
        return new ResponseEntity<UploadAttachmentResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
	
	public ResponseEntity<MediaPreviewURLFetchResponse> uploadMediaToAWSAndFetchMediaURL(DocFileDTO docFileDTO) {
		
		String extension = docFileDTO.getFileExtension();
		String fileName = docFileDTO.getDocFileName();
		
		MediaPreviewURLFetchResponse mediaPreviewURLFetchResponse = new MediaPreviewURLFetchResponse();
		
		if (attachmentUtil.isVideoFromExtension(extension) || attachmentUtil.isAudioFromExtension(extension)) {
			try {

	            HttpURLConnection conn = null;
                URL source = new URL(fileName);
                conn = (HttpURLConnection) source.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(60000);
                conn.connect();

                int status = conn.getResponseCode();
                LOGGER.info(String.format("attachmentUrl=%s Response Code=%s", fileName, status));

                boolean redirect = false;
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                        redirect = true;
                    }
                }

                if (redirect) {
                    // Get redirect URL from "location" header field and cookie for login
                    String newUrl = conn.getHeaderField("Location");
                    String cookies = conn.getHeaderField("Set-Cookie");

                    // open the new connection again
                    conn = (HttpURLConnection) new URL(newUrl).openConnection();
                    conn.setRequestProperty("Cookie", cookies);
                    conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
                    conn.addRequestProperty("User-Agent", "Mozilla");
                    conn.addRequestProperty("Referer", "google.com");

                    LOGGER.info("Redirect to URL : " + newUrl);
                }

                URI uri = new URI(fileName);
                String path = uri.getPath();
                String name = path.substring(path.lastIndexOf('/') + 1);
                name = name.substring(0, name.lastIndexOf('.'));
                
                String docFileName = new Date().getTime() + "_" + name;
				
                awsClientUtil.uploadVideoToS3(conn.getInputStream(), docFileName);

                String outputKey = docFileName + "_converted.mp4";
                LOGGER.info(String.format("outputKey=%s", outputKey));

                awsClientUtil.elasticTranscodeVideo(docFileName, outputKey, true);
                String mediaPreviewURL = awsVideoOutputBucketUrl + "/" + outputKey;
                
                Message message = messageRepo.findByuuid(docFileDTO.getMessageUuid());
                List<DocFile> docFiles = docFileRepository.findByMessageId(message.getId());
                
                DocFile docFileSelected = null; 
                if(docFiles!=null && !docFiles.isEmpty()) {
                    for(DocFile docFile: docFiles) {
                    	if(docFile.getDocFileName().equalsIgnoreCase(docFileDTO.getDocFileName())) {
                    		docFileSelected = docFile;
                    		break;
                    	}
                    }
                }
                
                docFileRepository.updateMediaPreviewURLForID(docFileSelected.getId(), mediaPreviewURL);
                
                mediaPreviewURLFetchResponse.setMediaPreviewURL(mediaPreviewURL);
                return new ResponseEntity<MediaPreviewURLFetchResponse>(mediaPreviewURLFetchResponse, HttpStatus.OK);
                
             
			} catch (Exception e) {
				LOGGER.error("uploadMediaToAWS failed for docfile for messageUUID={}", docFileDTO.getMessageUuid(), e);
				mediaPreviewURLFetchResponse.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.UPLOAD_ATTACHMENT_FAILED.name())));
                return new ResponseEntity<MediaPreviewURLFetchResponse>(mediaPreviewURLFetchResponse, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return null;
	}

}

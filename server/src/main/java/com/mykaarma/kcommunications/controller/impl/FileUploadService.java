package com.mykaarma.kcommunications.controller.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mykaarma.kcommunications.utils.AWSClientUtil;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.FileType;
import com.mykaarma.kcommunications_model.response.FileDeleteResponse;
import com.mykaarma.kcommunications_model.response.FileUploadResponse;

@Service
public class FileUploadService {
	
	@Autowired
	private KCommunicationsUtils kCommunicationsUtils;
	
	@Autowired
	private AWSClientUtil awsClientUtil;
	
	private final static Logger log = LoggerFactory.getLogger(FileUploadService.class);
	
	public ResponseEntity<FileUploadResponse> uploadFileToS3(MultipartFile fileItem, String contentType, FileType fileType) throws Exception {
		FileUploadResponse response = new FileUploadResponse();
		try {
			String fileName = fileItem.getOriginalFilename();
			String subFolderPrefix = fileType.getSubFolderPrefix();
			InputStream stream = new ByteArrayInputStream(fileItem.getBytes());
			log.info("file received with file_name={} content_type={} file_type={}", fileName, contentType, fileType);
			if (contentType == null && fileItem.getContentType() != null) {
				contentType = fileItem.getContentType();
				log.info("file contentType updated file_name={} content_type={} file_type={}", fileName, contentType, fileType);
			}
			String fileUrl = awsClientUtil.uploadMediaToAWSS3(stream, fileName, contentType, new Date().getTime(), null, null, false, subFolderPrefix);
			log.info("file uploaded file_name={} content_type={} file_type={}", fileName, contentType, fileType);
			if (fileUrl != null && !fileUrl.isEmpty()) {
				response.setUploadUrl(fileUrl);
				return new ResponseEntity<FileUploadResponse>(response, HttpStatus.OK);
			} else {
				log.warn("Couldn't get fileUrl for uploaded file file_name={} content_type={} file_type={}", fileName, contentType, fileType);
				response.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.UPLOAD_ATTACHMENT_FAILED.name())));
				return new ResponseEntity<FileUploadResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (Exception e) {
			log.error("exception in uploading file ", e);
			response.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.FILE_UPLOAD_FAILED.name())));
			return new ResponseEntity<FileUploadResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	public ResponseEntity<FileDeleteResponse> deleteFileFromS3(String fileUrl) {
		FileDeleteResponse response = new FileDeleteResponse();
		try {
			List<String> fileUploadUrlList = new ArrayList<String>();
			fileUploadUrlList.add(fileUrl);
			List<String> attachmentsDeletedList = awsClientUtil.deleteMediaFromAWSS3(fileUploadUrlList, null);
			if (attachmentsDeletedList != null && !attachmentsDeletedList.isEmpty()) {
				response.setIsDeleted(true);
				return new ResponseEntity<FileDeleteResponse>(response, HttpStatus.OK);
			} else {
				response.setIsDeleted(false);
				response.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.FILE_DELETE_FAILED.name())));
				return new ResponseEntity<FileDeleteResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (Exception e) {
			log.error("exception in uploading file fileUrl={}", fileUrl, e);
			response.setIsDeleted(false);
			response.setErrors(kCommunicationsUtils.getApiError(Arrays.asList(ErrorCode.FILE_DELETE_FAILED.name())));
			return new ResponseEntity<FileDeleteResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}

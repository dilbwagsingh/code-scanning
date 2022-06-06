package com.mykaarma.kcommunications.utils;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoderAsync;
import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoderAsyncClient;
import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoderAsyncClientBuilder;
import com.amazonaws.services.elastictranscoder.model.CreateJobOutput;
import com.amazonaws.services.elastictranscoder.model.CreateJobRequest;
import com.amazonaws.services.elastictranscoder.model.JobInput;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications_model.enums.AWSTag;


@Service
public class AWSClientUtil {

	@Autowired
	private KManageApiHelper kManageApiHelper;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	private static Logger LOGGER = Logger.getLogger(AWSClientUtil.class);
	
	@Value("${awsAccessKey}")
	private String awsAccessKey;
	
	@Value("${awsSecretKey}")
	private String awsSecretKey;
	
	@Value("${awsBucket}")
	private String awsBucket;
	
	@Value("${awsRegion:us-east-1}")
	private String awsRegion;
	
	@Value("${cfURLPrefix}")
	private String cfURLPrefix;
	
	@Value("${awsAccessKeyVideo}")
	private String awsAccessKeyVideo;
	
	@Value("${awsSecretKeyVideo}")
	private String awsSecretKeyVideo;
	
	@Value("${awsVideoInputBucket}")
	private String awsVideoInputBucket;
	
	@Value("${awsVideoOutputBucket}")
	private String awsVideoOutputBucket;
	
	@Value("${awsVideoOutputBucketUrl}")
	private String awsVideoOutputBucketUrl;
	
	@Value("${awsEncoderPipelineId}")
	private String awsEncoderPipelineId;
	
	@Value("${awsEncoderPresentId}")
	private String awsEncoderPresentId;
	
	private static AWSCredentials awsCredentials = null;
	private static AmazonS3 s3Client = null;
	private static TransferManager transferManager = null;
	static final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/");
	private static final String S3_FOLDER_PREFIX_COMMON = "common-files/";
	
	public String uploadMediaToAWSS3(InputStream stream, String originalFileName, String contentType,
			Long time, Long messageId, Long dealerID, Boolean setExpiration, String dealerFolderPrefix) throws Exception {
		
		String fileUrlS3 = "";
		try {
			
			if(awsCredentials == null) {
				awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
			}
			
			if(transferManager == null) {
				s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(Regions.fromName(awsRegion)).build();
				transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
			}
			
			ObjectMetadata objectMetadata = new ObjectMetadata();
			if(setExpiration) {
				Date dt = new Date();
				Calendar c = Calendar.getInstance(); 
				c.setTime(dt); 
				c.add(Calendar.DATE, 1);
				dt = c.getTime();
				objectMetadata.setExpirationTime(dt);
			}
			if (contentType != null && !contentType.trim().isEmpty()) {
				objectMetadata.setContentType(contentType);
			}
			String key = generateKeyForMessage(dealerID, time,originalFileName, dealerFolderPrefix);
			PutObjectRequest putObjectRequest = new PutObjectRequest(awsBucket, key, stream, objectMetadata);
			
			if (dealerID != null) {
				List<Tag> listOfTag = new ArrayList<Tag>();
				Tag dealerIDTag = new Tag(AWSTag.DEALER_ID.getTagName(), dealerID + "");
				listOfTag.add(dealerIDTag);
				ObjectTagging tagging = new ObjectTagging(listOfTag);
				putObjectRequest.setTagging(tagging);
			}
			transferManager.upload(putObjectRequest).waitForCompletion();
			
			String dealerUrlPrefix = cfURLPrefix;
			fileUrlS3 = dealerUrlPrefix + key;
			
		} catch(Exception e) {
			if(messageId!=null) {
				LOGGER.error("Error in uploading file to S3 dealer_id="+dealerID+" message_id="+messageId+" original_file_name="+originalFileName,e);
			}
			else {
				LOGGER.error("Error in uploading file to S3 dealer_id="+dealerID+" original_file_name="+originalFileName,e);
			}
			throw e;
		}
		
		return fileUrlS3;
		
	}
	
	/**
	 * @param attachmentUrlList
	 * @param dealerID
	 * @return list of attachmentUrl which were deleted successfully
	 * @throws Exception
	 */
	public List<String> deleteMediaFromAWSS3(List<String> attachmentUrlList, Long dealerID) throws Exception {
		try {
			
			// Getting aws credentials and initializing s3 client
			if(awsCredentials == null) {
				awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
			}
			
			if(transferManager == null) {
				s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(Regions.fromName(awsRegion)).build();
				transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
			}
			
			// Extracting attachment keys from its url
			LOGGER.info(String.format("Attachments to be deleted : %s for dealer_id=%s", 
					attachmentUrlList.toString(), dealerID));
			String attachmentKeys[] = new String[attachmentUrlList.size()];
			for(int i=0; i<attachmentUrlList.size(); i++) {
				String urlPath = new URL(attachmentUrlList.get(i)).getPath();
				attachmentKeys[i] = urlPath.replaceFirst("/", ""); 
			}
			LOGGER.info(String.format("Attachments keys to be deleted : %s for dealer_id=%s", 
					Arrays.asList(attachmentKeys).toString(), dealerID));
			
			// Deleting attachment from s3
			DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(awsBucket).withKeys(attachmentKeys);
			List<DeleteObjectsResult.DeletedObject> attachmentsDeleted = s3Client.deleteObjects(deleteObjectsRequest).getDeletedObjects();
			
			// Returning list of attachmentUrl successfully deleted
			List<String> attachmentDeletedList = new ArrayList<>();
			String dealerUrlPrefix = cfURLPrefix;
			for(DeleteObjectsResult.DeletedObject deletedObject: attachmentsDeleted) {
				attachmentDeletedList.add(dealerUrlPrefix + deletedObject.getKey());
			}
			LOGGER.info(String.format("Attachments deleted : %s for dealer_id=%s", 
					attachmentDeletedList.toString(), dealerID));
			
			return attachmentDeletedList;
			
		} catch (Exception e) {
			LOGGER.error(String.format("Error in deleting file from S3 dealer_id=%s , attachmentUrlList : %s", 
					dealerID, attachmentUrlList.toString()),e);
			throw e;
		}	
	}
	
	public void uploadVideoToS3(InputStream inputStream, String outputName) {
		AWSCredentials awsVideoCredentials = new BasicAWSCredentials(awsAccessKeyVideo, awsSecretKeyVideo);
		AmazonS3 awsS3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsVideoCredentials)).withRegion(Regions.US_EAST_1).build();
		
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentType("video/mp4");
		PutObjectRequest putObjectRequest = new PutObjectRequest(awsVideoInputBucket, outputName, inputStream, objectMetadata);
		awsS3Client.putObject(putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead));
		
		LOGGER.info(String.format("video with outputname=%s uploaded to s3", outputName));
	}
	
	public void elasticTranscodeVideo(String inputKey, String outputKey, Boolean checkVideoAccess) {
		JobInput input = new JobInput().withKey(inputKey);
		CreateJobOutput output = new CreateJobOutput().withKey(outputKey).withPresetId(awsEncoderPresentId);
		
		AWSCredentials awsVideoCredentials = new BasicAWSCredentials(awsAccessKeyVideo, awsSecretKeyVideo);
		
		AmazonElasticTranscoderAsyncClient amazonElasticTranscoderAsyncClient = new AmazonElasticTranscoderAsyncClient(awsVideoCredentials);
		
		CreateJobRequest createJobRequest = new CreateJobRequest().withPipelineId(awsEncoderPipelineId)
				.withInput(input)
				.withOutput(output);
		amazonElasticTranscoderAsyncClient.createJob(createJobRequest);
		
		if(checkVideoAccess != null && checkVideoAccess) {
			checkVideoAccessStatus(awsVideoCredentials, output.getKey());
		}
	}
	
	private void checkVideoAccessStatus(AWSCredentials credentials, String outputKey) {
		AmazonS3 awsS3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withRegion(Regions.US_EAST_1)
				.build();
		
		try {
			awsS3Client.getObject(awsVideoOutputBucket, outputKey);
		} catch (Exception e) {
			LOGGER.warn(String.format("Error occured in checking video access status for outputKey=%s", outputKey));
			LOGGER.info("Retrying...");
			try {
				awsS3Client.getObject(awsVideoOutputBucket, outputKey);
			} catch (Exception e2) {
				LOGGER.warn(String.format("Error occured again in checking video access status for outputKey=%s", outputKey));
			}
		}
	}
	
	private String generateKeyForMessage(Long dealerID, Long time, String originalFileName, String subFolderPrefix) throws Exception {
		String date = dateFormatter.print(time);
        String folderPrefix = "";
		if (dealerID != null) {
			String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
			folderPrefix = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID,
					DealerSetupOption.S3_BUCKET_FOLDER_PREFIX.getOptionKey());
			if (folderPrefix == null || folderPrefix.isEmpty()) {
				folderPrefix = getBase64EncodedSHA256(dealerID + "").substring(0, 5) + "-" + dealerID;
			}
			folderPrefix += "/";
		} else {
			folderPrefix = S3_FOLDER_PREFIX_COMMON;
		}
		
		return folderPrefix + getValidSubFolderPrefix(subFolderPrefix) + date + originalFileName;
	}
	
	private String getValidSubFolderPrefix(String subFolderPrefix) {
		if (subFolderPrefix == null) {
			subFolderPrefix = "";
		} else {
			if (subFolderPrefix.startsWith("/")) {
				subFolderPrefix = subFolderPrefix.substring(1);
			}
			if (!subFolderPrefix.endsWith("/")) {
				subFolderPrefix += "/";
			}
		}
		return subFolderPrefix;
	}
	
	private String getBase64EncodedSHA256(String uuid) {
		String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(uuid);
		return getUrlSafeBase64StrFromHexStr(sha256hex);
	}
	
	private String getUrlSafeBase64StrFromHexStr(String sha256hex) {	
		BigInteger sha256Num = new BigInteger(sha256hex, 16);
		byte[] encodedHexB64 = org.apache.commons.net.util.Base64.encodeInteger(sha256Num);
		return new String(encodedHexB64).replace("+", "-").replace("/", "_").replace("=", "").trim();
	}
	
}

package com.mykaarma.kcommunications.controller.impl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaarya.services.messaging.CommProviderFactory;
import com.kaarya.services.messaging.IEmailGateway;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.ConvertToJpaEntity;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications_model.common.SendEmailRequestBody;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.MessagePurpose;
import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.request.SendEmailRequest;
import com.mykaarma.kcommunications_model.request.SendMessageWithoutCustomerRequest;
import com.mykaarma.kcommunications_model.request.SendNotificationWithoutCustomerRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.SendEmailResponse;
import com.mykaarma.kcommunications_model.response.SendMessageWithoutCustomerResponse;
import com.mykaarma.kcommunications_model.response.SendNotificationWithoutCustomerResponse;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessage;
import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessageMetaData;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CommunicationsWithoutCustomerApiImpl {	
	
	
	@Value("${feature-email-host}")
	String featureEmailHost;
	
	@Value("${feature-email-port}")
	String featureEmailPort;
	
	@Value("${feature-email-username}")
	String featureEmailUsername;
	
	@Value("${feature-email-password}")
	String featureEmailPassword;
	
	@Autowired
	ValidateRequest validateRequest;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	private MessagingViewControllerHelper messagingViewControllerHelper;
	
	@Autowired
	private SendMessageHelper sendMessageHelper;
	
	@Autowired
	private ConvertToJpaEntity convertToJpaEntity;
	
	@Autowired
	private RabbitHelper rabbitHelper;
	
	@Autowired
	private Helper helper;
	
	@Autowired
	private SaveMessageHelper saveMessageHelper;
	
	public static final String HOURLY_TMP_DIR = "/consumer/h/";

	private static final String HEADER_REFERENCES = "References";
	
	ObjectMapper objectMapper = new ObjectMapper();

	public ResponseEntity<SendEmailResponse> sendEmail(String departmentUUID, SendEmailRequest sendEmailRequest) throws Exception{

		SendEmailResponse response = new SendEmailResponse();
		List<ApiError> errors = response.getErrors();
		log.info("in sendEmail for dealer_department_uuid={} request={}",departmentUUID,objectMapper.writeValueAsString(sendEmailRequest));
		try{
			if(sendEmailRequest!=null && sendEmailRequest.getSendEmailRequest()!=null &&
					!sendEmailRequest.getSendEmailRequest().isEmpty()){
				for(SendEmailRequestBody sendEmailRequestBody : sendEmailRequest.getSendEmailRequest()){
					log.info("Converting request body to external message JPA");
					ExternalMessage message = convertToJpaEntity.getExternalMessageJpaEntity(null, sendEmailRequestBody);
					log.info("in sendEmail iterating for dealer_department_uuid={} email_request_body={}",departmentUUID,objectMapper.writeValueAsString(sendEmailRequestBody));
					sendEmailWithEmailGateway(sendEmailRequestBody,departmentUUID,message);
				}
			}
			response.setStatus(Status.SUCCESS);
			
		} catch(Exception e){
			log.error(String.format("Exception in sending email error= %s exception= %s", e.getMessage(),e.getStackTrace()));
			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name() ,String.format("Internal error %s while sending email without customer", e.getMessage()));
			errors.add(apiError);
			response.setStatus(Status.FAILURE);
			response.setErrors(errors);
			return new ResponseEntity<SendEmailResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<SendEmailResponse>(response, HttpStatus.OK);
	}

	private void sendEmailWithEmailGateway(SendEmailRequestBody sendEmailRequestBody,String departmentUUID, ExternalMessage message) throws Exception {
		if(!ObjectUtils.isEmpty(message)) {
			InternetAddress from = new InternetAddress(sendEmailRequestBody.getFromEmail(), sendEmailRequestBody.getFromName());
			ArrayList<InternetAddress> alToList = new ArrayList<InternetAddress>();
			ArrayList<InternetAddress> alCCList = new ArrayList<InternetAddress>();
			ArrayList<InternetAddress> alBccList = new ArrayList<InternetAddress>();
			
			ExternalMessageMetaData metaData = message.getMessageMetaData();
			String metaDataString = metaData.getMetaData();
			HashMap<String, String> metaDataMap;
			try {
				metaDataMap = helper.getMessageMetaDatMap(metaDataString);
			} catch (Exception e) {
				log.info("Some exception occured while converting message meta data to map for message with uuid={} and error={}", message.getUuid(), e);
				metaDataMap = new HashMap<>();
			}
			
			ArrayList<String> uniqueEmailIds = new ArrayList<String>();
			if(sendEmailRequestBody!=null && sendEmailRequestBody.getToList()!=null){
				for (String s : sendEmailRequestBody.getToList().trim().split(",")) {
					if(s!=null && !s.isEmpty() && !uniqueEmailIds.contains(s)){
						alToList.add(new InternetAddress(s, ""));
						uniqueEmailIds.add(s);
					}
				}
			}
			if(sendEmailRequestBody.getCcList()!=null && !sendEmailRequestBody.getCcList().isEmpty()){
				for (String s : sendEmailRequestBody.getCcList().trim().split(",")) {
					if(s!=null && !s.isEmpty()){
						alCCList.add(new InternetAddress(s, ""));
					}
				}
			}
			if(sendEmailRequestBody.getBccList()!=null && !sendEmailRequestBody.getBccList().isEmpty()){
				for (String s : sendEmailRequestBody.getBccList().trim().split(",")) {
					if(s!=null && !s.isEmpty()){
						alBccList.add(new InternetAddress(s, ""));
					}
				}
			}
			log.info(String.format("unique_email_ids:%s email_subject: %s , email_body: %s", uniqueEmailIds,sendEmailRequestBody.getSubject(),sendEmailRequestBody.getMessage()));
			
			if(uniqueEmailIds==null || uniqueEmailIds.isEmpty()) {
				log.warn("not sending email since recipient list is empty for request={}",objectMapper.writeValueAsString(sendEmailRequestBody));
				return;
			}
			MimeBodyPart mBP = null;
			MimeMultipart content;
			IEmailGateway ieg = null;
			
			try {
				if(sendEmailRequestBody.getUseDealerEmailCredentials()){
					ieg=getDealerEmailGatewayCredentials(departmentUUID);
					
				} else {
					ieg = ((new CommProviderFactory()).getEmailGatewayFromCredentials(featureEmailHost,
							featureEmailPort, featureEmailUsername,featureEmailPassword, "false", "false", "false"));
				}
				MimeMessage email = ieg.createMessage();
				
				email.setFrom(from);

				Address[] iA = { from };
				email.setReplyTo(iA);

				if (alToList == null || alToList.isEmpty()) {
					throw new Exception("No recipients specified.");
				}
				
				for (InternetAddress to : alToList) {
					if (!to.getAddress().isEmpty()) {
						email.addRecipient(javax.mail.Message.RecipientType.TO, to);
					}	
				}

				if (alCCList != null) {
					for (InternetAddress cc : alCCList) {
						if (!cc.getAddress().isEmpty()) {
							email.addRecipient(javax.mail.Message.RecipientType.CC, cc);
						}
					}
				}

				if (alBccList != null) {
					for (InternetAddress bcc : alBccList) {
						if (!bcc.getAddress().isEmpty()) {
							email.addRecipient(javax.mail.Message.RecipientType.BCC, bcc);
						}
					}
				}
				
				if (sendEmailRequestBody.getSubject() != null) {
					if (!sendEmailRequestBody.getSubject().trim().isEmpty()) {
						email.setSubject(sendEmailRequestBody.getSubject());
					}
				}
				// Create the message part
				mBP = new MimeBodyPart();
				
				// Fill the message
				mBP.setContent(sendEmailRequestBody.getMessage(), "text/html; charset=utf-8");
				content = new MimeMultipart();
				content.addBodyPart(mBP);
				if (sendEmailRequestBody.getAttachmentUrlAndNameMap()!= null && !sendEmailRequestBody.getAttachmentUrlAndNameMap().isEmpty()) 
				{
					int numberofFiles = 0;
					
					Set<String> attachmentUrlKeySet = sendEmailRequestBody.getAttachmentUrlAndNameMap().keySet();
					List<String> attachmentUrlList=new ArrayList<String>();
					attachmentUrlList.addAll(attachmentUrlKeySet);
					numberofFiles=attachmentUrlList.size();
					for(int k = 0; k < numberofFiles; k++){
						String attachmentName=sendEmailRequestBody.getAttachmentUrlAndNameMap().get(attachmentUrlList.get(k));
						if(attachmentUrlList.get(k)!=null && !attachmentUrlList.get(k).trim().isEmpty()){
							log.info(String.format("in sendEmailWithEmailGateway: setting attachment_url=%s attachment_name=%s", attachmentUrlList.get(k),attachmentName));
							File temp = new File(HOURLY_TMP_DIR+attachmentName);
							FileUtils.copyURLToFile(new URL(attachmentUrlList.get(k)), temp);
							
							mBP = new MimeBodyPart();
							DataSource source = new FileDataSource(temp.getAbsolutePath());
							mBP.setDataHandler(new DataHandler(source));
							
							mBP.setFileName(attachmentName);
							content.addBodyPart(mBP);
							//kCommunicationsUtils.setAttachments(attachmentUrlList, k, mBP, content, attachmentName);
						}
					}
				}
				email.setContent(content);

				if(sendEmailRequestBody.getReference() != null && !sendEmailRequestBody.getReference().isEmpty()) {
					email.setHeader(HEADER_REFERENCES, sendEmailRequestBody.getReference());
				}

				log.info(String.format("Recipients: %s", email.getAllRecipients().toString()));
				ieg.sendMessage(email);
				ieg = null;
			}
			catch (Exception e) {
				log.error(String.format("Exception while sending email from: %s with message body: %s ",
						from,sendEmailRequestBody.getMessage()), e);
				metaDataMap.put(APIConstants.DELIVERY_FAILURE_REASON, e.toString());
			}
			try {
				if(message.getMessagePurpose() != null) {
					saveMessageHelper.saveMessageSentWithoutCustomer(message);
				} else {
					log.warn("message purpose uuid not passed while sending mail w/o customer so message not saved in DB with uuid = {}", message.getUuid());
				}
			} catch (Exception e) {
				log.error("Error while storing message data in DB ", e);
			}
		} else {
			log.info("empty message body received");
			throw new Exception("empty message body received");
		}
	}
	
	
	
	private IEmailGateway getDealerEmailGatewayCredentials(String departmentUUID){
		try{
			Long dealerID=generalRepository.getDealerIDFromDepartmentUUID(departmentUUID);
			String dealerUUID=generalRepository.getDealerUUIDFromDealerId(dealerID);
			IEmailGateway ieg=null;
			if("yes".equalsIgnoreCase(kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID,DealerSetupOption.CUSTOM_EMAIL_EXISTS.getOptionKey()))) {
				ieg = (new CommProviderFactory(dealerID)).getCustomEmailGateway("custom");
			} else {
				ieg = (new CommProviderFactory(dealerID)).getCustomEmailGateway("gmail");
			}
			return ieg;
		} catch (Exception e){
			log.error("in getDealerEmailGatewayCredentials for dealer_department_uuid={}",departmentUUID,e);
		}
		return null;
	}

	public ResponseEntity<SendNotificationWithoutCustomerResponse> sendNotification(String departmentUUID,
			String userUUID, SendNotificationWithoutCustomerRequest notificationRequest) throws Exception {
		log.info("received request for department_uuid={} user_uuid={} notification_without_customer_request={}", 
	            departmentUUID, userUUID, objectMapper.writeValueAsString(notificationRequest));
		
		SendNotificationWithoutCustomerResponse notificationResponse = validateRequest.validateNotificationWithoutCustomerRequest(notificationRequest);
		
		if (notificationResponse.getErrors() != null && !notificationResponse.getErrors().isEmpty()) {
			notificationResponse.setStatus(Status.FAILURE);
			return new ResponseEntity<SendNotificationWithoutCustomerResponse>(notificationResponse, HttpStatus.BAD_REQUEST);
		}
		
		GetDealerAssociateResponseDTO dealerAssociate = null;
		if(APIConstants.DEFAULT.equalsIgnoreCase(userUUID)) {
			dealerAssociate = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);
		} else {
			dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUUID, userUUID);
		}
		
		Long dealerAssociateId = dealerAssociate != null && dealerAssociate.getDealerAssociate() != null ? dealerAssociate.getDealerAssociate().getId() : null;
		MDC.put(APIConstants.DEALER_ASSOCIATE_ID, dealerAssociateId);
		
		Long dealerDepartmentId = (dealerAssociateId != null && dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO() != null) ? dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO().getId() : null;
		Long dealerId = (dealerDepartmentId != null && dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO().getDealerMinimalDTO() != null) ? dealerAssociate.getDealerAssociate().getDepartmentExtendedDTO().getDealerMinimalDTO().getId() : null; 
		
		String notificationUUID = messagingViewControllerHelper.publishNotificationWithoutCustomerEvent(notificationRequest, EventName.SYSTEM_NOTIFICATION_WITHOUT_CUSTOMER, dealerAssociateId, dealerId, dealerDepartmentId);
		notificationResponse.setNotificationMessageUUID(notificationUUID);
		
		return new ResponseEntity<SendNotificationWithoutCustomerResponse>(notificationResponse, HttpStatus.OK);
	}
	
	public SendMessageWithoutCustomerResponse sendMessage(SendMessageWithoutCustomerRequest sendMessageRequest) {
		ExternalMessage message = null;
		SendMessageWithoutCustomerResponse response = new SendMessageWithoutCustomerResponse();
		List<ApiError> errors = new ArrayList<>();
		List<ApiWarning> warnings = new ArrayList<>();
		try {
			message = convertToJpaEntity.getExternalMessageJpaEntity(sendMessageRequest, null);
			log.info("Sending message with purpose = {}", message.getMessagePurpose().getPurposeName());
			response.setMessageUUID(message.getUuid());
		} catch (Exception e) {
			log.info("Error while mapping SendMessageWithoutCustomer to ExternalMessage JPA object", e);
			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), String.format(e.getMessage()));
			errors.add(apiError);
			response.setErrors(errors);
			response.setWarnings(warnings);
			response.setStatus(Status.FAILURE);
			return response;
		}
		try {
			if(sendMessageRequest.getSendSynchronously()) {
				response = sendMessageHelper.sendTextMessageWithoutCustomer(message);
			} else {
				try {
					rabbitHelper.pushToMessageWithoutCustomerSendingQueue(message, sendMessageRequest.getDelayInSeconds() * 1000);
					response.setErrors(errors);
					response.setWarnings(warnings);
					response.setStatus(Status.SUCCESS);
				} catch (Exception e) {
					log.error("Some error occured while pushing message to delayed rmq with message_uuid={}", message.getUuid(), e);
					ApiError apiError = new ApiError(ErrorCode.QUEUE_PUSH_FAILUE.name(), String.format(e.getMessage()));
					errors.add(apiError);
					response.setErrors(errors);
					response.setWarnings(warnings);
					response.setStatus(Status.FAILURE);
				}
			}
		} catch (Exception e) {
			log.info("Error while Sending Message Without Customer with uuid={} ",message.getUuid(), e);
			ApiError apiError = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), String.format(e.getMessage()));
			errors.add(apiError);
			response.setErrors(errors);
			response.setWarnings(warnings);
			response.setStatus(Status.FAILURE);
		}
		return response;
	}
}

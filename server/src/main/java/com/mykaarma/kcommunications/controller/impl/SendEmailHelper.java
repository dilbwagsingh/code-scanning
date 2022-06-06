package com.mykaarma.kcommunications.controller.impl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.SendFailedException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kaarya.services.messaging.CommProviderFactory;
import com.kaarya.services.messaging.IEmailGateway;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.FailedDraftEnum;
import com.mykaarma.global.MessageType;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.jpa.DocFile;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.AppConfigHelper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications_model.enums.CommunicationsFeature;
import com.mykaarma.kcommunications_model.enums.DraftStatus;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.Status;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDealersResponseDTO;

@Service
public class SendEmailHelper {

	private Logger LOGGER = LoggerFactory.getLogger(SendEmailHelper.class);

	@Autowired
	private AppConfigHelper appConfigHelper;
	
	@Autowired
	private MessageSendingRules messageSendingRules;
	
	@Autowired
	private SendCallback sendCallback;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	@Autowired
	private RabbitHelper rabbitHelper;
	
	@Autowired
	private CustomerLockService customerLockService;

	@Autowired
	private SaveMessageHelper saveMessageHelper;
	
	@Autowired
	private RateControllerImpl rateControllerImpl;
	
	@Autowired
	private KCommunicationsUtils kCommunicationsUtils;
	
	@Autowired
	private KManageApiHelper kManageApiHelper;
	
	@Value("${cfURLPrefix}")
	private String communicationsCloudFrontURL;
	
	public static final String HOURLY_TMP_DIR = "/consumer/h/";
	
	private static final char[] CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
			.toCharArray();
	
	public static final String DEFAULT_EXTENSION = ".pdf";
	public SendMessageResponse sendEmail(String departmentUUID, Message message) throws Exception {
		
		SendMessageResponse response = new SendMessageResponse();
		String customerUUID =generalRepository.getCustomerUUIDFromCustomerID( message.getCustomerID());
		response.setCustomerUUID(customerUUID);
		response.setMessageUUID(message.getUuid());
		response.setStatus(Status.SUCCESS);
		List<ApiError> errors = new ArrayList<>();
		List<ApiWarning> warnings = new ArrayList<>();

        String dealerUUID = kCommunicationsUtils.getDealerUUIDFromDepartmentUUID(departmentUUID);
        HashMap<String, String> dsoMap = kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(dealerUUID, getDSOList());
        
		ArrayList<InternetAddress> toList = new ArrayList<InternetAddress>();
		ArrayList<InternetAddress> ccList = new ArrayList<InternetAddress>();
		ArrayList<InternetAddress> bccList = new ArrayList<InternetAddress>();
		Set<String> uniqueEmails = new HashSet<>();
		uniqueEmails.add(message.getToNumber());
		
		HashMap<String, String> metaDataMap = messageSendingRules.getMetaData(message);
		
		String callbackURL = metaDataMap.get(APIConstants.CALLBACK_URL);
		
		try {
			toList.add(new InternetAddress(message.getToNumber()));
		} catch (AddressException e) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_COMMUNICATION_VALUE.name(), String.format("Phone Number/Email = %s for the customer is invalid", message.getToNumber()));
			errors.add(apiError);
			response.setErrors(errors);
			response.setStatus(Status.FAILURE);
			sendCallback.sendCallback(callbackURL, response);
			return response;
		}
		
		Boolean overrideHolidays = messageSendingRules.overrideHolidays(metaDataMap, message, dsoMap);
		if(!overrideHolidays) {
			Date nextDate = KManageApiHelper.getNextAvailableSlotForDealer(appConfigHelper.getDealerUUIDForID(message.getDealerID()), message.getSentOn());
			if(nextDate.after(message.getSentOn())) {
				ApiError apiError = new ApiError(ErrorCode.DEALERSHIP_HOLIDAY.name(), String.format("Dealership is not working on this date", message.getSentOn()));
				errors.add(apiError);
				response.setErrors(errors);
				response.setStatus(Status.FAILURE);
				sendCallback.sendCallback(callbackURL, response);
				handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.TIME_NOT_CORRECT_FOR_SENDING_REASON);
				LOGGER.warn("Dealership is not working on this date for current_date={} next_date={} dealer_id={} ", message.getSentOn(), nextDate, message.getDealerID());
				return response;
			}
		}
		
		String[] ccEmailList = null;
		String[] bccEmailList = null;
		try {
			if(metaDataMap.containsKey(APIConstants.CC_LIST)) {
				ccEmailList = metaDataMap.get(APIConstants.CC_LIST).trim().split(",");
				ccList = getInternetAddressListForList(ccEmailList, uniqueEmails);
			}
		} catch (Exception e) {
			LOGGER.error(String.format("Error in getting ccList for message_id=%s ", message.getId()), e);
		}
		try {
			if(metaDataMap.containsKey(APIConstants.BCC_LIST)) {
				bccEmailList = metaDataMap.get(APIConstants.BCC_LIST).trim().split(",");
				bccList = getInternetAddressListForList(bccEmailList, uniqueEmails);
			}
		} catch (Exception e) {
			LOGGER.error(String.format("Error in getting cbcList for message_id=%s ", message.getId()), e);
		}
		
		List<String> optoutList = messageSendingRules.isEmailingAllowed(message, metaDataMap, dsoMap, ccList, bccList, toList);
		if(optoutList!=null) {
			for(InternetAddress ia: toList) {
				if(optoutList.contains(ia.getAddress())) {
					ApiError apiError = new ApiError(ErrorCode.OPTED_OUT_COMMUNICATION_VALUE.name(), 
							String.format("Phone Number/Email %s for the customer is opted-out.", ia.getAddress()));
					errors.add(apiError);
					response.setErrors(errors);
					response.setStatus(Status.FAILURE);
					sendCallback.sendCallback(callbackURL, response);
					handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.CUSTOMER_OPTED_OUT);
					return response;
				}
			}
			for(InternetAddress ia: ccList) {
				if(optoutList.contains(ia.getAddress())) {
					ApiWarning apiWarning = new ApiWarning(ErrorCode.OPTED_OUT_COMMUNICATION_VALUE.name(), 
							String.format("Phone Number/Email %s for the customer is opted-out.", ia.getAddress()));
					warnings.add(apiWarning);
				}
			}
			for(InternetAddress ia: bccList) {
				if(optoutList.contains(ia.getAddress())) {
					ApiWarning apiWarning = new ApiWarning(ErrorCode.OPTED_OUT_COMMUNICATION_VALUE.name(), 
							String.format("Phone Number/Email %s for the customer is opted-out.", ia.getAddress()));
					warnings.add(apiWarning);
				}
			}
		}
		
		Set<String> dealerUUIDSet = new HashSet<>();
		dealerUUIDSet.add(dealerUUID);
		GetDealersResponseDTO dealers = kManageApiHelper.sortInputAndGetDealersForUUID(dealerUUIDSet);
		
		String dealerName = dealers.getDealers().get(dealerUUID).getName();
		String dealerEmail = dealers.getDealers().get(dealerUUID).getEmailID();
		message.setFromNumber(dealerEmail);
		if(dealerEmail==null || dealerEmail.isEmpty()) {
			LOGGER.warn("Dealer email is null or empty for dealer_id={}", message.getDealerID());
			ApiError apiError = new ApiError(ErrorCode.MISSING_DEALER_EMAIL.name(), 
					"Dealer email is not present");
			errors.add(apiError);
			response.setErrors(errors);
			response.setStatus(Status.FAILURE);
			sendCallback.sendCallback(callbackURL, response);
			handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.DEALERSHIP_NOT_AUTHORIZED_TO_SEND_MESSAGE);
			return response;
		}
		
		if(message.getIsManual()!=null && message.getIsManual() && "true".equalsIgnoreCase(dsoMap.get(DealerSetupOption.MESSAGING_CUSTOMER_LOCK_ENABLE.getOptionKey()))) {
			try {
				customerLockService.obtainCustomerLock(message);
			} catch (Exception e) {
				ApiError apiError = new ApiError(ErrorCode.CUSTOMER_LOCKED.name(), String.format("Customer locked by another advisor. Please try later."));
				errors.add(apiError);
				response.setErrors(errors);
				response.setStatus(Status.FAILURE);
				sendCallback.sendCallback(callbackURL, response);
				return response;
			}
		}
		
		Boolean rateLimitReached = rateControllerImpl.rateLimitReached(departmentUUID, CommunicationsFeature.OUTGOING_EMAIL, message.getToNumber());
		if(rateLimitReached) {
			ApiError apiError = new ApiError(ErrorCode.RATE_LIMIT_REACHED.name(), String.format("Email sending limit reached for this number."));
			errors.add(apiError);
			response.setErrors(errors);
			response.setStatus(Status.FAILURE);
			sendCallback.sendCallback(callbackURL, response);
			handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.GENERIC_FAILURE_REASON);
			LOGGER.warn("Can not send message since rate limit reached for uuid={} for dealer_id={} customer_id={} dealer_department_id={} communication_value={} ",
					message.getUuid(), message.getDealerID(), message.getCustomerID(), message.getDealerDepartmentId(), message.getToNumber());
			return response;
		}
		
		message.getMessageExtn().setMessageBody(messageSendingRules.appendEmailSignature(message, metaDataMap, dsoMap));
		messageSendingRules.appendOptOutFooterForOutgoingEmail(message, dsoMap);
		
		String userUUID = generalRepository.getUserUUIDForDealerAssociateID(message.getDealerAssociateID());

		GetDealerAssociateResponseDTO dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUUID, userUUID);
		
		if(dealerAssociate==null || dealerAssociate.getDealerAssociate()==null) {
			ApiError apiError = new ApiError(ErrorCode.INVALID_USER.name(), String.format("USER_UUID=%s is invalid ", userUUID));
			errors.add(apiError);
			response.setErrors(errors);
			return response;
		}
		
		messageSendingRules.applySubjectRules(dealerName,dealerAssociate.getDealerAssociate(), message);
        try {
            sendEmail(dealerUUID, message, dsoMap, toList, ccList, bccList, dealerEmail, dealerName, message.getFromName(), dealerAssociate.getDealerAssociate().getEmailAddress());
        } catch(SendFailedException e) {
            if(e.getInvalidAddresses() != null && e.getInvalidAddresses().length != 0) {
                LOGGER.warn("sendEmail failed for message_uuid={} ", message.getUuid(), e);
                ApiError apiError = new ApiError(ErrorCode.INVALID_EMAIL_ADDRESS.name(), String.format("Failed to send to invalid email address(es) %s", e.getInvalidAddresses()));
                errors.add(apiError);
                response.setErrors(errors);
                response.setStatus(Status.FAILURE);
                sendCallback.sendCallback(callbackURL, response);
                handleDraftSendFailureAndPushToPostMessageSentQueue(message, FailedDraftEnum.NOT_OK_TO_EMAIL);
                return response;
            }
            throw e;
        }
		message.setSentOn(new Date());
		message.setReceivedOn(new Date());
		message.setRoutedOn(new Date());
		if(message.getMessageType().equalsIgnoreCase(MessageType.F.name())){
			message.setMessageType(MessageType.S.name());
			if(message.getDraftMessageMetaData()!= null){
				message.getDraftMessageMetaData().setStatus(DraftStatus.SENT.name());
			}
		}
		message = saveMessageHelper.saveMessage(message);
		rabbitHelper.pushToMessagePostSendingQueue(message, messageSendingRules.delegationRules(message, dsoMap), messageSendingRules.postMessageProcessingToBeDone(message, dsoMap), false, false, null);
		response.setWarnings(warnings);

		sendCallback.sendCallback(callbackURL, response);
		return response;
	}
	
	private ArrayList<InternetAddress> getInternetAddressListForList(String[] emailList, Set<String> uniqueEmails) {
		ArrayList<InternetAddress> emails = new ArrayList<InternetAddress>();
		if(emailList!=null) {
			for(String s: emailList) {
				try {
					if(!uniqueEmails.contains(s)) {
						emails.add(new InternetAddress(s));
						uniqueEmails.add(s);
					}
				} catch (Exception e) {
					LOGGER.error(String.format("Error in getting internet address for email_address=%s ", s), e);
				}
			}
		}
		return emails;
	}
	
	
	private void sendEmail(String dealerUUID, Message message, HashMap<String, String> dsoMap, List<InternetAddress> toList, List<InternetAddress> ccList, List<InternetAddress> bccList,
			String dealerEmailID, String dealerName, String dealerAssociateName, String dealerAssociateEmail) throws Exception {
		LOGGER.info("sendEmail for message_uuid={} dealer_email_id={} dealer_name={} dealer_associate_name={} dealer_associate_email={}",
				message.getUuid(), dealerEmailID, dealerName, dealerAssociateName, dealerAssociateEmail);
		try {
			MimeMessage email = null;
			IEmailGateway ieg = null;
			MessageExtn messageExtn = message.getMessageExtn();
			LOGGER.info("trying to email gateway for message_uuid={} dealer_email_id={} dealer_name={} dealer_associate_name={} dealer_associate_email={}",
					message.getUuid(), dealerEmailID, dealerName, dealerAssociateName, dealerAssociateEmail);
			if("yes".equalsIgnoreCase(dsoMap.get(DealerSetupOption.CUSTOM_EMAIL_EXISTS.getOptionKey()))) {
				ieg = (new CommProviderFactory(message.getDealerID())).getCustomEmailGateway("custom");
			} else {
				ieg = (new CommProviderFactory(message.getDealerID())).getCustomEmailGateway("gmail");
			}
			LOGGER.info("got email gateway for message_uuid={} dealer_email_id={} dealer_name={} dealer_associate_name={} dealer_associate_email={}",
					message.getUuid(), dealerEmailID, dealerName, dealerAssociateName, dealerAssociateEmail);
			email = ieg.createMessage();
			LOGGER.info("create ieg message for message_uuid={} dealer_email_id={} dealer_name={} dealer_associate_name={} dealer_associate_email={}",
					message.getUuid(), dealerEmailID, dealerName, dealerAssociateName, dealerAssociateEmail);
			InternetAddress from = new InternetAddress(dealerEmailID, dealerName);
			email.setFrom(from);
			Address[] iA = {new InternetAddress(dealerEmailID)};
			if(dealerAssociateEmail!=null && !dealerAssociateEmail.isEmpty()) {
				InternetAddress fromIA = new InternetAddress(dealerAssociateEmail, dealerAssociateName);
				if(dealerEmailID!=null && !dealerEmailID.isEmpty()) {
					InternetAddress replyTo = new InternetAddress(dealerEmailID, fromIA.getPersonal());
					iA[0] = replyTo;
				}
			} 
			
			email.setReplyTo(iA);
			for(InternetAddress to: toList) {
				email.addRecipient(javax.mail.Message.RecipientType.TO, to);
			}
			for(InternetAddress ccEmail: ccList) {
				email.addRecipient(javax.mail.Message.RecipientType.CC, ccEmail);
			}
			for(InternetAddress bccEmail: bccList) {
				email.addRecipient(javax.mail.Message.RecipientType.BCC, bccEmail);
			}
			if (messageExtn.getSubject() != null && !messageExtn.getSubject().trim().isEmpty()) {
					email.setSubject(messageExtn.getSubject() );
			}

			// Create the message part
			MimeBodyPart mBP = new MimeBodyPart();
			MimeMultipart content = new MimeMultipart();
			mBP.setContent(message.getMessageExtn().getMessageBody(), "text/html; charset=utf-8");
			content.addBodyPart(mBP);
			
			if (message.getDocFiles() != null && !message.getDocFiles().isEmpty()) 
			{
				// Safe Assumption, that there will be not more than 25 files. Gmail's Limit
				List<String> attachments = new ArrayList<String>();
				for(DocFile docFile: message.getDocFiles()) {
					attachments.add(docFile.getDocFileName());
				}
				List<String> attachNames = new ArrayList<String>();
				int numberofFiles = 0;
				attachNames=kCommunicationsUtils.getAttachmentNamesArrayForGivenListOfAttachments(attachments);
				
				numberofFiles=attachNames.size();

				for (int k = 0; k < numberofFiles; k++) {
					if (attachNames.get(k) != null && !attachNames.get(k).trim().isEmpty()) {
						if(attachNames.get(k).startsWith(communicationsCloudFrontURL)) {
							String name = attachNames.get(k).substring(attachNames.get(k).lastIndexOf('/') + 1);
							File temp = new File(HOURLY_TMP_DIR+name);
							FileUtils.copyURLToFile(new URL(attachNames.get(k)), temp);
							attachNames.set(k, temp.getAbsolutePath());
							LOGGER.info(String.format("in sendEmailInternal for dealer_id=%s attachment_name=%s temp_file_absolute_path=%s",message.getDealerID(),attachNames.get(k),temp.getAbsolutePath()));
							
							mBP = new MimeBodyPart();
							DataSource source = new FileDataSource(attachNames.get(k));
							mBP.setDataHandler(new DataHandler(source));
							
							name = name.substring(name.indexOf('_') + 1);
							name = name.substring(name.indexOf('_') + 1);
							mBP.setFileName(name);
							content.addBodyPart(mBP);
							
						} else {
							// the if condition is added when attachments were in /tmp folder of docker container instead of /files. added in apricot sprint
							LOGGER.info(String.format("in sendEmailInternal for dealer_id=%s attachment_name=%s ",message.getDealerID(),attachNames.get(k)));
							
							if(attachNames.get(k).startsWith("/tmp/")) {
								LOGGER.info("attaching file from /tmp");
								kCommunicationsUtils.setAttachments(attachNames, k, mBP, content, "");
								continue;
							}
							if (!UrlValidator.getInstance().isValid(attachNames.get(k))) {
								  String attachmentFilePath = kCommunicationsUtils.getFilePath(attachNames.get(k));
								  attachNames.set(k, attachmentFilePath);
							} else {
								
							  
							  String extension = DEFAULT_EXTENSION;
							  if (attachNames.get(k) != null && attachNames.get(k).contains("payment-files.mykaarma.com")) {
								  String ext = FilenameUtils.getExtension("");
								  if (ext != null && !ext.trim().isEmpty()) {
									  extension = "." + ext;
								  }
							  }
							  else {
								 
									String fileName = attachNames.get(k);
									try {
										extension = fileName.substring(fileName.lastIndexOf('.') + 1);
										if(extension==null || extension.trim().isEmpty()) {
											extension = DEFAULT_EXTENSION;
										}
										else {
											extension = "." + extension; 
										}
									}
									catch(Exception e) {
										extension = DEFAULT_EXTENSION;
									}
									LOGGER.info("fileName={} extension={} dealer-id={}", fileName, extension, message.getDealerID());
							  }
							  File temp = new File(HOURLY_TMP_DIR+System.currentTimeMillis()+getUuid()+extension);
							  FileUtils.copyURLToFile(new URL(attachNames.get(k)), temp);
							  attachNames.set(k, temp.getAbsolutePath());
						  }
							kCommunicationsUtils.setAttachments(attachNames, k, mBP, content, "");
						}
					}

				}

			}
			
			email.setContent(content);
			LOGGER.info("trying to send email for message_uuid={} dealer_email_id={} dealer_name={} dealer_associate_name={} dealer_associate_email={}",
					message.getUuid(), dealerEmailID, dealerName, dealerAssociateName, dealerAssociateEmail);
			ieg.sendMessage(email);
			LOGGER.info("email sent for message_uuid={} dealer_email_id={} dealer_name={} dealer_associate_name={} dealer_associate_email={} customer_email={} ",
					message.getUuid(), dealerEmailID, dealerName, dealerAssociateName, dealerAssociateEmail, toList.get(0));
        } catch(SendFailedException e) {
            if(e.getInvalidAddresses() != null && e.getInvalidAddresses().length != 0) {
                LOGGER.warn("sendEmail failed for message_uuid={} dealer_email_id={} dealer_name={} dealer_associate_name={} dealer_associate_email={} due to invalid_address={} ",
					message.getUuid(), dealerEmailID, dealerName, dealerAssociateName, dealerAssociateEmail, e.getInvalidAddresses());
				throw e;
            } else {
                LOGGER.error("Error in sendEmail for message_uuid={} dealer_email_id={} dealer_name={} dealer_associate_name={} dealer_associate_email={}",
					message.getUuid(), dealerEmailID, dealerName, dealerAssociateName, dealerAssociateEmail, e);
				throw e;
            }
		} catch (Exception e) {
			LOGGER.error("Error in sendEmail for message_uuid={} dealer_email_id={} dealer_name={} dealer_associate_name={} dealer_associate_email={}",
					message.getUuid(), dealerEmailID, dealerName, dealerAssociateName, dealerAssociateEmail, e);
				throw e;
		}catch (Throwable e) {
			LOGGER.error("throwable Error in sendEmail for message_uuid={} dealer_email_id={} dealer_name={} dealer_associate_name={} dealer_associate_email={}",
				message.getUuid(), dealerEmailID, dealerName, dealerAssociateName, dealerAssociateEmail, e);
			throw e;
		}
	}
	
	private Set<String> getDSOList() {
		Set<String> dsoList = new HashSet<>();
		dsoList.add(DealerSetupOption.MESSAGING_OPTOUT_EMAIL_ENABLED_ROLLOUT.getOptionKey());
		dsoList.add(DealerSetupOption.MESSAGING_OUTBOUND_EMAIL_FOOTER_ENABLE.getOptionKey());
		dsoList.add(DealerSetupOption.MESSAGING_OUTBOUND_EMAIL_FOOTER_TEXT.getOptionKey());
		dsoList.add(DealerSetupOption.MESSAGING_DRAFT_BLACKOUTDATE_ENABLE.getOptionKey());
		dsoList.add(DealerSetupOption.MESSAGING_CUSTOMER_LOCK_ENABLE.getOptionKey());
		dsoList.add(DealerSetupOption.COMMUNICATION_POST_MESSAGE_SENT.getOptionKey());
		dsoList.add(DealerSetupOption.COMMUNICATION_POST_MESSAGE_SENT_AUTOMATIC.getOptionKey());
		dsoList.add(DealerSetupOption.CUSTOM_EMAIL_EXISTS.getOptionKey());
		dsoList.add(DealerSetupOption.COMMUNICATIONS_TEXT_FOOTER.getOptionKey());
		return dsoList;
	}
	
	private String getUuid() {

		char[] uuid = new char[36];
		int r;

		// rfc4122 requires these characters
		uuid[8] = uuid[13] = uuid[18] = uuid[23] = '-';
		uuid[14] = '4';

		// Fill in random data. At i==19 set the high bits of clock sequence as
		// per rfc4122, sec. 4.1.5
		for (int i = 0; i < 36; i++) {
			if (uuid[i] == 0) {
				r = (int) (Math.random() * 16);
				uuid[i] = CHARS[(i == 19) ? (r & 0x3) | 0x8 : r & 0xf];
			}
		}
		return new String(uuid);

	}
	
	public void sendThreadPdfToDealerAssociates(String departmentUUID, List<String> attachNames, String subject, String messageBody, Long dealerId, List<InternetAddress> toList, List<InternetAddress> ccList, List<InternetAddress> bccList,
	String dealerEmailID, String dealerName) throws Exception{
		
		try {
			
			MimeMessage email = null;
			IEmailGateway ieg = null;
			LOGGER.info("trying to email gateway for message_uuid={} dealer_email_id={} dealer_name={} ",
					messageBody, dealerEmailID, dealerName);
			
            String dealerUUID = kCommunicationsUtils.getDealerUUIDFromDepartmentUUID(departmentUUID);
    		String customEmail = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, com.mykaarma.global.DealerSetupOption.CUSTOM_EMAIL_EXISTS.getOptionKey());
			if("yes".equalsIgnoreCase(customEmail)) {
				ieg = (new CommProviderFactory(dealerId)).getCustomEmailGateway("custom");
			} else {
				ieg = (new CommProviderFactory(dealerId)).getCustomEmailGateway("gmail");
			}
			LOGGER.info("got email gateway for dealer_email_id={} dealer_name={} ",
					 dealerEmailID, dealerName);
			email = ieg.createMessage();
			LOGGER.info("create ieg message for dealer_email_id={} dealer_name={}",
					 dealerEmailID, dealerName);
			InternetAddress from = new InternetAddress(dealerEmailID, dealerName);
			email.setFrom(from);
			
			for(InternetAddress to: toList) {
				email.addRecipient(javax.mail.Message.RecipientType.TO, to);
			}
			if(ccList!=null && ccList.size()>0) {
				for(InternetAddress ccEmail: ccList) {
					email.addRecipient(javax.mail.Message.RecipientType.CC, ccEmail);
				}
			}
			if(bccList!=null && bccList.size()>0) {
				for(InternetAddress bccEmail: bccList) {
					email.addRecipient(javax.mail.Message.RecipientType.BCC, bccEmail);
				}
			}
			if (subject!= null && !subject.trim().isEmpty()) {
					email.setSubject(subject);
			}

			// Create the message part
			MimeBodyPart mBP = new MimeBodyPart();
			MimeMultipart content = new MimeMultipart();
			mBP.setContent(messageBody, "text/html; charset=utf-8");
			content.addBodyPart(mBP);
			
			if (attachNames!= null && !attachNames.isEmpty()) 
			{
				int numberofFiles = 0;
				
				numberofFiles=attachNames.size();

				for (int k = 0; k < numberofFiles; k++) {
					if (attachNames.get(k) != null && !attachNames.get(k).trim().isEmpty()) {
							
						kCommunicationsUtils.setAttachments(attachNames, k, mBP, content, "");
					}


				}

			}
			
			email.setContent(content);
			LOGGER.info("trying to send email for dealer_email_id={} dealer_name={}",
					dealerEmailID, dealerName);
			ieg.sendMessage(email);
			LOGGER.info("email sent for dealer_email_id={} dealer_name={} customer_email={} ",
					dealerEmailID, dealerName, toList.get(0));
		} catch (Exception e) {
			LOGGER.error("Error in sendEmail for dealer_email_id={} dealer_name={} e={} ",
					dealerEmailID, dealerName, e);
				throw e;
		}
	}
    
    private void handleDraftSendFailureAndPushToPostMessageSentQueue(Message message, FailedDraftEnum failureReason) throws Exception {
        if(MessageType.F.name().equalsIgnoreCase(message.getMessageType()) && message.getDraftMessageMetaData() != null) {
            message.setSentOn(new Date());
			message.setReceivedOn(new Date());
            message.setRoutedOn(new Date());
            message.setDeliveryStatus("0");
            message.setMessageType(MessageType.S.name());
            if(message.getDraftMessageMetaData() != null && (message.getDraftMessageMetaData().getReasonForLastFailure() == null || message.getDraftMessageMetaData().getReasonForLastFailure().isEmpty())) {
                message.getDraftMessageMetaData().setStatus(DraftStatus.FAILED.name());
                message.getDraftMessageMetaData().setReasonForLastFailure(failureReason.getFailureReason());
            }
            message = saveMessageHelper.saveMessage(message);
            rabbitHelper.pushToMessagePostSendingQueue(message, null, false, false, false, null);
        }
    }
}

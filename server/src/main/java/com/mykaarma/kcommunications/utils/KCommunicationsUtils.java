package com.mykaarma.kcommunications.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.ModuleLogCodes;
import com.mykaarma.kcommunications.cache.CacheConfig;
import com.mykaarma.kcommunications.controller.impl.MongoService;
import com.mykaarma.kcommunications.controller.impl.SaveMessageHelper;
import com.mykaarma.kcommunications.controller.impl.VoiceCredentialsImpl;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCredentialsRepository;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.model.jpa.MessageThread;
import com.mykaarma.kcommunications.model.jpa.Thread;
import com.mykaarma.kcommunications.model.jpa.VoiceCredentials;
import com.mykaarma.kcommunications.model.kre.InboundTextRequest;
import com.mykaarma.kcommunications.model.kre.KaarmaRoutingResponse;
import com.mykaarma.kcommunications.model.mvc.SubscriptionSaveEventData;
import com.mykaarma.kcommunications_model.common.AttachmentAttributes;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.NotificationAttributes;
import com.mykaarma.kcommunications_model.common.NotificationButton;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessagePurpose;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.NotificationButtonTheme;
import com.mykaarma.kcommunications_model.enums.Tag;
import com.mykaarma.kcommunications_model.enums.WarningCode;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcustomer_model.dto.CustomerIdentifiers;
import com.mykaarma.kcustomer_model.dto.CustomerUpdateRequest;
import com.mykaarma.kcustomer_model.dto.PhoneDetails;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDepartmentResponseDTO;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.Message.Direction;

@Component
@Service
public class KCommunicationsUtils {

	@Autowired
	VoiceCredentialsRepository voiceCredentialsRepository;

	@Autowired
	AppConfigHelper appConfigHelper;

	@Autowired
	Helper helper;

	@Autowired
	KManageApiHelper kmanageApiHelper;

	@Autowired
	GeneralRepository generalRepo;

	@Autowired
	VoiceCredentialsImpl voiceCredentialsImpl;

	@Autowired
	SaveMessageHelper saveMessageHelper;

	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	MongoService mongoService;

	@Value("${file_save_directory}")
	private String fileSaveDirectory;

	@Value("${kfile_save_directory}")
	private String kfileSaveDirectory;

	@Value("${base_url}")
	private String baseFilePath;

	private final static Logger LOGGER = LoggerFactory.getLogger(KCommunicationsUtils.class);
	private final static Long STATUS_CODE = 404l;
	private static final String DEPT_FOR_DEFAULT_DA = "Service";

	private static XPath xpath = XPathFactory.newInstance().newXPath();

	private static final String OPT_OUT_KEYWORDS = "https://files.mykaarma.com/opt_out_keywords.txt";
	public static final String ATTACHMENT_FILE_PATH = "service/AccessUploadedFileServlet?filename=";
	private static String[] reactions = null;
	private static String[] defaultDealerFooters = null;
	private static String[] reactionSuffixList = new String[]{"\"", "“"};

	public static String getMD5Hash(Object inputObject) throws Exception {

		String orderString = new ObjectMapper().writeValueAsString(inputObject);

		return Arrays.toString(MessageDigest.getInstance("MD5").digest(orderString.getBytes()));
	}

	public static String getRawOrderMongoDocKey(String dealerID, String departmentName, String orderNumber) throws Exception{

		return String.format("%s/%s/%s", dealerID, departmentName, orderNumber);
	}

	public static void addOrRemoveLaborTypeNodes(Document roXMLDoc, int laborTypeCount) throws Exception{

		XPathExpression expr = xpath.compile("//RepairOrderWrap/LaborTypes");
		Node laborOpTypesNode = (Node)expr.evaluate(roXMLDoc, XPathConstants.NODE);
		expr = xpath.compile("//RepairOrderWrap/LaborTypes/LaborType");
		Node laborOpTypeChildNode = (Node)expr.evaluate(roXMLDoc, XPathConstants.NODE);

		if(laborTypeCount>1){

			for(int i=0; i<laborTypeCount-1; i++){

				laborOpTypesNode.appendChild(laborOpTypeChildNode.cloneNode(true));
			}
		}else if(laborTypeCount<1){

			laborOpTypesNode.removeChild(laborOpTypeChildNode);
		}
	}

	public static void addOrRemoveTechnicianNodes(Document roXMLDoc, int techCount) throws Exception{

		XPathExpression expr = xpath.compile("//RepairOrderWrap/Technicians");
		Node techsNode = (Node)expr.evaluate(roXMLDoc, XPathConstants.NODE);
		expr = xpath.compile("//RepairOrderWrap/Technicians/Technician");
		Node techNode = (Node)expr.evaluate(roXMLDoc, XPathConstants.NODE);

		if(techCount>1){

			for(int i=0; i<techCount-1; i++){

				techsNode.appendChild(techNode.cloneNode(true));
			}
		}else if(techCount<1){

			techsNode.removeChild(techNode);
		}
	}


	public static String convertDomToString(Document doc) throws Exception{
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			return writer.toString();
		} catch(TransformerException e){
			throw e;
		}
	}

	public static void setStringValue(Document pushJobsRequestDoc, String xpathStr, String value) throws Exception{

		XPathExpression expr = xpath.compile(xpathStr);
		Node resultNode = (Node)expr.evaluate(pushJobsRequestDoc, XPathConstants.NODE);

		if(value!=null){

			resultNode.setTextContent(value);
		}else{

			try {
				pushJobsRequestDoc.removeChild(resultNode);
			} catch (Exception e) {

			}
		}

	}

	public Response checkIfFeatureEnabledForDealership(String dealerUUID,String optionKey,Response response) throws Exception{

		String dsoForMessagingSignalingEnable = kmanageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, optionKey);

		LOGGER.info(String.format("in checkIfFeatureEnabledForDealership for dealer_uuid=%s dealer_setup_option=%s value=%s",dealerUUID,
			optionKey,dsoForMessagingSignalingEnable));

		if(dsoForMessagingSignalingEnable==null || dsoForMessagingSignalingEnable.isEmpty()){
			List<ApiWarning> warnings = new ArrayList<ApiWarning>();

			String warningDescription=String.format("dealer_setup_option=%s is not enabled for the message belonging to dealership"
				,optionKey );
			response =helper.getWarningResponse(WarningCode.FEATURE_NOT_ENABLED.name(), warningDescription, warnings, response);
		}
		return response;

	}

	public Date getPstDateFromIsoDate(String date) {
		try {
			DateTimeFormatter parser2 = ISODateTimeFormat.dateTimeNoMillis();
			LOGGER.info(String.format("getPstDateFromIsoDate iso_date=%s pst_date=%s ", date, parser2.parseDateTime(date).toDate()));
			return parser2.parseDateTime(date).toDate();
		} catch (Exception e) {
			LOGGER.error(String.format("Error in parsing date for date=%s ",date), e);
		}
		return null;
	}

	public Boolean validateEmail(String email) {
		if(email == null || email.length() == 0) {
			return false;
		}

		String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
			"[a-zA-Z0-9_+&*-]+)*@" +
			"(?:[a-zA-Z0-9-]+\\.)+[a-z" +
			"A-Z]{2,7}$";
		Pattern pattern = Pattern.compile(emailRegex);
		return pattern.matcher(email).matches();

	}

	public boolean validatePhoneNumber(String phoneNumber) {
		if(phoneNumber == null || phoneNumber.isEmpty()) {
			return false;
		}
		PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
		try {
			PhoneNumber number = phoneNumberUtil.parse(phoneNumber, APIConstants.US_COUNTRY_CODE);
			return phoneNumberUtil.isValidNumber(number);
		} catch (Exception e) {
			LOGGER.warn("error while parsing phone_number={}", phoneNumber, e);
			return false;
		}
	}

	public static String getNumberInInternationalFormat(String phoneNumber) {
		if(phoneNumber == null || phoneNumber.isEmpty()) {
			return null;
		}
		PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
		try {
			PhoneNumber number = phoneNumberUtil.parse(phoneNumber, APIConstants.US_COUNTRY_CODE);
			return "+" + number.getCountryCode() + number.getNationalNumber();
		} catch (Exception e) {
			LOGGER.warn("error in getNumberInInternationalFormat for phone_number={}", phoneNumber, e);
			if(phoneNumber.startsWith("+")) {
				return phoneNumber;
			} else {
				return APIConstants.COUNTRY_CODE + phoneNumber;
			}
		}
	}

	public static String addCountryCodeIfNotPresent(String phoneNumber) {
		if(phoneNumber == null || phoneNumber.isEmpty()) {
			return null;
		}
		PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
		try {
			PhoneNumber number = phoneNumberUtil.parse(phoneNumber, APIConstants.US_COUNTRY_CODE);
			return "" + number.getCountryCode() + number.getNationalNumber();
		} catch (Exception e) {
			LOGGER.warn("error in addCountryCodeIfNotPresent for phone_number={}", phoneNumber, e);
			if(phoneNumber.startsWith("+")) {
				return phoneNumber.substring(1);
			} else {
				return phoneNumber;
			}
		}
	}

	public static String removeCountryCodeIfPresent(String phoneNumber) {
		if(!StringUtils.hasText(phoneNumber)) {
			return null;
		}
		PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
		try {
			Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, APIConstants.US_COUNTRY_CODE);
			return "" + number.getNationalNumber();
		} catch (Exception e) {
			LOGGER.warn("error in removeCountryCodeIfPresent for phone_number={}", phoneNumber, e);
			if(phoneNumber.startsWith("+")) {
				return phoneNumber.substring(2); // removing "+1"
			}
			return phoneNumber;
		}
	}

	public void setAttachments(List<String> attachNames, int k, MimeBodyPart mBP, MimeMultipart content, String attachName) throws Exception {

		if (new java.io.File(attachNames.get(k)).exists()) {
			// Part two is attachment
			mBP = new MimeBodyPart();
			DataSource source = new FileDataSource(attachNames.get(k));
			mBP.setDataHandler(new DataHandler(source));

			if (attachName == null || attachName.equals("")) {
				String name = source.getName().substring(source.getName().indexOf('_') + 1);
				mBP.setFileName(name);
			} else {
				String name = (attachName.trim().equalsIgnoreCase("")) ? attachName : source.getName();
				mBP.setFileName(name);
			}
			content.addBodyPart(mBP);

			LOGGER.info(String.format("setAttachments attachment_url=%s attachment_name=%s ", attachNames.get(k),attachName));
		} else {
			LOGGER.error("Invalid file path: {}", attachNames.get(k));

		}
	}

	public String getRecordingSID(String recordingUrl) throws Exception{

		String recordingSID = recordingUrl.substring(recordingUrl.lastIndexOf("/")+1);
		return recordingSID;
	}

	public String getUpdatedRecordingurlinMessageBody(String messageBody, String recordingUrl) throws Exception{


		DocumentBuilderFactory dbFactory
			= DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(messageBody));
		Document parse = dBuilder.parse(is);
		Element documentElement = parse.getDocumentElement();
		documentElement.setAttribute("url", recordingUrl);

		DOMSource domSource = new DOMSource(parse);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);
		messageBody = writer.toString();
		return messageBody;

	}

	public void updateRecordingTranscript(com.mykaarma.kcommunications.model.jpa.Message message, String transcript) {

		String messageBody = message.getMessageExtn().getMessageBody();
		LOGGER.info("MessageBody = {}, messageID = {}", messageBody, message.getId());
		if (messageBody.contains("transcription=")) {
			try {
				DocumentBuilderFactory dbFactory
					= DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				InputSource is = new InputSource(new StringReader(messageBody));
				Document parse = dBuilder.parse(is);
				Element documentElement = parse.getDocumentElement();
				documentElement.setAttribute("transcription", transcript);

				DOMSource domSource = new DOMSource(parse);
				StringWriter writer = new StringWriter();
				StreamResult result = new StreamResult(writer);
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer transformer = tf.newTransformer();
				transformer.transform(domSource, result);
				messageBody = writer.toString();
				message.getMessageExtn().setMessageBody(messageBody);
				try {
					message = saveMessageHelper.saveMessage(message);
				} catch (Exception e) {
					LOGGER.info("Error while saving message for messageID = {}", message.getId(), e);
				}
			} catch (Exception e) {
				LOGGER.error("Unable to parse and update xml = {}, messageID = {}", messageBody, message.getId());
			}
		}
	}

	public String getRecordingURLForMessageBody(String messageBody) {

		if (messageBody.contains("url=")) {
			try {
				DocumentBuilderFactory dbFactory
					= DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				InputSource is = new InputSource(new StringReader(messageBody));
				Document parse = dBuilder.parse(is);
				Element documentElement = parse.getDocumentElement();
				return documentElement.getAttribute("url");

			}catch (Exception e){
				LOGGER.error("Unable to parse and get recording_url for message_body={}",messageBody);
			}
		}

		return null;
	}

	public InputStream getInputSreamOfAudio(String recordingurl) throws Exception{

		return new URL(recordingurl).openConnection().getInputStream();

	}

	@Cacheable(value=CacheConfig.DEALER_SUBACCOUNT_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public String getDealerSubAccountBySid(String accountSID) {

		LOGGER.info("fetching dealerSubaccount for account_sid={}", accountSID);
		return voiceCredentialsRepository.findByAccountSid(accountSID.trim() + "~" + "%");

	}
	
	@Cacheable(value=CacheConfig.CUSTOMER_MERGING_MONGO_THREAD_SCHEMA_CACHE, keyGenerator = "customKeyGenerator", unless="#result == null")
	public HashMap<String, List<String>> getCustomerMergeMongoThreadSchema() {
		return mongoService.getCustomerMergeMongoThreadSchema();
	}
	
	@Cacheable(value=CacheConfig.CUSTOMER_MERGING_MONGO_THREAD_SCHEMA_CACHE, keyGenerator = "customKeyGenerator", unless="#result == null")
	public HashMap<String, List<String>> getCustomerMergeMongoDealerOrderSchema() {
		return mongoService.getCustomerMergeMongoDealerOrderSchema();
	}

	public HttpHeaders getAuthorizationHeader(String dealerSubAccount) {

		String[] creds = dealerSubAccount.split("~");
		HttpHeaders headers = new HttpHeaders();
		String userPass = creds[0] + ":" + creds[1];
		String authHeaderValue = "Basic " + Base64.getEncoder().encodeToString(userPass.getBytes());
		headers.set(HttpHeaders.AUTHORIZATION, authHeaderValue);
		return headers;
	}

	public Long getBytesFromAudio(String recordingurl) throws Exception{

		HttpURLConnection conn = null;
		try {
			URL myURL = new URL(recordingurl);
			conn = (HttpURLConnection) myURL.openConnection();
			conn.setRequestMethod("HEAD");
			return conn.getContentLengthLong();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public String getBrokerNumberForCaller(String accountSID) {
		return voiceCredentialsRepository.getBrokerNumberForCaller(accountSID);
	}

	public String getBrokerNumber(Long dealerId, Long customerId) {
		return getBrokerNumberForDepartment(dealerId, customerId, DEPT_FOR_DEFAULT_DA);
	}

	public String getBrokerNumberForDepartment(Long dealerId, Long customerId, String department) {
		if (department == null || department.trim().isEmpty()) {
			department = DEPT_FOR_DEFAULT_DA;
		}

		String departmentUUID = generalRepo.getDepartmentUUIDForDealerID(dealerId, department);
		GetDealerAssociateResponseDTO da = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);

		com.mykaarma.kcommunications.model.jpa.Message message = new com.mykaarma.kcommunications.model.jpa.Message();
		message.setDealerDepartmentId(da.getDealerAssociate().getDepartmentExtendedDTO().getId());
		message.setCustomerID(customerId);
		message.setDealerID(dealerId);

		VoiceCredentials vc = voiceCredentialsImpl.getVoiceCredentialsForMessage(message);
		return vc.getBrokerNumber();

	}

	public List<String> getAttachmentNamesArrayForGivenListOfAttachments(List<String> attachments) {
		List<String> attachNames = new ArrayList<String>();
		int numberofFiles = 0;
		LOGGER.info(String.format(ModuleLogCodes.MESSAGING_INFO_CODE.getLogMessage() + "in getAttachmentNamesArrayForGivenListOfAttachments for attachments_list=%s", attachments));

		// It will contain atleast 1 element, unless it is a ":" string
		for (String s : attachments) {
			LOGGER.info(String.format(ModuleLogCodes.MESSAGING_INFO_CODE.getLogMessage() + "in getAttachmentNamesArrayForGivenListOfAttachments for attachment_name=%s", s));

			if(!s.trim().isEmpty()) {
				// Additional Safeguard	
				if(numberofFiles > 24)
					break;
				attachNames.add(s);
				LOGGER.info(String.format(ModuleLogCodes.MESSAGING_INFO_CODE.getLogMessage()+ "in getAttachmentNamesArrayForGivenListOfAttachments Uploaded File's via outgoing AWS=%s attachment_count=%s", s,numberofFiles+1));
				numberofFiles++;
			}
		}
		return attachNames;
	}

	public String getFilePath(String filePath) {
		String kfPath = kfileSaveDirectory+(filePath.startsWith("/")?filePath:("/"+filePath));
		String fPath = fileSaveDirectory+(filePath.startsWith("/")?filePath:("/"+filePath));
		File kfile = new File(kfPath);
		if(kfile.exists()) {
			LOGGER.info("File served from file_storage=kfiles filepath=\"{}\"",filePath);
			return kfPath;
		} else {
			LOGGER.info("File served from file_storage=files filepath=\"{}\"",filePath);
			return fPath;
		}
	}

	public InboundTextRequest createInboundTextRequest(Message message) {

		InboundTextRequest inboundTextRequest = new InboundTextRequest();
		inboundTextRequest.setAccountSid(message.getAccountSid());
		inboundTextRequest.setBody(message.getBody());
		inboundTextRequest.setSid(message.getSid());
		inboundTextRequest.setNumMedia("0");
		inboundTextRequest.setReceivedDate(Date.from(message.getDateSent().toInstant()));
		inboundTextRequest.setSentDate(Date.from(message.getDateSent().toInstant()));
		
		if(Direction.OUTBOUND_API.equals(message.getDirection())) {
			inboundTextRequest.setFrom(getNumber(message.getTo().toString()));
			inboundTextRequest.setTo(getNumber(message.getFrom().toString()));
		}
		else {
			inboundTextRequest.setFrom(getNumber(message.getFrom().toString()));
			inboundTextRequest.setTo(getNumber(message.getTo().toString()));
		}

		return inboundTextRequest;
	}

	private String getNumber(String phoneNumber) {

		String phoneNumberWithoutExtension = phoneNumber.substring(phoneNumber.length()-10);
		return phoneNumberWithoutExtension;
	}

	public com.mykaarma.kcommunications.model.jpa.Message createMessageObject(KaarmaRoutingResponse response, String messageBody, Date date,
																			  com.mykaarma.kcommunications_model.enums.MessageProtocol protocol, MessageType type, String fromNum, String toNum,
																			  String fromName, String toName, String communicationUid) {

		com.mykaarma.kcommunications.model.jpa.Message message = new com.mykaarma.kcommunications.model.jpa.Message();

		message.setDealerDepartmentId(response.getRoutingRuleResponse().getDealerDepartmentID());
		message.setIsManual(true);
		message.setMessageSize(messageBody.length());
		message.setMessageType(type.getMessageType());
		message.setNumberofMessageAttachments(0);
		message.setProtocol(protocol.getMessageProtocol());
		message.setReceivedOn(date);
		message.setRoutedOn(date);
		message.setSentOn(date);
		message.setToNumber(toNum);
		message.setToName(toName);
		message.setFromName(fromName);
		message.setFromNumber(fromNum);
		message.setUuid(helper.getBase64EncodedSHA256UUID());
		message.setCustomerID(response.getRoutingRuleResponse().getCustomerID());
		message.setDealerID(response.getRoutingRuleResponse().getDealerID());
		message.setDealerAssociateID(response.getRoutingRuleResponse().getDealerAssociateID());
		message.setDeliveryStatus("1");
		message.setCommunicationUid(communicationUid);
		return message;

	}

	public com.mykaarma.kcommunications.model.jpa.MessageExtn createMessageExtnObject(String messageBody, String suject) {

		MessageExtn messageExtn = new MessageExtn();

		messageExtn.setSubject(suject);
		messageExtn.setMessageBody(messageBody);

		return messageExtn;

	}

	public CustomerUpdateRequest createCustomerRequestObject(String firstName, String lastName, String phoneNumber) {

		CustomerUpdateRequest customerUpdateRequest = new CustomerUpdateRequest();
		CustomerIdentifiers CustomerIdentifiers = new CustomerIdentifiers();
		Set<PhoneDetails> set = new HashSet<PhoneDetails>();
		CustomerIdentifiers.setFirstName(firstName);
		CustomerIdentifiers.setLastName(lastName);
		PhoneDetails phoneDetails = new PhoneDetails();
		phoneDetails.setLabel("cell");
		phoneDetails.setOkToCall(true);
		phoneDetails.setOkToText(true);
		if(phoneNumber.contains(APIConstants.COUNTRY_CODE)) {
			phoneDetails.setPhoneNumber(phoneNumber);
		} else {
			phoneDetails.setPhoneNumber(APIConstants.COUNTRY_CODE+phoneNumber);
		}
		phoneDetails.setIsPreferred(true);
		set.add(phoneDetails);
		CustomerIdentifiers.setPhone(set);
		customerUpdateRequest.setCustomer(CustomerIdentifiers);

		return customerUpdateRequest;

	}

	public Thread createThreadObject(Long customerID, Long dealerAssociateID, Long departmentID, Long dealerID, Boolean isArchived,
									 Boolean isClosed, Date receivedDate) {

		Thread thread = new Thread();
		thread.setArchived(isArchived);
		thread.setClosed(isClosed);
		thread.setCustomerID(customerID);
		thread.setDealerDepartmentID(departmentID);
		thread.setDealerID(dealerID);
		thread.setLastMessageOn(receivedDate);
		thread.setDealerAssociateID(dealerAssociateID);
		return thread;
	}

	public MessageThread createMessageThreadObject(Long messageId, Long threadId) {

		MessageThread messageThread = new MessageThread();
		messageThread.setMessageID(messageId);
		messageThread.setThreadID(threadId);
		return messageThread;
	}

	public Boolean checkIfAudioExistsOrNot(String tempTwilioUrl) throws Exception{

		HttpURLConnection conn = null;
		try {
			URL myURL = new URL(tempTwilioUrl);
			conn = (HttpURLConnection) myURL.openConnection();
			conn.setRequestMethod("HEAD");
			int statusCode = conn.getResponseCode();

			if(!Long.valueOf(statusCode).equals(STATUS_CODE)) {
				LOGGER.info("status code for tempTwilioUrl={} status_code={} returning true", tempTwilioUrl, statusCode);
				return true;
			} else {
				LOGGER.info("status code for tempTwilioUrl={} status_code={} returning false", tempTwilioUrl, statusCode);
				return false;
			}
		} catch (Exception e) {
			LOGGER.error("unable to get twilio_url={}", tempTwilioUrl, e);
			throw e;
		}
	}

	public Date getXDaysBackDateForGivenHours(String delayHours) {

		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(new Date());
		calendar.add(Calendar.HOUR, Integer.valueOf("-"+delayHours));

		Date calendarXDaysBack = calendar.getTime();
		return calendarXDaysBack;
	}

	public String fetchTwilioUrlForMessage(Long messageID) {

		return generalRepo.getRecordingUrlFromTempTable(messageID);

	}

	public List<ApiError> getApiError(List<String> errorDescriptions){

		List<ApiError> apiErrors = new ArrayList<ApiError>();
		for(String errorDescription: errorDescriptions) {
			ApiError apiError = new ApiError();
			apiError.setErrorDescription(errorDescription);
			apiErrors.add(apiError);
		}
		return apiErrors;
	}

	@SuppressWarnings("deprecation")
	public Date getDealerDateForServerDate(Date date, String dealerUUID)
		throws Exception {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateStr = sdf.format(date);
		date = sdf.parse(dateStr);

		Date currentDate = new Date();
		int serverTimezoneOffset = -currentDate.getTimezoneOffset();
		TimeZone tz = getTimeZone(dealerUUID);
		int dealerTimezoneOffset = tz.getOffset(currentDate.getTime())
			/ (60 * 1000);
		int timezoneshift = serverTimezoneOffset - dealerTimezoneOffset;
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(date);
		cal.add(Calendar.MINUTE, -timezoneshift);

		Date dealerDate = new Date();
		dealerDate.setTime(cal.getTimeInMillis());
		return dealerDate;
	}

	public TimeZone getTimeZone(String dealerUUID) throws Exception {

		String dealerTimeZone = "PST";
		String timeZoneValue = kmanageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.DEALER_TIMEZONE_ID.getOptionKey());
		LOGGER.info("timeZpne received is={}", timeZoneValue);
		if (timeZoneValue != null && !timeZoneValue.isEmpty()) {
			dealerTimeZone = timeZoneValue;
		}
		return TimeZone.getTimeZone(dealerTimeZone);

	}

	public Boolean isMessageReactionType(String messageBody) {
		if(reactions == null || defaultDealerFooters == null) {
			KCommunicationsUtils.fillReactionsAndDefaultDealerFooters();
		}
		LOGGER.info(String.format("supported reactions=%s", Arrays.toString(reactions)));
		if(messageBody != null && !messageBody.isEmpty()) {
			for(String reaction : reactions) {
				for(String reactionSuffix : reactionSuffixList) {
					String prefix = reaction.toLowerCase() + " " + reactionSuffix;
					String suffix = reactionSuffix;
					if(messageBody.toLowerCase().startsWith(prefix) && messageBody.toLowerCase().endsWith(suffix)) {
						LOGGER.info(String.format("in isMessageReactionType message_body=%s is a reaction_type=%s message", messageBody, reaction));
						return true;
					}
				}
			}
		}
		return false;
	}

	public Boolean isMessageDealerType(String messageBody, String dealerFooterFromDSO) {
		if(messageBody == null || messageBody.isEmpty())
			return false;
		if(dealerFooterFromDSO != null) {
			if(messageBody.toLowerCase().contains(dealerFooterFromDSO.toLowerCase())) {
				LOGGER.info(String.format("in isMessageDealerType message_body=%s is a dealer_type=%s message", messageBody, dealerFooterFromDSO));
				return true;
			}
		}
		if(reactions == null || defaultDealerFooters == null) {
			KCommunicationsUtils.fillReactionsAndDefaultDealerFooters();
		}
		LOGGER.info(String.format("supported default_dealer_footers=%s", Arrays.toString(defaultDealerFooters)));
		for(String dealerFooter : defaultDealerFooters) {
			if(messageBody.toLowerCase().contains(dealerFooter.toLowerCase())) {
				LOGGER.info(String.format("in isMessageDealerType message_body=%s is a dealer_type=%s message", messageBody, dealerFooter));
				return true;
			}
		}
		return false;
	}


	public String lowerCaseAndStripReactionAndPunctuations(String messageBody) {
		if(messageBody == null || messageBody.isEmpty()) {
			return "";
		}
		messageBody = messageBody == null ? "" : messageBody;
		messageBody = messageBody.toLowerCase().trim();
		if(reactions == null || defaultDealerFooters == null) {
			KCommunicationsUtils.fillReactionsAndDefaultDealerFooters();
		}
		LOGGER.info(String.format("supported reactions=%s", Arrays.toString(reactions)));
		for(String reaction : reactions) {
			for(String reactionSuffix : reactionSuffixList) {
				String prefix = reaction.toLowerCase() + " " + reactionSuffix;
				String suffix = reactionSuffix;
				if(messageBody.startsWith(prefix) && messageBody.endsWith(suffix)) {
					LOGGER.info(String.format("in lowerCaseAndStripReactionAndPunctuations message_body=%s is a reaction_type=%s message", messageBody, reaction));
					messageBody = messageBody.substring(prefix.length(), messageBody.length() - suffix.length());
					break;
				}
			}
		}
		// replace all punctuations except # (# represents number sometimes)
		return messageBody.replaceAll("[\\p{Punct}&&[^#]]+", " ").replaceAll("[\\s]+", " ").trim();
	}

	private static void fillReactionsAndDefaultDealerFooters() {
		try {
			String[] keywords = null;
			URL url = new URL(OPT_OUT_KEYWORDS);
			keywords = IOUtils.toString(url.openStream(), "UTF-8").split(";");
			reactions = keywords[0].split("=")[1].split(",");
			defaultDealerFooters = keywords[1].split("=")[1].split(",");
		} catch(Exception e) {
			reactions = new String[] {
				"liked", "removed a like from",
				"loved", "removed a heart from",
				"exclaimed", "removed an exclaimation from",
				"laughed at", "removed a laugh from",
				"disliked", "removed a dislike from",
				"questioned", "removed a question from",
				"emphasized", "removed emphasis from"
			};
			defaultDealerFooters = new String[] {
				"Text STOP to opt-out",
				"Text STOP for NO text"
			};
		}
	}

	public String getCompleteFilePath(String attachment) {

		return baseFilePath + ATTACHMENT_FILE_PATH + attachment;

	}

	public SendMessageRequest getSendMessageRequest(MessageAttributes messageAttributes, MessageSendingAttributes messageSendingAttributes,
													NotificationAttributes notificationAttributes) {
		SendMessageRequest sendMessageRequest = new SendMessageRequest();
		sendMessageRequest.setMessageAttributes(messageAttributes);
		sendMessageRequest.setMessageSendingAttributes(messageSendingAttributes);
		sendMessageRequest.setNotificationAttributes(notificationAttributes);
		return sendMessageRequest;
	}

	public MessageAttributes getMessageAttributes(String messageBody, Boolean isManualMessage, MessageProtocol messageProtocol, MessagePurpose messagePurpose,
												  String subject, List<Tag> tags, MessageType messageType, Boolean updateThreadTimestamp, Boolean updateTotalMessageCount, Boolean showInCustomerConversation,
												  HashMap<String, String> metaData, ArrayList<AttachmentAttributes> attachments) {
		MessageAttributes messageAttributes = new MessageAttributes();
		messageAttributes.setBody(messageBody);
		messageAttributes.setIsManual(isManualMessage);
		messageAttributes.setProtocol(messageProtocol);
		messageAttributes.setPurpose(messagePurpose);
		messageAttributes.setSubject(subject);
		messageAttributes.setTags(tags);
		messageAttributes.setType(messageType);
		messageAttributes.setUpdateThreadTimestamp(updateThreadTimestamp);
		messageAttributes.setUpdateTotalMessageCount(updateTotalMessageCount);
		messageAttributes.setShowInCustomerConversation(showInCustomerConversation);
		messageAttributes.setMetaData(metaData);
		messageAttributes.setAttachments(attachments);
		return messageAttributes;
	}

	public MessageSendingAttributes getMessageSendingAttributes(List<String> listOfEmailsToBeCCed, List<String> listOfEmailsToBeBCCed,
																Boolean addFooter, Boolean addSignature, Boolean addTCPAFooter, String callbackURL, String communicationValueOfCustomer,
																Integer delay, Boolean overrideHolidays, Boolean overrideOptoutRules, Boolean sendSynchronously, Boolean sendVCard) {
		MessageSendingAttributes messageSendingAttributes = new MessageSendingAttributes();
		messageSendingAttributes.setListOfEmailsToBeCCed(listOfEmailsToBeCCed);
		messageSendingAttributes.setListOfEmailsToBeBCCed(listOfEmailsToBeBCCed);
		messageSendingAttributes.setAddFooter(addFooter);
		messageSendingAttributes.setAddSignature(addSignature);
		messageSendingAttributes.setAddTCPAFooter(addTCPAFooter);
		messageSendingAttributes.setCallbackURL(callbackURL);
		messageSendingAttributes.setCommunicationValueOfCustomer(communicationValueOfCustomer);
		messageSendingAttributes.setDelay(delay);
		messageSendingAttributes.setOverrideHolidays(overrideHolidays);
		messageSendingAttributes.setOverrideOptoutRules(overrideOptoutRules);
		messageSendingAttributes.setSendSynchronously(sendSynchronously);
		messageSendingAttributes.setSendVCard(sendVCard);
		return messageSendingAttributes;
	}

	public NotificationAttributes getNotificationAttributes(Boolean threadOwnerNotifierPop, Boolean externalSubscribersNotifierPop,
															Boolean internalSubscribersNotifierPop, List<String> additionalNotifierNotificationDAUUIDs, List<NotificationButton> notificationButtons) {
		NotificationAttributes notificationAttributes = new NotificationAttributes();
		notificationAttributes.setThreadOwnerNotifierPop(threadOwnerNotifierPop);
		notificationAttributes.setExternalSubscribersNotifierPop(externalSubscribersNotifierPop);
		notificationAttributes.setInternalSubscribersNotifierPop(internalSubscribersNotifierPop);
		notificationAttributes.setAdditionalNotifierNotificationDAUUIDs(additionalNotifierNotificationDAUUIDs);
		notificationAttributes.setNotificationButtons(notificationButtons);
		return notificationAttributes;
	}

	public NotificationButton getNotificationButton(String buttonTextTranslationWidgetKey, String buttonTextTranslationKey, String buttonDefaultText,
													NotificationButtonTheme buttonTheme, HashMap<String, String> buttonClickEventData) {
		NotificationButton notificationButton = new NotificationButton();
		notificationButton.setButtonTextTranslationWidgetKey(buttonTextTranslationWidgetKey);
		notificationButton.setButtonTextTranslationKey(buttonTextTranslationKey);
		notificationButton.setButtonDefaultText(buttonDefaultText);
		notificationButton.setButtonTheme(buttonTheme);
		notificationButton.setButtonActionEventData(buttonClickEventData);
		return notificationButton;
	}

	public String getDealerUUIDFromDepartmentUUID(String departmentUUID) {
		GetDepartmentResponseDTO getDepartmentResponseDTO = kmanageApiHelper.getDealerDepartment(departmentUUID);
		String dealerUUID = getDepartmentResponseDTO.getDepartmentExtendedDTO().getDealerMinimalDTO().getUuid();
		return dealerUUID;
	}

	public SubscriptionSaveEventData getSubscriptionSaveEventDataFromSubscriberInfo(Long dealerId, Long customerId, Long departmentId, String customerUuid) {

		SubscriptionSaveEventData subscriptionSaveEventData = new SubscriptionSaveEventData();
		subscriptionSaveEventData.setCustomerID(customerId);
		subscriptionSaveEventData.setCustomerUUID(customerUuid);
		subscriptionSaveEventData.setDealerDepartmentID(departmentId);
		subscriptionSaveEventData.setDealerID(dealerId);
		return subscriptionSaveEventData;

	}

}

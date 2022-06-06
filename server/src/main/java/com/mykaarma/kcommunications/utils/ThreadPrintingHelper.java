package com.mykaarma.kcommunications.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.mail.internet.InternetAddress;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.kaarya.utils.XMLHandler;
import com.mykaarma.kcommunications.controller.impl.CommunicationsApiImpl;
import com.mykaarma.kcommunications.controller.impl.SendEmailHelper;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageExtnRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.MessagingUtils;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.CommunicationHistoryResponse;

@Service
public class ThreadPrintingHelper {

	@Autowired
	GeneralRepository generalRepo;
	
	@Autowired
	KCustomerAPIHelper kCustomerApiHelper;
	
	@Autowired
	KCommunicationsUtils kCommunicationsUtils;
	
	@Autowired
	MessageRepository messageRepo;
	
	@Autowired
	MessageExtnRepository messageExtnRepo;
	
	@Autowired
	Helper helper;
	
	@Autowired
	SendEmailHelper sendEmailHellper;
	
	@Autowired
	AWSClientUtil awsClientUtil;
	
	@Autowired
	AppConfigHelper appConfigHelper;
	
	@PersistenceContext
	EntityManager entityManager;
	
	@Value("${file_save_directory}")
	private String filesSaveDirectory;

	@Value("${base_url}")
	private String baseUrl;
	
	@Value("${temp_save_directory}")
	private String tempSaveDirectory;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPrintingHelper.class);	
	private static final String PDF_EXTENSION = ".pdf";
	private static final String subjectMessage = "Message thread PDF for %s is Complete";
	private static final String messageBody = "Hi your requested PDF for customer %s is attached below. Thank you for trusting us with your communications.";
	private static final String PDF_CONTENT_TYPE = "application/pdf";
	public static final String COMMUNICATIONS_THREAD_PRINTING_FOLDER_PREFIX="/printing/";
	public ResponseEntity<CommunicationHistoryResponse> getCommunicationHistory(String departmentUUID, String customerUUID,
			CommunicationHistoryRequest commHistoryRequest) throws Exception {

		CommunicationHistoryResponse communicationHistoryResponse = new CommunicationHistoryResponse();
		List<Message> messages = new ArrayList<Message>();
		Long dealerID = generalRepo.getDealerIDFromDepartmentUUID(departmentUUID);
		String pdfUrl = null;
		String pdfPath = null;
		String dealerName = null;
		String dealerEmailId = null;
		Long customerID = null;
		String partner = null;
		String htmlReceipt=null;
		String htmlUrl=null;
		List<String> attachmentNames = new ArrayList<String>();
		List<InternetAddress> toList = new ArrayList<InternetAddress>();
		String customerName = null;
		
		try {
			try {
				messages = getCommunicationHistory(dealerID, customerUUID, commHistoryRequest, communicationHistoryResponse);
				LOGGER.info("message_size={} for customer_uuid={} dealer_id={}", messages.size(), customerUUID, dealerID);
			} catch(Exception e) {
				List<ApiError> apiErrors = new ArrayList<ApiError>();
				ApiError error = new ApiError();
				error.setErrorDescription(ErrorCode.PRINT_ERROR_GETLIST.name());
				apiErrors.add(error);
				communicationHistoryResponse.setErrors(apiErrors);
				LOGGER.error(String.format("Exception in getting list of messages for threads of dealer_uuid=%s customer_uuid=%s ",
					dealerID, customerUUID), e);
				return new ResponseEntity<CommunicationHistoryResponse>(communicationHistoryResponse, HttpStatus.BAD_REQUEST);
			}
			
			LOGGER.info("Completed fetching data from DB and received the response in HTML function for dealer_id={}, customer_uuid={}",dealerID,customerUUID);
			if(communicationHistoryResponse.getErrors() != null 
					) {
				return new ResponseEntity<CommunicationHistoryResponse>(communicationHistoryResponse,HttpStatus.BAD_REQUEST);
			}
			
			try {
				customerUUID = kCustomerApiHelper.getPrimaryCustomerGUIDForCustomerGUID(customerUUID, dealerID);
			} catch (Exception e) {
				LOGGER.error(String.format("Error in get primary customer for customer_uuid=%s dealer_id=%s ", customerUUID, dealerID), e);
			}
			
			customerID = generalRepo.getCustomerIDForUUID(customerUUID);
			
		    partner = "";
			try{
				partner=messageRepo.getFrontEndPartnerForDealer(dealerID);
			}catch(Exception e) {
				LOGGER.error(String.format("Error fetching front end partner for dealer_ID=%s",dealerID),e);
			}
			LOGGER.info("Fetched the partner in HTML function partner={} dealer_id={} customer_uuid={}", partner, dealerID, customerUUID);
			
			if(customerID!=null) {
				customerName = generalRepo.getCustomerNameFromId(customerID);
			}
			LOGGER.info("Before creating the html receipt in HTML function, messages_size={} for delaer_id={} customer_id={}  customer_uuid={}", messages == null ? 0 : messages.size(),dealerID,customerID, customerUUID);
			if(MessagingUtils.PARTNER.MK.getValue().equalsIgnoreCase(partner))
				htmlReceipt= getHTMLForPrintingCustomerThread(customerID, "html", "1", dealerID, "1", "messageThread_html_for_print.xslt",partner,messages, customerName);
			else 
				htmlReceipt= getHTMLForPrintingCustomerThread(customerID, "html", "1", dealerID, "1", "messageThread_html_for_print_affinitiv.xslt",partner,messages, customerName);
			LOGGER.info("Created the html receipt in HTML function htmlReceipt: for delaer_id={} customer_id={} customer_uuid={}",dealerID,customerID,customerUUID);
			
			if(htmlReceipt!=null && !htmlReceipt.isEmpty()) {
				htmlReceipt = htmlReceipt.replaceAll("&nbsp;", " ");
			}
			
			htmlUrl=getHtmlUrl(htmlReceipt,customerID);
			LOGGER.info("html_url={} for dealer_id={} customer_uuid={}", htmlUrl, dealerID, customerUUID);
			try {
				pdfUrl = getPdfUrlFromHtmlUrl(htmlUrl, customerID);
				pdfPath = pdfUrl;
				LOGGER.info("pdf_url={} for dealer_id={} customer_uuid={}", pdfUrl, dealerID, customerUUID);
			}
			catch(Exception e) {
				LOGGER.error("unable to generate pdf for dealer_id={} customer_uuid={}", dealerID, customerUUID,e);
				List<String> error = new ArrayList<String>();
				error.add(ErrorCode.PDF_GENERATION_FAILED.name());
				communicationHistoryResponse.setErrors(kCommunicationsUtils.getApiError(error));
				return new ResponseEntity<CommunicationHistoryResponse>(communicationHistoryResponse, HttpStatus.BAD_REQUEST);
			}
			if(!commHistoryRequest.getSendPdf()) {
				try {
					pdfUrl = uploadPdfToS3(pdfUrl, dealerID);
				}
				catch(Exception e) {
					LOGGER.error("unable to upload pdf for dealer_id={} customer_uuid={}", dealerID, customerUUID,e);
					List<String> error = new ArrayList<String>();
					error.add(ErrorCode.UPLOAD_ATTACHMENT_FAILED.name());
					communicationHistoryResponse.setErrors(kCommunicationsUtils.getApiError(error));
					return new ResponseEntity<CommunicationHistoryResponse>(communicationHistoryResponse, HttpStatus.BAD_REQUEST);
				}
			}
			else {
				attachmentNames.add(pdfUrl);
				if(commHistoryRequest.getToEmailList()!=null && !commHistoryRequest.getToEmailList().isEmpty()) {
					for(String emailId: commHistoryRequest.getToEmailList()) {
						try {
							toList.add(new InternetAddress(emailId));
						}
						catch(Exception e) {
							LOGGER.warn("this is an invalid email_id={}",emailId);
						}
					}
				}
					
				Object[] dealerContext = generalRepo.getDealerNameAndEmailFromDealerID(dealerID);
				
				try {
					if(dealerContext!=null) {
						dealerName = (String) dealerContext[0];
						dealerEmailId = (String) dealerContext[1];
					}
				}
				catch(Exception e) {
					LOGGER.error("no valid email id for dealer_id={} customer_uuid={}", dealerID, customerUUID,e);
					List<String> error = new ArrayList<String>();
					error.add(ErrorCode.PDF_SEND_FAILED.name());
					communicationHistoryResponse.setErrors(kCommunicationsUtils.getApiError(error));
					return new ResponseEntity<CommunicationHistoryResponse>(communicationHistoryResponse, HttpStatus.BAD_REQUEST);
				}
					
				try {
							
						sendEmailHellper.sendThreadPdfToDealerAssociates(departmentUUID, attachmentNames, String.format(subjectMessage, customerName), String.format(messageBody, customerName), dealerID, toList, null, null, dealerEmailId, dealerName);
				}
				catch(Exception e) {
					LOGGER.error("pdf generated but sending failed for dealer_id={} customer_uuid={}", dealerID, customerUUID,e);
					List<String> error = new ArrayList<String>();
					error.add(ErrorCode.PDF_SEND_FAILED.name());
					communicationHistoryResponse.setErrors(kCommunicationsUtils.getApiError(error));
					return new ResponseEntity<CommunicationHistoryResponse>(communicationHistoryResponse, HttpStatus.BAD_REQUEST);
				}
			}
			try {
				deleteHtmlAndPdfFile(tempSaveDirectory+htmlUrl, pdfPath);
			}
			catch(Exception e) {
				LOGGER.error("pdf deletion failed for dealer_id={} customer_uuid={} pdf_path={} html_url={}", dealerID, customerUUID, pdfPath, htmlUrl, e);
				List<String> error = new ArrayList<String>();
				error.add(ErrorCode.FILE_DELETION_FAILED.name());
				communicationHistoryResponse.setErrors(kCommunicationsUtils.getApiError(error));
				return new ResponseEntity<CommunicationHistoryResponse>(communicationHistoryResponse, HttpStatus.BAD_REQUEST);
			}
			
		}
		catch(Exception e) {
			List<String> error = new ArrayList<String>();
			error.add(ErrorCode.PDF_SEND_FAILED.name());
			communicationHistoryResponse.setErrors(kCommunicationsUtils.getApiError(error));
			return new ResponseEntity<CommunicationHistoryResponse>(communicationHistoryResponse, HttpStatus.BAD_REQUEST);
		}
		communicationHistoryResponse.setcommunicationHistoryPdfUrl(pdfUrl);
		return new ResponseEntity<CommunicationHistoryResponse>(communicationHistoryResponse, HttpStatus.OK);
	}

	private String uploadPdfToS3(String pdfUrl, Long dealerID) throws Exception {
		
		File pdfFile = new File(pdfUrl);
		InputStream pdfStream = new FileInputStream(pdfFile);
		UUID uuid = UUID.randomUUID();
		return awsClientUtil.uploadMediaToAWSS3(pdfStream, uuid.toString() + PDF_EXTENSION, PDF_CONTENT_TYPE, new Date().getTime(), null, dealerID,true, COMMUNICATIONS_THREAD_PRINTING_FOLDER_PREFIX);
	}

	private void deleteHtmlAndPdfFile(String htmlUrl, String pdfUrl) throws IOException {
		Files.deleteIfExists(Paths.get(htmlUrl));
		Files.deleteIfExists(Paths.get(pdfUrl));
		LOGGER.info("deleted files html_url={} pdf_url={}", htmlUrl, pdfUrl);
	}

	private String getPdfUrlFromHtmlUrl(String htmlUrl, Long customerID) throws Exception{
		
		String fileName="thread_communicationhistory_"+ Long.toString(customerID)+"currenttime_"+ System.currentTimeMillis();

		String pdfFilePath = createFilePath(fileName, "print-files");
		htmlUrl = tempSaveDirectory + htmlUrl;
		generatePdfFromHtml(tempSaveDirectory, htmlUrl, pdfFilePath, customerID);
		return tempSaveDirectory+pdfFilePath + PDF_EXTENSION;
	}

	public void generatePdfFromHtml(String FILE_SAVE_DIR, String htmlURL, String fileName, Long customerID) throws Exception{

		ProcessBuilder processbuilder = null;
		try 
		{
			fileName = fileName + PDF_EXTENSION;
			File dirFile=new File(FILE_SAVE_DIR);
			if(!dirFile.exists()) {
				dirFile.mkdirs();
			}
			LOGGER.info("file_input={} received for customer_id={} " + FILE_SAVE_DIR +fileName,  customerID);
			LOGGER.info("before wkhtmltopdf file_name={} for customer_id={}",fileName, customerID);
			processbuilder = new ProcessBuilder(
					"/usr/local/bin/wkhtmltopdf", htmlURL, FILE_SAVE_DIR
							+ fileName);
			LOGGER.info("after wkhtmltopdf file_name={} for customer_id={}",fileName, customerID);
			processbuilder.redirectErrorStream(true);
			Process process = processbuilder.start();
			process.waitFor();
			process.getOutputStream().flush();
			process.destroy();
			LOGGER.info("Pdf file created [" + htmlURL + "][" + FILE_SAVE_DIR + "][" + fileName + "]");
		} 
		catch(Exception e) 
		{
			LOGGER.error("Pdf generation failed [" + htmlURL + "][" + FILE_SAVE_DIR + "][" + fileName + "]", e);
			throw e;
		}
	}
	
	private String getHtmlUrl(String htmlReceipt, Long customerID) throws Exception {
		
		LOGGER.info("html_size={} for customer_id={}",htmlReceipt.length(), customerID);
		String fileName="thread_communicationhistory_"+ Long.toString(customerID)+"currenttime_"+ System.currentTimeMillis();
		File html = null;

		html = File.createTempFile("conversationhistory", ".html");


		String htmlFilePath=createHTMLandGetFilePath(fileName, htmlReceipt,html);

		return htmlFilePath;
		
	}

	public String createHTMLandGetFilePath(String fileName, String htmlSnippet, File html) throws Exception {
		String htmlFilePath=null;
		Long timeStart = System.currentTimeMillis();
		PrintStream out = new PrintStream(new FileOutputStream(html));
		out.println("<html>\n<body>");
		out.println(htmlSnippet);
		// declaring variables
		File htmlFile = null;
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {

			// 1. Create an HTML file, save the HTML content to it.
			LOGGER.info("Before creating an HTML file and saving the HTML content to it , file_name {} ",fileName);
			String filePath = createFilePath(fileName, "print-files");
			htmlFilePath = filePath + ".html";
			htmlFile = new File(tempSaveDirectory + "/" + htmlFilePath);
			LOGGER.info("After creating an HTML file and saving the HTML content to it, file_name {} ",fileName);
			if (htmlFile.exists()) {
				LOGGER.info("files already exists filepath={}", htmlFilePath);
			} else {
				htmlFile.createNewFile();
			}
			htmlFile.createNewFile();
			LOGGER.info("Before writing file using BufferedWriter, file_name {} ",fileName);
			fw = new FileWriter(htmlFile.getAbsoluteFile());
			bw = new BufferedWriter(fw);
			bw.write(htmlSnippet);
			LOGGER.info("After writing file using BufferedWriter, file_name {} ",fileName);
			bw.close();
			fw.close();

		} catch (Exception e) {
			LOGGER.error("Exception while generating html/pdf file filename={}", fileName, e);
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (Exception e1) {
				LOGGER.error("Exception while deleting html/pdf file filename={}", fileName, e);
			}
			throw e;
		} finally {
			out.println("</body>\n</html>");
			out.flush();
			out.close();
		}
		Long timeEnd = System.currentTimeMillis();
		LOGGER.info(String.format("CreateHtml in MessagingAPIImpl Total time_spent=%d ms, file_Name {} ", (timeEnd-timeStart),fileName));

		return htmlFilePath;
	}
	
	public String createFilePath(String fileName, String module) {

		fileName = helper.getBase64EncodedSHA256UUID()+"_"+ fileName;
		return String.format("%s/%s",getFilePathDir(module),fileName);
	}
	
	public String getFilePathDir(String module) {
		Date currDate = new Date();
		SimpleDateFormat dfyear = new SimpleDateFormat("yyyy");
		SimpleDateFormat dfmonth = new SimpleDateFormat("MM");
		SimpleDateFormat dfday = new SimpleDateFormat("dd");
		String yyyy = dfyear.format(currDate);
		String mm = dfmonth.format(currDate);
		String dd = dfday.format(currDate);
		String fileDir = String.format("/%s/%s/%s/%s",module,yyyy,mm,dd);
		File kFileDir = new File(tempSaveDirectory+fileDir);
		if(!kFileDir.exists()) {
			kFileDir.mkdirs();
		}
		return fileDir;
	}
	
	private String getHTMLForPrintingCustomerThread(Long customerid,String outputformat,String version,Long dealerId,String versionMessage,String xsltFileName,String partner,List<Message> messagesForCustomer, String customerName) {
		
		String html = "";

		String path = baseUrl+"service/AccessUploadedFileServlet?filename=/xmls/kaarma_message.xml";

		//check
		if(customerid == null) {
			html=helper.getHTMLMessageForError("Customer ID null", "", null);
			if(MessagingUtils.PARTNER.MK.getValue().equalsIgnoreCase(partner)) {
				html = "<!doctype html> "
						+ "<html>         "
						+ "<head>                 "
						+ "<meta name=\"viewport\" content=\"width=device-width,user-scalable=no\" />                 	"
						+ "<title>Connect!!!</title>         "
						+ "</head>         "
						+ "<body style=\"background-color:#F4F4F4;text-align: center;font-name:Lato;\">                 "
						+ "<img src=\"https://dev.kaar-ma.com/images/kaarma-med.png\">                 "
						+ "<h1>"+"Customer ID null"+"</h1>                            "
						+ "<h3></h3>                            "
						+ "<h5></h5> 		"
						+ " <br/> <br/> <p style=\"font-size: 14pt; color: #555;\">"
						+ "The Connect Team</p>                 "
						+ "</body> </html>";
			}
		}
		//fetch now
		if(html.equalsIgnoreCase("")){
			try {

				Document messageXml;

				messageXml = helper.getMessageXMLFromResultSet(messagesForCustomer, customerName, path, version, true);
				helper.addVersionMessageToMessages(messageXml, version, versionMessage);
				if(outputformat!=null && outputformat.equalsIgnoreCase("html") && xsltFileName!=null && !xsltFileName.isEmpty()) {
					html = XmlProcessorUtil.transformXmlUsingXslt(messageXml, baseUrl +"service/AccessUploadedFileServlet?filename=/xslts/"+xsltFileName);
				} else if(outputformat!=null && outputformat.equalsIgnoreCase("xml")) {
					html = XMLHandler.dom2Xml(messageXml);
				}	 


				LOGGER.info("Thread printing request outputformat={} customer_id={} dealer_id={} html_content successfully done",outputformat,customerid,dealerId);


			} catch (Exception e) {
				LOGGER.error("Error in thread printing request outputformat={} customer_id={} dealer_id={}",outputformat,customerid,dealerId, e);
			}
		}

		return html;
	}

	public List<Message> getCommunicationHistory (Long dealerID,String customerUUID,CommunicationHistoryRequest communicationHistoryRequest, CommunicationHistoryResponse communicationHistoryResponse) throws Exception{

		
		Date fromDate=communicationHistoryRequest.getFromDate();
		Date toDate=communicationHistoryRequest.getToDate();
		List<Message> messages = new ArrayList<Message>();
		LOGGER.info("Request received for dealer_id={} customer_uuid={} from_date={} to_date={}", dealerID, customerUUID, fromDate, toDate);
		if(fromDate!=null && toDate!=null){
			DateFormat writeFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			writeFormat.setTimeZone(TimeZone.getTimeZone("PST"));
			String fromDateTemp=writeFormat.format(fromDate);
			String toDateTemp=writeFormat.format(toDate);
			fromDate=writeFormat.parse(fromDateTemp);
			toDate=writeFormat.parse(toDateTemp);
		}
		int lastNMessages=communicationHistoryRequest.getLastNMessages();
		String messageType=communicationHistoryRequest.getMessageType();
		String messageProtocol=communicationHistoryRequest.getMessageProtocol();
		try {
			customerUUID = kCustomerApiHelper.getPrimaryCustomerGUIDForCustomerGUID(customerUUID, dealerID);
		} catch (Exception e) {
			LOGGER.error(String.format("Error in get primary customer for customer_uuid=%s dealer_id=%s ", customerUUID, dealerID), e);
		}
		Long customerID = generalRepo.getCustomerIDForUUID(customerUUID);
		if(customerUUID!=null && !customerUUID.isEmpty() && !customerUUID.equalsIgnoreCase("ALL")) {
			customerID = generalRepo.getCustomerIDForUUID(customerUUID);
			
			if(customerID == null) {
				List<String> error = new ArrayList<String>();
				error.add(ErrorCode.INVALID_CUSTOMER.name());
				communicationHistoryResponse.setErrors(kCommunicationsUtils.getApiError(error));
				return messages;
				// no matching customer found, raise error
			}
		}
		// pending handling for all
		LOGGER.info("customer_id={} for customer_uuid={} and dealer_id={} successfully stored", customerID, customerUUID, dealerID);
		List<Object[]> result=new ArrayList<Object[]>();
		if(fromDate==null && toDate==null && lastNMessages==0&& messageType==null&&messageProtocol==null) {
			List<String> error = new ArrayList<String>();
			error.add(ErrorCode.INVALID_PRINTING_PARAMS.name());
			communicationHistoryResponse.setErrors(kCommunicationsUtils.getApiError(error));
			return messages;
		} else if(lastNMessages==0) {
			result=messageRepo.getMessages(communicationHistoryRequest.getShowInternalComments(), dealerID, customerID, messageProtocol, messageType, fromDate, toDate);
		} else if(lastNMessages!=0) {
			result=messageRepo.getMessagesWithLastNMessages(communicationHistoryRequest.getShowInternalComments(), dealerID, customerID, messageProtocol, messageType, fromDate, toDate, lastNMessages);
		}
		LOGGER.info("Completed fetching data from DB and to make hashmap for customer_uuid={} and dealer_id={}", customerUUID, dealerID);
		HashMap<Long, String> messageIDBodyMap = new HashMap<>();
		for(Object[] object: result) {
			Long messageID = ((BigInteger)object[0]).longValue();
			messageIDBodyMap.put(messageID, ((String)object[1]));
		}
		LOGGER.info("Completed fetching data from DB and successfully made hashmap for customer_uuid={} and dealer_id={}", customerUUID, dealerID);
		
		if(messageIDBodyMap.isEmpty()) {
			List<String> error = new ArrayList<String>();
			error.add(ErrorCode.NO_MESSAGES.name());
			communicationHistoryResponse.setErrors(kCommunicationsUtils.getApiError(error));
			return messages;
		} else {
			LOGGER.info("Processing the data from DB, result_size={} for customer_uuid={} and dealer_id={}", customerUUID, dealerID, result.size());
			List<Message> messagesForGivenIDs = new ArrayList<Message>();
			List<MessageExtn> messageExtnForGivenIds = new ArrayList<MessageExtn>();
			
			messagesForGivenIDs = messageRepo.findAllById(messageIDBodyMap.keySet());
			messageExtnForGivenIds = messageExtnRepo.findAllById(messageIDBodyMap.keySet());
			
			for(Message me : messagesForGivenIDs) {
				entityManager.detach(me);
				Long meID = me.getId();
				for(MessageExtn mExtn : messageExtnForGivenIds) {
					if(mExtn.getMessageID().equals(meID)) {
						me.setMessageExtn(mExtn);
					}
				}
			}
			messages.addAll(messagesForGivenIDs);
			for(Message message: messages) {
				message.getMessageExtn().setMessageBody(messageIDBodyMap.get(message.getId()));
			}
			
			String dealerUUID = generalRepo.getDealerUUIDFromDealerId(dealerID);
			LOGGER.info("Updating dates based on dealership  for customer_uuid={} customer_id={} mList_size={} ", customerUUID, dealerID, messages.size());
			for(Message message: messages) {
				if(message.getSentOn() != null)
					message.setSentOn(kCommunicationsUtils.getDealerDateForServerDate(message.getSentOn(),
							dealerUUID));
				if(message.getReceivedOn() != null)
					message.setReceivedOn(kCommunicationsUtils.getDealerDateForServerDate(message.getReceivedOn(),
							dealerUUID));
				if(message.getRoutedOn() != null)
					message.setRoutedOn(kCommunicationsUtils.getDealerDateForServerDate(message.getRoutedOn(),
							dealerUUID));
			}
			LOGGER.info("Completing the Processing the data from DB for customer_uuid={} customer_id={} mList_size={} ", customerUUID, dealerID, messages.size());
		}

		return messages;
	}
	
}

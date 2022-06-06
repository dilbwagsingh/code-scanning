package com.mykaarma.kcommunications.utils;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mykaarma.global.DealerSetupOption;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications_model.common.DealerUrlUpdateFailure;
import com.mykaarma.kcommunications_model.common.DepartmentUrlUpdateFailure;
import com.mykaarma.kcommunications_model.common.VoiceCredentials;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.request.TwilioDealerIDRequest;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.GetDepartmentsUsingKaarmaTwilioURLResponse;
import com.mykaarma.kcommunications_model.response.UpdateVoiceUrlResponse;
import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.exception.TwilioException;
import com.twilio.http.HttpMethod;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.CallCreator;
import com.twilio.rest.api.v2010.account.IncomingPhoneNumber;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.jsonwebtoken.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TwilioClientUtil {
	
	@Autowired
	private GeneralRepository generalRepository;
	
	@Autowired
	private KCommunicationsUtils kCommunicationsUtils;
	
	@Autowired
	private RestTemplate restTemplate = new RestTemplate();
	
	@Value("${twilioMasterAccountSid}")
	private String twilioMasterAccountSid;
	
	@Value("${twilioMasterAccountAuthToken}")
	private String twilioMasterAccountAuthToken;
	
	@Value("${commApiTwilioVoiceUrl}")
	private String commApiTwilioVoiceUrl;
	
	public static final String COUNTRY_CODE = "+1";
	private final static Logger LOGGER = LoggerFactory.getLogger(TwilioClientUtil.class);	
	private final static String TWILIO_DELETE_URL = "https://api.twilio.com/2010-04-01/Accounts/%s/Recordings/%s.json";
	
	private static final String TWILIO_CHANGE_URL_OPERATION_VOICE = "voice";
	private static final String TWILIO_CHANGE_URL_OPERATION_SMS = "sms";

	private static final String PHONE_NUMBER_DOES_NOT_EXIST_IN_TWILIO = "PHONE_NUMBER_DOES_NOT_EXIST_IN_TWILIO";

	private static final String PHONE_SID_DOES_NOT_EXIST_IN_TWILIO = "PHONE_SID_DOES_NOT_EXIST_IN_TWILIO";

	public Call call(String ACCOUNT_SID, String AUTH_TOKEN, String to, String from, String url, String callerIdForFirstParty,
			String fallBackURL, String statusCallbackURL) throws TwilioException {
		Call call = null;
			
			
		LOGGER.info("Inside call function: Initiating call.. AccountSid = {}, Auth_token = {}", ACCOUNT_SID, AUTH_TOKEN); 
		try {

			Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
			LOGGER.info("Call details: to_number={} from_number={} caller_id_for_first_party={} callback_url={} fallback_url={} ",
					 to, from, callerIdForFirstParty, url, fallBackURL) ;

			if (to == null || from == null || 
					url == null || callerIdForFirstParty == null || callerIdForFirstParty.isEmpty()) {
			
			LOGGER.error("This account doesn't have a phoneNo associated with it. Contact Admin to buy a phone no.");
			throw new IllegalStateException("This account doesn't have a phoneNo associated with it. " +
									"Contact Admin to buy a phone no.");
			}
			
			
			String[] calledNumber = from.split(":");
			
			
			String sendDigits = null;
			if (calledNumber.length>1){
				LOGGER.info("Dialing number with extension = {}", from);	
				sendDigits =  calledNumber[1];
			}
			
			CallCreator callCreator = com.twilio.rest.api.v2010.account.Call.creator(new PhoneNumber(calledNumber[0]),new PhoneNumber(callerIdForFirstParty), 
			new URI(url));
			if(fallBackURL != null) {
				callCreator.setMethod(HttpMethod.POST).setFallbackUrl(fallBackURL);
			}
			if(sendDigits != null) {
				callCreator.setMethod(HttpMethod.GET).setSendDigits(sendDigits);
			}
			if(statusCallbackURL != null) {
				callCreator.setMethod(HttpMethod.GET).setStatusCallback(statusCallbackURL);
			}
			call = callCreator.create();
			return call;

		} catch (Exception ex) {
			LOGGER.error("error in call from_number={} to_number={} caller_id_for_first_party={} callback_url={} ",
					from, to, callerIdForFirstParty, url, ex);
			return null;			
		}
			
			
			
	}
	
	public com.twilio.rest.api.v2010.account.Call conferenceCall(String ACCOUNT_SID, String AUTH_TOKEN, String to, String from, String url, String brokerNumber,
			String fallBackURL, String statusCallbackURL) throws IOException {
		
		try {
			Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
			com.twilio.rest.api.v2010.account.Call call = null;
			

			LOGGER.info("Call details for conferenceCall: \n\tNumber to call: {}, \n\tBroker: {}, \n\tcallback url: {}, \n\tfallback url: {}", to, brokerNumber, url, fallBackURL);

			
			if (to == null || from == null || 
				url == null || brokerNumber == null || brokerNumber.isEmpty()) {
				
				throw new IllegalStateException("conferenceCall This account doesn't have a phoneNo associated with it. " +
												"Contact Admin to buy a phone no.");
			}
		
			
			String[] calledNumber = from.split(":");
			
			String sendDigits = null;
			if (calledNumber.length>1){
				LOGGER.info("conferenceCall Dialing number with extension= {} accountSid = {}", from, ACCOUNT_SID);
				sendDigits =  calledNumber[1];
			}

			CallCreator callCreator = com.twilio.rest.api.v2010.account.Call.creator(new PhoneNumber(calledNumber[0]),new PhoneNumber(brokerNumber), 
					new URI(url));
			if(fallBackURL != null) {
				callCreator.setMethod(HttpMethod.POST).setFallbackUrl(fallBackURL);
			}
			if(sendDigits != null) {
				callCreator.setMethod(HttpMethod.GET).setSendDigits(sendDigits);
			}
			if(statusCallbackURL != null) {
				callCreator.setMethod(HttpMethod.GET).setStatusCallback(statusCallbackURL);
			}
			call = callCreator.create();

			return call;
		} catch (Exception ex) {
			LOGGER.error("Error while initiating call for AccountSid = {}, to = {}, from = {}", ACCOUNT_SID, to, from, ex);
			return null;
		}
	}

	public ResponseEntity<UpdateVoiceUrlResponse> updateVoiceUrlForDealers(TwilioDealerIDRequest twilioDealerIDRequest)
	{
		UpdateVoiceUrlResponse  updateVoiceUrlResponse = new UpdateVoiceUrlResponse();
		List<ApiError> errors = new ArrayList<ApiError>();
		List<Long> dealerIDs = twilioDealerIDRequest.getDealerIDList();
		List<Long> dealersSuccessfullyProcessed = new ArrayList<Long>();
		List<DealerUrlUpdateFailure> dealersFailed = new ArrayList<DealerUrlUpdateFailure>();
		Boolean optionValue = twilioDealerIDRequest.getUrl().equals(commApiTwilioVoiceUrl);
		for(Long dealerID : dealerIDs) {
			
			List<DepartmentUrlUpdateFailure> departmentUrlUpdateFailureList = new ArrayList<DepartmentUrlUpdateFailure>();
			Boolean dealerProcessedSuccessfully = true;
			DealerUrlUpdateFailure dealerUrlUpdateFailure = new DealerUrlUpdateFailure();
			
			try {
				if(TWILIO_CHANGE_URL_OPERATION_VOICE.equalsIgnoreCase(twilioDealerIDRequest.getOperationType())){
					KManageApiHelper.updateDealerSetupOption(generalRepository.getDealerUUIDFromDealerId(dealerID), 
							DealerSetupOption.COMMUNICATION_API_VOICECALLING_ROLLOUT.getOptionKey(), optionValue.toString()
							);
				}
			}
			catch(Exception e) {
				LOGGER.error("unable to update dso value for dso for dealer_id={} skipping further processing", dealerID, e);
				ApiError error = new ApiError(ErrorCode.DSO_UPDATE_FAILED.name(), String.format("No Phone Numbers For dealer_id={}",dealerID));
				errors.add(error);
				dealerUrlUpdateFailure.setDealerId(dealerID);
				dealersFailed.add(dealerUrlUpdateFailure);
				continue;
			}
			try {
				List<VoiceCredentials> phoneNumberList = generalRepository.getAllNumbersForDealer(dealerID);
				if(phoneNumberList==null || phoneNumberList.isEmpty()) {
					LOGGER.warn("No phone number exists for hence skipping process for dealer_id={}", dealerID);
					ApiError error = new ApiError(ErrorCode.MISSING_PHONE_NUMBERS.name(), String.format("No Phone Numbers For dealer_id={}",dealerID));
					errors.add(error);
					dealerUrlUpdateFailure.setDealerId(dealerID);
					dealersFailed.add(dealerUrlUpdateFailure);
					continue;
				}
				for(VoiceCredentials voiceCredentials : phoneNumberList) {
					
					DepartmentUrlUpdateFailure departmentUrlUpdateFailure = new DepartmentUrlUpdateFailure();
					String brokerNumber = voiceCredentials.getBrokerNumber();
					Long dealerDepartmentID = voiceCredentials.getDeptID();
					String credentials = voiceCredentials.getDealerSubAccount();
					String accountSid = credentials.split("~")[0];
					String authToken = credentials.split("~")[1];
					
					LOGGER.info("UpdateVoiceURL dealer_department_id={} dealer_id={} broker_number={} account_sid={}  ",
							dealerDepartmentID, dealerID, brokerNumber, accountSid);
					try {
						String failureReason = updateUrlForDepartment(dealerDepartmentID, dealerID, brokerNumber, accountSid, authToken, 
								twilioDealerIDRequest.getUrl(), twilioDealerIDRequest.getOperationType());
						if(failureReason!=null) {
							dealerProcessedSuccessfully = false;
							departmentUrlUpdateFailure.setBrokerNumber(brokerNumber);
							departmentUrlUpdateFailure.setAccountSid(accountSid);
							departmentUrlUpdateFailure.setDealerDepartmentId(dealerDepartmentID);
							departmentUrlUpdateFailure.setFailureReason(failureReason);
							departmentUrlUpdateFailureList.add(departmentUrlUpdateFailure);
						}
					}
					catch(Exception e)
					{
						dealerProcessedSuccessfully = false;
						departmentUrlUpdateFailure.setBrokerNumber(brokerNumber);
						departmentUrlUpdateFailure.setAccountSid(accountSid);
						departmentUrlUpdateFailure.setDealerDepartmentId(dealerDepartmentID);
						departmentUrlUpdateFailure.setFailureReason(e.getMessage());
						departmentUrlUpdateFailureList.add(departmentUrlUpdateFailure);
						LOGGER.error("unable to update voiceURL for dealer_department_id={} dealer_id={} broker_number={} account_sid={}  ",
							dealerDepartmentID, dealerID, brokerNumber, accountSid, e);
						ApiError error = new ApiError(ErrorCode.UPDATE_TWILIO_FAILED.name(), String.format("Update Twilio VoiceUrl failed for department_id={} dealer_id={}",dealerDepartmentID, dealerID));
						errors.add(error);
					}
				}
			}
			catch(Exception e) {
				dealerProcessedSuccessfully = false;
				dealerUrlUpdateFailure.setDealerId(dealerID);
				LOGGER.error("Unable to update TwilioUrl for dealer_id={}", dealerID, e);
				ApiError error = new ApiError(ErrorCode.UPDATE_TWILIO_FAILED.name(), String.format("Update Twilio VoiceUrl failed for dealer_id={}",dealerID));
				errors.add(error);
			}
			
			if(departmentUrlUpdateFailureList!=null && !departmentUrlUpdateFailureList.isEmpty()) {
				dealerUrlUpdateFailure.setDealerId(dealerID);
				dealerUrlUpdateFailure.setDepartmentsFailed(departmentUrlUpdateFailureList);
			}
			
			if(dealerProcessedSuccessfully) {
				dealersSuccessfullyProcessed.add(dealerID);
			}
			else {
				dealersFailed.add(dealerUrlUpdateFailure);
			}
			
		}
		
		updateVoiceUrlResponse.setDealerIDsUpdated(dealerIDs);
		updateVoiceUrlResponse.setDealerIdsFailed(dealersFailed);
		updateVoiceUrlResponse.setDealerIdSuccessfullyProcessed(dealersSuccessfullyProcessed);
		
		if(!errors.isEmpty()) {
			updateVoiceUrlResponse.setErrors(errors);
		}
		
		try {
			LOGGER.info("update Url type={}  Response={} ", twilioDealerIDRequest.getOperationType(), new ObjectMapper().writeValueAsString(updateVoiceUrlResponse));
		} catch (JsonProcessingException e) {

			LOGGER.info("unable to print url update response");
		}
		
		if(!errors.isEmpty()) {
			return new ResponseEntity<UpdateVoiceUrlResponse>(updateVoiceUrlResponse, HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<UpdateVoiceUrlResponse>(updateVoiceUrlResponse, HttpStatus.OK);
	}

	public void getDepartmentsUsingKaarmaTwilioURL(Long dealerID, GetDepartmentsUsingKaarmaTwilioURLResponse response) {
		List<VoiceCredentials> phoneNumberList = generalRepository.getAllNumbersForDealer(dealerID);
		if(phoneNumberList == null || phoneNumberList.isEmpty()) {
			LOGGER.warn("No phone number exists for dealer_id={} ", dealerID);
			ApiWarning warning = new ApiWarning(ErrorCode.MISSING_PHONE_NUMBERS.name(), String.format("No Phone Numbers For dealer_id=%s",dealerID));
			response.getWarnings().add(warning);
			return;
		}
		List<Long> departmentsUsingKaarmaTwilioURL = new ArrayList<>();
		for(VoiceCredentials voiceCredentials : phoneNumberList) {

			String credentials = voiceCredentials.getDealerSubAccount();
			String accountSid = credentials.split("~")[0];
			String authToken = credentials.split("~")[1];
			String brokerNumber = voiceCredentials.getBrokerNumber();
			Long dealerDepartmentID = voiceCredentials.getDeptID();

			LOGGER.info("getVoiceUrlForDealers dealer_department_id={} dealer_id={} broker_number={} account_sid={}  ",
				dealerDepartmentID, dealerID, brokerNumber, accountSid);
			try {
				List<String> urls = getUrlsForDepartment(dealerDepartmentID, brokerNumber, accountSid, authToken);
				if (urls == null || urls.isEmpty()) {
					ApiWarning warning = new ApiWarning(ErrorCode.MISSING_PHONE_NUMBERS.name(),
						String.format("no phone number found for account_sid=%s broker_number=%s dealer_department_id=%s dealer_id=%s", accountSid, brokerNumber, dealerDepartmentID, dealerID));
					response.getWarnings().add(warning);
				} else {
					Boolean foundKaarma = urls.stream().anyMatch(url -> url != null && url.contains("kaar-ma"));
					if (foundKaarma) {
						LOGGER.warn("found kaar-ma twilio url usage for dealer_id={} department_id={} account_sid={} broker_number={}", dealerID, dealerDepartmentID, accountSid, brokerNumber);
						departmentsUsingKaarmaTwilioURL.add(dealerDepartmentID);
					}
				}
			} catch (Exception e) {
				LOGGER.error("unable to get voiceURL for dealer_department_id={} dealer_id={} broker_number={} account_sid={} ",
					dealerDepartmentID, dealerID, brokerNumber, accountSid, e);
				ApiError error = new ApiError(ErrorCode.INTERNAL_SERVER_ERROR.name(), String.format("Get departments using kaarma Twilio URL failed for department_id=%s dealer_id=%s", dealerDepartmentID, dealerID));
				response.getErrors().add(error);
			}
		}
		if(!departmentsUsingKaarmaTwilioURL.isEmpty()) {
			response.getDealerIDdepartmentIDListMap().put(dealerID, departmentsUsingKaarmaTwilioURL);
		}
	}

	private List<String> getUrlsForDepartment(Long dealerDepartmentID, String brokerNumber, String accountSid, String authToken) throws Exception {
		Twilio.init(accountSid, authToken);
		ResourceSet<IncomingPhoneNumber> incomingPhoneNumbers =
			IncomingPhoneNumber.reader()
				.setPhoneNumber(new com.twilio.type.PhoneNumber(COUNTRY_CODE+brokerNumber))
				.read();
		if(incomingPhoneNumbers == null) {
			LOGGER.info("no phone number found for account_sid={} broker_number={} dealer_department_id={}", accountSid, brokerNumber, dealerDepartmentID);
			return null;
		}
		List<String> urls = new ArrayList<>();
		for(IncomingPhoneNumber record : incomingPhoneNumbers) {
			if(record != null && record.getSmsUrl() != null) {
				urls.add(record.getSmsUrl().toString());
			}
			if(record != null && record.getVoiceUrl() != null) {
				urls.add(record.getVoiceUrl().toString());
			}
		}
		LOGGER.info("in getUrlsForDepartment found urls={}", urls);
		return urls;
	}

	private String updateUrlForDepartment(Long dealerDepartmentID, Long dealerID, String brokerNumber,
			String accountSid, String authToken, String url, String operationType) {
		
		String updateFailureReason = null;
		try {
			LOGGER.info("updateVoiceUrlInTwilio for account_sid={} broker_number={} dealerDepartment_id={}", accountSid, brokerNumber, dealerDepartmentID);
			Twilio.init(accountSid, authToken);
	        ResourceSet<IncomingPhoneNumber> incomingPhoneNumbers = 
	            IncomingPhoneNumber.reader()
	            .setPhoneNumber(new com.twilio.type.PhoneNumber(COUNTRY_CODE+brokerNumber))
	            .read();
	        String phoneSID = null;
	        if(incomingPhoneNumbers==null) {
	        	LOGGER.info("no phone number found for account_sid={} broker_number={} ", accountSid, brokerNumber);
	        	return PHONE_NUMBER_DOES_NOT_EXIST_IN_TWILIO;
	        }
	        for(IncomingPhoneNumber record : incomingPhoneNumbers) {
	            phoneSID = record.getSid();
	            break;
	        }
	        if(phoneSID==null) {
	        	LOGGER.info("no phoneSID found for account_sid={} broker_number={} ", accountSid, brokerNumber);
	        	return PHONE_SID_DOES_NOT_EXIST_IN_TWILIO;
	        }
	        
	        if(operationType.equalsIgnoreCase(TWILIO_CHANGE_URL_OPERATION_VOICE)) {
	        	IncomingPhoneNumber incomingPhoneNumber = IncomingPhoneNumber.updater(phoneSID)
	    	            .setAccountSid(accountSid)
	    	            .setVoiceMethod(HttpMethod.POST)
	    	            .setVoiceUrl(URI.create(url))
	    	            .update();
	        	
	    	    LOGGER.info("updateVoiceFallbackUrlInTwilio backup_url updated for broker_number={} accountSid={} ", 
	    	    		brokerNumber, accountSid);
	        } else if(operationType.equalsIgnoreCase(TWILIO_CHANGE_URL_OPERATION_SMS)) {
	        	IncomingPhoneNumber incomingPhoneNumber = IncomingPhoneNumber.updater(phoneSID)
	        			.setAccountSid(accountSid)
	        			.setSmsMethod(HttpMethod.GET)
	        			.setSmsUrl(URI.create(url))
	        			.update();
	    	            
	    	    LOGGER.info("updateMessageFallbackUrlInTwilio backup_url updated for broker_number={} accountSid={} ", 
	    	    		brokerNumber, accountSid);
	        }
	        
		}
		catch(Exception e) {
			LOGGER.error("Unable to update TwilioUrl for department_id={}",dealerDepartmentID, e);
			throw e;
		}
		
		return updateFailureReason;
	}
	
	public void deleteRecordingsFromTwilio(String recordingURL, Long messageID) {
		
		LOGGER.info("recording_url={} to be deleted from twilio for message_id={}",recordingURL, messageID);
		try {
			String[] arrSplit = recordingURL.split("/");
			String acconutSID = arrSplit[5];
			String recordingSID = arrSplit[7];
			String dealerSubAccount = null;

			dealerSubAccount = kCommunicationsUtils.getDealerSubAccountBySid(acconutSID);

			if(dealerSubAccount==null) {
				LOGGER.error("credentials for account_sid={} does not exist in data base", acconutSID);
				dealerSubAccount = twilioMasterAccountSid + "~" + twilioMasterAccountAuthToken;
			}
			LOGGER.info("VoiceCredentials for message_id={} are {} and account_sid={} and recording_sid={}",messageID, dealerSubAccount, acconutSID, recordingSID);
			HttpHeaders headers = kCommunicationsUtils.getAuthorizationHeader(dealerSubAccount);
		    HttpEntity<?> request = new HttpEntity<Object>(headers);
		    try {
		    	restTemplate.exchange(String.format(TWILIO_DELETE_URL, acconutSID, recordingSID), org.springframework.http.HttpMethod.DELETE, request, String.class);
		    	LOGGER.info("recording deleted from twilio for recording_url={} and message_id={} acconutSID={}  recordingSID={}",recordingURL, messageID, acconutSID, recordingSID);
		    }
		    catch(Exception e) {
		    	LOGGER.error("unable to delete recording_url={} for message_id={}", recordingURL, messageID, e);
		    	throw e;
		    }
		}
		catch(Exception e) {
			LOGGER.error("unable to process delete request for recording_url={} and message_id={}", recordingURL, messageID, e);
			throw e;
		}
	}

	public Message fetchMessageForMessageSid(String accountSid, String authToken, String messageSid) throws Exception{
		
		Twilio.init(accountSid, authToken);
		Message message = Message.fetcher(accountSid, messageSid).fetch();
		return message;
	}

	public ResourceSet<Call> fetchCallsForGivenTimeFrame(com.mykaarma.kcommunications.model.jpa.VoiceCredentials vc, LocalDateTime startDate, LocalDateTime endDate, Long departmentId) {
		
		ZonedDateTime startDateTime = null;
		ZonedDateTime endDateTime = null;
		String accountSid = null;
		String auth_token = null;
		String dealerSubAccount = null;

		try {
			LOGGER.info("fetching calls for department_id={} dealer_sub_account={}", departmentId, vc.getDealerSubaccount());
			try {
				startDateTime = ZonedDateTime.of(startDate, ZoneId.of("America/Los_Angeles"));
				endDateTime = ZonedDateTime.of(endDate, ZoneId.of("America/Los_Angeles"));
				dealerSubAccount = vc.getDealerSubaccount();
				accountSid = dealerSubAccount.split("~")[0];
				auth_token = dealerSubAccount.split("~")[1];
				Twilio.init(accountSid, auth_token);
			}
			catch(Exception e) {
				LOGGER.warn("dealer_sub_account not valid for department_id={} dealer_sub_account={}", departmentId, dealerSubAccount);
				return null;
			}
			
			try {
			 ResourceSet<Call> calls = Call.reader()
					 .setStartTimeAfter(startDateTime)
	        		 .setStartTimeBefore(endDateTime)
		        	 .read();
			 return calls;
			}
			catch(Exception e) {
				LOGGER.error("unable to fetch calls for department_id={} dealer_sub_account={} ", e);
				throw e;
			}
			
		}
		catch(Exception e) {
			LOGGER.error("unable to process request to fetch calls for department_id={} dealer_sub_account={} ", e);
			throw e;
		}
		
	}

	public ResourceSet<Message> fetchMessagesForGivenTimeFrame(com.mykaarma.kcommunications.model.jpa.VoiceCredentials vc,
			LocalDateTime startDate, LocalDateTime endDate, Long departmentId) {
		
		ZonedDateTime startDateTime = null;
		ZonedDateTime endDateTime = null;
		String accountSid = null;
		String auth_token = null;
		String dealerSubAccount = null;

		try {
			LOGGER.info("fetching texts for department_id={} dealer_sub_account={}", departmentId, vc.getDealerSubaccount());
			try {
				startDateTime = ZonedDateTime.of(startDate, ZoneId.of("America/Los_Angeles"));
				endDateTime = ZonedDateTime.of(endDate, ZoneId.of("America/Los_Angeles"));
				dealerSubAccount = vc.getDealerSubaccount();
				accountSid = dealerSubAccount.split("~")[0];
				auth_token = dealerSubAccount.split("~")[1];
				Twilio.init(accountSid, auth_token);
			}
			catch(Exception e) {
				LOGGER.warn("dealer_sub_account not valid for department_id={} dealer_sub_account={}", departmentId, dealerSubAccount);
				return null;
			}
			
			try {
				ResourceSet<Message> messages = Message.reader()
						.setDateSentAfter(startDateTime)
						.setDateSentBefore(endDateTime)
						.read();
				return messages;
			}
			catch(Exception e) {
				LOGGER.error("unable to fetch texts for department_id={} dealer_sub_account={} ", e);
				throw e;
			}
			
		}
		catch(Exception e) {
			LOGGER.error("unable to process request to fetch texts for department_id={} dealer_sub_account={} ", e);
			throw e;
		} 
	}

}
package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.MessagePurpose;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCredentialsRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.VoiceCredentials;
import com.mykaarma.kcommunications.redis.VoiceCredentialsService;
import com.mykaarma.kcommunications.utils.KCommunicationsException;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.response.GetDepartmentUUIDResponse;

@Service
public class VoiceCredentialsImpl {
		
	@Autowired
	VoiceCredentialsRepository voiceCredentialsRepository;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	VoiceCredentialsService voiceCredentialsService;
	
	@Autowired
	MessageRepository messageRepository;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired 
	private ValidateRequest validateRequest;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(VoiceCredentialsImpl.class);
	private static final String ADMIN = "Admin";

	
	public VoiceCredentials getVoiceCredentialsForMessage(Message message) {	
		
		LOGGER.info("in testing voice creds for customer_id={} department_id={}", message.getCustomerID(), message.getDealerDepartmentId());
		List<VoiceCredentials> whitelistCreds = new ArrayList<VoiceCredentials>();
		List<VoiceCredentials> graylistCreds = new ArrayList<VoiceCredentials>();
		List<VoiceCredentials> credentials = voiceCredentialsRepository.findAllByDeptID(message.getDealerDepartmentId());
		List<VoiceCredentials> alreadyUsed = new ArrayList<VoiceCredentials>();
		List<String> whiteListBrokerNumber = new ArrayList<String>();
		//populate whitelist and graylist from common list.
		
		List<String> brokerNumbersAlreadyTried = new ArrayList<>();
		if(message.getId()!=null) {
			brokerNumbersAlreadyTried = generalRepository.getBrokerNumbersAlreadyTriedForMessageID(message.getId());
		}
		LOGGER.info("broker numbers already tried are={} credentials={}", brokerNumbersAlreadyTried, credentials);
		for(VoiceCredentials creds: credentials) {
			if(brokerNumbersAlreadyTried.contains(creds.getBrokerNumber())) {
				alreadyUsed.add(creds);
			} else if(creds.getUseForOutgoingCommunication() != null && creds.getUseForOutgoingCommunication()) {
				whitelistCreds.add(creds);
				whiteListBrokerNumber.add(creds.getBrokerNumber());
			} else if(creds.getUseForStickiness() != null && creds.getUseForStickiness()) {
				graylistCreds.add(creds);
			}
		}
		
		credentials.removeAll(alreadyUsed);
		
		if(credentials.size() == 0)
			return null;
		
		//only 1 number in whitelist+graylist
		if(credentials.size() == 1) {
			return credentials.get(0);
		}
		
		List<String> specialPurposes = new ArrayList<String>();
		specialPurposes.add(MessagePurpose.DRIV_CALL.name());
		specialPurposes.add(MessagePurpose.PDM_CALL.name());
	
		
		String lastBrokerNumber = messageRepository.getLastBrokerNumberUsedForCustomerAndDepartment(message.getCustomerID(), message.getDealerDepartmentId(), specialPurposes);
		
		LOGGER.info("Last broker_number={} corresponding to customerID={}", lastBrokerNumber, message.getCustomerID());
		if(lastBrokerNumber != null) {
			for(VoiceCredentials creds: credentials) {
				if(creds.getBrokerNumber().equalsIgnoreCase(lastBrokerNumber)) {// last broker number is in G/W
					return creds;
				}
			}
		}

		List<String>redisBrokerNumbers = voiceCredentialsService.getVoiceCredentials(message.getDealerDepartmentId());
		LOGGER.info("redis_broker_numbers={} for department_id={} customer_id={} dealer_id={}", redisBrokerNumbers, message.getDealerDepartmentId(), 
				message.getCustomerID(), message.getDealerID());
		//update whitelist in redis
		if(whitelistCreds!=null && !whitelistCreds.isEmpty()) {
			for(VoiceCredentials creds: whitelistCreds) {
				if(redisBrokerNumbers!=null && !redisBrokerNumbers.isEmpty() && !redisBrokerNumbers.contains(creds.getBrokerNumber())) {
					voiceCredentialsService.addVoiceCredential(message.getDealerDepartmentId(), creds.getBrokerNumber());
				}
				else if(redisBrokerNumbers==null || redisBrokerNumbers.isEmpty()) {
					voiceCredentialsService.addVoiceCredential(message.getDealerDepartmentId(), creds.getBrokerNumber());
				}
			}
		}
		if(redisBrokerNumbers!=null && !redisBrokerNumbers.isEmpty()) {
			for(String redisNumber: redisBrokerNumbers) {
				if(whiteListBrokerNumber!=null && !whiteListBrokerNumber.isEmpty() && !whiteListBrokerNumber.contains(redisNumber)) {
					voiceCredentialsService.removeVoiceCredential(message.getDealerDepartmentId(), redisNumber);
				}
				else if(whiteListBrokerNumber==null || whiteListBrokerNumber.isEmpty()) {
					voiceCredentialsService.removeVoiceCredential(message.getDealerDepartmentId(), redisNumber);
				}
				
			}
		}
		
		//return VoiceCredentials object corresponding to redis lru broker number.
		String lruBrokerNumber = voiceCredentialsService.reQueueVoiceCredential(message.getDealerDepartmentId());
		for(VoiceCredentials creds: credentials) {
			if(creds.getBrokerNumber().equalsIgnoreCase(lruBrokerNumber)) {
				return creds;
			}
		}
		return null;
	
	}
	

	public ResponseEntity<GetDepartmentUUIDResponse> getDepartmentUUID(String brokerNumber) throws KCommunicationsException {
		GetDepartmentUUIDResponse response = new GetDepartmentUUIDResponse();
		
		try {
			response = validateRequest.validateGetDepartmentUUIDRequest(brokerNumber);
			if(response.getErrors() != null && !response.getErrors().isEmpty()) {
				return new ResponseEntity<GetDepartmentUUIDResponse>(response, HttpStatus.BAD_REQUEST);
			}
			
			String departmentUUID = voiceCredentialsRepository.getDepartmentUUIDForBrokerNumber(brokerNumber);
			response.setDepartmentUUID(departmentUUID);
			return new ResponseEntity<GetDepartmentUUIDResponse>(response, HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error("ERROR occurrred in getDepartmentUUID for broker_number={}", brokerNumber, e);
			throw new KCommunicationsException(ErrorCode.INTERNAL_SERVER_ERROR, 
					String.format("Internal error - %s while processing request! Please contact Communications API support", e.getMessage()));	
		}
	}
	
	public List<String> getVoiceCredentialsAndBrokerNumber(Message message, String dealerUUID, Long departmentID, Long customerID, boolean isSupport) throws Exception{
		
		VoiceCredentials voiceCredentials = getVoiceCredentialsForMessage(message); 
		Boolean isFallback = false;
		Long deptID = null;
		
		if(voiceCredentials == null) {
			voiceCredentials = getNonWhitelistVoiceCredentialsForDepartment(departmentID, customerID);
		}
		
		String[] cred = getSeparatedDetailsFromDealerSubaccount(voiceCredentials.getDealerSubaccount());

		String brokerNumber = voiceCredentials.getBrokerNumber();

		if (brokerNumber == null && cred == null) {

			if (isSupport) {

				String DealerAuth = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.VOICECALL_FALLBACK.getOptionKey());
			
				if (DealerAuth != null) {

					String Fallback[] = DealerAuth.split("~");
					if ("true".equalsIgnoreCase(Fallback[0])) {

						Long FallbackCred = Long.parseLong(Fallback[1]);
						
						String deptUUID = generalRepository.getDepartmentUUIDForDealerID(FallbackCred, ADMIN);
						deptID = generalRepository.getDepartmentIDForUUID(deptUUID);
						
						isFallback = true;
					}
				}
			}
		}
		
		if (isFallback) {
			
			message.setDealerDepartmentId(deptID);
			voiceCredentials = getVoiceCredentialsForMessage(message);
			
			cred = getSeparatedDetailsFromDealerSubaccount(voiceCredentials.getDealerSubaccount());
			brokerNumber = voiceCredentials.getBrokerNumber();
		}
		
		if (cred==null || cred[0] == null || cred[1] == null) {
			
			LOGGER.error("Voice credentials not available for dealerUUID={} customer_id={} dealer_department_id={} ", 
					dealerUUID, customerID, departmentID);
			return null;
		}
		
		List<String> result = new ArrayList<>();
		result.add(cred[0]);
		result.add(cred[1]);
		result.add(brokerNumber);
		
		return result;		
	}
	
	public VoiceCredentials getNonWhitelistVoiceCredentialsForDepartment(Long departmentID, Long customerID) {
		List<VoiceCredentials> credentials = voiceCredentialsRepository.findAllByDeptID(departmentID);
		//populate whitelist and graylist from common list.
			
		//only 1 number for department
		if(credentials.size() == 1) {
			return credentials.get(0);
		}
		
		if(customerID != null) {
			String lastBrokerNumber = messageRepository.getLastBrokerNumberUsedForCustomerAndDepartment(customerID, departmentID, null);
			LOGGER.info("Last broker number corresponding to customerID: {} is : {}", customerID, lastBrokerNumber);
			if(lastBrokerNumber != null) {
				for(VoiceCredentials creds: credentials) {
					if(creds.getBrokerNumber().equalsIgnoreCase(lastBrokerNumber)
							&& (creds.getIsHosted()==null || !creds.getIsHosted())) {// last broker number is in G/W
						LOGGER.info("Last broker number still valid: {} for customerID = {}", lastBrokerNumber, customerID);
						return creds;
					}
				}
			}
		}
		
		return credentials.get(0);
	}
	
	public String[] getSeparatedDetailsFromDealerSubaccount(String dealerSubaccount) {
		return dealerSubaccount.split("~");
	}

}

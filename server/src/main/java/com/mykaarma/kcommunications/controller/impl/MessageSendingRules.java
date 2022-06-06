package com.mykaarma.kcommunications.controller.impl;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mykaarma.global.Authority;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.FailedDraftEnum;
import com.mykaarma.global.Role;
import com.mykaarma.kcommunications.jpa.repository.DraftMessageMetaDataRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.model.api.CommunicationStatusResponse;
import com.mykaarma.kcommunications.model.api.OptOutStatus;
import com.mykaarma.kcommunications.model.jpa.DraftMessageMetaData;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.AppConfigHelper;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.KMessagingApiHelper;
import com.mykaarma.kcommunications_model.enums.DraftStatus;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.OptOutState;
import com.mykaarma.kcommunications_model.response.OptOutStatusResponse;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MessageSendingRules {
	
	private Logger LOGGER = LoggerFactory.getLogger(MessageSendingRules.class);
	
	@Value("${opt_out_email_url}")
	private String unsubscribeURL;
	
	@Value("${base_url}")
	private String baseURL;
	
	@Autowired
	AppConfigHelper appConfigHelper;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	KMessagingApiHelper kMessagingApiHelper;
	
	@Autowired
	MessageRepository messageRepository;

	@Autowired
	OptOutImpl optOutImpl;
	
	@Autowired
	DraftMessageMetaDataRepository draftMessageMetaDataRepository;
	
	public HashMap<String, String> getMetaData(Message message) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> map = new HashMap<String, String>();
		if(message.getMessageMetaData()!=null && message.getMessageMetaData().getMetaData()!=null) {
			String messageMetaData = message.getMessageMetaData().getMetaData();
			map = mapper.readValue(messageMetaData, Map.class);
		}
		return (HashMap<String, String>) map;
	}
	
	public String prepareMessageBody(Message message, MessageExtn messageExt, HashMap<String, String> map, HashMap<String, String> dsoMap) throws Exception {
		String messageBody = messageExt.getMessageBody();
		
		Boolean addTCPAFooter = addTCPAFooterRules(map, message, dsoMap);
		Boolean addSignature = addSignatureRules(map);
		Boolean addFooter = addFooterRules(map, dsoMap);
		
		if(addTCPAFooter) {
			messageBody = messageBody + "\n" + getTCPAFooter(dsoMap);
		}
		if(addSignature && addFooter) {
			messageBody = messageBody + "\n--" + message.getFromName() + " "+ dsoMap.get(DealerSetupOption.COMMUNICATIONS_TEXT_FOOTER.getOptionKey());
		} else if(addSignature) {
			messageBody = messageBody + "\n--" + message.getFromName();
		} else if(addFooter) {
			messageBody = messageBody + "\n" + dsoMap.get(DealerSetupOption.COMMUNICATIONS_TEXT_FOOTER.getOptionKey());
		}
		LOGGER.info("message_body={}", messageBody);
		return messageBody;
	}
	
	public Boolean addFooterRules(Map<String, String> map, HashMap<String, String> dsoMap) {
		if(map.containsKey(APIConstants.ADD_FOOTER) && "false".equalsIgnoreCase(map.get(APIConstants.ADD_FOOTER))) {
			return false;
		}
		if(!dsoMap.containsKey(DealerSetupOption.COMMUNICATIONS_TEXT_FOOTER.getOptionKey()) || 
			dsoMap.get(DealerSetupOption.COMMUNICATIONS_TEXT_FOOTER.getOptionKey())==null || 
			dsoMap.get(DealerSetupOption.COMMUNICATIONS_TEXT_FOOTER.getOptionKey()).isEmpty()) {
			return false;
		}
		return true;
	}
	
	public Boolean overrideHolidays(Map<String, String> map, Message message, HashMap<String, String> dsoMap) {
		if(map.containsKey(APIConstants.OVERRIDE_HOLIDAYS) && "true".equalsIgnoreCase(map.get(APIConstants.OVERRIDE_HOLIDAYS))) {
			LOGGER.info("overrideHolidays=true for dealer_id={} since overrideHolidays is true sent by caller.", message.getDealerID());
			return true;
		}
		if(!dsoMap.containsKey(DealerSetupOption.MESSAGING_DRAFT_BLACKOUTDATE_ENABLE.getOptionKey()) || 
				!"true".equalsIgnoreCase(dsoMap.get(DealerSetupOption.MESSAGING_DRAFT_BLACKOUTDATE_ENABLE.getOptionKey()))) {
			LOGGER.info("overrideHolidays=true for dealer_id={} since blackout dso is false MESSAGING_DRAFT_BLACKOUTDATE_ENABLE={}", message.getDealerID(),
					dsoMap.get(DealerSetupOption.MESSAGING_DRAFT_BLACKOUTDATE_ENABLE.getOptionKey()));
			return true;
		}
		LOGGER.info("override_holidays={} for message_uuid={}", map.get(APIConstants.OVERRIDE_HOLIDAYS), message.getUuid());
		if(map.containsKey(APIConstants.OVERRIDE_HOLIDAYS) && "false".equalsIgnoreCase(map.get(APIConstants.OVERRIDE_HOLIDAYS))) {
			LOGGER.info("overrideHolidays=false for dealer_id={} since overrideHolidays is false sent by caller.", message.getDealerID());
			return false;
		}
		return true;
	}
	
	public Boolean addSignatureRules(Map<String, String> map) {
		if(map.containsKey(APIConstants.ADD_SIGNATURE) && "true".equalsIgnoreCase(map.get(APIConstants.ADD_SIGNATURE))) {
			return true;
		}
		return false;
	}
	

	public Long delegationRules(Message message, HashMap<String, String> dsoMap) {
		String departmentUUID = appConfigHelper.getDealerDepartmentUUIDForID(message.getDealerDepartmentId());
		GetDealerAssociateResponseDTO defaultUser = kManageApiHelper.getDefaultDealerAssociateForDepartment(departmentUUID);
		if(message.getDealerAssociateID().equals(defaultUser.getDealerAssociate().getId())) {
			LOGGER.info("not delegating since dealer_associate is system user for dealer_associate_id={}", message.getDealerAssociateID());
			return null;
		}
		String userUUID = generalRepository.getUserUUIDForDealerAssociateID(message.getDealerAssociateID());
		GetDealerAssociateResponseDTO dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUUID, userUUID);
		if(dealerAssociate==null || dealerAssociate.getDealerAssociate()==null){
			LOGGER.info("not delegating since the user is invalid for dealer_associate_id={}", message.getDealerAssociateID());
			return null;
		}
		if(Role.DRIVER.getRoleName().equalsIgnoreCase(dealerAssociate.getDealerAssociate().getRoleDTO().getRoleName()) ||
				Role.CASHIER.getRoleName().equalsIgnoreCase(dealerAssociate.getDealerAssociate().getRoleDTO().getRoleName())) {
			LOGGER.info("not delegating since dealer_associate is either cashier or driver for dealer_associate_id={}", message.getDealerAssociateID());
			return null;
		}
		String defaultThreadOwnerUserUUID = dealerAssociate.getDealerAssociate().getDefaultThreadOwnerUserUUID();
		LOGGER.info("in delegationRules default dealer_associate_uuid={} received from kmanage ", defaultThreadOwnerUserUUID);
		if(defaultThreadOwnerUserUUID==null || defaultThreadOwnerUserUUID.isEmpty()) {
			defaultThreadOwnerUserUUID = userUUID;
		}
		LOGGER.info("in delegationRules final default dealer_associate_uuid={} ", defaultThreadOwnerUserUUID);
		if(kManageApiHelper.checkDealerAssociateAuthority(Authority.DO_NOT_ROUTE_INCOMING_MESSAGE.getAuthority(), defaultThreadOwnerUserUUID, departmentUUID)) {
			LOGGER.info("not delegating since dealer_associate has do not route authority for default_thread_owner_user_uuid={}",defaultThreadOwnerUserUUID);
			return null;
		}
		Long dealerAssociateID = generalRepository.getDealerAssociateIDForUserUUID(defaultThreadOwnerUserUUID, message.getDealerDepartmentId());
		if(dealerAssociateID==null) {
			LOGGER.info("not delegating since default dealer_associate does not belong to the same department default_thread_owner_user_uuid={}",defaultThreadOwnerUserUUID);
			return null;  
		}
		return dealerAssociateID;
	}
	
	
	
	public Boolean addTCPAFooterRules(Map<String, String> map, Message message, Map<String, String> dsoMap) {
		if(map.containsKey(APIConstants.ADD_TCPA_FOOTER) && "true".equalsIgnoreCase(map.get(APIConstants.ADD_TCPA_FOOTER))) {
			return true;
		} else if(message.getIsManual() != null && !message.getIsManual() && map.containsKey(APIConstants.ADD_TCPA_FOOTER) && APIConstants.FALSE.equalsIgnoreCase(map.get(APIConstants.ADD_TCPA_FOOTER))) {
            if(messageRepository.isTextMessageSentInLastXDays(message.getCustomerID(), message.getToNumber(), 30)>0) {
				return false;
			}
		} else if(message.getIsManual()!=null && message.getIsManual() && "true".equalsIgnoreCase(dsoMap.get(DealerSetupOption.COMMUNICATIONS_MANUAL_MESSAGE_OPT_OUT_FOOTER_DISABLE.getOptionKey()))) {
			if(messageRepository.isTextMessageSentInLastXDays(message.getCustomerID(), message.getToNumber(), 30)>0) {
				return false;
			}
		} 
		return true;
	}
	
	public String getTCPAFooter(Map<String, String> dsoMap) {
		String tcpaFooter = "Text STOP to opt-out.";
		if(dsoMap.containsKey(DealerSetupOption.COMMUNICATIONS_OPT_OUT_FOOTER_TEXT.getOptionKey()) && 
				dsoMap.get(DealerSetupOption.COMMUNICATIONS_OPT_OUT_FOOTER_TEXT.getOptionKey())!=null && 
				!dsoMap.get(DealerSetupOption.COMMUNICATIONS_OPT_OUT_FOOTER_TEXT.getOptionKey()).isEmpty()) {
			 tcpaFooter = dsoMap.get(DealerSetupOption.COMMUNICATIONS_OPT_OUT_FOOTER_TEXT.getOptionKey());
		}
		return tcpaFooter;
	}
	
	public Boolean postMessageProcessingToBeDone(Message message, Map<String, String> dsoMap) {
		if(message.getIsManual()!=null && message.getIsManual()  && "true".equalsIgnoreCase(dsoMap.get(DealerSetupOption.COMMUNICATION_POST_MESSAGE_SENT.getOptionKey()))) {
			return true;
		} else if((message.getIsManual()==null || !message.getIsManual()) && "true".equalsIgnoreCase(dsoMap.get(DealerSetupOption.COMMUNICATION_POST_MESSAGE_SENT_AUTOMATIC.getOptionKey()))) {
			return true;
		} else if(message.getIsManual()!=null && message.getIsManual()) {
			return true;
		}
		return false;
	}
	
	public Boolean isTextingAllowed(Message message, HashMap<String, String> metaDataMap, Boolean doubleOptInEnabled) throws Exception {
		if(metaDataMap.containsKey(APIConstants.OVERRIDE_OPT_OUT) && "true".equalsIgnoreCase(metaDataMap.get(APIConstants.OVERRIDE_OPT_OUT))) {
			return true;
		}
		String departmentUUID = generalRepository.getDepartmentUUIDForDepartmentID(message.getDealerDepartmentId());
		if(doubleOptInEnabled) {
			ResponseEntity<OptOutStatusResponse> response = optOutImpl.getOptOutStatus(departmentUUID, MessageProtocol.TEXT.name(), message.getToNumber());
			return OptOutState.OPTED_IN.name().equalsIgnoreCase(response.getBody().getOptOutStatus().getOptOutState());
		} else {
			String dealerUUID = generalRepository.getDealerUUIDFromDealerId(message.getDealerID());
			CommunicationStatusResponse response = kMessagingApiHelper.getCommunicationStatus(dealerUUID, departmentUUID, APIConstants.TEXT, message.getToNumber());
			if(OptOutStatus.OPTED_IN.name().equalsIgnoreCase(response.getOptoutStatus())) {
				return true;
			}
			return false;
		}
	}
	
	public List<String> isEmailingAllowed(Message message, HashMap<String, String> metaDataMap, HashMap<String, String> dsoMap, List<InternetAddress> ccList,
			List<InternetAddress> bccList, List<InternetAddress> toList) throws Exception {
		if(metaDataMap.containsKey(APIConstants.OVERRIDE_OPT_OUT) && "true".equalsIgnoreCase(metaDataMap.get(APIConstants.OVERRIDE_OPT_OUT))) {
				return null;
		}
		if(!"true".equalsIgnoreCase(dsoMap.get(DealerSetupOption.MESSAGING_OPTOUT_EMAIL_ENABLED_ROLLOUT.getOptionKey()))) {
			return null;
		}
		
		Set<InternetAddress> emails = new HashSet<>();
		emails.addAll(toList);
		emails.addAll(bccList);
		emails.addAll(ccList);
		List<String> emailList = new ArrayList<String>();
		for(InternetAddress cc : emails) {
			emailList.add(cc.getAddress());
		}
		String dealerUUID = appConfigHelper.getDealerUUIDForID(message.getDealerID());
		List<String> optOutList = kMessagingApiHelper.getOptedOutCommunicationList(dealerUUID, APIConstants.EMAIL, emailList);
		return optOutList;
	}
	
	public String appendEmailSignature(Message message, HashMap<String, String> metaDataMap, HashMap<String, String> dsoMap) {
		
		Boolean addSignature = addSignatureRules(metaDataMap);
		Boolean addDealerFooter = addFooterRules(metaDataMap, dsoMap);
		String messageBody = message.getMessageExtn().getMessageBody();
		
		if(addSignature && addDealerFooter) {
			messageBody = messageBody + "\n" + message.getFromName() + "<br>"+ dsoMap.get(DealerSetupOption.COMMUNICATIONS_TEXT_FOOTER.getOptionKey());
		} else if(addSignature) {
			messageBody = messageBody + "\n" + message.getFromName();
		} else if(addDealerFooter) {
			messageBody = messageBody + "\n" + dsoMap.get(DealerSetupOption.COMMUNICATIONS_TEXT_FOOTER.getOptionKey());
		}
		
		return messageBody;
	}
	
	public Message appendOptOutFooterForOutgoingEmail(Message message, HashMap<String, String> dsoMap) {

		String optOutEnabled = dsoMap.get(DealerSetupOption.MESSAGING_OPTOUT_EMAIL_ENABLED_ROLLOUT.getOptionKey());
		String optOutFooterEnabled = dsoMap.get(DealerSetupOption.MESSAGING_OUTBOUND_EMAIL_FOOTER_ENABLE.getOptionKey());
		String optOutFooter = dsoMap.get(DealerSetupOption.MESSAGING_OUTBOUND_EMAIL_FOOTER_TEXT.getOptionKey());

		if ( "true".equalsIgnoreCase(optOutFooterEnabled) && "true".equalsIgnoreCase(optOutEnabled) ) {
			try {
				LOGGER.info(String.format("Adding optout footer for dealer_id=%s ",message.getDealerID()));

				unsubscribeURL = unsubscribeURL.replace(APIConstants.EMAIL_OPTOUT_UUID_KEY, message.getUuid());

				LOGGER.info(String.format("Sending URL=%s OptOut Footer for email_id=%s for dealer_id=%s", unsubscribeURL,
						message.getToNumber(), message.getDealerID()));

				if(optOutFooter == null || optOutFooter.trim().isEmpty()) {
					optOutFooter = "<hr/><p>If you would like to stop receiving emails from this email address, please click <a href=\""+unsubscribeURL 
							+"\">here to unsubscribe</a>.</p>";
				} else {
					optOutFooter = optOutFooter.replace(APIConstants.OPT_OUT_DYNAMIC_URL, unsubscribeURL);
				}

				String body = message.getMessageExtn().getMessageBody();
				LOGGER.info(String.format("email Body=%s \n OptOut Footer for email_id=%s \n for dealer_id=%s ", body, message.getToNumber(), message.getDealerID()));

				if (body.startsWith("<html>")) {
					Document doc = Jsoup.parse(body);
					Element bodyNode = doc.body();
					bodyNode=bodyNode.getElementsByTag("body").get(0).append(optOutFooter);
					message.getMessageExtn().setMessageBody(doc.html());
				} else {
					body = "<html><body>"+body+optOutFooter+"</body></html>";
					message.getMessageExtn().setMessageBody(body);
				}
				LOGGER.info(String.format("Added OptOut Footer for email_id=%s for dealer_id=%s body={} ",message.getToNumber(), message.getDealerID(), message.getMessageExtn().getMessageBody()));
			} catch (Exception e) {
				LOGGER.warn(String.format("OptOut Footer Add Failed for email_id=%s for dealer_id=%s ", message.getToNumber(), message.getDealerID()), e);
			}
		}

		return message;
	}

	public Message applySubjectRules(String dealershipName, DealerAssociateExtendedDTO dealerAssociate, Message message) {
		if(message.getMessageExtn().getSubject()==null || message.getMessageExtn().getSubject().isEmpty()) {
			String dealerAssociateName="";
			if(dealerAssociate.getFirstName()!=null && !dealerAssociate.getFirstName().isEmpty()) {
				dealerAssociateName= dealerAssociate.getFirstName()+" ";
			}
			if(dealerAssociate.getLastName()!=null && !dealerAssociate.getLastName().isEmpty()) {
				dealerAssociateName= dealerAssociate.getLastName()+" ";
			}
			String subject = String.format(APIConstants.DEFAULT_SUBJECT, dealerAssociateName, dealershipName);
			message.getMessageExtn().setSubject(subject);
		}
		return message;
	}
	
	public List<URI> getMediaURLs(Message message, String brokerNumber) throws Exception {
		try {
			HashMap<String, String> metaData = getMetaData(message);
			if(metaData.get(APIConstants.SEND_VCARD)!=null && "true".equalsIgnoreCase(metaData.get(APIConstants.SEND_VCARD))) {
				List<URI> mediaURLs = new ArrayList<>();
				String url = baseURL+"rest/vcard/"+message.getDealerID()+"/"+message.getDealerDepartmentId()+"/"+brokerNumber;
				LOGGER.info("getMediaURLs v_card=true for message_uuid={} dealer_id={} dealer_department_id={} broker_number={}",
						message.getUuid(), message.getDealerID(), message.getDealerDepartmentId(), brokerNumber);
				URI uri = new URI(url);
				mediaURLs.add(uri);
				return mediaURLs;
			}
		} catch (Exception e) {
			LOGGER.error("Error in getMediaURLs for message_uuid={} broker_number={} ", message.getUuid(), brokerNumber, e);
			throw e;
		}
		return null;
	}
	
	public DraftMessageMetaData updateDraftMessageFailureReasonAndSave(Message message, DraftMessageMetaData dmmd, 
			FailedDraftEnum failureReason) throws JsonProcessingException {
		if(MessageType.DRAFT.getMessageType().equalsIgnoreCase(message.getMessageType()) && dmmd!=null && (dmmd.getReasonForLastFailure()==null || dmmd.getReasonForLastFailure().isEmpty())) {
			dmmd.setStatus(DraftStatus.FAILED.name());
			dmmd.setReasonForLastFailure(failureReason.getFailureReason());
			draftMessageMetaDataRepository.saveAndFlush(dmmd);
		}
		return dmmd;
	}
}

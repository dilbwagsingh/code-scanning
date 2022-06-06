package com.mykaarma.kcommunications.utils;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.StaleObjectStateException;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaarya.utils.XMLHandler;
import com.mykaarma.authenticationutilsmodel.model.v2.common.Resource;
import com.mykaarma.authenticationutilsmodel.model.v2.communications.AuthorizeMessageSendRequest;
import com.mykaarma.authenticationutilsmodel.model.v2.enums.Operation;
import com.mykaarma.authenticationutilsmodel.model.v2.enums.RequesterType;
import com.mykaarma.authenticationutilsmodel.model.v2.enums.ResourceType;
import com.mykaarma.authenticationutilsmodel.model.v2.request.AuthorizationRequest;
import com.mykaarma.global.Authority;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.Delegator;
import com.mykaarma.global.ModuleLogCodes;
import com.mykaarma.global.OptoutStatus;
import com.mykaarma.global.Preference;
import com.mykaarma.global.Role;
import com.mykaarma.global.RoleNames;
import com.mykaarma.kcommunications.controller.impl.SubscriptionsApiImpl;
import com.mykaarma.kcommunications.jpa.repository.DelegationHistoryRepository;
import com.mykaarma.kcommunications.jpa.repository.DocFileRepository;
import com.mykaarma.kcommunications.jpa.repository.DraftMessageMetaDataRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageAttributesRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageExtnRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageMetaDataRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.model.jpa.DelegationHistory;
import com.mykaarma.kcommunications.model.jpa.DocFile;
import com.mykaarma.kcommunications.model.jpa.DraftMessageMetaData;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageAttributes;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.model.jpa.MessageMetaData;
import com.mykaarma.kcommunications.model.jpa.Thread;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.mvc.SubscriptionSaveEventData;
import com.mykaarma.kcommunications.model.rabbit.PostIncomingMessageSave;
import com.mykaarma.kcommunications_model.common.InternalCommentAttributes;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.NotificationAttributes;
import com.mykaarma.kcommunications_model.common.Subscriber;
import com.mykaarma.kcommunications_model.common.SubscriberInfo;
import com.mykaarma.kcommunications_model.common.User;
import com.mykaarma.kcommunications_model.enums.EditorType;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessageType;
import com.mykaarma.kcommunications_model.enums.Tag;
import com.mykaarma.kcommunications_model.mvc.MVCConstants;
import com.mykaarma.kcommunications_model.mvc.SubscriptionList;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcommunications_model.request.SubscriptionRequest;
import com.mykaarma.kcommunications_model.response.ApiWarning;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcommunications_model.response.SaveMessageListResponse;
import com.mykaarma.kcommunications_model.response.SaveMessageResponse;
import com.mykaarma.kcustomer_model.dto.Customer;
import com.mykaarma.kcustomer_model.dto.CustomerUpdateRequest;
import com.mykaarma.kcustomer_model.dto.EmailDetails;
import com.mykaarma.kcustomer_model.dto.PhoneDetails;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.kmanage.model.dto.json.PreferenceDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetEmailTemplateResponseDTO;
import com.mykaarma.kmanage.model.dto.mykaarma.DealerAssociateGroupProto.DealerAssociateGroup;
import com.mykaarma.outofoffice_model.dto.DealerAssociateDto;


@Service
public class Helper {

	private final static Logger LOGGER = LoggerFactory.getLogger(Helper.class);
	public static String OPTED_OUT_SUSPECTED = "OPTED_OUT_SUSPECTED";
	private static final String LOCALE_ENUS ="en-us";
	
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Autowired
	AppConfigHelper appConfigHelper;
	
	@Autowired
	private KMessagingApiHelper kMessagingApiHelper;
	
	@Autowired
	DelegationHistoryRepository delegationHistoryRepository;
	
	@Autowired
	MessageRepository messageRepository;
	
	@Autowired
	MessageMetaDataRepository messageMetaDataRepository;
	
	@Autowired
	MessageExtnRepository messageExtnRepository;

	@Autowired
	DraftMessageMetaDataRepository draftMessageMetaDataRepository;

	@Autowired
	MessageAttributesRepository messageAttributesRepository;

	@Autowired
	private GeneralRepository generalRepository;

	@Autowired
	GeneralRepository generalRepo;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	KCommunicationsUtils kCommunicationsUtils;

	@Autowired
	ThreadRepository threadRepository;

	@Autowired
	SubscriptionsApiImpl subscriptionsApiImpl;

	@Autowired
	MessagingViewControllerHelper messagingViewControllerHelper;

	@Autowired
	private OutOfOfficeV2Helper outOfOfficeV2Helper;

	@Autowired
	private DocFileRepository docFileRepository;

	public String getDealerAssociateName(DealerAssociateExtendedDTO dealerAssociate) {
		String name = "";
		if (dealerAssociate.getFirstName() != null) {
			name = name + dealerAssociate.getFirstName() + " ";
		}
		if (dealerAssociate.getLastName() != null) {
			name = name + dealerAssociate.getLastName();
		}
		return name;
	}
	
	public String getCustomerName(Customer customer) {
		String firstName = customer.getFirstName();
		String lastName = customer.getLastName();
		if((firstName==null || firstName.trim().isEmpty()) && (lastName==null || lastName.trim().isEmpty())) {
			return customer.getCompany();
		}
		
		String customerName=((firstName!=null && !firstName.isEmpty())?firstName+" ":"")+((lastName!=null && !lastName.isEmpty())?lastName:"");
		return customerName;
	}
	
	public String getBase64EncodedSHA256UUID() {
		String uuid = UUID.randomUUID().toString();
		return getBase64EncodedSHA256(uuid);
	}
	
    public String getBase64EncodedSHA256(String text) {
        String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(text);
		return getUrlSafeBase64StrFromHexStr(sha256hex);
    }

	private String getUrlSafeBase64StrFromHexStr(String sha256hex) {	
		BigInteger sha256Num = new BigInteger(sha256hex, 16);
		byte[] encodedHexB64 = org.apache.commons.net.util.Base64.encodeInteger(sha256Num);
		return new String(encodedHexB64).replace("+", "-").replace("/", "_").replace("=", "").trim();
	}
	
	public Date getPstDateFromIsoDate(String date) {
		if(date==null || date.isEmpty()){
			return null;
		}
		try {
			DateTimeFormatter parser2 = ISODateTimeFormat.dateTimeNoMillis();
			LOGGER.info(String.format("getPstDateFromIsoDate iso_date=%s pst_date=%s ", date, parser2.parseDateTime(date).toDate()));
			return parser2.parseDateTime(date).toDate();
		} catch (Exception e) {
			LOGGER.error(String.format("Error in parsing date for date=%s ",date), e);
		}
		return null;
	}
	
	public String prepareMetaData(SendMessageRequest sendMessageRequest, Customer customer, Long departmentId) throws JSONException {
		
		HashMap<String, String> metaData = new HashMap<>();
		if(sendMessageRequest.getMessageAttributes().getMetaData()!=null) {
			metaData = sendMessageRequest.getMessageAttributes().getMetaData();
		}
		MessageSendingAttributes msa = sendMessageRequest.getMessageSendingAttributes();
		if(msa!=null) {
			
			if(msa.getAddSignature()!=null && msa.getAddSignature()) {
				metaData.put(APIConstants.ADD_SIGNATURE, APIConstants.TRUE);
			}
			if(msa.getAddTCPAFooter()!=null) {
                if(msa.getAddTCPAFooter()) {
                    metaData.put(APIConstants.ADD_TCPA_FOOTER, APIConstants.TRUE);
                }
				else {
                    metaData.put(APIConstants.ADD_TCPA_FOOTER, APIConstants.FALSE);
                }
			}
			if(msa.getOverrideHolidays()!=null && !msa.getOverrideHolidays()) {
				metaData.put(APIConstants.OVERRIDE_HOLIDAYS, APIConstants.FALSE);
			}
			if(msa.getOverrideOptoutRules()!=null && msa.getOverrideOptoutRules()) {
				metaData.put(APIConstants.OVERRIDE_OPT_OUT, APIConstants.TRUE);
			}
			if(msa.getSendVCard()!=null && msa.getSendVCard()) {
				metaData.put(APIConstants.SEND_VCARD, APIConstants.TRUE);
			}
			if(msa.getCallbackURL()!=null && !msa.getCallbackURL().isEmpty()) {
				metaData.put(APIConstants.CALLBACK_URL, msa.getCallbackURL());
			}
			if(msa.getListOfEmailsToBeCCed()!=null && !msa.getListOfEmailsToBeCCed().isEmpty()) {
				String ccList= String.join(",", msa.getListOfEmailsToBeCCed());
				LOGGER.info("cc_list={}", ccList);
				metaData.put(APIConstants.CC_LIST, ccList);
			}
			if(msa.getListOfEmailsToBeBCCed()!=null && !msa.getListOfEmailsToBeBCCed().isEmpty()) {
				String bccList= String.join(",", msa.getListOfEmailsToBeBCCed());
				LOGGER.info("bcc_list={}", bccList);
				metaData.put(APIConstants.BCC_LIST,bccList);
			}
			if(msa.getAddFooter()!=null && !msa.getAddFooter()) {
				metaData.put(APIConstants.ADD_FOOTER, APIConstants.FALSE);
			}
			if(Boolean.TRUE == msa.getQueueIfOptedOut()) {
				metaData.put(MessageMetaDataConstants.QUEUE_IF_OPTED_OUT, APIConstants.TRUE);
			}
		}
		if (sendMessageRequest.getNotificationAttributes() != null) {
			try {
				metaData.put(APIConstants.NOTIFICATION_ATTRIBUTES, objectMapper.writeValueAsString(sendMessageRequest.getNotificationAttributes()));
			} catch (JsonProcessingException e) {
				LOGGER.warn("Error while converting notificationAttributes to json {}", sendMessageRequest.getNotificationAttributes());
			}
		}

		if(MessageType.NOTE.getMessageType().equalsIgnoreCase(sendMessageRequest.getMessageAttributes().getType().getMessageType()) &&
				sendMessageRequest.getMessageAttributes().getIsManual()) {
			SubscriptionList subscriptionList = messagingViewControllerHelper.getSubscriptionForCustomer(getSubscriptionRequest(customer, departmentId));
			if(subscriptionList != null && subscriptionList.getInternalSubscriberList() != null && !subscriptionList.getInternalSubscriberList().isEmpty()) {
				List<User> followers = new ArrayList<>();
				for(Subscriber subscriber: subscriptionList.getInternalSubscriberList()) {
					User follower = getUserForSubscriber(subscriber);
					if(follower != null) {
						followers.add(follower);
					}
				}

				if(sendMessageRequest.getInternalCommentAttributes() == null) {
					sendMessageRequest.setInternalCommentAttributes(new InternalCommentAttributes());
				}

				sendMessageRequest.getInternalCommentAttributes().setUsersFollowing(followers);
			}

			try {
				HashMap<String, List<User>> groupMemberInfoMap = populateGroupInfoForUsersToNotify(sendMessageRequest.getInternalCommentAttributes().getUsersToNotify());
				sendMessageRequest.getInternalCommentAttributes().setGroupMembers(groupMemberInfoMap);
			}
			catch(Exception e) {
				LOGGER.error("Error while populating group members {}", sendMessageRequest.getInternalCommentAttributes(), e);
			}
			try {
				metaData.put(MessageMetaDataConstants.INTERNAL_COMMENT_ATTRIBUTES, objectMapper.writeValueAsString(sendMessageRequest.getInternalCommentAttributes()));
			} catch (JsonProcessingException e) {
				LOGGER.error("Error while converting internal comment attributes to json {}", sendMessageRequest.getInternalCommentAttributes(), e);
			}
		}

		if(metaData!=null && !metaData.isEmpty()) {
			org.json.JSONObject json = prepareMetadata(metaData);
			org.json.JSONObject currentJson = new org.json.JSONObject();
			Iterator<String> iter = json.keys();
			while(iter.hasNext()) {
				String key = iter.next();
				currentJson.put(key, json.get(key));
			}
			return currentJson.toString();
		}
		return null;
	}

	public HashMap<String, List<User>> populateGroupInfoForUsersToNotify(List<User> usersToNotify) {
		
		String dealerAssociateUuid = null;
		GetDealerAssociateResponseDTO getDealerAssociateResponseDto = null;
		DealerAssociateGroup dealerAssociateGroup = null;
		List<String> groupDealerAssociateUuids = new ArrayList<String>();
		HashMap<String, List<User>> groupMemberMap = new HashMap<String, List<User>>();
		if(usersToNotify != null && !usersToNotify.isEmpty()) {
			for(User user: usersToNotify) {
				if(!EditorType.USER.equals(user.getType())) {
					continue;
				}

				getDealerAssociateResponseDto = kManageApiHelper.getDealerAssociate(user.getDepartmentUuid(), user.getUuid());
				if(getDealerAssociateResponseDto==null || getDealerAssociateResponseDto.getDealerAssociate()==null) {
					continue;
				}

				dealerAssociateUuid = getDealerAssociateResponseDto.getDealerAssociate().getUuid();
				if(RoleNames.VIRTUAL_GROUP_DEALERASSOCIATE.equals(getDealerAssociateResponseDto.getDealerAssociate().getRoleDTO().getRoleName())) {
					List<User> userList = new ArrayList<User>();
					dealerAssociateGroup = kManageApiHelper.getDealerAssociateGroupForDA(user.getDepartmentUuid(), dealerAssociateUuid);
					if(dealerAssociateGroup!=null && dealerAssociateGroup.getDealerAssociateUuidsList()!=null
							&& !dealerAssociateGroup.getDealerAssociateUuidsList().isEmpty()) {
						groupDealerAssociateUuids = dealerAssociateGroup.getDealerAssociateUuidsList();

						for(String groupMemberDaUuid: groupDealerAssociateUuids) {
							String groupMemberDaDepartmentUuid = generalRepository.getDepartmentUuidForDealerAssociateUuid(groupMemberDaUuid);
							User groupUser = getUserForDealerAssociate(groupMemberDaUuid, groupMemberDaDepartmentUuid);
							userList.add(groupUser);
						}
						groupMemberMap.put(user.getUuid(), userList);
					}
				}
			}
		}
		return groupMemberMap;
	}
	public SubscriptionRequest getSubscriptionRequest(Customer customer, Long departmentId) {
		SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
		subscriptionRequest.setCustomerUUID(customer.getGuid());
		subscriptionRequest.setDealerID(customer.getDealerID());
		subscriptionRequest.setSubscriptionType(MVCConstants.INTERNAL_SUBSCRIPTION_TYPE);
		subscriptionRequest.setDealerDepartmentID(departmentId);
		return subscriptionRequest;
	}

	public SubscriptionRequest getSubscriptionRequest(String customerUuid, String departmentUuid) {
		SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
		subscriptionRequest.setCustomerUUID(customerUuid);
		subscriptionRequest.setDealerID(generalRepo.getDealerIDFromDepartmentUUID(departmentUuid));
		subscriptionRequest.setSubscriptionType(MVCConstants.INTERNAL_SUBSCRIPTION_TYPE);
		subscriptionRequest.setDealerDepartmentID(generalRepo.getDepartmentIDForUUID(departmentUuid));
		return subscriptionRequest;
	}
	
	public User getUserForSubscriber(Subscriber subscriber) {
		String dealerAssociateUuid = generalRepository.getDealerAssociateUuidFromDealerAssociateId(subscriber.getDealerAssociateID());
		String departmentUuid = generalRepository.getDepartmentUuidForDealerAssociateId(subscriber.getDealerAssociateID());

		return getUserForDealerAssociate(dealerAssociateUuid, departmentUuid);
	}

	@SuppressWarnings("unchecked")
	public String prepareMetaData(String key, List<Long> idList) throws JSONException {
		JSONObject jsonMeta = new JSONObject();
		if(idList != null) {
			JSONArray userJSONArray = new JSONArray();
			for(Long userID: idList) {
				userJSONArray.put(userID);
			}
			jsonMeta.put(key, userJSONArray);
		}
		return jsonMeta.toString();
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, String> getMessageMetaDatMap(String messageMetaData) throws Exception {
        try {
        	HashMap<String, String> map = objectMapper.readValue(messageMetaData, HashMap.class);
            return map;
        } catch (Exception e) {
            LOGGER.error("Error in converting metadata to map for meta_date={} ", messageMetaData, e);
            throw e;
        }
	}
	public String getMessageMetaData(HashMap<String, String> metadataMap) throws JSONException {
		org.json.JSONObject json = prepareMetadata(metadataMap);
		org.json.JSONObject currentJson = new org.json.JSONObject();
		Iterator<String> iter = json.keys();
		while(iter.hasNext()) {
			String key = iter.next();
			currentJson.put(key, json.get(key));
		}
		return currentJson.toString();
	}
	
	public static org.json.JSONObject prepareMetadata(HashMap<String, String> metadataMap) throws JSONException {
		org.json.JSONObject json = new org.json.JSONObject();
		for(Map.Entry<String, String> entry: metadataMap.entrySet()) {
			json.put(entry.getKey(), entry.getValue());
		}
		return json;
	}
	
	public String getPreferredCommunicationValueForProtocol(Customer customer, MessageProtocol messageProtocol) {
		if(messageProtocol.equals(MessageProtocol.TEXT)) {
			if (customer.getPhoneNumbers() != null) {
				for (PhoneDetails phone : customer.getPhoneNumbers()) {
					if (phone.getIsPreferred() && phone.getOkToText()) {
						return phone.getPhoneNumber();
					}
				}
			}
		} else if(messageProtocol.equals(MessageProtocol.EMAIL)) {
			if (customer.getEmails() != null) {
				for (EmailDetails email : customer.getEmails()) {
					if (email.getIsPreferred() && email.getOkToEmail()) {
						return email.getEmailAddress();
					}
				}
			}
		}
		return null;
	}

	public String getPreferredCommunicationValueForProtocolNoOk(Customer customer, MessageProtocol messageProtocol) {
		if (messageProtocol.equals(MessageProtocol.TEXT)) {
			if (customer.getPhoneNumbers() != null) {
				for (PhoneDetails phone : customer.getPhoneNumbers()) {
					if (phone.getIsPreferred()) {
						return phone.getPhoneNumber();
					}
				}
			}
		} else if (messageProtocol.equals(MessageProtocol.EMAIL)) {
			if (customer.getEmails() != null) {
				for (EmailDetails email : customer.getEmails()) {
					if (email.getIsPreferred()) {
						return email.getEmailAddress();
					}
				}
			}
		}
		return null;
	}

	public Thread getNewThread(Message message, Long threadDelegatee, Date date) {
		Thread thread = new Thread();
		thread.setArchived(false);
		thread.setClosed(false);
		thread.setCustomerID(message.getCustomerID());
		thread.setDealerDepartmentID(message.getDealerDepartmentId());
		thread.setDealerID(message.getDealerID());
		thread.setLastMessageOn(date);
		if (threadDelegatee != null) {
			thread.setDealerAssociateID(threadDelegatee);
		} else {
			GetDealerAssociateResponseDTO dealerAssociate = kManageApiHelper.getDefaultDealerAssociateForDepartment
				(appConfigHelper.getDealerDepartmentUUIDForID(message.getDealerDepartmentId()));
			thread.setDealerAssociateID(dealerAssociate.getDealerAssociate().getId());
		}
		return thread;
	}

	public Thread getNewThread(Long customerID, Long dealerDepartmentID, Long dealerID, Date date, Long threadDelegatee) {
		Thread thread = new Thread();
		thread.setArchived(false);
		thread.setClosed(false);
		thread.setCustomerID(customerID);
		thread.setDealerDepartmentID(dealerDepartmentID);
		thread.setDealerID(dealerID);
		thread.setLastMessageOn(date);
		if (threadDelegatee != null) {
			thread.setDealerAssociateID(threadDelegatee);
		} else {
			GetDealerAssociateResponseDTO dealerAssociate = kManageApiHelper.getDefaultDealerAssociateForDepartment
				(appConfigHelper.getDealerDepartmentUUIDForID(dealerDepartmentID));
			thread.setDealerAssociateID(dealerAssociate.getDealerAssociate().getId());
		}
		return thread;
	}

	public DelegationHistory getDelegationHistory(Message message, Thread thread, Long previousOwner, Delegator delegator) {
		DelegationHistory delegationHistory = new DelegationHistory();
		delegationHistory.setDelegatedFrom(previousOwner);
		delegationHistory.setDelegatedTo(thread.getDealerAssociateID());
		if (delegator == null) {
			delegator = Delegator.AUTOMATIC_DELEGATION;
		}
		delegationHistory.setDelegator(delegator.getDelegator());
		delegationHistory.setIsRevoked(false);
		delegationHistory.setThreadID(thread.getId());
		delegationHistory.setTimeOfChange(new Date());
		Long threadOwner = delegationHistoryRepository.getFirstThreadOwnerForThread(thread.getId());
		if (threadOwner == null) {
			threadOwner = previousOwner;
		}
		delegationHistory.setThreadOwner(threadOwner);
		return delegationHistory;
	}
	
	public Delegator getDelegatorForThreadDelegation(Long previousOwner){
		DealerAssociateDto delegatee = outOfOfficeV2Helper.getDelegatee(
			generalRepo.getDepartmentUuidForDealerAssociateId(previousOwner),
			generalRepo.getUserUUIDForDealerAssociateID(previousOwner)
		);
		
		Delegator delegator=Delegator.AUTOMATIC_DELEGATION;
		
		if(delegatee!=null){
			delegator=Delegator.OUT_OF_OFFICE_START;
		}
		
		return delegator;
	}
	
	public String getTags(List<Tag> tags) {
		String result=null;
		if(tags!=null && !tags.isEmpty()) {
			result = "";
			for(Tag t: tags) {
				result=result+t.getTag()+",";
			}
			return (result == null || result.length() == 0)
				      ? null
				      : (result.substring(0, result.length() - 1));
		}
		return result;
	}
	
	public Message getMessageObject(String messageUUID) {
		Message message = null;
		message = messageRepository.findByuuid(messageUUID);
		if(message!=null) {
			MessageExtn messageExtn = messageExtnRepository.findByMessageID(message.getId());
			message.setMessageExtn(messageExtn);
			MessageMetaData messageMetaData = messageMetaDataRepository.findByMessageID(message.getId());
			message.setMessageMetaData(messageMetaData);
			DraftMessageMetaData draftMessageMetaData = draftMessageMetaDataRepository.findByMessageID(message.getId());
			message.setDraftMessageMetaData(draftMessageMetaData);
			MessageAttributes messageAttributes = messageAttributesRepository.findByMessageID(message.getId());
			message.setMessageAttributes(messageAttributes);
		}
		return message;
	}

	public Message getMessageObjectWithDocFiles(String messageUUID) {
		Message message = getMessageObject(messageUUID);
		if(message!=null) {
			List<DocFile> docFiles = docFileRepository.findByMessageId(message.getId());
			message.setDocFiles(new HashSet<>(docFiles));
		}
		return message;
	}

	public Message getMessageObjectById(Long messageId) {
		Message message = null;
		message = messageRepository.findByid(messageId);
		if(message!=null) {
			MessageExtn messageExtn = messageExtnRepository.findByMessageID(message.getId());
			message.setMessageExtn(messageExtn);
			MessageMetaData messageMetaData = messageMetaDataRepository.findByMessageID(message.getId());
			message.setMessageMetaData(messageMetaData);
			DraftMessageMetaData draftMessageMetaData = draftMessageMetaDataRepository.findByMessageID(message.getId());
			message.setDraftMessageMetaData(draftMessageMetaData);
			MessageAttributes messageAttributes = messageAttributesRepository.findByMessageID(message.getId());
			message.setMessageAttributes(messageAttributes);
		}
		return message;
	}
	
	public Response getWarningResponse(String warningName, String warningDescription, List<ApiWarning> warnings, Response response) {
		
		ApiWarning apiWarn = new ApiWarning(warningName, warningDescription);
		warnings.add(apiWarn);
		response.setWarnings(warnings);
		
		return response;
	}
	
	public String getEmailTemplateTypeAndDealerID(Long dealerId,String emailTemplateType,String locale) throws Exception{
		String dealerUUID=generalRepository.getDealerUUIDFromDealerId(dealerId);
		GetEmailTemplateResponseDTO emailTemplateResponseDTO=KManageApiHelper.getEmailTemplate(dealerUUID, emailTemplateType,locale);
		ObjectMapper mapper = new ObjectMapper();
		
		LOGGER.info(String.format("in getEmailTemplateTypeAndDealerID  for locale=%s emailTemplate of type=%s  "
				+ "for dealer_id=%s response=%s",locale,emailTemplateType,dealerId,mapper.writeValueAsString(emailTemplateResponseDTO)));
		
		if(emailTemplateResponseDTO==null || emailTemplateResponseDTO.getEmailTemplate()==null || emailTemplateResponseDTO.getEmailTemplate().isEmpty()){
			LOGGER.info(String.format("in getEmailTemplateTypeAndDealerID  for locale=%s emailTemplate of type=%s not configured "
					+ "for dealer_id=%s",locale,emailTemplateType,dealerId));
			return null;
		}

		String messageBody=emailTemplateResponseDTO.getEmailTemplate();
		LOGGER.info(String.format("in getEmailTemplateTypeAndDealerID  for locale=%s emailTemplate of type=%s  "
				+ "for dealer_id=%s email_template=%s",locale,emailTemplateType,dealerId,messageBody));
		return messageBody;
	}
	
	public String getDealerPreferredLocale(String dealerUUID) {
		String preferredLanguagesDso = null;
		String result = null;
		try {
			preferredLanguagesDso = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.LDSOKEY.getOptionKey());
		} catch (Exception e) {
			LOGGER.warn("Error while getting DSO value for option_key={} dealer_uuid={}", DealerSetupOption.LDSOKEY.getOptionKey(), dealerUUID);
		}
		if (preferredLanguagesDso != null && !preferredLanguagesDso.isEmpty()) {
			result = preferredLanguagesDso.split(",")[0];
		} else {
			result = LOCALE_ENUS;
		}
		return result;
	}

	public String getDealerAssociatePreferredLocale(String departmentUuid, String userUuid) {
		String preferredLanguage = LOCALE_ENUS;
		try {
			Long departmentId = generalRepository.getDepartmentIDForUUID(departmentUuid);
			String daUuid = generalRepository.getDealerAssociateUuidForUserUuid(userUuid, departmentId);
			PreferenceDTO preferenceDTO = kManageApiHelper.getDAPreference(departmentUuid, daUuid, Preference.LANGUAGE.getKeyName());
			if(preferenceDTO != null) {
				preferredLanguage = preferenceDTO.getValue();
			}
		} catch (Exception e) {
			LOGGER.warn("error in getDealerAssociatePreferredLocale for department_uuid={} user_uuid={}", departmentUuid, userUuid);
		}
		return preferredLanguage;
	}
	
	public Message fetchMessageForGivenUUID(String messageUUID){
		Message message=null;
		MessageExtn messageExtn=new MessageExtn();
		List<Object[]> messagePropertyResult=messageRepository.fetchMessageForGivenUUID(messageUUID);
		if(messagePropertyResult!=null && !messagePropertyResult.isEmpty()){
			message=new Message();
			messageExtn=new MessageExtn();
			message.setMessageExtn(messageExtn);
			for(Object[] messageIterator:messagePropertyResult){
				if(messageIterator[0]!=null){
					BigInteger messageIdObj=(BigInteger)messageIterator[0];
					message.setId(messageIdObj.longValue());
				}
				if(messageIterator[1]!=null){
					String messageType=(String)messageIterator[1];
					message.setMessageType(messageType);
				}
				if(messageIterator[2]!=null){
					String messageProtcol=(String)messageIterator[2];
					message.setProtocol(messageProtcol);
				}
				if(messageIterator[3]!=null){
					BigInteger customerIdObj=(BigInteger)messageIterator[3];
					message.setCustomerID(customerIdObj.longValue());
				}
				if(messageIterator[4]!=null){
					BigInteger dealerIdObj=(BigInteger)messageIterator[4];
					message.setDealerID(dealerIdObj.longValue());
				}
				if(messageIterator[5]!=null){
					BigInteger dealerAssociateIdObj=(BigInteger)messageIterator[5];
					message.setDealerAssociateID(dealerAssociateIdObj.longValue());
				}
				if(messageIterator[6]!=null){
					String messageSubject=(String)messageIterator[6];
					messageExtn.setSubject(messageSubject);
				}
				if(messageIterator[7]!=null){
					String messageBody=(String)messageIterator[7];
					messageExtn.setMessageBody(messageBody);
				}
			}
		}
		return message;
	}
	
	public Document getMessageXMLFromResultSet(List<Message> messages,
			String latestCustomerName,
			String path,
			String version, Boolean retainMessageFormatting)
					throws Exception {

		Document messageXml = null;
		try {

			InputStream xmlStream = new URL(path).openConnection().getInputStream();
			messageXml =  XMLHandler.xmlString2Dom(xmlStream);
			
			if(messageXml!=null) {

				NamedNodeMap parentAttributes = messageXml.getFirstChild().getAttributes();

				if(messages==null) {
					throw new Exception("Customer not found");
				}
				
				
				parentAttributes.getNamedItem("customer").setTextContent(latestCustomerName);

				Node messageNode = null;

				Iterator<Message> it = messages.iterator();
				int i = 1;
				while(it.hasNext()) {
					
					//create a new message node
					createMessageNode(messageXml);

					//get the message node created for editing
					messageNode = XmlProcessorUtil.getNodeForXpath("/Messages/Message["+i+"]", messageXml);

					//get the message to fill values
					Message m = it.next();
					NamedNodeMap attributes = messageNode.getAttributes();

					//set daName
					String daName = "";
					if("I".equalsIgnoreCase(m.getMessageType())){
						if(m.getToName()!=null&&!m.getToName().isEmpty()){
							daName=m.getToName();
						} 
					} else {
						if(m.getFromName()!=null&&!m.getFromName().isEmpty()){
							daName=m.getFromName();
						}
					}
					//set protocol
					String protocol = m.getProtocol();
					attributes.getNamedItem("protocol").setTextContent(protocol);

					//set message type
					String type = m.getMessageType();
					attributes.getNamedItem("type").setTextContent(type);

					LOGGER.info("protocol "+protocol+" type "+ type);
					
					//set service advisor name
					attributes.getNamedItem("sa").setTextContent(daName); // change names here hardcoded

					//set date
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					attributes.getNamedItem("time").setTextContent(formatter.format(m.getReceivedOn()));

					//set communication value after editing
					String commvalue = "";
					if(type!=null && type.equalsIgnoreCase("S")) {
						commvalue = m.getToNumber();
					}
					else if(type!=null && type.equalsIgnoreCase("I")) {
						commvalue = m.getFromNumber();
					}
					if(protocol!=null) {
						if(protocol.equalsIgnoreCase("V") || protocol.equalsIgnoreCase("X"))
							commvalue = formatPhone(commvalue);
					}

					attributes.getNamedItem("comm").setTextContent(commvalue);
					
					String customerName=null;
					if("I".equalsIgnoreCase(m.getMessageType())){
						if(m.getFromName()!=null&&!m.getFromName().isEmpty()){
							customerName=m.getFromName();
						} else {
							customerName=latestCustomerName;
						}
					} else {
						if(m.getToName()!=null&&!m.getToName().isEmpty()){
							customerName=m.getToName();
						} else {
							customerName=latestCustomerName;
						}
					}
					if("I".equalsIgnoreCase(m.getMessageType())
							&& ("X".equalsIgnoreCase(protocol) ||  "E".equalsIgnoreCase(protocol))){
						String optOutStatus=checkForOptOut(m);
						if(optOutStatus!=null){
							attributes.getNamedItem("optoutstatus").setTextContent(optOutStatus);
							if(OptoutStatus.OPTED_IN.name().equalsIgnoreCase(optOutStatus)){
								attributes.getNamedItem("optoutstatusmessage").setTextContent("This message indicated that the customer has opted-back to communications."
										+ "The customer communication preferences have been changed. ");
							} else if(OptoutStatus.OPTED_OUT.name().equalsIgnoreCase(optOutStatus) || OPTED_OUT_SUSPECTED.equalsIgnoreCase(optOutStatus)){
								attributes.getNamedItem("optoutstatusmessage").setTextContent("This message has been detected as an opt-out."
										+ "The customer communication preferences have been changed to disable outbound texts. ");
							}
							LOGGER.info(ModuleLogCodes.MESSAGING_INFO_CODE.getLogMessage()+" successfully setted the attribute optoutstatus="
							+attributes.getNamedItem("optoutstatus").getTextContent()+
							"optoutstatusmessage="+attributes.getNamedItem("optoutstatusmessage").getTextContent());
						}
					}
					
					attributes.getNamedItem("customername").setTextContent(customerName);
					//set message body after editing
					String message = (m.getMessageExtn().getMessageBody()==null||m.getMessageExtn().getMessageBody().isEmpty())?m.getMessageExtn().getSubject():m.getMessageExtn().getMessageBody();

					if(protocol!=null) {

						if(protocol.equalsIgnoreCase("P") || protocol.equalsIgnoreCase("R")) {
							message = m.getMessageExtn().getSubject();

							if(message==null || message.isEmpty())
								message = "No message to show";
						}

						else if(protocol.equalsIgnoreCase("V")) {
							message="[VOICE RECORDING] can be played through Kaarma app";
						}

						else if(protocol.equalsIgnoreCase("E") && !retainMessageFormatting) {
							message = message.replaceAll("\\<[^\\>]*\\>", "");

							if(message.indexOf("\">")>0)
								message = message.substring(message.indexOf("\">")+3, message.length()-1);
						}
					}
					
					if(!retainMessageFormatting)
						message = message.replaceAll("&nbsp;", " ").trim();
					attributes.getNamedItem("note").setTextContent(message);

					//to edit the next node if required
					i++;
					//re allocating resources
					m = null;
				}
				it = null;
				messages.clear();
				if(messages.isEmpty())
					messages = null;
			}

		} catch (Exception e) {

			LOGGER.error("", e);
			throw e;

		}

		return messageXml;
	}

	public String prepareObjectAndCallKcustomerV2ToSaveNewCustomer(String firstName, String lastName, String phoneNumber, Long departmentID, String messageSid) {

		CustomerUpdateRequest customerUpdateRequest = kCommunicationsUtils.createCustomerRequestObject(firstName,
			lastName, phoneNumber);
		String departmentUUID = appConfigHelper.getDealerDepartmentUUIDForID(departmentID);
		String customerUUID = KCustomerApiHelperV2.saveCustomer(departmentUUID, customerUpdateRequest);
		LOGGER.info("customerUUID={} received after saving customer for message_sid={}", customerUUID, messageSid);

		return customerUUID;

	}

	public PostIncomingMessageSave createPostIncomingMessageSaveObject(Message callmessage, Long dealerAssociateID) {
		PostIncomingMessageSave postIncomingMessageSave = new PostIncomingMessageSave();
		postIncomingMessageSave.setMessage(callmessage);
		postIncomingMessageSave.setThreadDelegatee(dealerAssociateID);
		postIncomingMessageSave.setUpdateThreadTimestamp(true);
		postIncomingMessageSave.setExpiration(null);

		return postIncomingMessageSave;
	}

	public Long createNewCustomerForUnknownNumber(String caller, Long departmentID) {

		LOGGER.info("customer does not exist for contact number={} , departmentID = {}", caller, departmentID);
		Long customerID = null;
		String customerUUID = prepareObjectAndCallKcustomerV2ToSaveNewCustomer(caller,
			"Unknown Number", caller, departmentID, null);
		Object[] customerInfo = generalRepository.getCustomerIDandNameFromUUID(customerUUID);
		customerID = ((BigInteger) customerInfo[0]).longValue();

		return customerID;
	}

	public void addVersionMessageToMessages(Document messageXml, String version, String versionMessage) {

		if ((versionMessage != null && !versionMessage.isEmpty()) || (version != null && !version.isEmpty())) {
			NamedNodeMap parentAttributes = messageXml.getFirstChild().getAttributes();

			if (versionMessage != null && !versionMessage.isEmpty())
				parentAttributes.getNamedItem("version").setTextContent(versionMessage);
			else
				parentAttributes.getNamedItem("version").setTextContent(version);
		}

	}
	
	public String getHTMLMessageForError(String major, String minor, String small) {
		return "<!doctype html> "
				+ "<html>         "
				+ "<head>                 "
				+ "<meta name=\"viewport\" content=\"width=device-width,user-scalable=no\" />                 	"
				+ "<title>myKaarma - API!!!</title>         "
				+ "</head>         "
				+ "<body style=\"background-color:#F4F4F4;text-align: center;font-name:Lato;\">                 "
				+ "<img src=\"https://dev.kaar-ma.com/images/kaarma-med.png\">                 "
				+ "<h1>"+major+"</h1>                            "
				+ "<h3>"+minor+"</h3>                            "
				+ "<h5>"+(small!=null?small:"") + "</h5> 		"
				+ " <br/> <br/> <p style=\"font-size: 14pt; color: #555;\">"
				+ "The Kaar-ma.com Team</p>                 "
				+ "<p style=\"font-size:12pt;color:#999;\">&copy; Copyright 2010-2011 Kaar-ma.  All Rights Reserved.</p>         "
				+ "</body> </html>";
	}
	
	private void createMessageNode(Document messageXml) {

		//create message element
		Element message = messageXml.createElement("Message");

		//set protocol attribute
		Attr protocol = messageXml.createAttribute("protocol");
		message.setAttributeNode(protocol);

		//set type attribute
		Attr type = messageXml.createAttribute("type");
		message.setAttributeNode(type);

		//set sa attribute
		Attr sa = messageXml.createAttribute("sa");
		message.setAttributeNode(sa);

		//set time attribute
		Attr time = messageXml.createAttribute("time");
		message.setAttributeNode(time);

		//set communication value attribute
		Attr comm = messageXml.createAttribute("comm");
		message.setAttributeNode(comm);

		//set message body attribute
		Attr note = messageXml.createAttribute("note");
		message.setAttributeNode(note);
		
		Attr customername=messageXml.createAttribute("customername");
		message.setAttributeNode(customername);
		
		Attr optOutStatus=messageXml.createAttribute("optoutstatus");
		message.setAttributeNode(optOutStatus);
		
		Attr optOutStatusMessage=messageXml.createAttribute("optoutstatusmessage");
		message.setAttributeNode(optOutStatusMessage);

		messageXml.getElementsByTagName("Messages").item(0).appendChild(message);
	}

	private String checkForOptOut(Message msg) {
		// TODO Auto-generated method stub
		String optOutStatus=null;
		if(!MessageProtocol.EMAIL.getMessageProtocol().equalsIgnoreCase(msg.getProtocol())
				&&!MessageProtocol.TEXT.getMessageProtocol().equalsIgnoreCase(msg.getProtocol())){
			return null;
		}
		if(msg.getMessageMetaData()!=null && msg.getMessageMetaData().getMetaData()!=null){
			LOGGER.info("metadata "+msg.getMessageMetaData()+" mtdata "+ msg.getMessageMetaData().getMetaData());
			JSONParser parser = new JSONParser();
			JSONObject jsonObject=null;
			try {
				jsonObject = (JSONObject) parser.parse(msg.getMessageMetaData().getMetaData());
			} catch (ParseException e) {
				LOGGER.error(String.format("Error in parsing metadata for metadata=%s ", msg.getMessageMetaData().getMetaData()), e);
			}
			if(jsonObject!=null &&jsonObject.get("OPT_OUT_STATUS")!=null && jsonObject.get("OPT_OUT_STATUS").toString()!=null){
				optOutStatus=(String)jsonObject.get("OPT_OUT_STATUS");
				LOGGER.info("metadata "+msg.getMessageMetaData()+" mtdata "+ msg.getMessageMetaData().getMetaData()+"optoutstatus="+optOutStatus);
			}
			LOGGER.info("metadata "+msg.getMessageMetaData()+" mtdata "+ msg.getMessageMetaData().getMetaData());
		}

		if(OptoutStatus.OPTED_OUT.name().equalsIgnoreCase(optOutStatus) || OPTED_OUT_SUSPECTED.equalsIgnoreCase(optOutStatus) || OptoutStatus.OPTED_IN.name().equalsIgnoreCase(optOutStatus)){
			if(OPTED_OUT_SUSPECTED.equalsIgnoreCase(optOutStatus)) {
				return OptoutStatus.OPTED_OUT.name();
			}
			return optOutStatus;
		}
		return optOutStatus;
	}
	
	public NotificationAttributes getNotificationAttributesFromMetaData(Message message) {
		NotificationAttributes notificationAttributes = null;
		if (message != null && message.getMessageMetaData() != null && message.getMessageMetaData().getMetaData() != null) {
			HashMap<String, String> metaDataMap = new HashMap<String, String>();
			try {
				metaDataMap = getMessageMetaDatMap(message.getMessageMetaData().getMetaData());
				String notificationAttrsJson = metaDataMap.get(APIConstants.NOTIFICATION_ATTRIBUTES);
				if (notificationAttrsJson != null) {
					notificationAttributes = objectMapper.readValue(notificationAttrsJson, new TypeReference<NotificationAttributes>() {});
				}
			} catch (Exception e) {
				LOGGER.error("Error while deserializing notification attributes message_id={} message_uuid={} ",
						(message != null ? message.getId() : null), (message!= null ? message.getUuid() : null), e);
			}
		}
		return notificationAttributes;
	}
	
	public static String formatPhone(String Phone) {
		String formatted = null;
		
		if(null == Phone)
			return Phone;

		if (Phone.length() != 10 && Phone.length() != 11)
			return Phone;

		if (Phone.length() == 10)
			formatted = "(" + Phone.substring(0, 3) + ") "
				+ Phone.substring(3, 6) + "-" + Phone.substring(6, 10);
		else
			formatted = " (" + Phone.substring(1, 4) + ") "
				+ Phone.substring(4, 7) + "-" + Phone.substring(7, 11);

		return formatted;
	}

	public String getCustomerPreferredLocale(Long customerID) {
		String customerPreferredLocale = generalRepository.getPreferredLocaleForCustomerID(customerID);
		LOGGER.info(String.format("in getCustomerPreferredLocale for customer_id=%s locale=%s", customerID, customerPreferredLocale));
		if (customerPreferredLocale == null || customerPreferredLocale.isEmpty()) {
			customerPreferredLocale = LOCALE_ENUS;
		}
		return customerPreferredLocale;
	}

	public void sendResponseToCallBack(String callBackUrl, SaveMessageResponse saveMessageResponse) throws
		Exception {

		String errors = null;
		String warnings = null;
		Boolean isFailure = false;

		try {
			LOGGER.info("sending response to callback for source_uuid={} message_uuid={} errors={}", saveMessageResponse.getSourceUuid(),
				saveMessageResponse.getMessageUuid(), saveMessageResponse.getErrors() != null && !saveMessageResponse.getErrors().isEmpty() ? new ObjectMapper().writeValueAsString(saveMessageResponse.getErrors()) : "no errors");

//			if(saveMessageResponse.getErrors()!=null && !saveMessageResponse.getErrors().isEmpty()){
//				errors = objectMapper.writeValueAsString(saveMessageResponse.getErrors());
//				isFailure = true;
//			}
//			if(saveMessageResponse.getWarnings()!=null && !saveMessageResponse.getWarnings().isEmpty()){
//				warnings = objectMapper.writeValueAsString(saveMessageResponse.getWarnings());
//			}
//			generalRepo.insertIntoSaveResponse(saveMessageResponse.getSourceUuid(),
//					saveMessageResponse.getMessageUuid(), isFailure, errors, warnings);
			restTemplate.postForLocation(callBackUrl, saveMessageResponse);
		} catch (Exception e) {
			LOGGER.error("unable to call={} for saving request response", callBackUrl, e);
		}
	}

	public boolean isListEmpty(List<? extends Object> list) {

		return (list == null || list.isEmpty());
	}

	public void sendResponseToCallBack(String callBackPathUrl, SaveMessageListResponse saveMessageListResponse) {
		// TODO Auto-generated method stub

	}

	public Boolean checkIfDealerAssociateCanOwnThread(Message message, String dARoleName, String
		departmentUUID, String userUUID) {
		if (Role.CASHIER.getRoleName().equalsIgnoreCase(dARoleName) || Role.DRIVER.getRoleName().equalsIgnoreCase(dARoleName)) {
			LOGGER.info(String.format("Thread Subscriber Will not be updated to this user as user's role=%s is not"
				+ " appropriate for being owner of thread", dARoleName));
			return false;
		} else {
			Boolean doNotRouteIncomingMessageAuthority = KManageApiHelper.checkDealerAssociateAuthority(Authority.DO_NOT_ROUTE_INCOMING_MESSAGE.getAuthority(), userUUID, departmentUUID);
			if (doNotRouteIncomingMessageAuthority != null && doNotRouteIncomingMessageAuthority) {
				LOGGER.info(String.format("Thread Subscriber Will not be updated to this user=%s as user has do.not.route.incoming.message authority", userUUID));
				return false;
			}
		}

		return true;
	}

	public static Date zeroTime(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	public Long getRecentThreadIDForCustomer(Long customerID, Long dealerDepartmentID, Long dealerID, Date
		date, Long messageDealerAssociateID, String userUuid) {

		Long threadID = null;
		com.mykaarma.kcommunications.model.jpa.Thread thread = null;
		thread = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(customerID, dealerDepartmentID, false);

		if (thread == null) {
			Long threadDelegatee = subscriptionsApiImpl.getSubscribersForCustomer(customerID, dealerDepartmentID);
			if (threadDelegatee == null) {
				threadDelegatee = messageDealerAssociateID;
				SubscriberInfo subscriberInfo = new SubscriberInfo();
				subscriberInfo.setIsAsignee(true);
				subscriberInfo.setUserUuid(userUuid);

				SubscriptionSaveEventData saveEventData = new SubscriptionSaveEventData();
				saveEventData.setCustomerID(customerID);
				saveEventData.setDealerDepartmentID(dealerDepartmentID);
				saveEventData.setDealerID(dealerID);
				saveEventData.setEventName(EventName.EXTERNAL_SUBSRIPTION_ADDED.name());
				saveEventData.setEventRaisedBy(threadDelegatee);
				saveEventData.setIsAssignee(true);
				saveEventData.setIsHistoricalMessage(true);
				saveEventData.setSubscriberDAID(threadDelegatee);
				saveEventData.setCustomerUUID(generalRepository.getCustomerUUIDFromCustomerID(customerID));
				try {
					LOGGER.info("subscriber not found for customer_id={} hence adding dealer_Associate_id={} as thread owner", customerID, messageDealerAssociateID);
					subscriptionsApiImpl.populateSubscriptionSaveObjectAndCallMessagingViewController(EventName.EXTERNAL_SUBSRIPTION_ADDED, subscriberInfo, saveEventData, dealerDepartmentID);
				} catch (Exception e) {
					LOGGER.error("unable to save EXTERNAL_SUBSCRIPTION for customer_id={} dealer_associate_id={}", customerID, messageDealerAssociateID, e);
				}
			}
			thread = getNewThread(customerID, dealerDepartmentID, dealerID, date, threadDelegatee);
			try {
				thread = threadRepository.saveAndFlush(thread);
			} catch (Exception e) {

				if (e instanceof ObjectOptimisticLockingFailureException || e instanceof StaleObjectStateException) {
					try {
						LOGGER.info(String.format("ObjectOptimisticLockingFailureException / StaleObjectStateException occured in saving thread for "
							+ " customerId=%s , dealerId=%s", customerID, dealerID));
						com.mykaarma.kcommunications.model.jpa.Thread threadNew = threadRepository.findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(customerID,
							dealerDepartmentID, false);
						if (threadNew != null) {
							thread = threadRepository.saveAndFlush(thread);
						}
					} catch (Exception e2) {
						LOGGER.error("Error in creating thread for customerId={} , dealerId={}", customerID, dealerID, e);
					}
				} else {
					LOGGER.error("Error in creating thread for customerId={} , dealerId={}", customerID, dealerID, e);
				}

			}
			threadID = thread.getId();
		} else {
			threadID = thread.getId();
		}
		return threadID;
	}

	public Map<String, String> getDSOValuesFromKManage(Long dealerID, Set<String> dsoKeys) {
		try {
			String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
			return kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(dealerUUID, dsoKeys);
		} catch(Exception e) {
			LOGGER.error(String.format("Error in fetching dso_list=%s for dealer_id=%s ",
					dsoKeys, dealerID), e);
			return null;
		}
	}

	public User getUserForDealerAssociate(String dealerAssociateUuid, String departmentUuid) {
		DealerAssociateExtendedDTO dealerAssociateExtendedDTO = kManageApiHelper.getDealerAssociateForDealerAssociateUUID(departmentUuid, dealerAssociateUuid);

		if(dealerAssociateExtendedDTO != null) {
			User user = new User();
			user.setUuid(dealerAssociateExtendedDTO.getUserUuid());
			user.setName(getDealerAssociateName(dealerAssociateExtendedDTO));
			user.setType(EditorType.USER);
			user.setDepartmentUuid(dealerAssociateExtendedDTO.getDepartmentExtendedDTO().getUuid());

			return user;
		}

		return null;
	}

	public DealerAssociateExtendedDTO getInternalUsersFromGlobalUser(User user){
		
		DealerAssociateExtendedDTO dealerAssociateList = new DealerAssociateExtendedDTO();
		if(EditorType.USER.equals(user.getType())) {
			
			GetDealerAssociateResponseDTO getDealerAssociateResponseDTO = kManageApiHelper.getDealerAssociate(user.getDepartmentUuid(), user.getUuid());
			if(getDealerAssociateResponseDTO==null) {
				return null;
			}
			return getDealerAssociateResponseDTO.getDealerAssociate();
		}
		
		return null;
		
	}

	public static String toString(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			LOGGER.error("Error while parsing json to string", e);
			return null;
		}
	}

	public AuthorizationRequest getAuthorizationRequestForCreatingMessage(User requester, String departmentUuid, SendMessageRequest sendMessageRequest) {
		AuthorizationRequest authorizationRequest = new AuthorizationRequest();
		authorizationRequest.setOperation(Operation.CREATE);
		authorizationRequest.setRequesterType(RequesterType.USER);

		AuthorizeMessageSendRequest authorizeMessageSendRequest = new AuthorizeMessageSendRequest();
		authorizeMessageSendRequest.setMessageProtocol(sendMessageRequest.getMessageAttributes().getProtocol());
		authorizeMessageSendRequest.setDepartmentUuid(departmentUuid);

		if(MessageType.NOTE.getMessageType().equalsIgnoreCase(sendMessageRequest.getMessageAttributes().getType().getMessageType()) &&
				sendMessageRequest.getMessageAttributes().getIsManual()) {
			// TODO : Add Tagged Users not belonging to departmentUuid in authorizeMessageSendRequest
		}

		Resource<AuthorizeMessageSendRequest> resource = new Resource<>();
		resource.setType(ResourceType.MESSAGE);
		resource.setBody(authorizeMessageSendRequest);

		authorizationRequest.setResource(resource);

		return authorizationRequest;
	}
	
}

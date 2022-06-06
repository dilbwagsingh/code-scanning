package com.mykaarma.kcommunications.jpa.repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.cache.CacheConfig;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.mvc.DealerOrderSaveEventData;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.mvc.MessageSaveEventData;
import com.mykaarma.kcommunications.model.mvc.ThreadSaveEventData;
import com.mykaarma.kcommunications_model.common.VoiceCredentials;

@Component
@Repository
@Transactional(readOnly = true)
@SuppressWarnings("unchecked")
public class GeneralRepository {

	@Autowired
	MessageRepository messgRepo;

	@PersistenceContext
	EntityManager entityManager;


	public List<String> getBrokerNumbersAlreadyTriedForMessageID(Long messageID) {

		List<String> resultSet = entityManager.createNativeQuery("select brokernumberused from MessageFlaggedObjectionable where messageID = :messageID")
			.setParameter("messageID", messageID).getResultList();

		return resultSet;
	}
	
	public Boolean waitingForResponseStatusForCustomerAndDepartment(Long customerId,Long departmentId) {
		boolean isWaitingForResponse = false;
		try {
			Object isWaitingForResponseObj = entityManager.createNativeQuery("select IsWaitingForResponse from Thread where customerId= :customerId and DealerDepartmentId =:departmentId ;")
					.setParameter("customerId", customerId).setParameter("departmentId", departmentId).getSingleResult();
			
			if(isWaitingForResponseObj!=null) {
				isWaitingForResponse=(Boolean)isWaitingForResponseObj;
			}
					
		} catch(NoResultException nre) {
			
		} catch(Exception e) {
			throw e;
		}
		return isWaitingForResponse;
		
	}
	
	public Boolean authenticateSubscriberForScope(String userName, String dealerDepartmentUuid, String apiScope) {
		
		BigInteger serviceSubscriberResult= (BigInteger) entityManager.createNativeQuery("select ssd.ID from ServiceSubscriberDepartment ssd"
				+ "	join ServiceSubscriber ss on ss.ID=ssd.ServiceSubscriberID"
				+ " join ApiScope apsc on apsc.ID=ssd.ApiScopeID"
				+ " join DealerDepartment dd on dd.ID = ssd.DealerDepartmentID"
				+ " where ss.Username= :username and apsc.Scope = :scope"
				+ " and dd.UUID = :dealerDepartmentUuid "
				+ " and ssd.IsValid = 1 "
				+ " and ss.IsValid = 1 ").setParameter("username", userName)
				.setParameter("scope", apiScope).setParameter("dealerDepartmentUuid", dealerDepartmentUuid).getSingleResult();
		return !(serviceSubscriberResult == null);
		
	}
	
	public Boolean authenticateSubscriberForDealerLevelScope(String userName, String dealerUuid, String apiScope) {
		
		BigInteger serviceSubscriberResult= (BigInteger) entityManager.createNativeQuery("select ssd.ID from ServiceSubscriberDealer ssd"
				+ "	join ServiceSubscriber ss on ss.ID=ssd.ServiceSubscriberID"
				+ " join ApiScope apsc on apsc.ID=ssd.ApiScopeID"
				+ " join Dealer d on d.ID = ssd.DealerID"
				+ " where ss.Username= :username and apsc.Scope = :scope"
				+ " and d.UUID = :dealerUuid "
				+ " and ssd.IsValid = 1 "
				+ " and ss.IsValid = 1 ").setParameter("username", userName)
				.setParameter("scope", apiScope).setParameter("dealerUuid", dealerUuid).getSingleResult();
		return !(serviceSubscriberResult == null);
		
	}
	
	@Cacheable(value=CacheConfig.SERVICE_SUBSCRIBER_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public HashMap<String, String> findFirstServiceSubscriberByUserName(String userName) throws Exception{
		
		Object[] serviceSubscriberResult = (Object[]) entityManager.createNativeQuery("select Username, Password,IsValid,Name "
				+ " from ServiceSubscriber where username = :username").
				setParameter("username", userName).getSingleResult();

		HashMap<String, String> serviceSubscriberInfo = new HashMap<String, String>();
		if(serviceSubscriberResult!=null && serviceSubscriberResult[0]!=null){
			
			serviceSubscriberInfo.put("username", String.valueOf(serviceSubscriberResult[0]));
			serviceSubscriberInfo.put("password", String.valueOf(serviceSubscriberResult[1]));
			serviceSubscriberInfo.put("valid", String.valueOf(serviceSubscriberResult[2]));
			serviceSubscriberInfo.put("name", String.valueOf(serviceSubscriberResult[3]));
			return serviceSubscriberInfo;
		}else{		
			return null;
		}
	}
	
	public String getDealerContextForLoggingfromDealerDepartmentUUID(String dealerDepartmentUUID) {
		Object[] resultSet = (Object[]) entityManager.createNativeQuery("select ID as dealerDepartmentID, dealerID as dealerID "
				+ " from DealerDepartment dd "
				+ " where dd.UUID= :dealerDepartmentId").setParameter("dealerDepartmentId", dealerDepartmentUUID).getSingleResult();

		Object[] data = (Object[]) resultSet;
		BigInteger dealerDepartmentID = (BigInteger) data[0];
		String dealerDepartmentId = dealerDepartmentID.toString();
		BigInteger dealerID = (BigInteger) data[1];
		String dealerId = dealerID.toString();

		String result = dealerDepartmentId + "," + dealerId;
		return result;
	}

	public String getAutoReplyCallURLForCurrentOutOfOfficeForDealerAssociate(Long dealerAssociateID) {
		String result = (String) entityManager.createNativeQuery("select o.autoCallReplyUrl from OutOfOffice  as"
				+ " o where o.dealerAssociateID = :id and o.isValid = true and o.fromDate <= :currentDate ").
			setParameter("id", dealerAssociateID)
			.setParameter("currentDate", new Date())
			.getSingleResult();
		return result;
	}

	public String getUserUUIDForDealerAssociateID(Long dealerAssociateID) {
		String result = (String) entityManager.createNativeQuery("select userUUID from DealerAssociate where id= :dealerAssociateID ").
			setParameter("dealerAssociateID", dealerAssociateID).getSingleResult();
		return result;
	}

	public Boolean checkIfCommValueCanBeUsed(String communicationValue) {

		List<String> q = (List<String>) entityManager.createNativeQuery("select CommunicationValue from DoNotUseCommunicationValues where communicationValue= :communicationValue ")
			.setParameter("communicationValue", communicationValue).getResultList();

		if (q == null || (q != null && q.isEmpty())) {
			return true;
		} else {
			return false;
		}

	}

	public String getPreferredLocaleForCustomerID(Long customerID) {
		String result = (String) entityManager.createNativeQuery("select PreferredLocale from Customer where id= :customerID ").
			setParameter("customerID", customerID).getSingleResult();
		return result;
	}

	public String getEmailForDealership(Long dealerID) {
		String result = (String) entityManager.createNativeQuery("select emailID from Dealer where id= :dealerID ").
			setParameter("dealerID", dealerID).getSingleResult();
		return result;
	}
	
	@Cacheable(value=CacheConfig.CUSTOMER_ID_CUSTOMER_UUID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public Long getCustomerIDForUUID(String customerUUID) {
		
		BigInteger result = (BigInteger) entityManager.createNativeQuery("select ID from Customer where GUID= :customerUUID ").setParameter("customerUUID", customerUUID)
				.getSingleResult();

		return result.longValue();
	}
	
	public Long getDealerAssociateIDForUserUUID(String userUUID, Long departmentID) {
		
		BigInteger result = null;
		
		try {
			result = (BigInteger) entityManager.createNativeQuery("select ID from DealerAssociate where UserUUID= :userUUID and dealerDepartmentID= :departmentID").setParameter("userUUID", userUUID)
				.setParameter("departmentID", departmentID)
				.getSingleResult();
		}
		catch(NoResultException nre) {
			return null;
		}

		return result.longValue();
	}

	public String getDealerAssociateUuidForUserUuid(String userUuid, Long departmentId) {
		try {
			return (String) entityManager.createNativeQuery("select UUID from DealerAssociate where UserUUID= :userUuid and dealerDepartmentID= :departmentId")
				.setParameter("userUuid", userUuid).setParameter("departmentId", departmentId)
				.getSingleResult();
		}
		catch(NoResultException nre) {
			return null;
		}
	}
	
	public Long getDealerAssociateIDForUUID(String dealerAssociateUUID) {
		
		BigInteger result = null;
		
		try {
			result = (BigInteger) entityManager.createNativeQuery("select ID from DealerAssociate where UUID= :dealerAssociateUUID")
				.setParameter("dealerAssociateUUID", dealerAssociateUUID)
				.getSingleResult();
		}
		catch(NoResultException nre) {
			return null;
		}

		return result.longValue();
	}
	
	@Cacheable(value=CacheConfig.CUSTOMER_UUID_CUSTOMER_ID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	@Transactional(readOnly=true)
	public String getCustomerUUIDFromCustomerID(Long customerID) {
		String result = (String) entityManager.createNativeQuery("select GUID from Customer where ID = :customerID ").
				setParameter("customerID", customerID).getSingleResult();
		return result;

	}
	
	@Cacheable(value=CacheConfig.DEALER_ID_FOR_DEPARTMENT_UUID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	@Transactional(readOnly=true)
	public Long getDealerIDFromDepartmentUUID(String departmentUUID) {
		BigInteger result = (BigInteger) entityManager.createNativeQuery("select DealerID from DealerDepartment where UUID = :departmentUUID ").
				setParameter("departmentUUID", departmentUUID).getSingleResult();
		return result.longValue();

	}
	
	@Cacheable(value=CacheConfig.DEALER_ASSOCIATE_UUID_ID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	@Transactional(readOnly=true)
	public String getDealerAssociateUuidFromDealerAssociateId(Long dealerAssociateId) {
		String result = (String) entityManager.createNativeQuery("select UUID from DealerAssociate where ID = :dealerAssociateId ").
				setParameter("dealerAssociateId", dealerAssociateId).getSingleResult();
		return result;

	}

	@Cacheable(value=CacheConfig.DEPARTMENT_ID_FROM_DEPARTMENT_UUID_CACHE,keyGenerator = "customKeyGenerator", unless = "#result == null")
	@Transactional(readOnly = true)
	public Long getDepartmentIDForUUID(String departmentUUID) {
		BigInteger result = (BigInteger) entityManager.createNativeQuery("select ID from DealerDepartment where UUID= :departmentUUID ").setParameter("departmentUUID", departmentUUID)
				.getSingleResult();
		return result.longValue();
	}
	
	public String getAutomationFilterValueForAutomationFilter(Long dealerID, String automationFilterTypeKey) {
		List<String> resultSet = entityManager.createNativeQuery("select af.value from AutomationFilter af inner join AutomationFilterType aft on aft.id=af.AutomationFilterTypeid"
				+ " where dealerid= :dealerID and isenabled=1 and aft.key= :automationFilterTypeKey ")
				.setParameter("dealerID", dealerID).setParameter("automationFilterTypeKey", automationFilterTypeKey).getResultList();
		if(resultSet!=null && !resultSet.isEmpty()) {
			return resultSet.get(0);
		} else {
			return null;
		}
		 
	}

	public List<VoiceCredentials> getAllNumbersForDealer(Long dealerID) {

		List<VoiceCredentials> voiceCredentialsList = new ArrayList<VoiceCredentials>();
		List<Object[]> phoneNumberList = (List<Object[]>) entityManager.createNativeQuery("select dealersubaccount, brokernumber, deptid from VoiceCredentials where dealerid = :dealerID ;")
				.setParameter("dealerID", dealerID).getResultList();
		
		for(Object[] o : phoneNumberList) {
			
			VoiceCredentials voiceCredentials = new VoiceCredentials();
			String credentials = (String) o[0];
			String brokerNumber = (String) o[1];
			BigInteger dealerDepartmentID = (BigInteger) o[2];
			
			voiceCredentials.setDealerSubAccount(credentials);
			voiceCredentials.setBrokerNumber(brokerNumber);
			voiceCredentials.setDeptID(dealerDepartmentID.longValue());
			
			voiceCredentialsList.add(voiceCredentials);
		}
		
		return voiceCredentialsList;
	}
	
	@Transactional(readOnly=false)
	public void updateVoiceCall(Long messageID, String messageBody, String recordingURL) {

		entityManager.createNativeQuery("update VoiceCall vc "
						+ " join Message m on m.communicationUid=vc.callIdentifier "
						+ " Set vc.recordingURL = :recordingURL where m.id = :messageID")
							.setParameter("recordingURL", recordingURL)
							.setParameter("messageID", messageID)
							.executeUpdate();
		
	}


	@Transactional(readOnly = false)
	public void updateMessageExtn(Long messageID, String messageBody, String recordingURL) {

		entityManager.createNativeQuery("update MessageExtn  "
				+ " Set messageBody = :messageBody where messageid = :messageID")
			.setParameter("messageID", messageID)
			.setParameter("messageBody", messageBody)
			.executeUpdate();

	}

	private void updateMessageExtnForCallSid(String callSid, String messageBody, String recordingURL) {

		entityManager.createNativeQuery("update MessageExtn me join Message m on m.ID=me.MessageID "
				+ " Set me.messageBody = :messageBody where m.communicationUid = :callIdentifier")
			.setParameter("callIdentifier", callSid)
			.setParameter("messageBody", messageBody)
			.executeUpdate();
	}

	@Transactional(readOnly = false)
	public void updateCallInfo(String callSid, String recordingUrl, int recordingDuration, String transcription) {

		//updating MessageExtn
		String messagebody = "<recording url=\"" + recordingUrl
			+ "\" transcription=\"" + transcription + "\" duration=\""
			+ recordingDuration + "\"/>";


		updateMessageExtnForCallSid(callSid, messagebody, recordingUrl);
		//updating VoiceCall
		updateVoiceCallForCallSid(callSid, messagebody, recordingUrl, transcription, recordingDuration);
	}

	private void updateVoiceCallForCallSid(String callSid, String messagebody, String recordingURL, String transcription, int recordingDuration) {

		entityManager.createNativeQuery("update VoiceCall v "
				+ "  set v.recordingUrl = :url,  v.transcribedText = :text,    v.duration = :recordingDuration "
				+ "  where v.callIdentifier = :callIdentifier")
			.setParameter("url", recordingURL)
			.setParameter("text", transcription)
			.setParameter("callIdentifier", callSid)
			.setParameter("recordingDuration", recordingDuration)
			.executeUpdate();

	}

	public String fetchNewBrokerNumberFromPool(List<String> listOfPhoneNumbers) {

		String brokerNumber = null;

		List<String> temp = null;

		if (listOfPhoneNumbers != null && listOfPhoneNumbers.size() > 0) {

			temp = (List<String>) entityManager.createNativeQuery("select brokerNumber from ForwardingBrokerNumberPool bn where bn.isActive = true "
					+ "and bn.brokerNumber not in :listOfToNumbers")
				.setParameter("listOfToNumbers", listOfPhoneNumbers)
				.getResultList();
		} else {

			temp = (List<String>) entityManager.createNativeQuery("select brokerNumber from ForwardingBrokerNumberPool bn where bn.isActive = true ")
				.getResultList();
		}

		if (temp != null && temp.size() > 0) {
			brokerNumber = temp.get(0);
		}
		return brokerNumber;
	}


	public String getRecordingUrlFromTempTable(Long messageID) {

		List<String> resultSet = entityManager.createNativeQuery("select twilioURL from RecordingTemp where messageID = :messageID ;")
			.setParameter("messageID", messageID).getResultList();
		if (resultSet != null && !resultSet.isEmpty()) {
			return resultSet.get(0);
		} else {
			return null;
		}	
	}

	@Transactional(readOnly=false)
	public void insertInRecordingTemp(Long messageID, String recordingUrl, Long dealerID) {
		
		entityManager.createNativeQuery("insert into RecordingTemp (messageID, twilioURL, dealerID) values(:messageID, :recordingUrl, :dealerID)")
					.setParameter("messageID", messageID)
					.setParameter("recordingUrl", recordingUrl).
					 setParameter("dealerID", dealerID).executeUpdate();
		
	}
	
	@Transactional(readOnly=false)
	public void updateBulkMessageResponse(String requestUUID, String customerUUID, String messageUUID, 
			String failureReason, String dealerDepartmentUUID, String status) {
		
		entityManager.createNativeQuery("insert into BulkMessageResponse(requestUUID, dealerDepartmentUUID, customerUUID, messageUUID, failureReason, status)  "
				+ " values ( :requestUUID, :dealerDepartmentUUID, :customerUUID, :messageUUID, :failureReason, :status) "
				+ " on duplicate key update status= :status, failureReason= :failureReason ")
		.setParameter("requestUUID", requestUUID)
		.setParameter("customerUUID", customerUUID)
		.setParameter("messageUUID", messageUUID)
		.setParameter("failureReason", failureReason)
		.setParameter("dealerDepartmentUUID", dealerDepartmentUUID)
		.setParameter("status", status).
		executeUpdate();
		
	}
	
	@Transactional(readOnly=false)
	public void updateRecordingTemp(Long messageID, String recordingUrl) {
		
		entityManager.createNativeQuery("update RecordingTemp set awsURL= :recordingUrl where messageID= :messageID")
					.setParameter("messageID", messageID)
					.setParameter("recordingUrl", recordingUrl)
					.executeUpdate();
		
	}
	
	public Object[] getRecordingTempAttributesForAMessage(Long messageID) {
		
		List<Object[]> resultSet = entityManager.createNativeQuery("select * from RecordingTemp where messageID = :messageID ;")
					.setParameter("messageID", messageID).getResultList();
		if(resultSet!=null && !resultSet.isEmpty()) {
			return resultSet.get(0);
		} else {
			return null;
		}	
	}
	
	public String getCustomerNameFromId(Long customerID) {
		
		String customerName = (String) entityManager.createNativeQuery("select CONCAT(COALESCE(firstName,' '), ' ', COALESCE(lastName, ' ')) from Customer where id = :customerID ;").
			setParameter("customerID", customerID).getSingleResult();
		return customerName;
	}

	public String getDepartmentNameFromId(Long departmentID) {

		return (String) entityManager.createNativeQuery("select Name from DealerDepartment where id = :departmentID ;").
				setParameter("departmentID", departmentID).getSingleResult();
	}

	public String getDealerNameFromId(Long dealerID) {

		return (String) entityManager.createNativeQuery("select Name from Dealer where id = :dealerID ;").
				setParameter("dealerID", dealerID).getSingleResult();
	}

	
	public String getDealerAssociateName(Long dealerAssociateID) {
		
		String dealerAssociaterName = (String) entityManager.createNativeQuery("select CONCAT(COALESCE(Fname,' '), ' ', COALESCE(Lname, ' ')) from DealerAssociate where id = :dealerAssociateID ;").
			setParameter("dealerAssociateID", dealerAssociateID).getSingleResult();
		return dealerAssociaterName;
	}
	
	public Object[] getCustomerIDandNameFromUUID(String customerUUID) {
		
		List<Object[]> resultSet = entityManager.createNativeQuery("select ID, CONCAT(COALESCE(firstName,' '), ' ', COALESCE(lastName, ' ')) from Customer where GUID= :customerUUID ;")
					.setParameter("customerUUID", customerUUID).getResultList();
		if(resultSet!=null && !resultSet.isEmpty()) {
			return resultSet.get(0);
		} else {
			return null;
		}	
	}


	public List<BigInteger> getVoiceCallsForGivenTimeFrame(Date fromDate, Date toDate) {
		
		List<BigInteger> resultSet = entityManager.createNativeQuery("select m.id from Message m " 
								+ " join DealerAssociateAuthority daa on daa.DealerAssociateID=m.DealerAssociateID "
								+ " where m.ReceivedOn >= :fromDate AND m.ReceivedOn <= :toDate"
								+ " and daa.AuthorityID=(select ID from Authority where Authority='voice.record')"
								+ " AND m.DealerID in (select DealerID from DealerSetupOption where OptionKey='messaging.call.record.enable' and OptionValue='true') "
								+ "	and m.Protocol='V'")
								.setParameter("fromDate", fromDate)
								.setParameter("toDate", toDate)
								.getResultList();
		
		return resultSet;
	}
	
	public List<BigInteger> getVoiceCallsForGivenTimeFrameForGivenDealerIDs(Date fromDate, Date toDate, List<Long> dealerIds) {
		
		List<BigInteger> resultSet = entityManager.createNativeQuery("select m.id from Message m " 
								+ " join DealerAssociateAuthority daa on daa.DealerAssociateID=m.DealerAssociateID "
								+ " where m.ReceivedOn >= :fromDate AND m.ReceivedOn <= :toDate"
								+ " and daa.AuthorityID=(select ID from Authority where Authority='voice.record')"
								+ " and m.DealerID in (select DealerID from DealerSetupOption where OptionKey='messaging.call.record.enable' and OptionValue='true' and dealerID in (:dealerIds)) "
								+ "	and m.Protocol='V'")
								.setParameter("fromDate", fromDate)
								.setParameter("toDate", toDate)
								.setParameter("dealerIds", dealerIds)
								.getResultList();
		
		return resultSet;
	}


	public Object[] getDataForCall(Long messageID) {
		
		List<Object[]> resultSet = entityManager.createNativeQuery("select vc.recordingUrl, vc.Duration, vc.callStatus, vc.callIdentifier, m.dealerId "
												+ " from Message m inner join VoiceCall vc on vc.callIdentifier = m.communicationUid where m.id = :messageID")
				.setParameter("messageID", messageID).getResultList();
		if(resultSet!=null && !resultSet.isEmpty()) {
			return resultSet.get(0);
		} else {
			return null;
		}
	}

	public Object[] getDataForCall(String callIdentifier) {
		
		List<Object[]> resultSet = entityManager.createNativeQuery("select vc.recordingUrl, vc.Duration, vc.callStatus, vc.callIdentifier "
												+ " from VoiceCall vc where vc.callIdentifier = :callIdentifier")
				.setParameter("callIdentifier", callIdentifier).getResultList();
		if(resultSet!=null && !resultSet.isEmpty()) {
			return resultSet.get(0);
		} else {
			return null;
		}
	}
	public Boolean authenticateSubscriberForScope(String serviceSubscriberName, String apiScope) {
		
		BigInteger serviceSubscriberResult= (BigInteger) entityManager.createNativeQuery("select ssas.ID from ServiceSubscriberApiScope ssas"
				+ "	join ServiceSubscriber ss on ss.ID=ssas.ServiceSubscriberID"
				+ " join ApiScope apsc on apsc.ID=ssas.ApiScopeID"
				+ " where ss.Name= :serviceSubscriberName and apsc.Scope = :scope"
				+ " and ssas.IsValid = 1 "
				+ " and ss.IsValid = 1 ").setParameter("serviceSubscriberName", serviceSubscriberName)
				.setParameter("scope", apiScope).getSingleResult();
		return !(serviceSubscriberResult == null);
	}
	
	@Cacheable(value=CacheConfig.DEALER_ID_DEALER_UUID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public Long getDealerIdFromDealerUUID(String dealerUUID) {
		
		BigInteger dealerID = null;
		try {
			dealerID = (BigInteger) entityManager.createNativeQuery("select id from Dealer where uuid = :dealerUUID ;").
				setParameter("dealerUUID", dealerUUID).getSingleResult();
		}
		catch(Exception e) {
			throw e;
		}
		return dealerID.longValue();
	}

	@Cacheable(value=CacheConfig.DEALER_UUID_DEALER_ID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public String getDealerUUIDFromDealerId(Long dealerID) {
		
		String dealerUUID = null;
		try {
			dealerUUID = (String) entityManager.createNativeQuery("select uuid from Dealer where id = :dealerID ;").
				setParameter("dealerID", dealerID).getSingleResult();
		} catch (Exception e) {
			throw e;
		}
		return dealerUUID;
	}

	@Cacheable(value = CacheConfig.DEALER_ID_FOR_CUSTOMER_ID_CACHE, keyGenerator = "customKeyGenerator", unless="#result == null")
	public Long getDealerIdForCustomerId(Long customerId) throws Exception {
		BigInteger dealerId = null;
		try {
			dealerId = (BigInteger) entityManager.createNativeQuery("select DealerID from Customer where ID = :customerId ").
					setParameter("customerId", customerId).getSingleResult();
		} catch (Exception e) {
			throw e;
		}

		return dealerId.longValue();
	}

	public boolean checkIfDealerIsValid(Long dealerID) {

		boolean isValid = false;
		try {
			isValid = (Boolean) entityManager.createNativeQuery("select isValid from Dealer where id =:dealerID").
				setParameter("dealerID", dealerID).getSingleResult();

		} catch (Exception e) {
			throw e;
		}
		return isValid;
	}

	public List<Message> getMessagesForIds(Set<Long> messageIds) {

		List<Message> messages = new ArrayList<Message>();
		List<Object> messageList = entityManager.createNativeQuery("select * from Message m where m.id in  ( :messageIdList ) order by m.id desc ")
			.setParameter("messageIdList", messageIds)
			.getResultList();
		if (messageList != null) {
			for (Object messageObject : messageList) {
				Message m = new Message();
				m = (Message) messageObject;
				messages.add(m);
 			}
		}
		return messages;
	}


	@Cacheable(value=CacheConfig.DEPARTMENT_UUID_DEALER_ID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public String getDepartmentUUIDForDealerID(Long dealerID, String departmentType) {
		
		String departmentUUID = (String) entityManager.createNativeQuery("select UUID from DealerDepartment where DepartmentType = :departmentType and DealerID = :dealerID ;").
				setParameter("dealerID", dealerID).
				setParameter("departmentType", departmentType).
				getSingleResult();
		
		return departmentUUID;
	}

	@Cacheable(value=CacheConfig.DEPARTMENT_UUID_FROM_DEPARTMENT_ID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public String getDepartmentUUIDForDepartmentID(Long departmentID) {
		
		String departmentUUID = (String) entityManager.createNativeQuery("select UUID from DealerDepartment where ID = :departmentID ").
				setParameter("departmentID", departmentID).
				getSingleResult();
		
		return departmentUUID;
	}
	
	public Object[] getDealerNameAndEmailFromDealerID(Long dealerId) {
		
		List<Object[]> resultSet = entityManager.createNativeQuery("select name, emailid from Dealer where id = :dealerId ")
			.setParameter("dealerId", dealerId).getResultList();
		if(resultSet!=null && !resultSet.isEmpty()) {
			return resultSet.get(0);
		} else {
			
			return null;
		}		
	}


	public List<BigInteger> fetchCustomersForDealer(Long dealerId, Long offSet, Long batchSize) {
		
		List<BigInteger> resultSet = entityManager.createNativeQuery("select id from Customer c " 
				+ " where dealerId = :dealerId order by ID desc limit :batchSize offset :offSet ")
				.setParameter("dealerId", dealerId)
				.setParameter("batchSize", batchSize)
				.setParameter("offSet", offSet)
				.getResultList();

		return resultSet;
	}


	public List<BigInteger> getAllDepartments() {
		
		List<BigInteger> resultSet = entityManager.createNativeQuery("select id from DealerDepartment")
				.getResultList();

		return resultSet;
	}
	
	public Object[] getMessageDataForCommunicationUid(String communicationuid) {
		
		List<Object[]> messageList = entityManager.createNativeQuery("select m.id,  m.MessageSize from Message m where m.communicationuid = :communicationuid")
			.setParameter("communicationuid", communicationuid)
			.getResultList();
		if(messageList!=null && !messageList.isEmpty()) {
			return messageList.get(0);
		}
		return null;
	}

	public List<BigInteger> getAllDepartemntIDsForDealerId(Long dealerId) {
		
		
		List<BigInteger> resultSet = entityManager.createNativeQuery("select id from DealerDepartment " 
				+ " where dealerId = :dealerId ")
				.setParameter("dealerId", dealerId)
				.getResultList();

		return resultSet;
	}
	
	public Object[] getManualTempateForUuid(String templateUuid) {
		List<Object[]> messageList = entityManager.createNativeQuery("select id, title, Template, DealerID, DealerDepartmentId,"
				+ " Locale,Slug,UUID,SortOrder from TextMessageTemplate where UUID = :templateUuid ;")
				.setParameter("templateUuid", templateUuid)
				.getResultList();
			if(messageList!=null && !messageList.isEmpty()) {
				return messageList.get(0);
			}
		return null;
	}
	
	public List<Object[]> getManualTempateForDealerId(Long dealerId) {
		List<Object[]> messageList = entityManager.createNativeQuery("select id, title, Template, DealerID, DealerDepartmentId,"
				+ " Locale,Slug,UUID,SortOrder from TextMessageTemplate where dealerId = :dealerId ;")
				.setParameter("dealerId", dealerId)
				.getResultList();
		
		return messageList;
	}
	
	public List<Object[]> getManualTempateForDepartmentId(Long dealerDepartmentId) {
		List<Object[]> messageList = entityManager.createNativeQuery("select id, title, Template, DealerID, DealerDepartmentId,"
				+ " Locale,Slug,UUID,SortOrder from TextMessageTemplate where dealerDepartmentId = :dealerDepartmentId ;")
				.setParameter("dealerDepartmentId", dealerDepartmentId)
				.getResultList();
		
		return messageList;
	}
	
	public Long getManualTemplateIdFromUuid(String templateUuid) {
		BigInteger result = null;
		
		try {
			result = (BigInteger) entityManager.createNativeQuery("select ID from TextMessageTemplate where UUID= :templateUuid")
				.setParameter("templateUuid", templateUuid)
				.getSingleResult();
		}
		catch(NoResultException nre) {
			return null;
		}

		return result.longValue();
	}

    public List<Object[]> getCustomerCommunicationAttributesForUUIDs(List<String> customerUUIDs) {
        Query q = entityManager.createNativeQuery(
            "SELECT cc.CommunicationTypeID, cc.CommunicationValue"
            + " FROM Customer c JOIN CustomerCommunication cc ON c.ID = cc.CustomerID" 
            + " WHERE c.GUID in (:customerUUIDs) AND cc.IsValid = 1"
        ).setParameter("customerUUIDs", customerUUIDs);
        return q.getResultList();
    }

	public List<Object[]> getCustomerCommunicationAttributesForDealer(Long dealerID, List<String> communicationTypeIDs, Long minCustomerCommunicationID, Long maxEntriesToBeFetched) {
		Query q = entityManager.createNativeQuery(
            "SELECT cc.ID, cc.CustomerID, cc.CommunicationTypeID, cc.CommunicationValue, cc.CreatedBy FROM CustomerCommunication cc"
			+ " JOIN Customer c on cc.CustomerID = c.ID"
			+ " WHERE c.DealerID = :dealerID AND cc.ID >= :minCustomerCommunicationID AND cc.CommunicationTypeID IN (:communicationTypeIDs) AND cc.IsValid = 1"
			+ " ORDER BY cc.ID ASC"
			+ " LIMIT :maxEntriesToBeFetched"
		).setParameter("dealerID", dealerID)
		.setParameter("communicationTypeIDs", communicationTypeIDs)
		.setParameter("minCustomerCommunicationID", minCustomerCommunicationID)
		.setParameter("maxEntriesToBeFetched", maxEntriesToBeFetched);
        return q.getResultList();
	}

	@Cacheable(value = CacheConfig.ALL_DEPARTMENT_ID_NAME_FOR_DEALER_CACHE, keyGenerator = "customKeyGenerator", unless = "#result == null")
	public List<Object[]> getAllDepartmentIDAndNameForDealerId(Long dealerId) {


		List<Object[]> resultSet = entityManager.createNativeQuery("select id, name from DealerDepartment "
			+ " where dealerId = :dealerId ")
			.setParameter("dealerId", dealerId)
			.getResultList();

		return resultSet;
	}

	public Long getLatestMessageDepartmentIDForCustomerIDAndDepartmentIDListAndCommunicationValue(Long customerID,
	  	List<Long> departmentIDList, String communicationValue, List<String> messageTypeList, List<String> messageProtocolList) {
		List<BigInteger> resultSet = entityManager.createNativeQuery(
			"SELECT DealerDepartmentID from Message m"
			+ " WHERE CustomerID = :customerID AND FromNumber = :communicationValue"
			+ " AND MessageType IN (:messageTypeList) AND Protocol IN (:messageProtocolList)"
			+ " AND DealerDepartmentID IN (:departmentIDList)"
			+ " ORDER BY ID DESC LIMIT 1"
		).setParameter("customerID", customerID)
		.setParameter("communicationValue", communicationValue)
		.setParameter("departmentIDList", departmentIDList)
		.setParameter("messageTypeList", messageTypeList)
		.setParameter("messageProtocolList", messageProtocolList).getResultList();
		if(resultSet == null || resultSet.isEmpty()) {
			return null;
		}
		return resultSet.get(0).longValue();
	}

	public List<Object[]> filterMessagesForDepartments(List<Long> departmentIDList, List<String> messageUUIDList) {
		return entityManager.createNativeQuery("SELECT UUID, DealerDepartmentID FROM Message WHERE"
				+ " UUID IN (:messageUUIDList) AND DealerDepartmentID IN (:departmentIDList)")
			.setParameter("messageUUIDList", messageUUIDList)
			.setParameter("departmentIDList", departmentIDList)
			.getResultList();
	}

	public List<BigInteger> getAllDealers(Boolean onlyValid, Long minDealerID, Long maxDealerID) {
		String querySql = "SELECT ID FROM Dealer WHERE ID >= " + minDealerID + " AND ID <= " + maxDealerID;
		if (onlyValid) {
			querySql += " AND IsValid = 1";
		}
		return (List<BigInteger>) entityManager.createNativeQuery(querySql)
			.getResultList();
	}
	
	@Cacheable(value=CacheConfig.SUPPORTED_TRANSLATION_LANGUAGES_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public Map<String,String> getTranslateLanguages(){

		List<Object[]> resultList =  entityManager.createNativeQuery("select t.LanguageName, t.LanguageCode  from TranslateLanguages t ").getResultList();
		if(resultList!=null && !resultList.isEmpty()){
			Map <String,String> map = new HashMap<String,String>();
			for (Object row : resultList){
				Object[] data = (Object [])row;
				if(null!=data[0] && !data[0].toString().isEmpty() && null!=data[1] && !data[1].toString().isEmpty()){
					map.put(data[0].toString(), data[1].toString());
				}
			}
			return map;
		}
		return null;
	}

	@Cacheable(value=CacheConfig.DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_ID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public String getDepartmentUuidForDealerAssociateId(Long dealerAssociateID) {
		
		String dealerUUID = null;
		try {
			dealerUUID = (String) entityManager.createNativeQuery("select d.uuid from DealerDepartment d inner join DealerAssociate da on da.dealerdepartmentid ="
					+ " d.id where da.id = :dealerAssociateID ;").
				setParameter("dealerAssociateID", dealerAssociateID).getSingleResult();
		} catch (Exception e) {
			throw e;
		}
		return dealerUUID;
	}

	@Cacheable(value=CacheConfig.DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_UUID_CACHE, keyGenerator = "customKeyGenerator", unless="#result == null")
	public String getDepartmentUuidForDealerAssociateUuid(String dealerAssociateUuid) {

		String departmentUuid = null;
		try {
			departmentUuid = (String) entityManager.createNativeQuery("select dd.UUID from DealerDepartment dd inner join DealerAssociate da " +
				"on da.DealerDepartmentID = dd.ID where da.UUID = :dealerAssociateUuid ")
				.setParameter("dealerAssociateUuid", dealerAssociateUuid).getSingleResult();
		} catch (Exception e) {
			throw e;
		}
		return departmentUuid;
	}
	
	public List<MessageSaveEventData> getMessageSaveEventDataFromMessage(List<Long> messageIDs, Long eventDealerAssociateID) {
		List<MessageSaveEventData> result =  new ArrayList<MessageSaveEventData>();
		String queryString = "select m.ID, m.DealerID, m.DealerDepartmentID, m.DealerAssociateID, m.CustomerID,"
				+ " m.MessageType, m.Protocol, m.DeliveryStatus, m.MessagePurpose, t.ID as ThreadID , t.DealerAssociateID as ThreadDealerAssociateID,"
				+ " mext.MessageBody, m.ReceivedOn "
				+ " from Message m join MessageExtn mext on mext.MessageID = m.ID left outer join MessageThread mt on mt.MessageID =  m.ID "
				+ " left outer join Thread t on t.ID = mt.ThreadID where m.ID in (:messageIDs)";
		Query query =  entityManager.createNativeQuery(queryString);
		query.setParameter("messageIDs", messageIDs);
		List<Object[]> list = query.getResultList();
		for (Object[] obj : list) {
			Long messageID = ((BigInteger)obj[0]).longValue();
			Long dealerID = ((BigInteger)obj[1]).longValue();
			Long dealerDepartmentID = ((BigInteger)obj[2]).longValue();
			Long dealerAssociateID =  ((BigInteger)obj[3]).longValue();
			Long customerID = ((BigInteger)obj[4]).longValue();
			String messageType = (String)obj[5];
			String protocol = (String)obj[6];
			String deliveryStatus = (String)obj[7];
			String purpose = (String)obj[8];
			Long threadID = (obj[9] != null?((BigInteger)obj[9]).longValue():null);
			Long threadDAID = (obj[10] != null ? ((BigInteger)obj[10]).longValue():null );
			String messageBody = (String)obj[11];
			Date receivedOn = (Date)obj[12];
			
			MessageSaveEventData messageSaveEventData = new MessageSaveEventData();
			messageSaveEventData.setCustomerID(customerID);
			messageSaveEventData.setDealerDepartmentID(dealerDepartmentID);
			messageSaveEventData.setDealerID(dealerID);
			messageSaveEventData.setEventName(EventName.MESSAGE_CUSTOMER_UPDATE.name());
			messageSaveEventData.setEventRaisedBy(eventDealerAssociateID);
			messageSaveEventData.setMessageBody(messageBody);
			messageSaveEventData.setMessageDate(receivedOn);
			messageSaveEventData.setMessageDealerAssociateID(dealerAssociateID);
			messageSaveEventData.setMessageID(messageID);
			messageSaveEventData.setMessagePurpose(purpose);
			messageSaveEventData.setMessageType(messageType);
			messageSaveEventData.setProtocol(protocol);
			messageSaveEventData.setStatus("1".equalsIgnoreCase(deliveryStatus));
			messageSaveEventData.setThreadDealerAssociateID(threadDAID);
			messageSaveEventData.setThreadID(threadID);
			result.add(messageSaveEventData);	
		}
		return result;
	}
	
	public List<ThreadSaveEventData> getThreadSaveEventDataFromThread(List<Long> threadIDs, Long eventDealerAssociateID, String eventName) {
		List<ThreadSaveEventData> result =  new ArrayList<ThreadSaveEventData>();
		String queryString = "select t.ID , t.LastDelegationOn, t.DealerAssociateID, t.LastMessageOn, da.FName, da.LName, t.CustomerID, t.DealerDepartmentID, t.DealerID "
				+ "	from Thread t join DealerAssociate da on da.ID = t.DealerAssociateID where t.ID in  (:threadIDs) ";
		Query query =  entityManager.createNativeQuery(queryString);
		query.setParameter("threadIDs", threadIDs);
		List<Object[]> list = query.getResultList();
		for (Object[] obj : list) {
			Long threadID = ((BigInteger)obj[0]).longValue();
			BigInteger lastDelegationOnMillis = (BigInteger)obj[1];
			Date lastDelegationOn =  null;
			if(lastDelegationOnMillis !=null)
				lastDelegationOn = new Date(lastDelegationOnMillis.longValue());
			Long dealerAssociateID =  ((BigInteger)obj[2]).longValue();
			Date lastMessageOn = (Date)obj[3];
			String daFName = (String)obj[4];
			String daLName = (String)obj[5];
			Long customerID =  ((BigInteger)obj[6]).longValue();
			Long dealerDepartmentID =  ((BigInteger)obj[7]).longValue();
			Long dealerID = ((BigInteger)obj[8]).longValue();
			Date updatedDate = findThreadUpdateDate(lastDelegationOn, lastMessageOn);
			
			ThreadSaveEventData threadSaveEventData = new ThreadSaveEventData();
			threadSaveEventData.setAssigneeName(getDealerAssociateName(daFName, daLName));
			threadSaveEventData.setCurrentThreadOwnerDAID(dealerAssociateID);
			threadSaveEventData.setCustomerID(customerID);
			threadSaveEventData.setDealerDepartmentID(dealerDepartmentID);
			threadSaveEventData.setDealerID(dealerID);
			threadSaveEventData.setEventName(eventName);
			threadSaveEventData.setEventRaisedBy(eventDealerAssociateID);
			if (eventName.equalsIgnoreCase(EventName.THREAD_DELEGATED.name())) 
				threadSaveEventData.setIsThreadDelegated(true);
			else
				threadSaveEventData.setIsThreadDelegated(false);
			threadSaveEventData.setLastMessageOn(lastMessageOn);
			threadSaveEventData.setThreadID(threadID);
			threadSaveEventData.setThreadUpdatedDate(updatedDate);
			threadSaveEventData.setStatus(getThreadDeliveryStatusForCustomer(customerID, dealerDepartmentID));
			threadSaveEventData.setPreviousThreadOwnerDAID(getThreadOwnerDAID(threadID));
			
			result.add(threadSaveEventData);
		}
		return result;
	}
	
	public List<DealerOrderSaveEventData> getDealerOrderSaveEventDataFromThread(List<Long> dealerOrderIDs, Long eventDealerAssociateID) {
		List<DealerOrderSaveEventData> result =  new ArrayList<DealerOrderSaveEventData>();
		String queryString = "select dor.ID, dor.DealerAssociateID, dor.DMSID, dor.OrderStatus, dor.OrderNumber, dor.OrderType, "
				+ " dor.OrderDate, dor.IsPaid, dor.IsPaidInKaarma, dor.IsPaymentRequestSent, dor.Amount, dor.PaidAmount, dor.InvoiceUrl, "
				+ " dor.Created_dt, dor.Updated_dt, dor.CustomerID , da.DealerDepartmentID, dor.DealerID "
				+ " from DealerOrder dor join DealerAssociate da on da.ID =  dor.DealerAssociateID "
				+ " where dor.ID in  (:dealerOrderIDs) ";
		Query query =  entityManager.createNativeQuery(queryString);
		query.setParameter("dealerOrderIDs", dealerOrderIDs);
		List<Object[]> list = query.getResultList();
		for (Object[] obj : list) {
			Long dealerOrderID = ((BigInteger)obj[0]).longValue();
			Long dealerAssociateID =  ((BigInteger)obj[1]).longValue();
			Long dmsID =  ((BigInteger)obj[2])!=null?((BigInteger)obj[2]).longValue():null;
			String orderStatus = (String)obj[3];
			String orderNumber =(String)obj[4];
			String orderType =(String)obj[5];
			Date orderDate = (Date)obj[6];
			Boolean isPaid  =(Boolean)obj[7];
			Boolean isPaidInKaarma  =(Boolean)obj[8];
			Boolean isPaymentRequestSent  =(Boolean)obj[9];
			BigDecimal orderAmount = (BigDecimal)obj[10];
			BigDecimal paidAmount = (BigDecimal)obj[11];
			String invoiceUrl = (String)obj[12];
			Date createdDate =(Date)obj[13];
			Date updatedDate = (Date)obj[14];
			Long customerID =  ((BigInteger)obj[15]).longValue();
			Long dealerDepartmentID = ((BigInteger)obj[16]).longValue();
			Long dealerID = ((BigInteger)obj[17]).longValue();
			
			DealerOrderSaveEventData dealerOrderSaveEventData = new DealerOrderSaveEventData();
			dealerOrderSaveEventData.setCreatedDate(createdDate);
			dealerOrderSaveEventData.setCustomerID(customerID);
			dealerOrderSaveEventData.setDealerAssociateID(dealerAssociateID);
			dealerOrderSaveEventData.setDealerDepartmentID(dealerDepartmentID);
			dealerOrderSaveEventData.setDealerID(dealerID);
			dealerOrderSaveEventData.setDealerOrderID(dealerOrderID);
			dealerOrderSaveEventData.setDmsID(dmsID);
			dealerOrderSaveEventData.setEventName(EventName.RO_CUSTOMER_UPDATE.name());
			dealerOrderSaveEventData.setEventRaisedBy(eventDealerAssociateID);
			dealerOrderSaveEventData.setInvoiceUrl(invoiceUrl);
			dealerOrderSaveEventData.setIsPaid(isPaid);
			dealerOrderSaveEventData.setIsPaidInKaarma(isPaidInKaarma);
			dealerOrderSaveEventData.setIsPaymentRequestSent(isPaymentRequestSent);
			dealerOrderSaveEventData.setOrderAmount(orderAmount);
			dealerOrderSaveEventData.setOrderDate(orderDate);
			dealerOrderSaveEventData.setOrderNumber(orderNumber);
			dealerOrderSaveEventData.setOrderStatus(orderStatus);
			dealerOrderSaveEventData.setOrderType(orderType);
			dealerOrderSaveEventData.setPaidAmount(paidAmount);
			dealerOrderSaveEventData.setUpdatedDate(updatedDate);
			result.add(dealerOrderSaveEventData);
		}
		return result;
	}
	
	private Boolean getThreadDeliveryStatusForCustomer(Long customerID, Long dealerDepartmentID) {
		Query query = entityManager.createNativeQuery("select ID from Message where CustomerID= :customerID"
				+ " and DealerDepartmentID = :dealerDepartmentID and DeliveryStatus = '0' limit 1 ");
		query.setParameter("customerID", customerID);
		query.setParameter("dealerDepartmentID", dealerDepartmentID);
		List<BigInteger> list = query.getResultList();
		if (list != null && list.size() > 0)
			return false;
		else
			return true;
	}
	
	public Long getThreadOwnerDAID(Long threadID) {
		Query query = entityManager.createNativeQuery(" select DealerAssociateID from Thread where ID = :threadID ");
		query.setParameter("threadID", threadID);
		List<BigInteger> list = query.getResultList();
		if (list != null && !list.isEmpty() && list.get(0) != null)
			return list.get(0).longValue();
		else
			return null;
	}
	
	public List<Long> getThreadIDsForCustomer(Long customerID){
		List<BigInteger> threadIDList = entityManager.createNativeQuery("select ID from Thread where Closed = 0 and Archived = 0 "
					+ " and CustomerID = :customerID ")
				.setParameter("customerID", customerID).getResultList();
		return convertBigIntegerListToLong(threadIDList);
	}
	
	public HashMap<Long, Long> getDepartmentThreadMapForCustomer(Long customerID){
		String query = "select ID, DealerDepartmentID from Thread where CustomerID = :customerID and Closed = 0 and Archived = 0 ";
		List<Object[]> threads =  entityManager.createNativeQuery(query).setParameter("customerID", customerID).getResultList();
		HashMap<Long, Long> departmentThreadMap = new HashMap<Long,Long>();
		for (Object[] obj : threads) {
			departmentThreadMap.put(((BigInteger)obj[1]).longValue(), ((BigInteger)obj[0]).longValue());
		}
		return departmentThreadMap;
	}
	
	public HashMap<Long, Long> getThreadDepartmentMap(List<Long> threadIDs){
		Query query = entityManager.createNativeQuery("select ID, DealerDepartmentID from Thread where ID in (:threadIDs) ");
		query.setParameter("threadIDs", threadIDs);
		List<Object[]> list = query.getResultList();
		HashMap<Long, Long> result = new HashMap<Long,Long>();
		for (Object[] obj : list) {
			result.put(((BigInteger)obj[0]).longValue(), ((BigInteger)obj[1]).longValue());
		}
		return result;
	}
	
	public String getLastCustomerMessageType( Long threadID) {
		Query query =  entityManager.createNativeQuery(" select m.MessageType from Message m join MessageThread mt on mt.MessageID = m.ID "
				+ " join Thread t on t.ID  = mt.ThreadID  where t.ID = :threadID and m.Protocol in ('X','V','E') order by m.ReceivedOn desc Limit 1 ");
		query.setParameter("threadID", threadID);
		List<String> list = query.getResultList();
		if(list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}
	
	public String getLastCustomerMessageTypeForDepartment( Long dealerDepartmentID, Long customerID) {
		Query query =  entityManager.createNativeQuery(" select m.MessageType from Message m where m.DealerDepartmentID = :departmentID "
				+ " and m.CustomerID = :customerID and m.Protocol in ('X','V','E') order by m.ReceivedOn desc Limit 1 ");
		query.setParameter("departmentID", dealerDepartmentID);
		query.setParameter("customerID", customerID);
		List<String> list = query.getResultList();
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}
	
	public HashMap<Long,Long> getDealerAssociateDepartmentMap(List<Long> dealerAssociateIDList) {
		Query query = entityManager.createNativeQuery("select ID, DealerDepartmentID from DealerAssociate where ID in  (:dealerAssociateIDList) ");
		query.setParameter("dealerAssociateIDList", dealerAssociateIDList);
		List<Object[]> list= query.getResultList();
		HashMap<Long, Long> result = new HashMap<Long,Long>();
		for(Object[] obj: list) {
			result.put(((BigInteger)obj[0]).longValue(), ((BigInteger)obj[1]).longValue());
		}
		return result;
	}
	
	public String getOrderNumberForID(Long dealerOrderID) {
		Query query = entityManager.createNativeQuery("select OrderNumber from DealerOrder where ID = :id ");
		query.setParameter("id", dealerOrderID);
		return (String) query.getSingleResult();
	}
	
	public HashMap<Long,List<Long>> getDepartmentDealerOrderMapForCustomer(Long customerID){
		Query query = entityManager.createNativeQuery("select da.DealerDepartmentID, dor.ID from DealerOrder  dor join DealerAssociate da "
				+ " on dor.DealerAssociateID = da.ID where dor.CustomerID = :customerID and IsArchive=0 order by dor.ID desc ");
		query.setParameter("customerID", customerID);
		List<Object[]> result =  query.getResultList();
		HashMap<Long, List<Long>> map = new HashMap<>();
		for (Object[] obj : result) {
			Long dealerDepartmentID = ((BigInteger)obj[0]).longValue();
			Long dealerOrderID = ((BigInteger)obj[1]).longValue();
			List<Long> dorIDList = map.get(dealerDepartmentID);
			if (dorIDList == null)
				dorIDList = new ArrayList<>();
			dorIDList.add(dealerOrderID);
			map.put(dealerDepartmentID, dorIDList);
		}
		return map;
	}
	
	public List<Long> convertBigIntegerListToLong(List<BigInteger> list) {
		List<Long> result = new ArrayList<Long>();
		if (list != null && !list.isEmpty()) {
			for (BigInteger item : list) {
				result.add(item.longValue());
			}
		}
		return result;
	}
	
	private Date findThreadUpdateDate(Date lastDelegationOn, Date lastMessageOn) {
		if (lastDelegationOn == null && lastMessageOn != null)
			return lastMessageOn;
		if (lastMessageOn == null && lastDelegationOn != null)
			return lastDelegationOn;
		if (lastDelegationOn != null && lastMessageOn != null) {
			if (lastDelegationOn.compareTo(lastMessageOn) > 0) {
				return lastDelegationOn;
			} else {
				return lastMessageOn;
			}
		}
		return null;
	}
	
	public String getDealerAssociateName(String daFName, String daLName) {
		return (daFName != null ? daFName : "") + " " + (daLName != null ? daLName : "");
	}
}

package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.mongo.CustomerMergeLog;
import com.mykaarma.kcommunications.model.mongo.CustomerMergeLog.MergeProgress;
import com.mykaarma.kcommunications.model.mvc.DealerOrderSaveEventData;
import com.mykaarma.kcommunications.model.mvc.EventName;
import com.mykaarma.kcommunications.model.mvc.ExtendedDealerOrderSaveEventData;
import com.mykaarma.kcommunications.model.mvc.ExtendedThreadSaveEventData;
import com.mykaarma.kcommunications.model.mvc.MessageSaveEventData;
import com.mykaarma.kcommunications.model.mvc.SubscriptionSaveEventData;
import com.mykaarma.kcommunications.model.mvc.ThreadSaveEventData;
import com.mykaarma.kcommunications.model.rabbit.PostMariaDBMergeRequest;
import com.mykaarma.kcommunications.redis.CustomerLockRedisService;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kcommunications_model.common.Subscriber;
import com.mykaarma.kcommunications_model.mvc.MVCConstants;
import com.mykaarma.kcommunications_model.mvc.SubscriptionList;
import com.mykaarma.kcommunications_model.request.SubscriptionRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MergeCustomerService {
	
	public static final String PROCESS_TYPE = "PROCESS";
	public static final String PROCESS_TYPE_CUSTOMER_MERGE = "CUSTOMER_MERGE";
	public static final String DB_TYPE = "DB_TYPE";
	public static final String DB_TYPE_MONGO ="MONGO";
	public static final String PRIMARY_CUSTOMER_ID = "PRIMARY_CUSTOMER_ID";
	public static final String CUSTOMER_MERGE_LOG_COLLECTION = "CUSTOMER_MERGE_LOG_COMMUNICATIONS";
	public static final String TABLE_NAME = "TABLE_NAME";
	public static final String MESSAGE_TABLE = "Message";
	public static final String THREAD_TABLE = "Thread";
	public static final String DEALER_ORDER_TABLE = "DealerOrder";
	public static final String THREAD_COLLECTIONS = "THREAD_COLLECTIONS";
	public static final String DEALERORDER_COLLECTIONS = "DEALERORDER_COLLECTIONS";
	public static final String ARCHIVE_COLLECTION = "ARCHIVE_COLLECTION";
	public static final String DELEGATION_COLLECTION = "DELEGATION_COLLECTION";
	public static final String UNASSIGNED_COLLECTION = "UNASSIGNED_COLLECTION";
	public static final String READY_FOR_FOLLOW_UP_COLLECTION ="READY_FOR_FOLLOW_UP_COLLECTION";
	public static final String FOLLOWED_UP_WITH_COLLECTION ="FOLLOWED_UP_WITH_COLLECTION";
	public static final String UNRESPONDED_COLECTION = "UNRESPONDED_COLLECTION";
	public static final String DELEGATION_HISTORY = "DelegationHistory";
	
	@Autowired
	private CustomerLockRedisService customerLockRedisService;
	
	@Autowired
	private MongoService mongoService;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	@Autowired
	private MessagingViewControllerHelper messagingViewControllerHelper;
	
	public void handlePostMergeRequest(String jsonObject) throws Exception {
		MDC.put(MergeCustomerService.PROCESS_TYPE, MergeCustomerService.PROCESS_TYPE_CUSTOMER_MERGE);
		MDC.put(MergeCustomerService.DB_TYPE, MergeCustomerService.DB_TYPE_MONGO);
		ObjectMapper mapper = new ObjectMapper();
		PostMariaDBMergeRequest postMariaDBMergeRequest = mapper.readValue(jsonObject, PostMariaDBMergeRequest.class);
		MDC.put(MergeCustomerService.PRIMARY_CUSTOMER_ID, postMariaDBMergeRequest.getPrimaryCustomerID().toString());
		log.info("Mongo customer merge process starting");
		Lock lock = customerLockRedisService.obtainLockForCustomerMongoMerge(postMariaDBMergeRequest.getPrimaryCustomerGUID());
		if (lock.tryLock()) {
			CustomerMergeLog customerMergeLog = createUpdateMergeLog(postMariaDBMergeRequest.getPrimaryCustomerID(),
					postMariaDBMergeRequest.getPrimaryCustomerGUID(), true, postMariaDBMergeRequest.getMergedCustomerIDs());
			
			if (postMariaDBMergeRequest.getMapUpdatedRecords() != null && !postMariaDBMergeRequest.getMapUpdatedRecords().isEmpty()) {
				HashMap<Long, HashMap<String, Set<String>>> mapUpdatedRecords = getStringValueSetFromLongValue(postMariaDBMergeRequest.getMapUpdatedRecords());
				updateCustomerMergeLogForMongoRecords(customerMergeLog, mapUpdatedRecords);
				List<Long> affectedSecondaryCustomerIDs = new ArrayList<Long>();
				if (customerMergeLog.getUpdatedMongoRecords() != null) {
					affectedSecondaryCustomerIDs.addAll(customerMergeLog.getUpdatedMongoRecords().keySet());
				} else {
					log.warn("Merge log for mongo empty primary_customer_id={}", customerMergeLog.getPrimaryCustomerID());
				}
				updateMessageData(mapUpdatedRecords, postMariaDBMergeRequest.getMergedByDealerAssociateID());
				updateThreadDataForMerge(customerMergeLog, mapUpdatedRecords, postMariaDBMergeRequest.getMergedByDealerAssociateID());
				updateDealerOrderDataForMerge(customerMergeLog,mapUpdatedRecords, postMariaDBMergeRequest.getMergedByDealerAssociateID());
			}

			updateCustomerSubscriptionsForMerge(postMariaDBMergeRequest, customerMergeLog.getDealerID());

			mongoService.updateMergeProgress(customerMergeLog.getPrimaryCustomerGUID(), MergeProgress.MONGO, true);
			lock.unlock();
		} else {
			log.info("lock could not be acquired");
		}

		log.info("Mongo customer merge process complete");
		MDC.clear();
	}
	
	private void updateMessageData(HashMap<Long, HashMap<String,Set<String>>> updatedRecordsMap, Long eventDealerAssociateID) {
		MDC.put(MergeCustomerService.TABLE_NAME, MergeCustomerService.MESSAGE_TABLE);
		List<Long> updateMessageList = new ArrayList<Long>();
		for (Long customerID: updatedRecordsMap.keySet()) {
			HashMap<String, Set<String>> recordsMap = updatedRecordsMap.get(customerID);
			if (recordsMap != null && recordsMap.get(MergeCustomerService.MESSAGE_TABLE) != null) {
				for (String messageID :recordsMap.get(MergeCustomerService.MESSAGE_TABLE) ) {
					updateMessageList.add(Long.parseLong(messageID));
				}
			}
		}
		if (updateMessageList.isEmpty()) {
			MDC.remove(MergeCustomerService.TABLE_NAME);
			return;
		}
		List<MessageSaveEventData> messageSaveEventDataList = generalRepository.getMessageSaveEventDataFromMessage(updateMessageList, eventDealerAssociateID);
		for (MessageSaveEventData messageSaveEventData : messageSaveEventDataList) {
			messagingViewControllerHelper.publishMessageSaveEvent(messageSaveEventData);
		}
		MDC.remove(MergeCustomerService.TABLE_NAME);
	}
	
	private void updateThreadDataForMerge(CustomerMergeLog customerMergeLog, HashMap<Long, HashMap<String,Set<String>>> updatedRecordsMap, Long eventDealerAssociateID) {
		MDC.put(MergeCustomerService.TABLE_NAME, MergeCustomerService.THREAD_TABLE);
		List<Long> updateThreadList = new ArrayList<Long>();
		
		for (Long customerID: updatedRecordsMap.keySet()) {
			HashMap<String, Set<String>> recordsMap = updatedRecordsMap.get(customerID);
			if (recordsMap != null && recordsMap.get(MergeCustomerService.THREAD_TABLE) != null) {
				for (String threadID : recordsMap.get(MergeCustomerService.THREAD_TABLE)) {
					updateThreadList.add(Long.parseLong(threadID));
				}
			}
		}
		if (updateThreadList.isEmpty()) {
			MDC.remove(MergeCustomerService.TABLE_NAME);
			return;
		}
		HashMap<Long, Set<String>> threadCollectionMapSecondary = getThreadCollectionMap(updateThreadList);
		
		addExtendedThreadDataToLog(customerMergeLog, threadCollectionMapSecondary);
		
		List<ThreadSaveEventData> threadSaveEventDataList = generalRepository.getThreadSaveEventDataFromThread(updateThreadList,
				eventDealerAssociateID, EventName.THREAD_CLOSED.name());
		for (ThreadSaveEventData threadSaveEventData : threadSaveEventDataList) {
			messagingViewControllerHelper.publishThreadSaveEnvent(threadSaveEventData);
		}
		List<Long> threadIDListForPrimaryCustomer = generalRepository.getThreadIDsForCustomer(customerMergeLog.getPrimaryCustomerID());
		List<ThreadSaveEventData> threadSaveEventDataListForPrimary =  generalRepository.getThreadSaveEventDataFromThread(threadIDListForPrimaryCustomer,
				eventDealerAssociateID, EventName.THREAD_OPEN.name());
		HashMap<Long, Set<String>> threadCollectionsPrimary = getThreadOpenCollectionsForMerge(customerMergeLog, threadCollectionMapSecondary);
		for (ThreadSaveEventData threadSaveEventData : threadSaveEventDataListForPrimary) {
			ExtendedThreadSaveEventData extendedThreadSaveEventData = new ExtendedThreadSaveEventData();
			extendedThreadSaveEventData.setThreadSaveEventData(threadSaveEventData);
			
			Set<String> collectionSet = null;
			if (threadCollectionsPrimary != null) {
				collectionSet = threadCollectionsPrimary.get(threadSaveEventData.getThreadID());
			}
			if (collectionSet != null && !collectionSet.isEmpty()) {
				List<String> collectionList = new ArrayList<String>();
				collectionList.addAll(collectionSet);
				extendedThreadSaveEventData.setCollectionNames(collectionList);
				messagingViewControllerHelper.publishThreadOpenEnvent(extendedThreadSaveEventData);
			} else {
				threadSaveEventData.setEventName(EventName.THREAD_CUSTOMER_UPDATE.name());
				messagingViewControllerHelper.publishThreadSaveEnvent(threadSaveEventData);
			}
		}
		MDC.remove(MergeCustomerService.TABLE_NAME);
	}
	
	private HashMap<Long, Set<String>> getThreadOpenCollectionsForMerge(CustomerMergeLog customerMergeLog,
			HashMap<Long, Set<String>> threadCollectoinsMapSecondary) {

		if (customerMergeLog.getUpdatedMongoRecords() == null || customerMergeLog.getExtendedRecords() == null || threadCollectoinsMapSecondary == null)
			return null;
		List<Long> threadIDListSecondary = new ArrayList<Long>();
		threadIDListSecondary.addAll(threadCollectoinsMapSecondary.keySet());
		if (threadIDListSecondary.isEmpty())
			return null;
		HashMap<Long, Long> departmentThreadMapPrimary = generalRepository.getDepartmentThreadMapForCustomer(customerMergeLog.getPrimaryCustomerID());

		HashMap<Long, Long> threadDeapartmentMapForSecondary = generalRepository.getThreadDepartmentMap(threadIDListSecondary);
		HashMap<Long, Set<String>> result = new HashMap<Long, Set<String>>();

		List<Long> primaryThreadList = new ArrayList<Long>();
		for (Long dealerDepartmentID : departmentThreadMapPrimary.keySet()) {
			primaryThreadList.add(departmentThreadMapPrimary.get(dealerDepartmentID));
		}
		HashMap<Long, Set<String>> primaryThreadCollectionMap = getThreadCollectionMap(primaryThreadList);

		for (Long dealerDepartmentID : departmentThreadMapPrimary.keySet()) {
			Long primaryCustomerThread = departmentThreadMapPrimary.get(dealerDepartmentID);
			Boolean allArchived = true;
			Boolean allUnassigned = true;
			Boolean ready_for_followUp = false;
			Boolean waitin_for_response = false;
			for (Long secondaryThread : threadDeapartmentMapForSecondary.keySet()) {
				if (threadDeapartmentMapForSecondary.get(secondaryThread).longValue() == dealerDepartmentID.longValue()) {
					if (threadCollectoinsMapSecondary.get(secondaryThread) != null && !threadCollectoinsMapSecondary.get(secondaryThread).isEmpty()) {
						Set<String> collectionsSecondary = customerMergeLog.getExtendedRecords().get(MergeCustomerService.THREAD_COLLECTIONS).get(secondaryThread);
						if (!collectionsSecondary.contains(MergeCustomerService.ARCHIVE_COLLECTION))
							allArchived = false;
						if (!collectionsSecondary.contains(MergeCustomerService.UNASSIGNED_COLLECTION))
							allUnassigned = false;
						if (collectionsSecondary.contains(MergeCustomerService.READY_FOR_FOLLOW_UP_COLLECTION))
							ready_for_followUp = true;
						if (collectionsSecondary.contains(MergeCustomerService.UNRESPONDED_COLECTION))
							waitin_for_response = true;
						if (result.get(primaryCustomerThread) == null) {
							result.put(primaryCustomerThread, new HashSet<String>());
						}
						Set<String> collectionSet = result.get(primaryCustomerThread);
						collectionSet.addAll(collectionsSecondary);
						result.put(primaryCustomerThread, collectionSet);
					}
				}
			}
			Set<String> collectionsPrimary = result.get(primaryCustomerThread);
			if (collectionsPrimary == null) {
				collectionsPrimary = new HashSet<String>();
			}
			if (primaryThreadCollectionMap.get(primaryCustomerThread) != null && !primaryThreadCollectionMap.get(primaryCustomerThread).isEmpty()) {
				if (!primaryThreadCollectionMap.get(primaryCustomerThread).contains(MergeCustomerService.ARCHIVE_COLLECTION))
					allArchived = false;
				if (!primaryThreadCollectionMap.get(primaryCustomerThread).contains(MergeCustomerService.UNASSIGNED_COLLECTION))
					allUnassigned = false;
				if (primaryThreadCollectionMap.get(primaryCustomerThread).contains(MergeCustomerService.READY_FOR_FOLLOW_UP_COLLECTION))
					ready_for_followUp = true;
				if (primaryThreadCollectionMap.get(primaryCustomerThread).contains(MergeCustomerService.UNRESPONDED_COLECTION))
					waitin_for_response = true;
				collectionsPrimary.addAll(primaryThreadCollectionMap.get(primaryCustomerThread));
			}

			if (!allArchived)
				collectionsPrimary.remove(MergeCustomerService.ARCHIVE_COLLECTION);
			if (!allUnassigned)
				collectionsPrimary.remove(MergeCustomerService.UNASSIGNED_COLLECTION);
			if (ready_for_followUp)
				collectionsPrimary.remove(MergeCustomerService.FOLLOWED_UP_WITH_COLLECTION);
			if (waitin_for_response) {
				String lastMessageType = generalRepository.getLastCustomerMessageType(primaryCustomerThread);
				if (!"I".equalsIgnoreCase(lastMessageType))
					collectionsPrimary.remove(MergeCustomerService.UNRESPONDED_COLECTION);
			}

			// Add delegation collection to set when primary customer thread has been delegated because of merge
			// Remove primaryCustomer's thread from customer merge log once you add DelegationCollection to open collections set
			// This is done to prevent primaryCustomer's thread from being added to delegationCollection again and again over multiple merge cycles
			HashMap<String, HashMap<Long, Set<String>>> extendedRecords = customerMergeLog.getExtendedRecords();
			if (extendedRecords != null && !extendedRecords.isEmpty()) {
				HashMap<Long, Set<String>> threadDelegationMap = extendedRecords.get(MergeCustomerService.DELEGATION_HISTORY);
				if (threadDelegationMap != null && !threadDelegationMap.isEmpty()) {
					if (threadDelegationMap.containsKey(primaryCustomerThread)) {
						collectionsPrimary.add(MergeCustomerService.DELEGATION_COLLECTION);
						threadDelegationMap.remove(primaryCustomerThread);
					}
				}
				extendedRecords.put(MergeCustomerService.DELEGATION_HISTORY, threadDelegationMap);
				customerMergeLog.setExtendedRecords(extendedRecords);
				mongoService.saveCustomerMergeUpdatedExtendedRecords(customerMergeLog);
			}

			log.info("primary_customer_id=\"{}\" primary_customer_thread=\"{}\" would be added/updated in following collections={} ",
					customerMergeLog.getPrimaryCustomerID(), primaryCustomerThread, collectionsPrimary);
			result.put(primaryCustomerThread, collectionsPrimary);
		}
		return result;
	}
	
	private HashMap<Long, Set<String>> getDealerOrderOpenCollections(Long primaryCustomerID, HashMap<Long, Set<String>> dealerOrderCollectionsSecondary,
			HashMap<Long, Set<String>> dealerOrderCollectionsPrimary, HashMap<Long, Set<String>> threadCollectionsPrimary,
			HashMap<Long, Long> departmentThreadMapPrimary, HashMap<Long, List<Long>> departmentDealerOrderMapPrimary){
		HashMap<Long, Set<String>> result = new HashMap<>();
		if (departmentDealerOrderMapPrimary == null)
			return result;
		for (Long dealerDepartmentID: departmentDealerOrderMapPrimary.keySet()) {
				List<Long> dealerOrders = departmentDealerOrderMapPrimary.get(dealerDepartmentID);
				if (dealerOrders  == null)
					continue;
				Boolean allArchived = true;
				Boolean allUnassigned = true;
				Boolean ready_for_followUp =false;
				Boolean waitin_for_response = false;
				for (Long dealerOrderID : dealerOrders) {
					Set<String> collectionsForDealerOrder = new HashSet<>();
					if (dealerOrderCollectionsSecondary.get(dealerOrderID)!= null) {
						if (!dealerOrderCollectionsSecondary.get(dealerOrderID).contains(MergeCustomerService.ARCHIVE_COLLECTION))
							allArchived=false;
						if (!dealerOrderCollectionsSecondary.get(dealerOrderID).contains(MergeCustomerService.UNASSIGNED_COLLECTION))
							allUnassigned=false;
						if (dealerOrderCollectionsSecondary.get(dealerOrderID).contains(MergeCustomerService.READY_FOR_FOLLOW_UP_COLLECTION))
							ready_for_followUp=true;
						if (dealerOrderCollectionsSecondary.get(dealerOrderID).contains(MergeCustomerService.UNRESPONDED_COLECTION))
							waitin_for_response=true;
						collectionsForDealerOrder.addAll(dealerOrderCollectionsSecondary.get(dealerOrderID));
					}
					
					if (dealerOrderCollectionsPrimary.get(dealerOrderID)!= null) {
						if (!dealerOrderCollectionsPrimary.get(dealerOrderID).contains(MergeCustomerService.ARCHIVE_COLLECTION))
							allArchived=false;
						if (!dealerOrderCollectionsPrimary.get(dealerOrderID).contains(MergeCustomerService.UNASSIGNED_COLLECTION))
							allUnassigned=false;
						if (dealerOrderCollectionsPrimary.get(dealerOrderID).contains(MergeCustomerService.READY_FOR_FOLLOW_UP_COLLECTION))
							ready_for_followUp=true;
						if (dealerOrderCollectionsPrimary.get(dealerOrderID).contains(MergeCustomerService.UNRESPONDED_COLECTION))
							waitin_for_response=true;
						collectionsForDealerOrder.addAll(dealerOrderCollectionsPrimary.get(dealerOrderID));
						
					}
					result.put(dealerOrderID, collectionsForDealerOrder);
				}
				
				if (departmentThreadMapPrimary != null) {
					Long threadPrimary = departmentThreadMapPrimary.get(dealerDepartmentID);
					if (threadPrimary != null ) {
						Set<String> collectionsThread =threadCollectionsPrimary.get(threadPrimary);
						if (collectionsThread != null) {
							if (!collectionsThread.contains(MergeCustomerService.ARCHIVE_COLLECTION))
								allArchived=false;
							if (!collectionsThread.contains(MergeCustomerService.UNASSIGNED_COLLECTION))
								allUnassigned=false;
							if (collectionsThread.contains(MergeCustomerService.READY_FOR_FOLLOW_UP_COLLECTION))
								ready_for_followUp=true;
							if (collectionsThread.contains(MergeCustomerService.UNRESPONDED_COLECTION))
								waitin_for_response=true;
						}
					}
				}
				
				//Once you have datapoints for all dealerOrders and thread of a department, now take a decision
				String lastMessageType = generalRepository.getLastCustomerMessageTypeForDepartment(dealerDepartmentID, primaryCustomerID);
				for (Long dealerOrderID: dealerOrders) {
					
					if (!allArchived) {
						Set<String> collectionList = result.get(dealerOrderID);
						if (collectionList != null && collectionList.contains(MergeCustomerService.ARCHIVE_COLLECTION)) {
							collectionList.remove(MergeCustomerService.ARCHIVE_COLLECTION);
							result.put(dealerOrderID, collectionList);
						}
					}
					
					if (!allUnassigned) {
						Set<String> collectionList = result.get(dealerOrderID);
						if (collectionList != null && collectionList.contains(MergeCustomerService.UNASSIGNED_COLLECTION)) {
							collectionList.remove(MergeCustomerService.UNASSIGNED_COLLECTION);
							result.put(dealerOrderID, collectionList);
						}
					}
					
					if (ready_for_followUp) {
						Set<String> collectionList = result.get(dealerOrderID);
						if (collectionList != null && collectionList.contains(MergeCustomerService.FOLLOWED_UP_WITH_COLLECTION)) {
							collectionList.remove(MergeCustomerService.FOLLOWED_UP_WITH_COLLECTION);
							result.put(dealerOrderID, collectionList);
						}
					}
					
					if (waitin_for_response) {
						if (!"I".equalsIgnoreCase(lastMessageType)) {
							Set<String> collectionList = result.get(dealerOrderID);
							if (collectionList != null && collectionList.contains(MergeCustomerService.UNRESPONDED_COLECTION)) {
								collectionList.remove(MergeCustomerService.UNRESPONDED_COLECTION);
								result.put(dealerOrderID, collectionList);
							}
						}
					}
				}
		}
		return result;
	}
	
	private void updateDealerOrderDataForMerge(CustomerMergeLog customerMergeLog,HashMap<Long, HashMap<String,Set<String>>> updatedRecordsMap, Long eventDealerAssociateID) {
		MDC.put(MergeCustomerService.TABLE_NAME, MergeCustomerService.DEALER_ORDER_TABLE);
		HashMap<Long,Long> dealerOrderCustomerMapSecondary = new HashMap<>();
		for (Long customerID: updatedRecordsMap.keySet()) {
			HashMap<String, Set<String>> recordsMap = updatedRecordsMap.get(customerID);
			if (recordsMap != null && recordsMap.get(MergeCustomerService.DEALER_ORDER_TABLE) != null) {
				for (String dealerOrderID: recordsMap.get(MergeCustomerService.DEALER_ORDER_TABLE)) {
					dealerOrderCustomerMapSecondary.put(Long.parseLong(dealerOrderID), customerID);
				}
			}
		}
		if (dealerOrderCustomerMapSecondary.isEmpty()) {
			MDC.remove(MergeCustomerService.TABLE_NAME);
			return;
		}
		List<Long> dealerOrderIDListSecondary = new ArrayList<>();
		dealerOrderIDListSecondary.addAll(dealerOrderCustomerMapSecondary.keySet());
		HashMap<Long, Set<String>> dealerOrderCollectionsSecondary = getDealerOrderCollectionMap(dealerOrderCustomerMapSecondary);
		addExtendedDealerOrderDataToLog(customerMergeLog, dealerOrderCollectionsSecondary);
		for (Long dealerOrderID : dealerOrderCustomerMapSecondary.keySet()) {
			ThreadSaveEventData threadSaveEventData=  new ThreadSaveEventData();
			threadSaveEventData.setCustomerID(dealerOrderCustomerMapSecondary.get(dealerOrderID));
			threadSaveEventData.setDealerID(customerMergeLog.getDealerID());
			threadSaveEventData.setEventRaisedBy(eventDealerAssociateID);
			threadSaveEventData.setEventName(EventName.THREAD_CLOSED.name());
			messagingViewControllerHelper.publishThreadSaveEnvent(threadSaveEventData);
		}
		HashMap<Long, List<Long>> departmentDealerOrderMapPrimary = generalRepository.getDepartmentDealerOrderMapForCustomer(customerMergeLog.getPrimaryCustomerID());
		if (departmentDealerOrderMapPrimary == null || departmentDealerOrderMapPrimary.isEmpty()) {
			MDC.remove(MergeCustomerService.TABLE_NAME);
			return;
		}
		HashMap<Long, Long> dealerOrderCustomerMapPrimary = new HashMap<>();
		for (Long dealerDepartmentID : departmentDealerOrderMapPrimary.keySet()) {
			for (Long dealerOrderPrimary : departmentDealerOrderMapPrimary.get(dealerDepartmentID)) {
				dealerOrderCustomerMapPrimary.put(dealerOrderPrimary, customerMergeLog.getPrimaryCustomerID());
			}
		}
		
		HashMap<Long, Set<String>> dealerOrderCollectionsPrimary = mongoService.findCollectionsForDealerOrder(dealerOrderCustomerMapPrimary);
		HashMap<Long, Long> departmetnThreadMapPrimary = generalRepository.getDepartmentThreadMapForCustomer(customerMergeLog.getPrimaryCustomerID());
		HashMap<Long, Set<String>> threadCollectionsMapPrimary= null;
		if (departmetnThreadMapPrimary != null && !departmetnThreadMapPrimary.isEmpty()) {
			List<Long> primaryThreadIDList = new ArrayList<>();
			primaryThreadIDList.addAll(departmetnThreadMapPrimary.values());
			threadCollectionsMapPrimary = mongoService.findCollectionsForThread(primaryThreadIDList);
		}
		
		HashMap<Long, Set<String>> dealerOrderOpenCollections = getDealerOrderOpenCollections(customerMergeLog.getPrimaryCustomerID(), dealerOrderCollectionsSecondary, dealerOrderCollectionsPrimary, threadCollectionsMapPrimary, departmetnThreadMapPrimary, departmentDealerOrderMapPrimary);
		if (dealerOrderOpenCollections == null || dealerOrderOpenCollections.isEmpty()) {
			MDC.remove(MergeCustomerService.TABLE_NAME);
			return;
		}
		List<Long> candidateDealerOrdersPrimary = new ArrayList<>();
		candidateDealerOrdersPrimary.addAll(dealerOrderOpenCollections.keySet());
		Collections.sort(candidateDealerOrdersPrimary);
		List<DealerOrderSaveEventData> dealerOrderSaveEventDataListPrimary = generalRepository.getDealerOrderSaveEventDataFromThread(candidateDealerOrdersPrimary, eventDealerAssociateID);
		for (DealerOrderSaveEventData dealerOrderSaveEventData : dealerOrderSaveEventDataListPrimary) {
			if (dealerOrderOpenCollections.get(dealerOrderSaveEventData.getDealerOrderID()) == null || 
					dealerOrderOpenCollections.get(dealerOrderSaveEventData.getDealerOrderID()).isEmpty())
				continue;
			ExtendedDealerOrderSaveEventData extendedDealerOrderSaveEventData = new ExtendedDealerOrderSaveEventData();
			extendedDealerOrderSaveEventData.setDealerOrderSaveEventData(dealerOrderSaveEventData);
			List<String> collectionList = new ArrayList<>();
			collectionList.addAll(dealerOrderOpenCollections.get(dealerOrderSaveEventData.getDealerOrderID()));
			extendedDealerOrderSaveEventData.setCollectionNames(collectionList);
			messagingViewControllerHelper.publishDealerOrderCustomerUpdateEvent(extendedDealerOrderSaveEventData);
			
		}
		MDC.remove(MergeCustomerService.TABLE_NAME);
	}
	
	private HashMap<Long, Set<String>> getDealerOrderCollectionMap(HashMap<Long, Long> dealerOrderCustomerMap){
		HashMap<Long, Set<String>> dealerOrderCollectionMap = mongoService.findCollectionsForDealerOrder(dealerOrderCustomerMap);
		for (Long dealerOrderID:  dealerOrderCollectionMap.keySet()) {
			Set<String> collectionSet = dealerOrderCollectionMap.get(dealerOrderID);
			if (collectionSet == null) {
				collectionSet = new HashSet<String>();
			}
			dealerOrderCollectionMap.put(dealerOrderID, collectionSet);
		}
		return dealerOrderCollectionMap;
	}
	
	public void updateCustomerSubscriptionsForMerge(PostMariaDBMergeRequest postMariaDBMergeRequest, Long dealerID) {
		SubscriptionList subscriptionList = getNewSubscriptionListForMergedCustomer(postMariaDBMergeRequest.getPrimaryCustomerID(),postMariaDBMergeRequest.getMergedCustomerIDs(), dealerID);
		if (subscriptionList == null)
			return;
		List<Long> dealerAssociateIDList = new ArrayList<Long>();
		if (subscriptionList.getExternalSubscriberList() != null) {
			for (Subscriber subscribers : subscriptionList.getExternalSubscriberList()) {
				dealerAssociateIDList.add(subscribers.getDealerAssociateID());
			}
		}
		if (subscriptionList.getInternalSubscriberList() != null) {
			for (Subscriber subscribers : subscriptionList.getInternalSubscriberList()) {
				dealerAssociateIDList.add(subscribers.getDealerAssociateID());
			}
		}
		if(dealerAssociateIDList.isEmpty())
			return;
		HashMap<Long, Long> dealerAssociateDealerDepartmentMap = generalRepository.getDealerAssociateDepartmentMap(dealerAssociateIDList);
		if (subscriptionList.getExternalSubscriberList()!=null) {
			MDC.put(MergeCustomerService.TABLE_NAME, EventName.EXTERNAL_SUBSRIPTION_ADDED.name());
			for (Subscriber subscribers : subscriptionList.getExternalSubscriberList()) {
				SubscriptionSaveEventData subscriptionSaveEventData = new SubscriptionSaveEventData();
				subscriptionSaveEventData.setCustomerID(postMariaDBMergeRequest.getPrimaryCustomerID());
				subscriptionSaveEventData.setDealerDepartmentID(dealerAssociateDealerDepartmentMap.get(subscribers.getDealerAssociateID()));
				subscriptionSaveEventData.setDealerID(dealerID);
				subscriptionSaveEventData.setEventName(EventName.EXTERNAL_SUBSRIPTION_ADDED.name());
				subscriptionSaveEventData.setSubscriberDAID(subscribers.getDealerAssociateID());
				subscriptionSaveEventData.setEventRaisedBy(postMariaDBMergeRequest.getMergedByDealerAssociateID());
				messagingViewControllerHelper.publishSubscriptionSaveEvent(subscriptionSaveEventData);
			}
			MDC.remove(MergeCustomerService.TABLE_NAME);
		}
		if (subscriptionList.getInternalSubscriberList() != null) {
			MDC.put(MergeCustomerService.TABLE_NAME, EventName.INTERNAL_SUBSRICTION_ADDED.name());
			for (Subscriber subscribers : subscriptionList.getInternalSubscriberList()) {
				SubscriptionSaveEventData subscriptionSaveEventData = new SubscriptionSaveEventData();
				subscriptionSaveEventData.setCustomerID(postMariaDBMergeRequest.getPrimaryCustomerID());
				subscriptionSaveEventData.setDealerDepartmentID(dealerAssociateDealerDepartmentMap.get(subscribers.getDealerAssociateID()));
				subscriptionSaveEventData.setDealerID(dealerID);
				subscriptionSaveEventData.setEventName(EventName.INTERNAL_SUBSRICTION_ADDED.name());
				subscriptionSaveEventData.setSubscriberDAID(subscribers.getDealerAssociateID());
				subscriptionSaveEventData.setEventRaisedBy(postMariaDBMergeRequest.getMergedByDealerAssociateID());
				messagingViewControllerHelper.publishSubscriptionSaveEvent(subscriptionSaveEventData);
			}
			MDC.remove(MergeCustomerService.TABLE_NAME);
		}
	}
	
	private SubscriptionList getNewSubscriptionListForMergedCustomer(Long primaryCustomerID, List<Long> mergedCustomerIDList, Long dealerID) {
		SubscriptionList result = new SubscriptionList();
		result.setCustomerID(primaryCustomerID);
		if(result.getExternalSubscriberList() == null)
			result.setExternalSubscriberList(new ArrayList<Subscriber>());
		if(result.getInternalSubscriberList() == null)
			result.setInternalSubscriberList(new ArrayList<Subscriber>());
		List<Long> customerIDList = new ArrayList<Long>();
		customerIDList.addAll(mergedCustomerIDList);
		HashMap<Long, Subscriber> internalSubscribersMap = new HashMap<>();
		HashMap<Long, Subscriber> externalSubscribersMap = new HashMap<>();
		for (Long customerID: customerIDList) {
			SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
			subscriptionRequest.setCustomerUUID(generalRepository.getCustomerUUIDFromCustomerID(customerID));
			subscriptionRequest.setDealerID(dealerID);
			subscriptionRequest.setSubscriptionType(MVCConstants.SUBSCRIPTION_TYPE_BOTH);
			SubscriptionList subscriptionList = messagingViewControllerHelper.getSubscriptionForCustomer(subscriptionRequest);
			if (subscriptionList != null && subscriptionList.getExternalSubscriberList() != null) {
				for (Subscriber subscribers : subscriptionList.getExternalSubscriberList()) {
					externalSubscribersMap.put(subscribers.getDealerAssociateID(), subscribers);
				}
			}
			if (subscriptionList != null && subscriptionList.getInternalSubscriberList() != null) {
				for (Subscriber subscribers : subscriptionList.getInternalSubscriberList()) {
					internalSubscribersMap.put(subscribers.getDealerAssociateID(), subscribers);
				}
			}
		}
	
		SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
		subscriptionRequest.setCustomerUUID(generalRepository.getCustomerUUIDFromCustomerID(primaryCustomerID));
		subscriptionRequest.setDealerID(dealerID);
		subscriptionRequest.setSubscriptionType(MVCConstants.SUBSCRIPTION_TYPE_BOTH);
		SubscriptionList primarySubscriptions  = messagingViewControllerHelper.getSubscriptionForCustomer(subscriptionRequest);
		
		if (primarySubscriptions != null && primarySubscriptions.getExternalSubscriberList() != null) {
			for (Subscriber primarySubscribers: primarySubscriptions.getExternalSubscriberList()) {
				externalSubscribersMap.remove(primarySubscribers.getDealerAssociateID());
			}
		}
		
		if (primarySubscriptions != null && primarySubscriptions.getInternalSubscriberList() != null) {
			for (Subscriber primarySubscribers: primarySubscriptions.getInternalSubscriberList()) {
				internalSubscribersMap.remove(primarySubscribers.getDealerAssociateID());
			}
		}
		
		if(!externalSubscribersMap.isEmpty()) {
			List<Subscriber> externalSubscribers=  result.getExternalSubscriberList();
			externalSubscribers.addAll(externalSubscribersMap.values());
			result.setExternalSubscriberList(externalSubscribers);
		}
		
		if(!internalSubscribersMap.isEmpty()) {
			List<Subscriber> internalSubscribers = result.getInternalSubscriberList();
			internalSubscribers.addAll(internalSubscribersMap.values());
			result.setInternalSubscriberList(internalSubscribers);
		}
			
		return result;
	}
	
	private CustomerMergeLog createUpdateMergeLog(Long primaryCustomerID, String primaryCustomerGUID, Boolean isMergeAction, List<Long> mergedCustomerIDsList) {
		CustomerMergeLog customerMergeLog = mongoService.getCustomerMergeLogForPrimaryCustomer(primaryCustomerID);
		if (customerMergeLog == null) {
			customerMergeLog = new CustomerMergeLog();
			customerMergeLog.setPrimaryCustomerID(primaryCustomerID);
			customerMergeLog.setPrimaryCustomerGUID(primaryCustomerGUID);
			customerMergeLog.setCreatedDate(new Date());
			try {
				customerMergeLog.setDealerID(generalRepository.getDealerIdForCustomerId(primaryCustomerID));
			} catch (Exception e) {
				log.error("Error while getting dealerid for customer_id={}", primaryCustomerID);
			}
		}
		
		if (mergedCustomerIDsList != null && !mergedCustomerIDsList.isEmpty()) {
			customerMergeLog.getMergedCustomerIDs().addAll(mergedCustomerIDsList);
		}
		
		mongoService.saveOrUpdateCustomerMergeLog(customerMergeLog);
		mongoService.updateMergeProgress(customerMergeLog.getPrimaryCustomerGUID(), MergeProgress.MONGO, false);
		return customerMergeLog;
	}
	
	private void updateCustomerMergeLogForMongoRecords(CustomerMergeLog customerMergeLog, HashMap<Long, HashMap<String,Set<String>>> mapCustomerUpdatedRecords) {
		if (mapCustomerUpdatedRecords != null && !mapCustomerUpdatedRecords.isEmpty())
			for (Long customerID: mapCustomerUpdatedRecords.keySet()) {
				if (mapCustomerUpdatedRecords.get(customerID) != null && !mapCustomerUpdatedRecords.get(customerID).isEmpty()) {
					for (String tableName : mapCustomerUpdatedRecords.get(customerID).keySet()) {
						Set<String> updatedRecords = mapCustomerUpdatedRecords.get(customerID).get(tableName);
						if (tableName.equalsIgnoreCase(MergeCustomerService.MESSAGE_TABLE) && updatedRecords != null) {
							copyRecordsToMongoMap(customerMergeLog, customerID, updatedRecords, tableName);
						} else if (tableName.equalsIgnoreCase(MergeCustomerService.THREAD_TABLE) && updatedRecords != null) {
							copyRecordsToMongoMap(customerMergeLog, customerID, updatedRecords, tableName);
						} else if(tableName.equalsIgnoreCase(MergeCustomerService.DEALER_ORDER_TABLE)
								&&mapCustomerUpdatedRecords.get(customerID).get(tableName) !=null) {
							copyRecordsToMongoMap(customerMergeLog, customerID, mapCustomerUpdatedRecords.get(customerID).get(tableName) , tableName);
						}
					}
				}
			}
		mongoService.saveCustomerMergeUpdatedMongoRecords(customerMergeLog);
	}
	
	private void addExtendedThreadDataToLog(CustomerMergeLog customerMergeLog, HashMap<Long, Set<String>> threadCollectionMap) {
		HashMap<String, HashMap<Long,Set<String>>> extendedRecordsMap = customerMergeLog.getExtendedRecords();
		if (extendedRecordsMap == null) {
			extendedRecordsMap = new HashMap<String,HashMap<Long,Set<String>>>();
		}
		HashMap<Long,Set<String>> existingThreadCollectionMap = extendedRecordsMap.get(MergeCustomerService.THREAD_COLLECTIONS);
		if (existingThreadCollectionMap == null) {
			existingThreadCollectionMap = threadCollectionMap;
		} else {
			for (Long threadID : threadCollectionMap.keySet()) {
				Set<String> collectionSet =  existingThreadCollectionMap.get(threadID);
				if (collectionSet == null)
					collectionSet = new HashSet<String>();
				if (threadCollectionMap.get(threadID) != null)
					collectionSet.addAll(threadCollectionMap.get(threadID));
				existingThreadCollectionMap.put(threadID, collectionSet);
			}
		}
		
		extendedRecordsMap.put(MergeCustomerService.THREAD_COLLECTIONS, existingThreadCollectionMap);
		customerMergeLog.setExtendedRecords(extendedRecordsMap);
		mongoService.saveCustomerMergeUpdatedExtendedRecords(customerMergeLog);
	}
	
	private void addExtendedDealerOrderDataToLog(CustomerMergeLog customerMergeLog, HashMap<Long, Set<String>> dealerOrderCollectionMap) {
		HashMap<String, HashMap<Long,Set<String>>> extendedRecordsMap = customerMergeLog.getExtendedRecords();
		if (extendedRecordsMap == null) {
			extendedRecordsMap = new HashMap<String,HashMap<Long,Set<String>>>();
		}
		HashMap<Long,Set<String>> existingDealerOrderCollectionMap = extendedRecordsMap.get(MergeCustomerService.DEALERORDER_COLLECTIONS);
		if (existingDealerOrderCollectionMap == null) {
			existingDealerOrderCollectionMap = dealerOrderCollectionMap;
		} else {
			for (Long dealerOrder : dealerOrderCollectionMap.keySet()) {
				Set<String> collectionSet =  existingDealerOrderCollectionMap.get(dealerOrder);
				if (collectionSet == null)
					collectionSet = new HashSet<String>();
				if (dealerOrderCollectionMap.get(dealerOrder) != null)
					collectionSet.addAll(dealerOrderCollectionMap.get(dealerOrder));
				existingDealerOrderCollectionMap.put(dealerOrder, collectionSet);
			}
		}
		
		extendedRecordsMap.put(MergeCustomerService.DEALERORDER_COLLECTIONS, existingDealerOrderCollectionMap);
		customerMergeLog.setExtendedRecords(extendedRecordsMap);
		mongoService.saveCustomerMergeUpdatedExtendedRecords(customerMergeLog);
	}
	
	private HashMap<Long, Set<String>> getThreadCollectionMap(List<Long> threadIDList){
		HashMap<Long, Set<String>> threadCollectionMap = mongoService.findCollectionsForThread(threadIDList);
		for (Long threadID:  threadCollectionMap.keySet()) {
			Set<String> collectionSet = threadCollectionMap.get(threadID);
			if (collectionSet == null) {
				collectionSet = new HashSet<String>();
			}
			threadCollectionMap.put(threadID, collectionSet);
		}
		return threadCollectionMap;
		
	}
	
	private void copyRecordsToMongoMap(CustomerMergeLog customerMergeLog, Long customerID, Set<String> records, String tableName) {
		HashMap<Long, HashMap<String,Set<String>>> mongoRecordsMap = customerMergeLog.getUpdatedMongoRecords();
		if (mongoRecordsMap == null) {
			mongoRecordsMap = new HashMap<Long,HashMap<String, Set<String>>>();
		}
		
		HashMap<String, Set<String>> recordsMap = mongoRecordsMap.get(customerID);
		if (recordsMap == null) {
			recordsMap = new HashMap<String, Set<String>>();
		}
		
		Set<String> existingRecords = recordsMap.get(tableName);
		if (existingRecords == null) {
			existingRecords = new HashSet<String>();
		}
		existingRecords.addAll(records);
		recordsMap.put(tableName, existingRecords);
		
		mongoRecordsMap.put(customerID, recordsMap);
		customerMergeLog.setUpdatedMongoRecords(mongoRecordsMap);
	}
	
	private HashMap<Long,HashMap<String,Set<String>>> getStringValueSetFromLongValue(HashMap<Long,HashMap<String,Set<Long>>> inputMap){
		HashMap<Long, HashMap<String, Set<String>>> mapUpdatedRecords = new HashMap<Long, HashMap<String, Set<String>>>();
		for (Long customerID : inputMap.keySet()) {
			HashMap<String, Set<String>> mapRecords = new HashMap<String, Set<String>>();
			if (inputMap.get(customerID) != null) {
				for (String table : inputMap.get(customerID).keySet()) {
					if (inputMap.get(customerID).get(table) != null) {
						for (Long recordID : inputMap.get(customerID).get(table)) {
							if (mapRecords.get(table) == null) {
								mapRecords.put(table, new HashSet<String>());
							}
							Set<String> set = mapRecords.get(table);
							set.add(recordID.toString());
							mapRecords.put(table, set);
						}
					}
				}
				mapUpdatedRecords.put(customerID, mapRecords);
			}
		}
		return mapUpdatedRecords;
	}
}

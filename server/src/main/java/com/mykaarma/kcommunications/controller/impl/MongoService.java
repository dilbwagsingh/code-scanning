package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.mongo.CustomerMergeLog;
import com.mykaarma.kcommunications.model.mongo.CustomerMergeLog.MergeProgress;
import com.mykaarma.kcommunications.model.mongo.MergeIgnoreCollection;
import com.mykaarma.kcommunications.model.mongo.ThreadIgnoreCollection;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.MongoConstants;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MongoService {
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
	private KCommunicationsUtils kCommunicationUtils;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	public void saveOrUpdateCustomerMergeLog(CustomerMergeLog customerMergeLog) {
		if (customerMergeLog.getCreatedDate() == null)
			customerMergeLog.setCreatedDate(new Date());
		else
			customerMergeLog.setUpdatedDate(new Date());
		Criteria criteria = new Criteria(MongoConstants.MONGO_FIELD_PRIMARY_CUSTOMER_ID).is(customerMergeLog.getPrimaryCustomerID());
		Query query = new Query(criteria);
		Update update = new Update();
		update.set(MongoConstants.MONGO_FIELD_UPDATED_MONGO_RECORDS, customerMergeLog.getUpdatedMongoRecords());
		update.set(MongoConstants.MONGO_FIELD_CREATED_DATE, customerMergeLog.getCreatedDate());
		update.set(MongoConstants.MONGO_FIELD_UPDATED_DATE, customerMergeLog.getUpdatedDate());
		update.set(MongoConstants.MONGO_FIELD_PRIMARY_CUSTOMER_GUID, customerMergeLog.getPrimaryCustomerGUID());
		update.set(MongoConstants.MONGO_FIELD_EXTENDED_RECORDS, customerMergeLog.getExtendedRecords());
		update.set(MongoConstants.MONGO_FIELD_DEALER_ID, customerMergeLog.getDealerID());
		update.set(MongoConstants.MONGO_FIELD_MERGED_CUSTOMER_IDS, customerMergeLog.getMergedCustomerIDs());
		mongoTemplate.upsert(query, update, MergeCustomerService.CUSTOMER_MERGE_LOG_COLLECTION);
		log.info("Saving customer_merge_log={} for primary_customer_id={}", Helper.toString(customerMergeLog),
				customerMergeLog.getPrimaryCustomerID());
	}
	
	public CustomerMergeLog getCustomerMergeLogForPrimaryCustomer(Long primaryCustomerID) {
		Criteria criteria = new Criteria(MongoConstants.MONGO_FIELD_PRIMARY_CUSTOMER_ID).is(primaryCustomerID);
		Query query = new Query(criteria);
		CustomerMergeLog customerMergeLog = mongoTemplate.findOne(query, CustomerMergeLog.class,
				MergeCustomerService.CUSTOMER_MERGE_LOG_COLLECTION);
		return customerMergeLog;
	}
	
	public void saveCustomerMergeUpdatedMongoRecords(CustomerMergeLog customerMergeLog) {
		log.info("Adding following mongo record {} for primary_customer_id=\"{}\"",
				Helper.toString(customerMergeLog.getUpdatedMongoRecords()), customerMergeLog.getPrimaryCustomerID());
		Criteria criteria = new Criteria(MongoConstants.MONGO_FIELD_PRIMARY_CUSTOMER_ID).is(customerMergeLog.getPrimaryCustomerID());
		Query query = new Query(criteria);
		Update update = new Update();
		update.set(MongoConstants.MONGO_FIELD_UPDATED_MONGO_RECORDS, customerMergeLog.getUpdatedMongoRecords());
		mongoTemplate.updateFirst(query, update, MergeCustomerService.CUSTOMER_MERGE_LOG_COLLECTION);
		log.info("updated Mongo Records for merge log {}", Helper.toString(customerMergeLog));
	}
	
	public void saveCustomerMergeUpdatedExtendedRecords(CustomerMergeLog customerMergeLog) {
		log.info("Adding following extended record {} for primary_customer_id=\"{}\"",
				Helper.toString(customerMergeLog.getExtendedRecords()), customerMergeLog.getPrimaryCustomerID());
		Criteria criteria = new Criteria(MongoConstants.MONGO_FIELD_PRIMARY_CUSTOMER_ID).is(customerMergeLog.getPrimaryCustomerID());
		Query query = new Query(criteria);
		Update update = new Update();
		update.set(MongoConstants.MONGO_FIELD_EXTENDED_RECORDS, customerMergeLog.getExtendedRecords());
		mongoTemplate.updateFirst(query, update, MergeCustomerService.CUSTOMER_MERGE_LOG_COLLECTION);
		log.info("updated Extended Records for merge log {}", Helper.toString(customerMergeLog));
	}
	
	public void updateMergeProgress(String primaryCustomerGuid, MergeProgress mergeProgress, boolean isCompleted) {
		Criteria criteria = new Criteria(MongoConstants.MONGO_FIELD_PRIMARY_CUSTOMER_GUID).is(primaryCustomerGuid);
		Query query = new Query(criteria);
		Update update = Update.update(mergeProgress.getProperty(), isCompleted);
		mongoTemplate.updateFirst(query, update, MergeCustomerService.CUSTOMER_MERGE_LOG_COLLECTION);
	}
	
	public HashMap<Long, Set<String>> findCollectionsForThread(List<Long> threadIDs) {
		HashMap<Long, Set<String>> threadCollectionMap = new HashMap<Long, Set<String>>();
		HashMap<String, List<String>> threadCollections = kCommunicationUtils.getCustomerMergeMongoThreadSchema();
		for (String collection : threadCollections.keySet()) {
			if (MergeIgnoreCollection.exists(collection) != null)
				continue;
			if (ThreadIgnoreCollection.exists(collection) != null)
				continue;
			MDC.put(MergeCustomerService.TABLE_NAME, collection);
			for (Long threadID : threadIDs) {
				try {
					Criteria criteria = new Criteria(threadCollections.get(collection).get(0)).is(threadID);
					Query query = new Query(criteria);
					log.info("Finding if thread_id={} exists in collection={} using count query", threadID, collection);
					long count = mongoTemplate.count(query, collection);
					if (count > 0l) {
						Set<String> collectionList = threadCollectionMap.get(threadID);
						if (collectionList == null) {
							collectionList = new HashSet<String>();
						}
						collectionList.add(collection);
						threadCollectionMap.put(threadID, collectionList);
					}
				} catch (Exception e) {
					log.warn("Unable to find existence of thread_id=\"{}\" in collection=\"{}\" ", threadID, collection,
							e);
				}
			}
			MDC.remove(MergeCustomerService.TABLE_NAME);
		}
		return threadCollectionMap;
	}
	
	public HashMap<Long, Set<String>> findCollectionsForDealerOrder(HashMap<Long, Long> dealerOrderCustomerMap) {
		HashMap<Long, Set<String>> dealerOrderCollectionMap = new HashMap<Long, Set<String>>();
		HashMap<String, List<String>> customerCollections = kCommunicationUtils.getCustomerMergeMongoDealerOrderSchema();
		for (String collection : customerCollections.keySet()) {
			if (MergeIgnoreCollection.exists(collection) != null)
				continue;
			if (ThreadIgnoreCollection.exists(collection) != null)
				continue;
			MDC.put(MergeCustomerService.TABLE_NAME, collection);
			for (Long dealerOrderID : dealerOrderCustomerMap.keySet()) {
				try {
					String orderNumber = generalRepository.getOrderNumberForID(dealerOrderID);
					Criteria criteria = new Criteria(MongoConstants.MONGO_FIELD_DEALERORDER_NUMBER).is(orderNumber)
							.and(MongoConstants.MONGO_FIELD_CUSTOMER_ID).is(dealerOrderCustomerMap.get(dealerOrderID));
					Query query = new Query(criteria);
					log.info("Finding if dealer_order_id={} exists in collection={} using count query", dealerOrderID,
							collection);
					long count = mongoTemplate.count(query, collection);
					if (count > 0l) {
						Set<String> collectionList = dealerOrderCollectionMap.get(dealerOrderID);
						if (collectionList == null) {
							collectionList = new HashSet<String>();
						}
						collectionList.add(collection);
						dealerOrderCollectionMap.put(dealerOrderID, collectionList);
					}
				} catch (Exception e) {
					log.warn(
							"Unable to find existence of dealer_order_id=\"{}\" customer_id=\"{}\" in collection=\"{}\" ",
							dealerOrderID, dealerOrderCustomerMap.get(dealerOrderID), collection, e);
				}
			}
			MDC.remove(MergeCustomerService.TABLE_NAME);
		}
		return dealerOrderCollectionMap;
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, List<String>> getCustomerMergeMongoDealerOrderSchema(){
		log.info("Finding Customer Merge related Mongo kaarmadb DealerOrder Schema");
		Set<String> collections = mongoTemplate.getCollectionNames();
		List<String> collectionsList = new ArrayList<>(collections);
		Collections.sort(collectionsList);
		HashMap<String, List<String>> result = new HashMap<String, List<String>>();
		for(String collection : collectionsList) {
			try {
				if (MergeIgnoreCollection.exists(collection) != null)
					continue;
				
				log.info("Finding existence of orderNumber field in collection=\"{}\" ",collection);
				
				List<String> sortField = new ArrayList<String>();
				sortField.add("_id");
				
				Query query = new Query();
				query.with(new Sort(Direction.DESC, sortField));
				
				HashMap<String, Object> document  = mongoTemplate.findOne(query, HashMap.class,collection);
				if (document != null)
					for (String key : document.keySet()) {
						if (key.equalsIgnoreCase(MongoConstants.MONGO_FIELD_DEALERORDER_NUMBER)) {
							if (!result.containsKey(collection)) {
								result.put(collection, new ArrayList<String>());
							}
							List<String> fields = result.get(collection);
							fields.add(key);
							result.put(collection, fields);
						}
					}
			} catch (Exception e) {
				log.warn("Exception occured while trying to find existence of orderNumber field in collection =\"{}\"",collection,e);
			}
			
		}
		return result;
		
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, List<String>> getCustomerMergeMongoThreadSchema(){
		log.info("Finding Customer Merge related Mongo kaarmadb Thread Schema");
		Set<String> collections = mongoTemplate.getCollectionNames();
		List<String> collectionsList = new ArrayList<>(collections);
		Collections.sort(collectionsList);
		HashMap<String, List<String>> result = new HashMap<String, List<String>>();	
		for (String collection : collectionsList) {
			try {
				if (MergeIgnoreCollection.exists(collection) != null)
					continue;
				log.info("Finding threadID field in collection=\"{}\" ",collection);
				
				List<String> sortField = new ArrayList<String>();
				sortField.add("_id");
				
				Query query = new Query();
				query.with(new Sort(Direction.DESC, sortField));
				HashMap<String, Object> document  = mongoTemplate.findOne(query, HashMap.class, collection);
				
				if (document != null)
					for (String key : document.keySet()) {
						if (key.equalsIgnoreCase(MongoConstants.MONGO_FIELD_THREAD_ID)) {
							if (!result.containsKey(collection)) {
								result.put(collection, new ArrayList<String>());
							}
							List<String> fields = result.get(collection);
							fields.add(key);
							result.put(collection, fields);
						}
					}
			} catch (Exception e) {
				log.warn("Exception occurred while trying to find existence of threadID field in collection=\"{}\"", collection,e);
			}
		}
		return result;
		
	}
}

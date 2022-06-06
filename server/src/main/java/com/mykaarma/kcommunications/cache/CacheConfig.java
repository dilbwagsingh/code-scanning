package com.mykaarma.kcommunications.cache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class CacheConfig {

	private final static Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);

	public final static String DEALER_UUID_CACHE = "dealer_uuid_cache";
	public final static String DEALER_UUID_CACHE_KEY="DEALER_UUID";
	
	public final static String DEALER_UUID_DEALER_ID_CACHE = "dealer_uuid_dealer_id_cache";
	public final static String DEALER_UUID_DEALER_ID_CACHE_KEY="DEALER_UUID_DEALER_ID";
	
	public final static String DEALER_ID_FOR_DEPARTMENT_UUID_CACHE = "dealer_id_department_uuid_cache";
	public final static String DEALER_ID_FOR_DEPARTMENT_UUID_CACHE_KEY="DEALER_ID_DEPARTMENT_UUID";
	
	public final static String CUSTOMER_UUID_CUSTOMER_ID_CACHE = "customer_uuid_customer_id_cache";
	public final static String CUSTOMER_UUID_CUSTOMER_ID_CACHE_KEY="CUSTOMER_ID_CUSTOMER_UUID";

	public final static String DEALER_DEPARTMENT_UUID_CACHE = "dealer_department_uuid_cache";
	public final static String DEALER_DEPARTMENT_UUID_CACHE_KEY="DEALER_DEPARTMENT_UUID";

	public final static String DEALER_ASSOCIATE_UUID_CACHE = "dealer_associate_uuid_cache";
	public final static String DEALER_ASSOCIATE_UUID_CACHE_KEY="DEALER_ASSOCIATE_UUID";
	
	public final static String DEALER_ASSOCIATE_UUID_ID_CACHE = "dealer_associate_uuid_id_cache";
	public final static String DEALER_ASSOCIATE_UUID_ID_CACHE_KEY="DEALER_ASSOCIATE_UUID_ID";

	public final static String DEALER_ASSOCIATE_CACHE = "dealer_associate_cache";
	public final static String DEALER_ASSOCIATE_CACHE_KEY="DEALER_ASSOCIATE";
	
	public final static String DEALER_ASSOCIATE_FOR_DA_UUID_CACHE = "dealer_associate_for_da_uuid_cache";
	public final static String DEALER_ASSOCIATE_FOR_DA_UUID_CACHE_KEY="DEALER_ASSOCIATE_FOR_DA_UUID";

	public final static String DEALERS_CACHE = "dealers_cache";
	public final static String DEALERS_CACHE_KEY="DEALERS";

	public final static String DEFAULT_DEALER_ASSOCIATE_CACHE = "default_dealer_associate_cache";
	public final static String DEFAULT_DEALER_ASSOCIATE_CACHE_KEY="DEFAULT_DEALER_ASSOCIATE";

	public final static String DEALER_DEPARTMENT_CACHE = "dealer_department_cache";
	public final static String DEALER_DEPARTMENT_CACHE_KEY="DEALER_DEPARTMENT";

	public final static String DSO_CACHE = "dso_cache";
	public final static String DSO_CACHE_KEY="DSO";
	
	public final static String DEALER_ASSOCIATES_FOR_DEPARTMENT_UUID_CACHE = "dealer_associates_for_department_uuid_cache";
	public final static String DEALER_ASSOCIATES_FOR_DEPARTMENT_UUID_CACHE_KEY="DEALER_ASSOCIATES_FOR_DEPARTMENT_UUID";
	
	public final static String DEALER_ASSOCIATES_FOR_DEALER_UUID_CACHE = "dealer_associates_for_dealer_uuids_cache";
	public final static String DEALER_ASSOCIATES_FOR_DEALER_UUID_CACHE_KEY="DEALER_ASSOCIATES_FOR_dealer_UUIDS";
	
	public final static String DSO_LIST_CACHE = "dso_list_cache";
	public final static String DSO_LIST_CACHE_KEY="DSO_LIST";
	
	public final static String VOICE_CREDENTIALS_LIST_CACHE = "voice_credentials_list_cache";
	public final static String VOICE_CREDENTIALS_LIST_CACHE_KEY="VOICE_CREDENTIALS_LIST_CACHE";
	
	public final static String DEALER_ID_FOR_ACCOUNTSID_CACHE = "dealer_id_for_accountsid";
	public final static String DEALER_ID_FOR_ACCOUNTSID_CACHE_KEY = "DEALER_ID_FOR_ACCOUNTSID_CACHE";
	
	public final static String DEALER_SUBACCOUNT_CACHE = "dealer_subaccount_cache";
	public final static String DEALER_SUBACCOUNT_CACHE_KEY="DEALER_SUBACCOUNT";
	
	public final static String SERVICE_SUBSCRIBER_CACHE = "service_subscriber_cache";
	public final static String SERVICE_SUBSCRIBER_CACHE_KEY="SERVICE_SUBSCRIBER_CACHE";

	public final static String DEALER_ID_DEALER_UUID_CACHE = "dealer_id_dealer_uuid_cache";
	public final static String DEALER_ID_DEALER_UUID_KEY="DEALER_ID_DEALER_UUID_CACHE";
	
	public final static String CUSTOMER_ID_CUSTOMER_UUID_CACHE = "customer_id_customer_uuid_cache";
	public final static String CUSTOMER_ID_CUSTOMER_UUID_KEY="CUSTOMER_ID_CUSTOMER_UUID_CACHE";
	
	public final static String DEPARTMENT_UUID_DEALER_ID_CACHE = "department_uuid_dealer_id_cache";
	public final static String DEPARTMENT_UUID_DEALER_ID_KEY="DEPARTMENT_UUID_DEALER_ID_CACHE";
	
	public final static String DEPARTMENT_UUID_FROM_DEPARTMENT_ID_CACHE = "department_uuid_from_department_id_cache";
	public final static String DEPARTMENT_UUID_FROM_DEPARTMENT_ID_CACHE_KEY="DEPARTMENT_UUID_FROM_DEPARTMENT_ID_CACHE";
	
	public final static String TEXT_TRANSLATION_CACHE = "text_translation_cache";
	public final static String TEXT_TRANSLATION_KEY = "TEXT_TRANSLATION_CACHE";
	
	public final static String SUPPORTED_TRANSLATION_LANGUAGES_CACHE = "SUPPORTED_TRANSLATION_LANGUAGES_CACHE";
	
	public final static String DSO_KMANAGE_CACHE = "dso_kmanage_cache";
	public final static String DSO_KMANAGE_CACHE_KEY="DSO_KMANAGE_CACHE";
	
	public final static String DSO_KMANAGE_FOR_MULTIPLE_KEYS_CACHE = "dso_kmanage_for_multiple_keys_cache";
	public final static String DSO_KMANAGE_FOR_MULTIPLE_KEYS_CACHE_KEY="DSO_KMANAGE_FOR_MULTIPLE_KEYS_CACHE";
	
	public final static String DEPARTMENT_ID_FROM_DEPARTMENT_UUID_CACHE = "department_id_from_department_uuid_cache";
	public final static String DEPARTMENT_ID_FROM_DEPARTMENT_UUID_CACHE_KEY = "DEPARTMENT_ID_FROM_DEPARTMENT_UUID_CACHE";
	
	public final static String COMMUNICATIONS_TEXT_TRANSLATION_CACHE = "communications_text_translation_cache";
	public final static String COMMUNICATIONS_TEXT_TRANSLATION_CACHE_KEY = "COMMUNICATIONS_TEXT_TRANSLATION_CACHE";
	
	public final static String DEPARTMENT_GROUP_KMANAGE_CACHE = "department_group_kmanage_cache";
	public final static String DEPARTMENT_GROUP_KMANAGE_CACHE_KEY = "DEPARTMENT_GROUP_KMANAGE_CACHE";

	public static final String FEATURE_KMANAGE_CACHE = "feature_kmanage_cache";
	public static final String FEATURE_KMANAGE_CACHE_KEY = "FEATURE_KMANAGE_CACHE";

	public static final String ALL_DEPARTMENT_ID_NAME_FOR_DEALER_CACHE = "all_department_id_name_for_dealer_cache";
	public static final String ALL_DEPARTMENT_ID_NAME_FOR_DEALER_CACHE_KEY = "ALL_DEPARTMENT_ID_NAME_FOR_DEALER_CACHE";

	public static final String DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_ID_CACHE = "DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_ID_CACHE";
	public static final String DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_ID_CACHE_KEY = "DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_ID_CACHE_KEY";

	public static final String DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_UUID_CACHE = "DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_UUID_CACHE";
	public static final String DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_UUID_CACHE_KEY = "DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_UUID_CACHE_KEY";

	public static final String DEALER_ID_FOR_CUSTOMER_ID_CACHE = "DEALER_ID_FOR_CUSTOMER_ID_CACHE";
	public static final String DEALER_ID_FOR_CUSTOMER_ID_CACHE_KEY = "DEALER_ID_FOR_CUSTOMER_ID_CACHE_KEY";

	public static final String DEALER_ASSOCIATE_AUTHORITY_CACHE = "DEALER_ASSOCIATE_AUTHORITY_CACHE";
	public static final String DEALER_ASSOCIATE_AUTHORITY_CACHE_KEY = "DEALER_ASSOCIATE_AUTHORITY_CACHE_KEY";

	public static final String DEALER_ASSOCIATES_AUTHORITIES_CACHE = "DEALER_ASSOCIATES_AUTHORITIES_CACHE";
	public static final String DEALER_ASSOCIATES_AUTHORITIES_CACHE_KEY = "DEALER_ASSOCIATES_AUTHORITIES_CACHE_KEY";

	public static final String DEALER_ASSOCIATE_GROUP_FOR_DA_CACHE = "DEALER_ASSOCIATE_GROUP_FOR_DA_CACHE";
	public static final String DEALER_ASSOCIATE_GROUP_FOR_DA_CACHE_KEY = "DEALER_ASSOCIATE_GROUP_FOR_DA_CACHE_KEY";
	
	public static final String THREAD_COUNT_FOR_DEALER_ASSOCIATE_ID_CACHE = "THREAD_COUNT_FOR_DEALER_ASSOCIATE_ID_CACHE";
	public static final String THREAD_COUNT_FOR_DEALER_ASSOCIATE_ID_CACHE_KEY = "THREAD_COUNT_FOR_DEALER_ASSOCIATE_ID_CACHE_KEY";
	
	public static final String CUSTOMER_MERGING_MONGO_THREAD_SCHEMA_CACHE = "CUSTOMER_MERGING_MONGO_THREAD_SCHEMA_CACHE";
	public static final String CUSTOMER_MERGING_MONGO_THREAD_SCHEMA_CACHE_KEY = "CUSTOMER_MERGING_MONGO_THREAD_SCHEMA_CACHE_KEY";
	
	public static final String CUSTOMER_MERGING_MONGO_DEALERORDER_SCHEMA_CACHE = "CUSTOMER_MERGING_MONGO_DEALERORDER_SCHEMA_CACHE";
	public static final String CUSTOMER_MERGING_MONGO_DEALERORDER_SCHEMA_CACHE_KEY = "CUSTOMER_MERGING_MONGO_DEALERORDER_SCHEMA_CACHE_KEY";
	
	public final int MAX_SIZE = 50000;

	@Bean
	public CacheManager cacheManager() {

		LOGGER.info("Initializing simple Caffenine Cache manager.");
		CaffeineCache dealerUUID = new CaffeineCache(DEALER_UUID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());

		CaffeineCache dealerDepartmentUUID = new CaffeineCache(DEALER_DEPARTMENT_UUID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());

		CaffeineCache dealerDepartment = new CaffeineCache(DEALER_DEPARTMENT_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dealerIDForDepartmentUUID = new CaffeineCache(DEALER_ID_FOR_DEPARTMENT_UUID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache customerUUIDForCustomerID = new CaffeineCache(CUSTOMER_UUID_CUSTOMER_ID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dsoListCache = new CaffeineCache(DSO_LIST_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dealerAssociatesForDepartmentUUIDCache = new CaffeineCache(DEALER_ASSOCIATES_FOR_DEPARTMENT_UUID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(2, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dealerAssociatesForDealerUUIDCache = new CaffeineCache(DEALER_ASSOCIATES_FOR_DEALER_UUID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(2, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dealerAssociateUUID = new CaffeineCache(DEALER_ASSOCIATE_UUID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache serviceSubscriber = new CaffeineCache(SERVICE_SUBSCRIBER_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(3, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE).build());

		CaffeineCache dealerAssociate = new CaffeineCache(DEALER_ASSOCIATE_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dealerAssociateForDAUUID = new CaffeineCache(DEALER_ASSOCIATE_FOR_DA_UUID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dealerAssociateUuidForID = new CaffeineCache(DEALER_ASSOCIATE_UUID_ID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache voiceCredentialsList = new CaffeineCache(VOICE_CREDENTIALS_LIST_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dealerIDFromAccountSid = new CaffeineCache(DEALER_ID_FOR_ACCOUNTSID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());

		CaffeineCache dealers = new CaffeineCache(DEALERS_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());

		CaffeineCache defaultDealerAssociate = new CaffeineCache(DEFAULT_DEALER_ASSOCIATE_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dso = new CaffeineCache(DSO_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE).build());

		CaffeineCache dealerSubAccount = new CaffeineCache(DEALER_SUBACCOUNT_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dealerIdForUuid = new CaffeineCache(DEALER_ID_DEALER_UUID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache customerIdForUuid = new CaffeineCache(CUSTOMER_ID_CUSTOMER_UUID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache departmentUuidForDealerId = new CaffeineCache(DEPARTMENT_UUID_DEALER_ID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache departmentUuidFromDepartmentId = new CaffeineCache(DEPARTMENT_UUID_FROM_DEPARTMENT_ID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dsoKmanage = new CaffeineCache(DSO_KMANAGE_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dsoKmanageForMultipleKeys = new CaffeineCache(DSO_KMANAGE_FOR_MULTIPLE_KEYS_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache dealerUUIDFromID = new CaffeineCache(DEALER_UUID_DEALER_ID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache textTranslationCache = new CaffeineCache(TEXT_TRANSLATION_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());

		CaffeineCache departmentIDFromDepartmentUUID = new CaffeineCache(DEPARTMENT_ID_FROM_DEPARTMENT_UUID_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache communicationsTextTranslationCache = new CaffeineCache(COMMUNICATIONS_TEXT_TRANSLATION_CACHE,
			Caffeine.newBuilder()
			.expireAfterWrite(1, TimeUnit.DAYS)
			.maximumSize(MAX_SIZE).build());
		
		CaffeineCache supportedTranslationLanguagesCache = new CaffeineCache(SUPPORTED_TRANSLATION_LANGUAGES_CACHE,
				Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE).build());

		CaffeineCache departmentGroupCache = new CaffeineCache(DEPARTMENT_GROUP_KMANAGE_CACHE,
			Caffeine.newBuilder()
			.expireAfterWrite(1, TimeUnit.DAYS)
			.maximumSize(MAX_SIZE).build());

		CaffeineCache featureCache = new CaffeineCache(FEATURE_KMANAGE_CACHE,
			Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());

		CaffeineCache allDepartmentIDNameForDealerCache = new CaffeineCache(ALL_DEPARTMENT_ID_NAME_FOR_DEALER_CACHE,
			Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.DAYS)
				.maximumSize(MAX_SIZE).build());
		
		CaffeineCache departmentUuidForDealerAssociateIdCache = new CaffeineCache(DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_ID_CACHE,
				Caffeine.newBuilder()
					.expireAfterWrite(1, TimeUnit.DAYS)
					.maximumSize(MAX_SIZE).build());

		CaffeineCache departmentUuidForDealerAssociateUuidCache = new CaffeineCache(DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_UUID_CACHE,
				Caffeine.newBuilder()
						.expireAfterWrite(1, TimeUnit.DAYS)
						.maximumSize(MAX_SIZE).build());

		CaffeineCache dealerIdForCustomerIdCache = new CaffeineCache(DEALER_ID_FOR_CUSTOMER_ID_CACHE,
				Caffeine.newBuilder()
						.expireAfterWrite(1, TimeUnit.DAYS)
						.maximumSize(MAX_SIZE).build());

		CaffeineCache dealerAssociateAuthorityCache = new CaffeineCache(DEALER_ASSOCIATE_AUTHORITY_CACHE,
				Caffeine.newBuilder()
						.expireAfterWrite(1, TimeUnit.HOURS)
						.maximumSize(MAX_SIZE).build());

		CaffeineCache dealerAssociatesAuthoritiesCache = new CaffeineCache(DEALER_ASSOCIATES_AUTHORITIES_CACHE,
				Caffeine.newBuilder()
						.expireAfterWrite(1, TimeUnit.HOURS)
						.maximumSize(MAX_SIZE).build());

		CaffeineCache dealerAssociateGroupForDaCache = new CaffeineCache(DEALER_ASSOCIATE_GROUP_FOR_DA_CACHE,
				Caffeine.newBuilder()
						.expireAfterWrite(2, TimeUnit.HOURS)
						.maximumSize(MAX_SIZE).build());
		
		CaffeineCache threadCountForDealerAssociateIdCache = new CaffeineCache(THREAD_COUNT_FOR_DEALER_ASSOCIATE_ID_CACHE,
				Caffeine.newBuilder()
					.expireAfterWrite(5, TimeUnit.MINUTES)
					.maximumSize(MAX_SIZE).build());
		
		CaffeineCache customerMergeMongoThreadSchemaCache = new CaffeineCache(CUSTOMER_MERGING_MONGO_THREAD_SCHEMA_CACHE,
				Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).maximumSize(MAX_SIZE).build());
		
		CaffeineCache customerMergeMongoDealerOrderSchemaCache = new CaffeineCache(CUSTOMER_MERGING_MONGO_THREAD_SCHEMA_CACHE,
				Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).maximumSize(MAX_SIZE).build());
		
		SimpleCacheManager manager = new SimpleCacheManager();
		List<CaffeineCache> caches = new ArrayList<CaffeineCache>();
		caches.add(dealerUUID);
		caches.add(dealerDepartmentUUID);
		caches.add(dealerDepartment);
		caches.add(dealerIDForDepartmentUUID);
		caches.add(customerUUIDForCustomerID);
		caches.add(dealerAssociateUUID);
		caches.add(dealerAssociateForDAUUID);
		caches.add(dealerAssociate);
		caches.add(dealers);
		caches.add(defaultDealerAssociate);
		caches.add(serviceSubscriber);
		caches.add(dso);
		caches.add(voiceCredentialsList);
		caches.add(dealerIDFromAccountSid);
		caches.add(dealerAssociateUuidForID);
		caches.add(dsoListCache);
		caches.add(dealerAssociatesForDepartmentUUIDCache);
		caches.add(dealerAssociatesForDealerUUIDCache);
		caches.add(dealerSubAccount);
		caches.add(dealerIdForUuid);
		caches.add(customerIdForUuid);
		caches.add(departmentUuidForDealerId);
		caches.add(departmentUuidFromDepartmentId);
		caches.add(departmentIDFromDepartmentUUID);
		caches.add(dsoKmanage);
		caches.add(dsoKmanageForMultipleKeys);
		caches.add(dealerUUIDFromID);
		caches.add(textTranslationCache);
		caches.add(communicationsTextTranslationCache);
		caches.add(supportedTranslationLanguagesCache);
		caches.add(departmentGroupCache);
		caches.add(featureCache);
		caches.add(allDepartmentIDNameForDealerCache);
		caches.add(departmentUuidForDealerAssociateIdCache);
		caches.add(departmentUuidForDealerAssociateUuidCache);
		caches.add(dealerIdForCustomerIdCache);
		caches.add(customerMergeMongoThreadSchemaCache);
		caches.add(customerMergeMongoDealerOrderSchemaCache);
		caches.add(threadCountForDealerAssociateIdCache);
		caches.add(dealerAssociateAuthorityCache);
		caches.add(dealerAssociatesAuthoritiesCache);
		caches.add(dealerAssociateGroupForDaCache);
		manager.setCaches(caches);

		LOGGER.info("Setuped caffeine in memory cache");
		return manager;
	}

	@Bean
	public KeyGenerator customKeyGenerator() {
		return new KeyGenerator() {

			@Override
			public Object generate(Object o, Method method, Object... objects) {
				if((method.getName().equalsIgnoreCase("getDealerUUIDForID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("AppConfigHelper")))
				{
					return getDealerUUIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerDepartmentUUIDForID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("AppConfigHelper")))
				{
						return getDealerDepartmentUUIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDepartmentIDForUUID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")))
				{
						return getDepartmentIDForDepartmentUUIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerIDFromDepartmentUUID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")))
				{
						return getDealerIDFromDepartmentUUIDCacheyKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDepartmentUUIDForDepartmentID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")))
				{
						return getDepartmentUUIDForDepartmentIDCacheyKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getCustomerUUIDFromCustomerID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")))
				{
						return getCustomerUUIDFromCustomerIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getCustomerIDForUUID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")))
				{
						return getCustomerIDFromCustomerUUIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDepartmentUUIDForDealerID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")))
				{
						return getDepartmentUUIDForDealerIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerIdFromDealerUUID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")))
				{
						return getDealerIDFromDealerUUIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerUUIDFromDealerId") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")))
				{
						return getDealerUUIDFromDealerIdCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerAssociateUUIDForID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("AppConfigHelper")))
				{
						return getDealerAssociateUUIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("findFirstServiceSubscriberByUserName") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")))
				{
						return getServiceSubscriberCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerDepartment") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")))
				{
						return getDealerDepartmentCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerAssociate") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")))
				{
						return getDealerAssociateCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerAssociateForDealerAssociateUUID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")))
				{
						return getDealerAssociateForDAUUIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealersForUUID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")))
				{
						return getDealersCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerAssociateUuidFromDealerAssociateId") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")))
				{
						return getDealerAssociateUuidForIdCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getAllDealerAssociatesForDepartmentUUID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")))
				{
						return getAllDealerAssociatesForDepartmentUUIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getAllDealerAssociatesForDealerUUID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")))
				{
						return getAllDealerAssociatesForDealerUUIDCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerSetupOptionValueForADealer") || method.getName().equalsIgnoreCase("getAndUpdateDealerSetupOptionValueInCache")) && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper"))
				{
						return getDealerSetupOptionValueForADealerCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerSetupOptionValuesForADealer") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")))
				{
						return getDealerSetupOptionValueForMultipleKeysCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerSetupOptionsFromConfigService") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("AppConfigHelper")))
				{
						return getDSOListCacheKey(objects);
				}
				if (method.getName().equalsIgnoreCase("getTranslatedText") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("AppConfigHelper")) {
					return getTextTranslationCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("findAllByDeptID") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("VoiceCredentialsRepository")))
				{
						return getVoiceCredentialsForDepartmentListCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerIDForAccountSid") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("VoiceCredentialsRepository")))
				{
						return getDealerIDForAccountSidCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDefaultDealerAssociateForDepartment") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")))
				{
						return getDefaultDealerAssociateCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDefaultDealerAssociateForDepartment") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper"))) {
					return getDSOCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerSetupOptionValueFromConfigService") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("AppConfigHelper"))) {
					return getDSOCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getDealerSubAccountBySid") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KCommunicationsUtils"))) {
					return getDealerSubAccountCacheKey(objects);
				}
				if((method.getName().equalsIgnoreCase("getCustomerMergeMongoThreadSchema") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KCommunicationsUtils"))) {
					return CUSTOMER_MERGING_MONGO_THREAD_SCHEMA_CACHE_KEY;
				}
				if((method.getName().equalsIgnoreCase("getCustomerMergeMongoDealerOrderSchema") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KCommunicationsUtils"))) {
					return CUSTOMER_MERGING_MONGO_DEALERORDER_SCHEMA_CACHE_KEY;
				}
				if (method.getName().equalsIgnoreCase("getTranslatedText") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("UIElementTranslationRepository")) {
					return getCommunicationsTextTranslationCacheKey(objects);
				}
				if (method.getName().equalsIgnoreCase("getTranslateLanguages") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")) {
					return getSupportedTranslationLanguagesCacheKey(objects);
				}
				if(method.getName().equalsIgnoreCase("getDepartmentGroupByFeature") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")) {
					return getDepartmentGroupCacheKey(objects);
				}
				if(method.getName().equalsIgnoreCase("getFeatureByKey") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")) {
					return getFeatureCacheKey(objects);
				}
				if(method.getName().equalsIgnoreCase("getAllDepartmentIDAndNameForDealerId") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")) {
					return getAllDepartmentIdNameForDealerCacheKey(objects);
				}
				if(method.getName().equalsIgnoreCase("getDepartmentUuidForDealerAssociateId") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")) {
					return getDepartmentUuidForDealerAssociateIdCacheKey(objects);
				}
				if(method.getName().equalsIgnoreCase("getDepartmentUuidForDealerAssociateUuid") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")) {
					return getDepartmentUuidForDealerAssociateUuidCacheKey(objects);
				}
				if(method.getName().equalsIgnoreCase("getDealerIdForCustomerId") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("GeneralRepository")) {
					return getDealerIdForCustomerIdCacheKey(objects);
				}
				if(method.getName().equalsIgnoreCase("getThreadCountForDealerAssociateId") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("ThreadRepository")) {
					return getThreadCountForDealerAssociateIdCacheKey(objects);
				}
				if(method.getName().equalsIgnoreCase("checkDealerAssociateAuthority") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")) {
					return getDealerAssociateAuthorityCacheKey(objects);
				}
				if(method.getName().equalsIgnoreCase("getDealerAssociatesAuthoritiesDTO") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")) {
					return getDealerAssociatesAuthoritiesCacheKey(objects);
				}
				if(method.getName().equalsIgnoreCase("getDealerAssociateGroupForDA") && method.getDeclaringClass().getSimpleName().equalsIgnoreCase("KManageApiHelper")) {
					return getDealerAssociateGroupForDaCacheKey(objects);
				}
				LOGGER.error("attempted to get cached value for incorrect method={} and class={}  pair", method.getName(), method.getDeclaringClass().getSimpleName());
				return UUID.randomUUID();
			}
		};
	}
	

	public static String getDepartmentGroupCacheKey(Object[] objects) {
		String key  = DEPARTMENT_GROUP_KMANAGE_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getFeatureCacheKey(Object[] objects) {
		String key  = FEATURE_KMANAGE_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getAllDepartmentIdNameForDealerCacheKey(Object[] objects) {
		String key  = ALL_DEPARTMENT_ID_NAME_FOR_DEALER_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDepartmentIDForDepartmentUUIDCacheKey(Object[] objects) {
		String key  = DEPARTMENT_ID_FROM_DEPARTMENT_UUID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDepartmentUUIDForDepartmentIDCacheyKey(Object[] objects) {
		
		String key  = DEPARTMENT_UUID_FROM_DEPARTMENT_ID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getDealerDepartmentCacheKey(Object[] objects) {
		
		String key  = DEALER_DEPARTMENT_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getDealerUUIDFromDealerIdCacheKey(Object[] objects) {
		
		String key  = DEALER_UUID_DEALER_ID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getDealerAssociateUuidForIdCacheKey(Object[] objects) {
		String key  = DEALER_ASSOCIATE_UUID_ID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerSetupOptionValueForADealerCacheKey(Object[] objects) {
		
		String key  = DSO_KMANAGE_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerSetupOptionValueForMultipleKeysCacheKey(Object[] objects) {
		
		String key  = DSO_KMANAGE_FOR_MULTIPLE_KEYS_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerIDFromDealerUUIDCacheKey(Object[] objects) {
		
		String key  = DEALER_ID_DEALER_UUID_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getCustomerIDFromCustomerUUIDCacheKey(Object[] objects) {
		
		String key  = CUSTOMER_ID_CUSTOMER_UUID_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getServiceSubscriberCacheKey(Object...objects)
	{
		String key  = DEALER_UUID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getAllDealerAssociatesForDepartmentUUIDCacheKey(Object...objects)
	{
		String key =  DEALER_ASSOCIATES_FOR_DEPARTMENT_UUID_CACHE_KEY ;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		LOGGER.info("getAllDealerAssociatesForDepartmentUUIDCacheKey key={}",key);
		return key;
	}
	
	public static String getAllDealerAssociatesForDealerUUIDCacheKey(Object...objects)
	{
		String key =  DEALER_ASSOCIATES_FOR_DEALER_UUID_CACHE_KEY ;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		LOGGER.info("getAllDealerAssociatesForDealerUUIDCacheKey key={}",key);
		return key;
	}
	
	public static String getDealerUUIDCacheKey(Object...objects)
	{
		String key  = DEALER_UUID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerDepartmentUUIDCacheKey(Object...objects)
	{
		String key  = DEALER_DEPARTMENT_UUID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getDSOListCacheKey(Object...objects)
	{
		String key  = DSO_LIST_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getTextTranslationCacheKey(Object...objects) {
		String key = TEXT_TRANSLATION_KEY;
		for (Object obj : objects) {
			key = key + ":" + obj.toString();
		}
		return key;
	}
	
	public static String getCommunicationsTextTranslationCacheKey(Object...objects) {
		String key = COMMUNICATIONS_TEXT_TRANSLATION_CACHE_KEY;
		for (Object obj : objects) {
			key = key + ":" + obj.toString();
		}
		return key;
	}
	
	public static String getSupportedTranslationLanguagesCacheKey(Object...objects) {
		String key = SUPPORTED_TRANSLATION_LANGUAGES_CACHE;
		for (Object obj : objects) {
			key = key + ":" + obj.toString();
		}
		return key;
	}
	public static String getDealerIDFromDepartmentUUIDCacheyKey(Object...objects)
	{
		String key  = DEALER_ID_FOR_DEPARTMENT_UUID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		LOGGER.info("getDealerIDFromDepartmentUUIDCacheyKey key={}",key);
		return key;
	}
	
	public static String getCustomerUUIDFromCustomerIDCacheKey(Object...objects)
	{
		String key  = CUSTOMER_UUID_CUSTOMER_ID_CACHE;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerAssociateUUIDCacheKey(Object...objects)
	{
		String key  = DEALER_ASSOCIATE_UUID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerAssociateCacheKey(Object...objects)
	{
		String key  = DEALER_ASSOCIATE_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getDealerAssociateForDAUUIDCacheKey(Object...objects)
	{
		String key  = DEALER_ASSOCIATE_FOR_DA_UUID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}


	public static String getDealersCacheKey(Object...objects)
	{
		String key  = DEALERS_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDefaultDealerAssociateCacheKey(Object...objects)
	{
		String key  = DEFAULT_DEALER_ASSOCIATE_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		LOGGER.info("getDefaultDealerAssociateCacheKey key={}",key);
		return key;
	}
	
	public static String getDSOCacheKey(Object...objects)
	{
		String key  = DSO_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getVoiceCredentialsForDepartmentListCacheKey(Object...objects)
	{
		String key  = VOICE_CREDENTIALS_LIST_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getDealerIDForAccountSidCacheKey(Object...objects)
	{
		String key  = DEALER_ID_FOR_ACCOUNTSID_CACHE;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerSubAccountCacheKey(Object...objects)
	{
		String key  = DEALER_SUBACCOUNT_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDepartmentUUIDForDealerIDCacheKey(Object...objects)
	{
		String key  = DEPARTMENT_UUID_DEALER_ID_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getDepartmentUuidForDealerAssociateIdCacheKey(Object...objects)
	{
		String key  = DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_ID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDepartmentUuidForDealerAssociateUuidCacheKey(Object...objects) {
		String key  = DEPARTMENT_UUID_FOR_DEALER_ASSOCIATE_UUID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerIdForCustomerIdCacheKey(Object...objects) {
		String key  = DEALER_ID_FOR_CUSTOMER_ID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerAssociateAuthorityCacheKey(Object...objects) {
		String key  = DEALER_ASSOCIATE_AUTHORITY_CACHE_KEY;
		for(Object obj : objects) {
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerAssociatesAuthoritiesCacheKey(Object...objects) {
		String key  = DEALER_ASSOCIATES_AUTHORITIES_CACHE_KEY;
		for(Object obj : objects) {
			key = key+":"+obj.toString();
		}
		return key;
	}

	public static String getDealerAssociateGroupForDaCacheKey(Object...objects) {
		String key  = DEALER_ASSOCIATE_GROUP_FOR_DA_CACHE_KEY;
		for(Object obj : objects) {
			key = key+":"+obj.toString();
		}
		return key;
	}
	
	public static String getThreadCountForDealerAssociateIdCacheKey(Object...objects)
	{
		String key  = THREAD_COUNT_FOR_DEALER_ASSOCIATE_ID_CACHE_KEY;
		for(Object obj : objects)
		{
			key = key+":"+obj.toString();
		}
		return key;
	}
	
}

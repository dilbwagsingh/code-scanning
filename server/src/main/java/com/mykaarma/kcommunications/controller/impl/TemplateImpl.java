package com.mykaarma.kcommunications.controller.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.MessageProtocol;
import com.mykaarma.kcommunications.elasticsearch.model.Template;
import com.mykaarma.kcommunications.jpa.repository.EmailTemplateRepository;
import com.mykaarma.kcommunications.jpa.repository.EmailTemplateTypeRepository;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.mapper.TemplateDTOMapper;
import com.mykaarma.kcommunications.model.jpa.EmailTemplate;
import com.mykaarma.kcommunications.model.jpa.EmailTemplateType;
import com.mykaarma.kcommunications.utils.ElasticSearchUtils;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.TemplateType;
import com.mykaarma.kcommunications_model.dto.TemplateDTO;
import com.mykaarma.kcommunications_model.request.CreateFreemarkerTemplatesRequest;
import com.mykaarma.kcommunications_model.request.DealersTemplateIndexRequest;
import com.mykaarma.kcommunications_model.request.EnableFreemarkerTemplatesRequest;
import com.mykaarma.kcommunications_model.request.TemplateTagsRequest;
import com.mykaarma.kcommunications_model.request.TemplateSearchRequest;
import com.mykaarma.kcommunications_model.response.TemplateTagsResponse;
import com.mykaarma.kcustomer_model.enums.QueryOperator;
import com.mykaarma.templateengine.utils.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TemplateImpl {
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired 
	RestTemplate restTemplate;
	
	@Autowired
	TemplateDTOMapper templateDTOMapper;
	
	@Autowired
	EmailTemplateTypeRepository emailTemplateTypeRepository; 
	
	@Autowired
	EmailTemplateRepository emailTemplateRepository;
	
	@Autowired
	Helper helper;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	@Value("${elastic_search_base_url:http://20.0.197.253:9201}")
	String elasticSearchBaseUrl;

	private static final int MAX_NGRAM_SIZE = 10;
	
	private static String FREEMARKER_TEMPLATE_TYPE_SUFFIX = "-FM";
	private static String DT_TEMPLATE = "DT";
	private static String DE_TEMPLATE = "DE";
	private static String DTZ_TEMPLATE = "DTZ";
	private static String DEZ_TEMPLATE = "DEZ";
	private static String DT_FM_TEMPLATE = "DT-FM";
	private static String DE_FM_TEMPLATE = "DE-FM";
	
	private ObjectMapper objectMapper=new ObjectMapper();
	
	private SimpleClientHttpRequestFactory getClientHttpRequestFactory() {
		SimpleClientHttpRequestFactory clientHttpRequestFactory
		= new SimpleClientHttpRequestFactory();
		//Connect timeout
		clientHttpRequestFactory.setConnectTimeout(10000);

		//Read timeout
		clientHttpRequestFactory.setReadTimeout(10000);
		return clientHttpRequestFactory;
	}
	
	private RestTemplate restTemplateSearch = new RestTemplate(getClientHttpRequestFactory());
	
	public void indexTemplate(TemplateType templateType,String templateUuid) throws Exception {
		if(TemplateType.AUTOMATIC.equals(templateType)) {
			
		} else if(TemplateType.MANUAL.equals(templateType)) {
			//currently only Text Manual Template is supported
			Template template=getManualTemplateForTemplateUuid(templateUuid);
			pushTemplateToElasticSearch(template);
		}
	}
	
	private Template getManualTemplateForTemplateUuid(String templateUuid) {
		Object[] templateDetails = generalRepository.getManualTempateForUuid(templateUuid);
		return getTemplateObject(templateDetails);
	}
	
	private Template getTemplateObject(Object[] templateDetails) {
		if(templateDetails==null) {
			return null;
		}
		Template template=new Template();
		if(templateDetails[0]!=null) {
			BigInteger templateId = (BigInteger)templateDetails[0];
			template.setId(templateId.longValue());
		}
		if(templateDetails[1]!=null) {
			String templateTitle = (String)templateDetails[1];
			template.setTitle(templateTitle);
		}
		if(templateDetails[2]!=null) {
			String templateBody = (String)templateDetails[2];
			template.setBody(templateBody);
		}
		if(templateDetails[3]!=null) {
			BigInteger dealerIdInt = (BigInteger)templateDetails[3];
			template.setDealerUuid(generalRepository.getDealerUUIDFromDealerId(dealerIdInt.longValue()));
		}
		if(templateDetails[4]!=null) {
			BigInteger dealerDepartmentIdInt = (BigInteger)templateDetails[4];
			template.setDepartmentUuid(generalRepository.getDepartmentUUIDForDepartmentID(dealerDepartmentIdInt.longValue()));
		}
		if(templateDetails[5]!=null) {
			String locale = (String)templateDetails[5];
			template.setLocale(getCleanedSearchTerm(locale));
		}
		
		if(templateDetails[6]!=null) {
			String slug = (String)templateDetails[6];
			template.setSlug(slug);
		}
		
		if(templateDetails[7]!=null) {
			String uuid = (String)templateDetails[7];
			template.setUuid(uuid);
		}
		
		if(templateDetails[8]!=null) {
			BigInteger sortOrder = (BigInteger)templateDetails[8];
			template.setSortOrder(sortOrder.intValue());
		}
		
		template.setIsManual(true);
		template.setProtocol(MessageProtocol.X.name());
		return template;
	}
	
	private Boolean pushTemplateToElasticSearch(Template template) throws Exception{

		Boolean isPushedToES = false;
		String TEMPLATE_PUT_ENDPOINT = String.format("%s/templates/_doc/", elasticSearchBaseUrl);
		Long endTime=0l,startTime=0l;
		try {

			HttpEntity<Template> requestEntity = new HttpEntity<Template>(template);
			String url = String.format("%s%d", TEMPLATE_PUT_ENDPOINT, template.getId());
			startTime = System.currentTimeMillis();
			log.info("ES URL = " + url);
			log.info("ES requestEntity = " + requestEntity);
			ResponseEntity<LinkedHashMap> response = restTemplateSearch.exchange(url, HttpMethod.PUT, requestEntity, LinkedHashMap.class);

			LinkedHashMap<?, ?> mapResponse = response.getBody();
			LinkedHashMap<?, ?> mapShards = (LinkedHashMap<?, ?>) mapResponse.get("_shards");
			Integer successful = (Integer) mapShards.get("successful");

			if(successful>=1) {
				isPushedToES = true;
			}
			else {
				throw new Exception("Indexing Failed because successful flag returned by ES < 1");
			}

			endTime = System.currentTimeMillis();

			log.info("Time taken to push Template to "
					+ "		ElasticSearch for dealer_uuid={} template_id={} template_uuid={} indexing_time={} ms"
					,	template.getDealerUuid(), template.getId(),
					template.getUuid(),(endTime-startTime)
					);

		} catch (Exception e) {

			log.warn("Exception in pushTemplateToElasticSearch "
					+ " for dealer_uuid={} template_id={} template_uuid={} MESSAGE_PUT_ENDPOINT={} "
					,template.getDealerUuid(), template.getId(),
					template.getUuid(), TEMPLATE_PUT_ENDPOINT
					,e); 
			throw e;
		}

		return isPushedToES;

	}
	
	
	public  List<TemplateDTO> searchTemplatesFromElasticSearch(String departmentUuid,TemplateSearchRequest templateSearchRequest) throws JsonProcessingException{
		List<String> dealerDepartmentUuidList = new ArrayList<String>();
		dealerDepartmentUuidList.add(departmentUuid);
		List<String> fieldsToSearch = new ArrayList<String>();
		fieldsToSearch.add(ElasticSearchUtils.TEMPLATE_BODY.getValue());
		fieldsToSearch.add(ElasticSearchUtils.TEMPLATE_TITLE.getValue());
		
		boolean isManual=true;
		if(TemplateType.AUTOMATIC.toString().equalsIgnoreCase(templateSearchRequest.getTemplateType())){
			isManual=false;
		}
		List<Template> listTemplateInfo = getTemplateSearchResults(templateSearchRequest.getTemplateSearchContext(), 
				dealerDepartmentUuidList, fieldsToSearch, templateSearchRequest.getLimit(), QueryOperator.OR,isManual,getCleanedSearchTerm(templateSearchRequest.getLocale()));
		log.info("in searchTemplatesFromElasticSearch response={}",new ObjectMapper().writeValueAsString(listTemplateInfo));
		
		if(listTemplateInfo!=null) {
			List<TemplateDTO> templateDTOList = templateDTOMapper.map(listTemplateInfo);
			return templateDTOList;
		}
		return null;
	}

	public List<Template> getTemplateSearchResults(String searchTerm, List<String> dealerDepartmentUuidList, List<String> fieldsToSearch,
			int limit, QueryOperator qo,boolean isManual,String locale) {
		List<Template> listTemplate = new ArrayList<Template>();
		Long t1 = 0l, t2 = 0l;
		try {

			if(searchTerm!=null && !searchTerm.isEmpty()) {
				searchTerm = removeWildcardChars(searchTerm);
			}
			
			String MESSAGE_SEARCH_ENDPOINT = String.format(
					"%s/templates/_search?filter_path=hits.hits._source,_shards.successful",
					elasticSearchBaseUrl);
			String searchQuery = "";
			searchQuery = getMessageTermGenericSearchQuery(searchTerm, dealerDepartmentUuidList, fieldsToSearch,
					limit,qo,isManual,locale);

			log.info(searchQuery);
			t1 = System.currentTimeMillis();
			ResponseEntity<LinkedHashMap> response = null;
			try {
				response = getMessageSearchResponse(searchQuery, MESSAGE_SEARCH_ENDPOINT);
			} catch (HttpClientErrorException e) {
				if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
					log.info("search term having special chars - search_term=\"{}\" dealer_department_id={}", searchTerm,
							(dealerDepartmentUuidList.size()==1?dealerDepartmentUuidList.get(0):dealerDepartmentUuidList));
					searchTerm = getCleanedSearchTerm(searchTerm);
					searchQuery = getMessageTermGenericSearchQuery(searchTerm, dealerDepartmentUuidList, fieldsToSearch,
							limit,QueryOperator.AND,isManual,locale);
					response = getMessageSearchResponse(searchQuery, MESSAGE_SEARCH_ENDPOINT);
					log.info(
							"search term having special chars - after cleaning success search_term=\"{}\" dealer_department_id={}",
							searchTerm, (dealerDepartmentUuidList.size()==1?dealerDepartmentUuidList.get(0):dealerDepartmentUuidList));
				} else {
					throw e;
				}
			} catch (Exception e) {
				throw e;
			}
			log.info("response received={}",new ObjectMapper().writeValueAsString(response));
			LinkedHashMap<?, ?> mapResponse = response.getBody();
			LinkedHashMap<?, ?> mapShards = (LinkedHashMap<?, ?>) mapResponse.get("_shards");
			Integer successful = (Integer) mapShards.get("successful");
			if (successful >= 1) {
				LinkedHashMap<?, ?> mapHits = (LinkedHashMap<?, ?>) mapResponse.get("hits");
				if (mapHits != null) {
					List<LinkedHashMap<?, ?>> listHitsHits = (List<LinkedHashMap<?, ?>>) mapHits.get("hits");
					ObjectMapper objectMapper = new ObjectMapper();
					objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
					if (listHitsHits != null && !listHitsHits.isEmpty()) {
						log.info(String.format("dealer__department_id=%s search_term=\"%s\" Total hits=%d", (dealerDepartmentUuidList.size()==1?dealerDepartmentUuidList.get(0):dealerDepartmentUuidList), searchTerm,
								listHitsHits.size()));
						for (LinkedHashMap<?, ?> mapSource : listHitsHits) {
							LinkedHashMap<?, ?> mapResult = (LinkedHashMap<?, ?>) mapSource.get("_source");
							Template messageLite = objectMapper.convertValue(mapResult, Template.class);
							listTemplate.add(messageLite);

						}
					}
				}
			} else {
				throw new Exception("Error in searching");
			}
		} catch (Exception e) {

			log.error(String.format(
					"Error in fetching customers from elasticsearch for Dealer. dealer_department_id=%s search_term=\"%s\" ",
					(dealerDepartmentUuidList.size()==1?dealerDepartmentUuidList.get(0):dealerDepartmentUuidList), searchTerm), e);
		}
		t2 = System.currentTimeMillis();
		log.info(String.format("Time taken to get results from ElasticSearch for "
				+ "dealer_department_id=%s search_term=\"%s\" search_time=%d ms", (dealerDepartmentUuidList.size()==1?dealerDepartmentUuidList.get(0):dealerDepartmentUuidList), searchTerm, (t2 - t1)));
		return listTemplate;
	}

	public static String removeWildcardChars(String searchTerm) {
		Pattern pt = Pattern.compile(ElasticSearchUtils.WILDCHAR_REGEX.getValue());
		Matcher match = pt.matcher(searchTerm);
		while(match.find()) {
			String s = match.group();
			searchTerm=searchTerm.replaceAll("\\"+s, " ");
		}
		return searchTerm;
	}

	public Boolean deleteTemplateFromElasticSearch(String templateUuid,TemplateType templateType) throws Exception {
		Boolean isDeletedFromES = false;
		String TEMPLATE_PUT_ENDPOINT = String.format("%s/templates/_doc/", elasticSearchBaseUrl);
		Long endTime=0l,startTime=0l;
		try {
			if(TemplateType.AUTOMATIC.equals(templateType)) {
				// not handling for automatic templates as of now
				return isDeletedFromES;
			} else if(TemplateType.MANUAL.equals(templateType)) {
				//currently only Text Manual Template is supported
				Long templateId=generalRepository.getManualTemplateIdFromUuid(templateUuid);
				
				if(templateId!=null) {
					String url = String.format("%s%d", TEMPLATE_PUT_ENDPOINT,templateId);
					startTime = System.currentTimeMillis();
					log.info("ES URL = " + url);
					ResponseEntity<LinkedHashMap> response = restTemplateSearch.exchange(url, HttpMethod.DELETE, null, LinkedHashMap.class);

					LinkedHashMap<?, ?> mapResponse = response.getBody();
					LinkedHashMap<?, ?> mapShards = (LinkedHashMap<?, ?>) mapResponse.get("_shards");
					Integer successful = (Integer) mapShards.get("successful");

					if(successful>=1) {
						isDeletedFromES = true;
					}
					else {
						throw new Exception("Index deletion Failed because successful flag returned by ES < 1");
					}

					endTime = System.currentTimeMillis();

					log.info("Time taken to delete Template to "
							+ "		ElasticSearch for template_type={} template_uuid={} indexing_time={} ms"
							,	 templateType,templateUuid,(endTime-startTime)
							);
				} else {
					
				}
			}
			

		} catch (Exception e) {

			log.warn("Exception in deleting template from elastic search "
					+ " for template_type={} template_uuid={} TEMPLATE_PUT_ENDPOINT={} "
					,templateType,templateUuid, TEMPLATE_PUT_ENDPOINT
					,e); 
			throw e;
		}

		return isDeletedFromES;
	}
	
	public String getMessageTermGenericSearchQuery(String searchTerm, List<String> dealerDepartmentUuidList, 
			List<String> fieldsToSearch, int limit, QueryOperator qo,Boolean isManual,String locale) throws Exception{

		

		List<JSONObject> filterArray=new ArrayList<JSONObject>();
		
		if(dealerDepartmentUuidList != null && !dealerDepartmentUuidList.isEmpty()) {
			
			JSONObject  departmentJsonFilter = getTermsObject(ElasticSearchUtils.DEPARTMENT_UUID.getValue(), dealerDepartmentUuidList);
			JSONObject isManualFilter = getTermObject(ElasticSearchUtils.IS_MANUAL.getValue(), isManual.toString());
			JSONObject localeFilter = getTermObject(ElasticSearchUtils.LOCALE.getValue(), locale);
			
			filterArray.add(departmentJsonFilter);
			filterArray.add(isManualFilter);
			filterArray.add(localeFilter);
		}
		
		JSONObject boolParams = new JSONObject();
		
		if(searchTerm!=null && !searchTerm.isEmpty()) {
			String searchRegex = getInfixSearchRegex(searchTerm);
		
			boolParams = new JSONObject()
				.put(ElasticSearchUtils.MUST.getValue(), new JSONObject()
						.put(ElasticSearchUtils.QUERY_STRING.getValue(), new JSONObject()
								.put(ElasticSearchUtils.QUERY.getValue(), searchRegex)
								.put(ElasticSearchUtils.DEFAULT_OPERATOR.getValue(), qo.getValue())
								.put(ElasticSearchUtils.FIELDS.getValue(), new JSONArray(fieldsToSearch))));
		}

		if(filterArray != null && !filterArray.isEmpty()) {
			boolParams = boolParams.put(ElasticSearchUtils.FILTER.getValue(), new JSONArray(filterArray));
		}

		String searchQuery = new JSONObject()
				.put(ElasticSearchUtils.FROM.getValue(), 0)
				.put(ElasticSearchUtils.SIZE.getValue(),limit)
				.put(ElasticSearchUtils.QUERY.getValue(), new JSONObject()
						.put(ElasticSearchUtils.BOOL.getValue(), boolParams))
				.put(ElasticSearchUtils.SORT.getValue(), new JSONArray(getSortOrder())).toString();

		return searchQuery;
	}
	
	public List<JSONObject> getSortOrder() throws JSONException {
		JSONObject sortDescQuery = new JSONObject();
		sortDescQuery.put(ElasticSearchUtils.ORDER.getValue(), ElasticSearchUtils.DESC_SORT.getValue());
		JSONObject sortAscQuery = new JSONObject();
		sortAscQuery.put(ElasticSearchUtils.ORDER.getValue(), ElasticSearchUtils.ASC_SORT.getValue());
		JSONObject orderSortQuery=new JSONObject();
		orderSortQuery.put(ElasticSearchUtils.SORT_ORDER.getValue(), sortAscQuery);
		JSONObject scoreSortQuery=new JSONObject();
		scoreSortQuery.put(ElasticSearchUtils.ELASTIC_SEARCH_SCORE.getValue(), sortDescQuery);
		
		List<JSONObject> filterArray=new ArrayList<JSONObject>();
		filterArray.add(scoreSortQuery);
		filterArray.add(orderSortQuery);
		
		return filterArray;
	}
	
	
	public boolean reindexTemplatesForDepartment(String dealerDepartmentUuid) throws Exception {
		try{
			boolean deleteTemplates = deleteTemplatesfromElasticSearchForDepartment(dealerDepartmentUuid);
			log.info("in reindexTemplatesForDepartment existing templates deleted successfully"
					+ " from elastic search for department_uuid={} response={}",dealerDepartmentUuid,deleteTemplates);
			Long dealerDepartmentId=generalRepository.getDepartmentIDForUUID(dealerDepartmentUuid);
			List<Object[]> manualTemplatesList = generalRepository.getManualTempateForDepartmentId(dealerDepartmentId);
			if(manualTemplatesList!=null && !manualTemplatesList.isEmpty()){
				for(Object[] templateIterator:manualTemplatesList) {
					Template template=getTemplateObject(templateIterator);
					pushTemplateToElasticSearch(template);
				}
			}
		} catch(Exception e) {
			log.error("in reindex templates for department_uuid={}",dealerDepartmentUuid,e);
			return false;
		}
		return true;
	}
	
	public boolean deleteTemplatesfromElasticSearchForDepartment(String dealerDepartmentUuid) throws Exception {
		boolean isDeletedFromES=false;
		try {
			String searchQuery=getDeleteAllTemplatesForDealerSearchQuery(dealerDepartmentUuid);
			Long endTime=0l,startTime=0l;
			
			String TEMPLATE_DELETE_ENDPOINT = String.format(
					"%s/templates/_delete_by_query?conflicts=proceed&pretty",
					elasticSearchBaseUrl);
			
			HttpEntity<String> requestEntity = new HttpEntity<String>(searchQuery,getHeaders());
			log.info("search_endpoint={} query={} request_entity={}", TEMPLATE_DELETE_ENDPOINT, searchQuery,objectMapper.writeValueAsString(requestEntity));
			
			ResponseEntity<LinkedHashMap> response = restTemplateSearch.exchange(TEMPLATE_DELETE_ENDPOINT, HttpMethod.POST, requestEntity, LinkedHashMap.class);
			
			isDeletedFromES=true;
			
			
			endTime = System.currentTimeMillis();

			log.info("Time taken to delete Templates for department_uuid={} from "
					+ "		ElasticSearch indexing_time={} ms response={}"
					,	 dealerDepartmentUuid,(endTime-startTime),new ObjectMapper().writeValueAsBytes(response)
					);
		} catch(Exception e) {
			log.error("in delete templates from elastic search for department_uuid={}",dealerDepartmentUuid,e);
		}
		return isDeletedFromES;
	}
	
	
	
	private String getDeleteAllTemplatesForDealerSearchQuery(String dealerDepartmentUuid) throws Exception {
		List<JSONObject> filterArray=new ArrayList<JSONObject>();
		
		if(dealerDepartmentUuid != null && !dealerDepartmentUuid.isEmpty()) {
			
			JSONObject  dealerUuidJsonFilter = getTermObject(ElasticSearchUtils.DEPARTMENT_UUID.getValue(), dealerDepartmentUuid);
			filterArray.add(dealerUuidJsonFilter);
		}
		
		JSONObject boolParams = new JSONObject();
		
		if(filterArray != null && !filterArray.isEmpty()) {
			boolParams = boolParams.put("filter", new JSONArray(filterArray));
		}

		String searchQuery = new JSONObject()
				.put("query", new JSONObject()
						.put("bool", boolParams)
						).toString();

		return searchQuery;
	}
	
	

	private ResponseEntity<LinkedHashMap> getMessageSearchResponse(String searchQuery, String searchEndPoint) {
		HttpEntity<String> requestEntity = new HttpEntity<String>(searchQuery,getHeaders());
		log.info("search_endpoint={} query={}", searchEndPoint, searchQuery);
		return restTemplate.exchange(searchEndPoint, HttpMethod.POST, requestEntity, LinkedHashMap.class);
	}

	private static String getInfixSearchRegex(String searchTerm) {
		String searchRegex = "";
		if(searchTerm!=null && !searchTerm.trim().isEmpty()) {
			String searchTerms[] = searchTerm.split(" ");
			StringBuffer sTerms = new StringBuffer();
			for(String str : searchTerms) {
				if(str!=null && !str.isEmpty()) {
					if(str.length()>MAX_NGRAM_SIZE) { 
						for(int i=0;i<str.length()&&i<=str.length()-MAX_NGRAM_SIZE;i++) {
							sTerms.append(str.substring(i, i+MAX_NGRAM_SIZE)+" ");
						}
					} else {
						sTerms.append(str+" ");
					}
				}
			}
			searchRegex = sTerms.toString();
		}
		return searchRegex;
	}

	private static <T> JSONObject getTermsObject(String key, Collection<T> values ) throws Exception {
		return new JSONObject().put("terms", new JSONObject().put(key,new JSONArray(values)));
	}

	public static String getCleanedSearchTerm(String searchTerm) {

		Pattern pt = Pattern.compile("[^a-zA-Z0-9 ]");
		Matcher match= pt.matcher(searchTerm);
		while(match.find())
		{
			String s= match.group();
			searchTerm=searchTerm.replaceAll("\\"+s, "");
		}
		return searchTerm;
	}

	private static <T> JSONObject getTermsObject(String key, Set<String> values ) throws Exception {
		return new JSONObject().put("terms", new JSONObject().put(key,new JSONArray(values)));
	}
	
	private static <T> JSONObject getTermObject(String key, String value ) throws Exception {
		return new JSONObject().put("term", new JSONObject().put(key,value));
	}

	public Boolean indexTemplatesForDealers(DealersTemplateIndexRequest dealersTemplateIndexRequest) {
		if(dealersTemplateIndexRequest!=null 
				&& dealersTemplateIndexRequest.getDealerUuids()!=null 
				&& !dealersTemplateIndexRequest.getDealerUuids().isEmpty()) {
			for(String dealerUuid :dealersTemplateIndexRequest.getDealerUuids()) {
				try{
					Long dealerId=generalRepository.getDealerIdFromDealerUUID(dealerUuid);
					List<Object[]> manualTemplatesList = generalRepository.getManualTempateForDealerId(dealerId);
					if(manualTemplatesList!=null && !manualTemplatesList.isEmpty()){
						for(Object[] templateIterator:manualTemplatesList) {
							Template template=getTemplateObject(templateIterator);
							pushTemplateToElasticSearch(template);
						}
					}
				} catch (Exception e) {
					log.info("in indexTemplatesForDealers error while indexing templates for dealer_uuid={}",dealerUuid);
				}
			}
		}
		return true;
	}
	
	public Boolean updateFreemarkerTemplateDealerSetupOption(EnableFreemarkerTemplatesRequest request) throws Exception {
		
		for (Long dealerId = request.getFromDealerID(); dealerId <= request.getToDealerID(); dealerId++) {

			try {
				String dealerUuid = generalRepository.getDealerUUIDFromDealerId(dealerId);
				String freemarkerDsoValue = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUuid, DealerSetupOption.COMMUNICATIONS_TEMPLATE_FREEMARKER_ROLLOUT.getOptionKey());
				Map<String, Boolean> freemarkerDSOValues = new HashMap<>(); 
	
				ObjectMapper mapper = new ObjectMapper();
		        TypeFactory typeFactory = mapper.getTypeFactory();
		        MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, Boolean.class);
	
		        try {
		        	if(freemarkerDsoValue!=null && !freemarkerDsoValue.isEmpty()) {
		        		freemarkerDSOValues = mapper.readValue(freemarkerDsoValue, mapType);
	        			freemarkerDSOValues.put(request.getServiceName(), true);
		        	} else {
	        			freemarkerDSOValues.put(request.getServiceName(), true);
		        	}
		        	JSONObject jsonObject = new JSONObject(freemarkerDSOValues);
		        	KManageApiHelper.updateDealerSetupOption(dealerUuid, DealerSetupOption.COMMUNICATIONS_TEMPLATE_FREEMARKER_ROLLOUT.getOptionKey(), jsonObject.toString());
		        	kManageApiHelper.getAndUpdateDealerSetupOptionValueInCache(dealerUuid, DealerSetupOption.COMMUNICATIONS_TEMPLATE_FREEMARKER_ROLLOUT.getOptionKey());
		        	
		        } catch(Exception e) {
		        	log.warn("Could not calculate value of Freemarker DSO for dealerID={}", dealerId, e);
		        }
			} catch (Exception e) {
	        	log.warn("Could not enable freemarker for service = {} for dealerID={}", request.getServiceName(), dealerId, e);
		    }
		}
		return true;
	}

	public Boolean createFreemarkerTemplates(CreateFreemarkerTemplatesRequest createFreemarkerTemplatesRequest) throws Exception{

		log.info("fetching freemarker template for template type={}", createFreemarkerTemplatesRequest.getTemplateType());
		EmailTemplateType ettOriginal = emailTemplateTypeRepository.findByTypeName(createFreemarkerTemplatesRequest.getTemplateType());
		EmailTemplateType ettFreemarker = new EmailTemplateType();
		
		if(DT_TEMPLATE.equalsIgnoreCase(createFreemarkerTemplatesRequest.getTemplateType()) || DTZ_TEMPLATE.equalsIgnoreCase(createFreemarkerTemplatesRequest.getTemplateType())) {
			ettFreemarker = emailTemplateTypeRepository.findByTypeName(DT_FM_TEMPLATE);
		} else if(DE_TEMPLATE.equalsIgnoreCase(createFreemarkerTemplatesRequest.getTemplateType()) || DEZ_TEMPLATE.equalsIgnoreCase(createFreemarkerTemplatesRequest.getTemplateType())) {
			ettFreemarker = emailTemplateTypeRepository.findByTypeName(DE_FM_TEMPLATE);
		} else {
			ettFreemarker = emailTemplateTypeRepository.findByTypeName(createFreemarkerTemplatesRequest.getTemplateType()+FREEMARKER_TEMPLATE_TYPE_SUFFIX);
		}

		for(Long dealerId=createFreemarkerTemplatesRequest.getFromDealerID(); dealerId<=createFreemarkerTemplatesRequest.getToDealerID(); dealerId++) {
			
			List<EmailTemplate> emailTemplatesList = emailTemplateRepository.findAllByEmailTemplateTypeIDAndDealerID(ettOriginal.getId(), dealerId);
			
			if(emailTemplatesList!=null && !emailTemplatesList.isEmpty()) {

				for(EmailTemplate emailTemplate: emailTemplatesList) {
					
					EmailTemplate freemarkerTemplate = new EmailTemplate();
					
					if(DT_TEMPLATE.equalsIgnoreCase(createFreemarkerTemplatesRequest.getTemplateType()) 
							|| DTZ_TEMPLATE.equalsIgnoreCase(createFreemarkerTemplatesRequest.getTemplateType())) {
						
						Integer delayDays = getDelayDaysDealerSetupOption(DealerSetupOption.RO_CLOSE_DELAYED_TEXT_DELAY.getOptionKey(), dealerId);
						String template = "";
						
						if(delayDays>0) {
							template = emailTemplateRepository.findEmailTemplateByEmailTemplateTypeNameAndDealerIDAndLocale(DT_TEMPLATE, dealerId, emailTemplate.getLocale());	
						} else {
							template = emailTemplateRepository.findEmailTemplateByEmailTemplateTypeNameAndDealerIDAndLocale(DTZ_TEMPLATE, dealerId, emailTemplate.getLocale());	
						}
						freemarkerTemplate.setEmailTemplate(convertToFreemarkerTemplate(template));
						
					} else if(DE_TEMPLATE.equalsIgnoreCase(createFreemarkerTemplatesRequest.getTemplateType()) 
							|| DEZ_TEMPLATE.equalsIgnoreCase(createFreemarkerTemplatesRequest.getTemplateType())) {
						
						Integer delayDays = getDelayDaysDealerSetupOption(DealerSetupOption.RO_CLOSE_DELAYED_EMAIL_DELAY.getOptionKey(), dealerId);
						String template = "";
						
						if(delayDays>0) {
							template = emailTemplateRepository.findEmailTemplateByEmailTemplateTypeNameAndDealerIDAndLocale(DE_TEMPLATE, dealerId, emailTemplate.getLocale());	
						} else {
							template = emailTemplateRepository.findEmailTemplateByEmailTemplateTypeNameAndDealerIDAndLocale(DEZ_TEMPLATE, dealerId, emailTemplate.getLocale());	
						}
						freemarkerTemplate.setEmailTemplate(convertToFreemarkerTemplate(template));
						
					} else {
						freemarkerTemplate.setEmailTemplate(convertToFreemarkerTemplate(emailTemplate.getEmailTemplate()));
					}
					
					freemarkerTemplate.setDealerId(emailTemplate.getDealerId());
					freemarkerTemplate.setDealerUserId(emailTemplate.getDealerUserId());
					freemarkerTemplate.setEmailTemplateTypeId(ettFreemarker.getId());
					freemarkerTemplate.setLocale(emailTemplate.getLocale());
					freemarkerTemplate.setUuid(helper.getBase64EncodedSHA256UUID());
					
					emailTemplateRepository.upsertFreemarkerTemplate(freemarkerTemplate.getDealerId(), 
							freemarkerTemplate.getDealerUserId(), freemarkerTemplate.getEmailTemplateTypeId(), 
							freemarkerTemplate.getEmailTemplate(), freemarkerTemplate.getLocale(), freemarkerTemplate.getUuid());

					log.info("created freemarker template for template type={} dealerId={}", ettFreemarker.getTypeName(), dealerId);
				}
			} else {
				log.info("no template present for template type={} dealerId={}", ettFreemarker.getTypeName(), dealerId);
			}
		}
		
		return true;
	}
	
	private Integer getDelayDaysDealerSetupOption(String dso, Long dealerId) {
		try {
			String dealerUuid = generalRepository.getDealerUUIDFromDealerId(dealerId);
			String delayDaysDSO = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUuid, DealerSetupOption.RO_CLOSE_DELAYED_TEXT_DELAY.getOptionKey());
			Integer delayDays = Integer.parseInt(delayDaysDSO);
			return delayDays;
		} catch (Exception e) {
			log.warn("could not calculate the value of the dso={} and dealer_id={}", dso, dealerId);
			return 1;
		}
	}
	
	private String convertToFreemarkerTemplate(String template) {
		
		String freemarkerTemplate = template;
		
		freemarkerTemplate = freemarkerTemplate.replace("_customer_firstname", 
				"<#if customer.isBusiness?? && customer.isBusiness && customer.company?? && customer.company!=\"\">${customer.company}"
				+ "<#elseif customer.firstName?? && customer.firstName!=\"\">${customer.firstName}</#if>");
		freemarkerTemplate = freemarkerTemplate.replace("_customer_lastname", 
				"<#if !(customer.isBusiness?? && customer.isBusiness) && customer.lastName?? && customer.lastName!=\"\">${customer.lastName}</#if>");
		freemarkerTemplate = freemarkerTemplate.replace("_dealer_name", "${dealer.name}");
		freemarkerTemplate = freemarkerTemplate.replace("_customer_customerkey", "${customer.customerKey}");
		freemarkerTemplate = freemarkerTemplate.replace("_dealerassociate_fname", "${dealerAssociate.firstName}");
		freemarkerTemplate = freemarkerTemplate.replace("_dealerassociate_lname", "${dealerAssociate.lastName}");
		freemarkerTemplate = freemarkerTemplate.replace("_onlinecardsave_shorturl", "${onlineCardSaveShortUrl}");
		freemarkerTemplate = freemarkerTemplate.replace("_brand_brandname", "${vehicle.make}");
		freemarkerTemplate = freemarkerTemplate.replace("_delay_days", "${delayDays}");
		freemarkerTemplate = freemarkerTemplate.replace("_vehicle_model", "${vehicle.model}");
		
		return freemarkerTemplate;
	}
	
	private HttpHeaders getHeaders()
	{
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-type", "application/json");
		return headers;
	}

	public TemplateTagsResponse getTemplateTags(TemplateTagsRequest getTemplateTagsRequest) throws Exception{

		log.info("fetching freemarker template for template types={}", objectMapper.writeValueAsString(getTemplateTagsRequest));
		
		Set<String> tags = new HashSet<String>();
		TemplateTagsResponse getTemplateTagsResponse = new TemplateTagsResponse();

		for(String templateType: getTemplateTagsRequest.getTemplateTypes()) {
			
			EmailTemplateType ett = emailTemplateTypeRepository.findByTypeName(templateType);
			List<EmailTemplate> emailTemplatesList = emailTemplateRepository.findAllByEmailTemplateTypeID(ett.getId());
			
			if(emailTemplatesList!=null && !emailTemplatesList.isEmpty()) {

				for(EmailTemplate emailTemplate: emailTemplatesList) {
					
					String template = emailTemplate.getEmailTemplate();
					
					log.info("template for template type={} template={}", objectMapper.writeValueAsString(getTemplateTagsRequest), template);

					template = template.replaceAll("<", " ");
					template = template.replaceAll(">", " ");
					String[] words = template.split(" ");
					
					log.info("template for words={}", objectMapper.writeValueAsString(words));
					
					for (String word : words) {

						if(word.startsWith("_")) {
							tags.add(word);	
						}
					}
				}
			} else {
				log.info("no template present for template type={}", templateType);
			}
		}
		
		getTemplateTagsResponse.setTags(tags);
		return getTemplateTagsResponse;
	}
	
}

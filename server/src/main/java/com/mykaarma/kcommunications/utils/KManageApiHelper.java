package com.mykaarma.kcommunications.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import com.mykaarma.api.MkApiClient;
import com.mykaarma.kcommunications.cache.CacheConfig;
import com.mykaarma.kmanage.client.KManageApiClientService;
import com.mykaarma.kmanage.model.dto.json.AuthorityDTO;
import com.mykaarma.kmanage.model.dto.json.DSODTO;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.DepartmentGroupExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.ErrorDTO;
import com.mykaarma.kmanage.model.dto.json.FeatureDTO;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociatesSetResponseDTO;
import com.mykaarma.kmanage.model.dto.json.PreferenceDTO;
import com.mykaarma.kmanage.model.dto.json.response.DealerAssociateGroupResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.DealerAssociateHasAuthorityResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDealerAssociatesAuthoritiesResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDealerAssociatesForDealerUUIDsResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDealerAssociatesPreferencesResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDealerAssociatesResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDealersResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDealersSetupOptionsResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDepartmentGroupByFeatureResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDepartmentResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetEmailTemplateResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetFeatureResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.ResponseDTO;
import com.mykaarma.kmanage.model.dto.manage.ErrorProto;
import com.mykaarma.kmanage.model.dto.mykaarma.DealerAssociateGroupProto.DealerAssociateGroup;
import com.mykaarma.kmanage.model.dto.response.AvailabilityResponseProto.AvailabilityResponse;
import com.mykaarma.kmanage.model.dto.response.DealerAssociateGroupListResponseProto.DealerAssociateGroupListResponse;
import com.mykaarma.kmanage.model.dto.response.DealerAssociateHasAuthorityResponseProto.DealerAssociateHasAuthorityResponse;
import com.mykaarma.kmanage.model.dto.response.GetDealerAssociateGroupResponseProto.GetDealerAssociateGroupResponse;
import com.mykaarma.kmanage.model.dto.response.GetDealerAssociateResponseProto.GetDealerAssociateResponse;
import com.mykaarma.kmanage.model.dto.response.GetDealerAssociatesAuthoritiesResponseProto.GetDealerAssociatesAuthoritiesResponse;
import com.mykaarma.kmanage.model.dto.response.GetDealerAssociatesForDealerUUIDsResponseProto.GetDealerAssociatesForDealerUUIDsResponse;
import com.mykaarma.kmanage.model.dto.response.GetDealerAssociatesPreferencesResponseProto;
import com.mykaarma.kmanage.model.dto.response.GetDealerAssociatesResponseProto.GetDealerAssociatesResponse;
import com.mykaarma.kmanage.model.dto.response.GetDealerAssociatesSetResponseProto;
import com.mykaarma.kmanage.model.dto.response.GetDealersResponseProto.GetDealersResponse;
import com.mykaarma.kmanage.model.dto.response.GetDealersSetupOptionsResponseProto;
import com.mykaarma.kmanage.model.dto.response.GetDealersSetupOptionsResponseProto.GetDealersSetupOptionsResponse;
import com.mykaarma.kmanage.model.dto.response.GetDepartmentGroupByFeatureResponseProto.GetDepartmentGroupByFeatureResponse;
import com.mykaarma.kmanage.model.dto.response.GetDepartmentResponseProto.GetDepartmentResponse;
import com.mykaarma.kmanage.model.dto.response.GetEmailTemplateResponseProto.GetEmailTemplateResponse;
import com.mykaarma.kmanage.model.dto.response.GetFeatureResponseProto.GetFeatureResponse;
import com.mykaarma.kmanage.model.dto.response.ResponseProto.Response;
import com.mykaarma.kmanage.model.enums.ErrorCodes;

@Service
public class KManageApiHelper {
	private final static Logger LOGGER = LoggerFactory.getLogger(KManageApiHelper.class);
	private static String username ;
	private static String password ;
	private static String base_url ;
	
	private static ObjectMapper objectMapper= new ObjectMapper();
	
	private static KManageApiClientService clientService = null;

	@Value("${kcommunications_basic_auth_user}")
	public  void setUsername(String username) {
		KManageApiHelper.username = username;
	}

	
    @Value("${kcommunications_basic_auth_pass}")
	public  void setPassword(String password) {
    	KManageApiHelper.password = password;
	}

    @Value("${kmanage_api_url_v2}")
	public  void setBase_url(String base_url) {
    	KManageApiHelper.base_url = base_url;
	}


	private static KManageApiClientService getKManageClientService(){
		if(clientService==null){
			clientService = new KManageApiClientService(base_url,username,password, MkApiClient.MK_COMMUNICATIONS_API, LOGGER);
		}
		return clientService;
		
	}
	
	@Cacheable(value=CacheConfig.DEALER_DEPARTMENT_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public GetDepartmentResponseDTO getDealerDepartment(String departmentUUID) {
		try {
			LOGGER.info(String.format("Sending get deparment request to kmanage, departmentUUID=%s",departmentUUID));
			GetDepartmentResponse dealerDepartment = getKManageClientService().getDepartment(departmentUUID);
			GetDepartmentResponseDTO getDealerAssociateResponseDTO = (GetDepartmentResponseDTO) getDTOfromProto(dealerDepartment, GetDepartmentResponseDTO.class);
			return getDealerAssociateResponseDTO;
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getDealerDepartment department_uuid=%s ", departmentUUID),e);
			return null;
		}
	}

	@Cacheable(value = CacheConfig.DEALER_ASSOCIATE_CACHE, keyGenerator = "customKeyGenerator", unless = "#result == null")
	public GetDealerAssociateResponseDTO getDealerAssociate(String departmentUUID, String userUUID) {
		try {
			GetDealerAssociateResponse dealerAssociate = getKManageClientService().getDealerAssociate(userUUID, departmentUUID);
			GetDealerAssociateResponseDTO getDealerAssociateResponseDTO = (GetDealerAssociateResponseDTO) getDTOfromProto(dealerAssociate, GetDealerAssociateResponseDTO.class);
			return getDealerAssociateResponseDTO;
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getDealerAssociate user_uuid=%s department_uuid=%s ",
				userUUID, departmentUUID), e);
			return null;
		}
	}

	@Cacheable(value = CacheConfig.DEALER_ASSOCIATE_FOR_DA_UUID_CACHE, keyGenerator = "customKeyGenerator", unless = "#result == null")
	public DealerAssociateExtendedDTO getDealerAssociateForDealerAssociateUUID(String departmentUUID, String dealerAssociateUUID) {
		try {
			Set<String> departmentUUIDs = new HashSet<String>();
			Set<String> dealerAssociateUUIDs = new HashSet<String>();

			departmentUUIDs.add(departmentUUID);
			dealerAssociateUUIDs.add(dealerAssociateUUID);
			GetDealerAssociatesResponseDTO dealerAssociateList = getDealerAssociatesForDealerAssociateUUIDList(departmentUUIDs, dealerAssociateUUIDs);

			LOGGER.info(String.format("in getDealerAssociateForDealerAssociateUUID for dealer_associate_uuid=%s"
				+ " department_uuid=%s response=%s", dealerAssociateUUID, departmentUUID, new ObjectMapper().writeValueAsString(dealerAssociateList)));
			if(dealerAssociateList==null){
				return null;
			}
			
			if(dealerAssociateList.getErrors()!=null && dealerAssociateList.getErrors().size() > 0) {
				for(ErrorDTO e : dealerAssociateList.getErrors()) {
					LOGGER.error(String.format("Error occurred while fetching dealer associate for dealer_associate_uuid=%s department_uuid=%s",
							dealerAssociateUUID,departmentUUID),e);
				}
			}
				
			if(dealerAssociateList.getDealerAssociates()==null){
				return null;
			}
			
			return dealerAssociateList.getDealerAssociates().get(dealerAssociateUUID);
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getDealerAssociate dealer_associate_uuid=%s department_uuid=%s ", 
					dealerAssociateUUID,departmentUUID),e);
			return null;
		}
	}
	
	public static GetDealerAssociatesResponseDTO getDealerAssociatesForDealerAssociateUUIDList(Set<String> departmentUUIDs,Set<String> dealerAssociateUUIDs){
		try {
			GetDealerAssociatesResponse dealerAssociateList = getKManageClientService().getDealerAssociatesForUuids(dealerAssociateUUIDs, departmentUUIDs);
			GetDealerAssociatesResponseDTO getDealerAssociatesResponseDTO = (GetDealerAssociatesResponseDTO) getDTOfromProto(dealerAssociateList,GetDealerAssociatesResponseDTO.class);
			return getDealerAssociatesResponseDTO;
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getDealerAssociatesForDealerAssociateUUIDList dealer_associate_uuids=%s department_uuids=%s ", 
					dealerAssociateUUIDs,departmentUUIDs),e);
			return null;
		}
	}
	
	public static GetEmailTemplateResponseDTO getEmailTemplate(String dealerUUID, String emailTemplateType,String locale) {
		try {
			GetEmailTemplateResponse emailTemplateResponse = getKManageClientService().getEmailTemplateForDealer(dealerUUID, emailTemplateType, locale);
			GetEmailTemplateResponseDTO emailTemplateResponseDTO = (GetEmailTemplateResponseDTO) getDTOfromProto(emailTemplateResponse,GetEmailTemplateResponseDTO.class);
			return emailTemplateResponseDTO;
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getEmailTemplate dealer_uuid=%s email_template_type=%s ", 
					dealerUUID,emailTemplateType),e);
			return null;
		}
	}

	public GetDealersResponseDTO sortInputAndGetDealersForUUID(Set<String> dealerUUIDList) {
		List<String> dealerUUIDListSorted = new ArrayList<>(dealerUUIDList);
		Collections.sort(dealerUUIDListSorted);
		return getDealersForUUID(dealerUUIDListSorted);
	}

	@Cacheable(value = CacheConfig.DEALERS_CACHE, keyGenerator = "customKeyGenerator", unless = "#result == null")
	public GetDealersResponseDTO getDealersForUUID(List<String> dealerUUIDList) {
		Set<String> dealerUUIDSet = new HashSet<>(dealerUUIDList);

		try {
			GetDealersResponse dealers = getKManageClientService().getDealersForUuids(dealerUUIDSet);
			GetDealersResponseDTO dealerDTO = (GetDealersResponseDTO) getDTOfromProto(dealers, GetDealersResponseDTO.class);
			LOGGER.info("dealerDTO={}", new ObjectMapper().writeValueAsString(dealerDTO));
			return dealerDTO;
		} catch (Exception e) {
			LOGGER.error("Error while getDealersForUUID for dealer_uuid_list={}", dealerUUIDList, e);
			return null;
		}
	}

	public HashMap<String, String> sortInputAndGetDealerSetupOptionValuesForADealer(String dealerUUID, Set<String> keys) throws Exception {
		List<String> keysSorted = new ArrayList<>(keys);
		Collections.sort(keysSorted);
		return getDealerSetupOptionValuesForADealer(dealerUUID, keysSorted);
	}

	@Cacheable(value=CacheConfig.DSO_KMANAGE_FOR_MULTIPLE_KEYS_CACHE,keyGenerator = "customKeyGenerator")
	public HashMap<String, String> getDealerSetupOptionValuesForADealer(String dealerUUID, List<String> keys) throws Exception {
		Set<String> keysSet = new HashSet<>(keys);

		try {	
			Set<String> dealerUuids = new HashSet<String>();	
			dealerUuids.add(dealerUUID);	
			HashMap<String, String> dsoValueMap = new HashMap<String, String>();	
			GetDealersSetupOptionsResponseProto.GetDealersSetupOptionsResponse resp = getDealersSetupOptions(dealerUuids, keysSet);
			GetDealersSetupOptionsResponseDTO dtoResp = (GetDealersSetupOptionsResponseDTO) getDTOfromProto(resp, GetDealersSetupOptionsResponseDTO.class);	
			Map<String, Set<DSODTO>> set = dtoResp.getDealersSetupOptionsDTO();	
			if(set ==  null) {	
				return null;	
			}	
			Set<DSODTO> setDSO = set.get(dealerUUID);	
			for(DSODTO dsoDTO : setDSO) {	
				dsoValueMap.put(dsoDTO.getOptionKey(), dsoDTO.getOptionValue());	
			}	
			return dsoValueMap;	
		} catch (Exception e) {	
			LOGGER.error("Error occured while getting dsos for dealer_uuid=" + dealerUUID + " keys=" + keys, e);	
			throw e;	
		}	

	}

	@Cacheable(value = CacheConfig.DEFAULT_DEALER_ASSOCIATE_CACHE, keyGenerator = "customKeyGenerator", unless = "#result == null")
	public GetDealerAssociateResponseDTO getDefaultDealerAssociateForDepartment(String departmentUUID) {
		try {
			GetDealerAssociateResponse dealerAssociate = getKManageClientService().getDefaultUserForDepartment(departmentUUID);
			GetDealerAssociateResponseDTO getDealerAssociateResponseDTO = (GetDealerAssociateResponseDTO) getDTOfromProto(dealerAssociate, GetDealerAssociateResponseDTO.class);
			return getDealerAssociateResponseDTO;
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getDefaultDealerAssociate department_uuid=%s ",
				departmentUUID), e);
			return null;
		}
	}
	
	public static  GetDealerAssociatesResponseDTO getAllDealerAssociatesFoDepartmentUUIDAndUserUUIDs(Set<String> departmentUUIDs ,Set<String> userUUIDs) throws Exception{
		try {
			GetDealerAssociatesResponse dealerAssociatesResponse =getKManageClientService().getDealerAssociatesForUserUuids(userUUIDs,departmentUUIDs );
			if(dealerAssociatesResponse==null){
				LOGGER.error(String.format("null response received in getAllDealerAssociatesFoDepartmentUUIDAndUserUUIDs department_uuid=%s user_uuids=%s ", 
						objectMapper.writeValueAsString(departmentUUIDs),objectMapper.writeValueAsString(userUUIDs)));
				return null;
			}
			if(dealerAssociatesResponse.getErrorsCount() > 0) {
				for(ErrorProto.Error e : dealerAssociatesResponse.getErrorsList()) {
					if(ErrorCodes.INVALID_REQUEST_DEALER_INVALID.getErrorTitle().equals(e.getErrorTitle())){
						LOGGER.warn("Error occurred in getAllDealerAssociatesFoDepartmentUUIDAndUserUUIDs"
								+ "for department_uuids={} user_uuids={} error_desc={}" ,objectMapper.writeValueAsString(departmentUUIDs),
								objectMapper.writeValueAsString(userUUIDs) , e.getErrorMessage());
					} else {
						LOGGER.error("Error occurred in getAllDealerAssociatesFoDepartmentUUIDAndUserUUIDs"
							+ "for department_uuids={} user_uuids={} error_desc={}" ,objectMapper.writeValueAsString(departmentUUIDs),
							objectMapper.writeValueAsString(userUUIDs) , e.getErrorMessage());
					}
				}
				return null;
			}
			GetDealerAssociatesResponseDTO getDealerAssociatesResponseDTO = (GetDealerAssociatesResponseDTO) getDTOfromProto(dealerAssociatesResponse,GetDealerAssociatesResponseDTO.class);
			return getDealerAssociatesResponseDTO;
		} catch (Exception e) {
			LOGGER.error(String.format("Error in getAllDealerAssociatesFoDepartmentUUIDAndUserUUIDs department_uuid=%s user_uuids=%s ", 
					objectMapper.writeValueAsString(departmentUUIDs),objectMapper.writeValueAsString(userUUIDs)),e);
			return null;
		}
	}
	
	@Cacheable(value=CacheConfig.DEALER_ASSOCIATES_FOR_DEPARTMENT_UUID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public GetDealerAssociatesResponseDTO getAllDealerAssociatesForDepartmentUUID(String departmentUUID){
		try {
			Set<String> departmentUUIDs=new HashSet<String>();
			departmentUUIDs.add(departmentUUID);
			GetDealerAssociatesResponseDTO getDealerAssociatesResponseDTO = getAllDealerAssociatesFoDepartmentUUIDAndUserUUIDs(departmentUUIDs,null);
			LOGGER.info(String.format("getAllDealerAssociatesForDepartmentUUIDs department_uuids=%s response=%s",objectMapper.writeValueAsString(departmentUUIDs),objectMapper.writeValueAsString(getDealerAssociatesResponseDTO)));
			return getDealerAssociatesResponseDTO;
		} catch (Exception e) {
			LOGGER.error("Error in getAllDealerAssociatesFoDepartmentUUIDAndUserUUIDs department_uuid={} ", 
					departmentUUID,e);
			return null;
		}
	}
	
	@Cacheable(value=CacheConfig.DEALER_ASSOCIATES_FOR_DEALER_UUID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	public GetDealerAssociatesForDealerUUIDsResponseDTO getAllDealerAssociatesForDealerUUID(String dealerUUID){
		try {
			ObjectMapper objectMapper=new ObjectMapper();
			Set<String> dealerUUIDs=new HashSet<String>();
			dealerUUIDs.add(dealerUUID);
			GetDealerAssociatesForDealerUUIDsResponseDTO dealerAssociatesForDealerUUIDs = getAllDealerAssociatesForDealerUUIDs(dealerUUIDs);
			LOGGER.info(String.format("getAllDealerAssociatesForDealerUUID dealer_uuid=%s response=%s",objectMapper.writeValueAsString(dealerUUIDs),objectMapper.writeValueAsString(dealerAssociatesForDealerUUIDs)));
			return dealerAssociatesForDealerUUIDs;
		} catch (Exception e) {
			LOGGER.error("Error in getAllDealerAssociatesFoDepartmentUUIDAndUserUUIDs dealer_uuid={} ", 
					dealerUUID,e);
			return null;
		}
	}
	
	public GetDealerAssociatesForDealerUUIDsResponseDTO getAllDealerAssociatesForDealerUUIDs(Set<String> dealerUUIDs) throws JsonProcessingException{
		ObjectMapper objectMapper=new ObjectMapper();
		try {
			
			GetDealerAssociatesForDealerUUIDsResponse dealerAssociatesForDealerUUIDs = getKManageClientService().getDealerAssociatesForDealerUuids(dealerUUIDs, Boolean.TRUE);
			if(dealerAssociatesForDealerUUIDs==null){
				LOGGER.error(String.format("null response received in getAllDealerAssociatesForDealerUUIDs dealer_uuids=%s ", 
						objectMapper.writeValueAsString(dealerUUIDs)));
				return null;
			}
			if(dealerAssociatesForDealerUUIDs.getErrorsCount() > 0) {
				for(ErrorProto.Error e : dealerAssociatesForDealerUUIDs.getErrorsList()) {
					if(ErrorCodes.INVALID_REQUEST_DEALER_INVALID.getErrorTitle().equals(e.getErrorTitle())){
						LOGGER.warn("Error occurred in getAllDealerAssociatesForDealerUUIDs"
								+ "for dealer_uuids={} error_desc={}" ,objectMapper.writeValueAsString(dealerUUIDs), e.getErrorMessage());
					} else {
						LOGGER.error("Error occurred in getAllDealerAssociatesForDealerUUIDs"
							+ "for dealer_uuids={} error_desc={}" ,objectMapper.writeValueAsString(dealerUUIDs), e.getErrorMessage());
					}
				}
				return null;
			}
			GetDealerAssociatesForDealerUUIDsResponseDTO getDealerAssociatesResponseDTO = (GetDealerAssociatesForDealerUUIDsResponseDTO) getDTOfromProto(dealerAssociatesForDealerUUIDs,GetDealerAssociatesForDealerUUIDsResponseDTO.class);
			return getDealerAssociatesResponseDTO;
		}  catch (Exception e) {
			LOGGER.error("Error in getAllDealerAssociatesForDealerUUIDs dealer_uuids={} ", 
					objectMapper.writeValueAsString(dealerUUIDs),e);
			return null;
		}
		
	}

	@Cacheable(value = CacheConfig.DEALER_ASSOCIATE_AUTHORITY_CACHE, keyGenerator = "customKeyGenerator", unless = "#result == null")
	public static Boolean checkDealerAssociateAuthority(String authority, String userUUID, String departmentUUID) {
		try {
			DealerAssociateHasAuthorityResponse hasAuthority = getKManageClientService().checkDealerAssociateAuthority(userUUID, departmentUUID, authority);
			DealerAssociateHasAuthorityResponseDTO hasAuthorityDTO = (DealerAssociateHasAuthorityResponseDTO) getDTOfromProto(hasAuthority,DealerAssociateHasAuthorityResponseDTO.class);
			LOGGER.info("checkDealerAssociateAuthority for user_uuid={} authority={} department_uuid={} has_authority={}", userUUID, authority, departmentUUID, hasAuthorityDTO.getHasAuthority());
			if(hasAuthorityDTO.getHasAuthority()==null)
				return false;
			return hasAuthorityDTO.getHasAuthority();
		} catch (Exception e) {
			LOGGER.error(String.format("Error while checkDealerAssociateAuthority department_uuid=%s user_uuid=%s authority=%s ", 
					departmentUUID, userUUID, authority),e);
			return null;
		}
	}
	
	public Set<DealerAssociateExtendedDTO> getDealerAssociatesHavingAuthority(String authority, List<String> departmentUuids) {
		GetDealerAssociatesSetResponseProto.GetDealerAssociatesSetResponse daSetResponse = null;
		try {
			daSetResponse = getKManageClientService().getAllValidDealerAssociatesByAuthorityForDepartmentList(authority, departmentUuids);
			GetDealerAssociatesSetResponseDTO daList = (GetDealerAssociatesSetResponseDTO) getDTOfromProto(daSetResponse, GetDealerAssociatesSetResponseDTO.class);
			Set<DealerAssociateExtendedDTO> daSet = daList.getDealerAssociates();
			return daSet;
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getDealerAssociatesHavingAuthority department_uuids=%s authority=%s ", 
					departmentUuids.toString(), authority),e);
			return null;
		}
	}
	
	
	public static Date getNextAvailableSlotForDealer(String dealerUuid, Date scheduledDate) throws Exception {
		AvailabilityResponse response = getKManageClientService().getDealerAvailabilityCalendar(dealerUuid, scheduledDate.getTime(), "");
		if(response!=null) {
			LOGGER.info("Response from KManage=" + TextFormat.shortDebugString(response));
		}
		if(response == null) {
			LOGGER.info("Received null response from manage api for dealerUuid=" + dealerUuid);
			// return the original date
			return scheduledDate;
		} else if(response.getErrorsCount() > 0) {
			for(ErrorProto.Error e : response.getErrorsList()) {
				LOGGER.info("Error occurred while fetching dealer calendar, dealer_uuid="+dealerUuid + " error_desc=" + e.getErrorMessage());
			}
			// return the original date
			return scheduledDate;
		} else {
			Boolean isProposedTimeCorrect = response.getIsProposedTimeGood();
			LOGGER.info("auto-csi check for proposedTime for dealerUuid=" + dealerUuid + " result" +
					"=" + isProposedTimeCorrect + " timestamp_millis=" + response.getNextGoodTimeInMillis());
			Long nextGoodTimeInMillis = response.getNextGoodTimeInMillis();
			Date revisedDate = new Date(nextGoodTimeInMillis);
			LOGGER.info("auto-csi final revised time for dealerUuid=" + dealerUuid + " revised_time=" + revisedDate.getTime());
			
			return revisedDate;
		}
	}

	public static DealerAssociateGroupResponseDTO getDealerAssociateGroup(String departmentUUID, String dealerAssociateGroupUUID) {
		try {
			GetDealerAssociateGroupResponse dealerAssociateGroup = getKManageClientService().getDealerAssociateGroup(departmentUUID, dealerAssociateGroupUUID);
			DealerAssociateGroupResponseDTO dealerAssociateGroupDTO = (DealerAssociateGroupResponseDTO) getDTOfromProto(dealerAssociateGroup,DealerAssociateGroupResponseDTO.class);
			return dealerAssociateGroupDTO;
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getDealerAssociateGroup department_uuid=%s dealer_associate_group_uuid=%s ", 
					departmentUUID, dealerAssociateGroupUUID),e);
			return null;
		}
	}

	@Cacheable(value = CacheConfig.DEALER_ASSOCIATE_GROUP_FOR_DA_CACHE, keyGenerator = "customKeyGenerator", unless = "#result == null")
	public DealerAssociateGroup getDealerAssociateGroupForDA( String departmentUUID, String dealerAssociateUuid) {
		try {
			DealerAssociateGroup daGroup = null;
			DealerAssociateGroupListResponse dealerAssociateGroupList = getKManageClientService().getDealerAssociateGroupForDealerAssociate(departmentUUID,dealerAssociateUuid);
			if(dealerAssociateGroupList==null){
				return null;
			}
			if(dealerAssociateGroupList.getDealerAssociateGroupsList() != null && !dealerAssociateGroupList.getDealerAssociateGroupsList().isEmpty()){
				for(DealerAssociateGroup dealerAssociateGroupIterator:dealerAssociateGroupList.getDealerAssociateGroupsList()) {
					if(dealerAssociateGroupIterator.getVirtualDealerAssociateUuid().equalsIgnoreCase(dealerAssociateUuid)){
						daGroup=dealerAssociateGroupIterator;
						break;
					}
				}
			}
			return daGroup;
		} catch (Exception e) {
			LOGGER.error("Error finding DealerAssociateGroupID for dealer_associate_uuid={} departmentUUID={}", dealerAssociateUuid,departmentUUID,e);
		}

		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object getDTOfromProto(Message message, Class<?> targetType) {
		ObjectMapper mapper = new ObjectMapper();
    	mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Printer printer = JsonFormat.printer();
        try {
        	String jsonRequest = printer.print(message);
        	Object object = (Object)mapper.readValue(jsonRequest, (Class)targetType);


			return object;
		} catch (Exception e) {
			LOGGER.error("Error while getting DTO from Proto ",e);
		}
        return null;
	}
	
	@Cacheable(value=CacheConfig.DSO_KMANAGE_CACHE,keyGenerator = "customKeyGenerator")
	public String getDealerSetupOptionValueForADealer(String dealerUUID, String key) throws Exception {
		return getDealerSetupOptionValueForADealerInternal(dealerUUID, key);
	}

	@CachePut(value=CacheConfig.DSO_KMANAGE_CACHE,keyGenerator = "customKeyGenerator")
	public String getAndUpdateDealerSetupOptionValueInCache(String dealerUUID, String key) throws Exception {
		return getDealerSetupOptionValueForADealerInternal(dealerUUID, key);
	}

	private String getDealerSetupOptionValueForADealerInternal(String dealerUUID, String key) throws Exception {
		try {
			Set<String> dealerUuids = new HashSet<>();
			dealerUuids.add(dealerUUID);
			Set<String> keys = new HashSet<>();
			keys.add(key);
			GetDealersSetupOptionsResponseProto.GetDealersSetupOptionsResponse resp = getDealersSetupOptions(dealerUuids, keys);
			GetDealersSetupOptionsResponseDTO dtoResp = (GetDealersSetupOptionsResponseDTO)getDTOfromProto(resp, GetDealersSetupOptionsResponseDTO.class);
			Map<String, Set<DSODTO>> set = dtoResp.getDealersSetupOptionsDTO();
			if(set ==  null) {
				return null;
			}
			Set<DSODTO> setDSO = set.get(dealerUUID);
			for(DSODTO dsoDTO : setDSO) {
				if(dsoDTO.getOptionKey().equalsIgnoreCase(key)) {
					return dsoDTO.getOptionValue();
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error occurred while getting dsos for dealer_uuid=" + dealerUUID + " key=" + key, e);
			throw e;
		}
		return null;
	}


	public static GetDealersSetupOptionsResponse getDealersSetupOptions(Set<String> dUuids, Set<String> keys) throws Exception
	{
		LOGGER.info(String.format("Sending get dso request to kmanage, uuids=%s and keys=%s",dUuids, keys));
		if(dUuids!=null && !dUuids.isEmpty())
		{
			GetDealersSetupOptionsResponse response = getKManageClientService().getDealersSetupOptions(dUuids, keys);
			LOGGER.info(String.format("Response received for get dealer setup options request, response=%s",response));
			ErrorProto.Error errorToThrow = null;
			if(response.getErrorsList().size()>0)
			{
				for(ErrorProto.Error e : response.getErrorsList())
				{
					if((e.getErrorCode()==ErrorCodes.USER_NOT_MIGRATED.getErrorCode()) || (e.getErrorCode()==ErrorCodes.USER_NOT_FOUND.getErrorCode()))
					{
						return null;
					}
					if(errorToThrow==null)
						errorToThrow = e;
				}

				throw new Exception(errorToThrow.getErrorMessage());
			}
			return response;
		}
		throw new Exception(ErrorCodes.INVALID_REQUEST_DATA.getErrorMessage());
	}

	public static void updateDealerSetupOption(String dealerUUID, String optionKey, String optionValue) throws Exception {
		LOGGER.info(String.format("Sending update dso request to kmanage, dealer_uuid=%s and key=%s value=%s", dealerUUID, optionKey, optionValue));
		if(dealerUUID != null && optionKey != null && !dealerUUID.isEmpty() && !optionKey.isEmpty()) {
			Response response = getKManageClientService().updateDealerSetupOption(optionKey, optionValue, dealerUUID);
			if(!response.getErrorsList().isEmpty()) {
				throw new Exception(response.getErrorsList().toString());
			}
			ResponseDTO responseDTO = (ResponseDTO) getDTOfromProto(response, ResponseDTO.class);
			LOGGER.info("Response received for update dealer setup options request, response={}", objectMapper.writeValueAsString(responseDTO));
		} else {
			throw new Exception(ErrorCodes.INVALID_REQUEST_DATA.getErrorMessage());
		}
	}

	@Cacheable(value = CacheConfig.DEPARTMENT_GROUP_KMANAGE_CACHE, keyGenerator = "customKeyGenerator", unless = "#result == null")
	public DepartmentGroupExtendedDTO getDepartmentGroupByFeature(String departmentUUID, String featureKey) throws Exception {
		LOGGER.info(String.format("Sending get department group by feature request to kmanage, department_uuid=%s, feature_key=%s",
				departmentUUID, featureKey));
		if(departmentUUID != null && featureKey != null && !departmentUUID.isEmpty() && !featureKey.isEmpty()) {
			GetDepartmentGroupByFeatureResponse response = getKManageClientService().getDepartmentGroupByFeature(departmentUUID, featureKey);
			if(!response.getErrorsList().isEmpty()) {
				throw new Exception(response.getErrorsList().toString());
			}
			GetDepartmentGroupByFeatureResponseDTO responseDTO = (GetDepartmentGroupByFeatureResponseDTO) getDTOfromProto(response, GetDepartmentGroupByFeatureResponseDTO.class);
			LOGGER.info("Response received for get department group by feature request for department_uuid={}, feature_key={}, response={}", departmentUUID, featureKey, objectMapper.writeValueAsString(responseDTO));
			return responseDTO.getDepartmentGroupExtendedDTO();
		} else {
			throw new Exception(ErrorCodes.INVALID_REQUEST_DATA.getErrorMessage());
		}
	}

	@Cacheable(value = CacheConfig.FEATURE_KMANAGE_CACHE, keyGenerator = "customKeyGenerator", unless = "#result == null")
	public FeatureDTO getFeatureByKey(String featureKey) throws Exception {
		LOGGER.info(String.format("Sending get feature request to kmanage, feature_key=%s", featureKey));
		if(featureKey != null && !featureKey.isEmpty()) {
			GetFeatureResponse response = getKManageClientService().getFeatureResponse(featureKey);
			if (!response.getErrorsList().isEmpty()) {
				throw new Exception(response.getErrorsList().toString());
			}
			GetFeatureResponseDTO responseDTO = (GetFeatureResponseDTO) getDTOfromProto(response, GetFeatureResponseDTO.class);
			LOGGER.info("Response received for get feature request for feature_key={}, response={}", featureKey, objectMapper.writeValueAsString(responseDTO));
			return responseDTO.getFeature();
		} else {
			throw new Exception(ErrorCodes.INVALID_REQUEST_DATA.getErrorMessage());
		}
	}

	public PreferenceDTO getDAPreference(String departmentUuid, String daUuid, String preferenceKey) throws Exception {
		try {
			LOGGER.info("in getDAPreference for department_uuid={} da_uuid={} preference={}", departmentUuid, daUuid, preferenceKey);
			GetDealerAssociatesPreferencesResponseProto.GetDealerAssociatesPreferencesResponse response = getKManageClientService().getDealerAssociatesPreferences(Collections.singleton(departmentUuid), Collections.singleton(daUuid), Collections.singleton(preferenceKey));
			GetDealerAssociatesPreferencesResponseDTO responseDTO = (GetDealerAssociatesPreferencesResponseDTO) getDTOfromProto(response, GetFeatureResponseDTO.class);
			LOGGER.info("in getDAPreference for department_uuid={} da_uuid={} preference={} response={}", departmentUuid, daUuid, preferenceKey, objectMapper.writeValueAsString(responseDTO));
			if(responseDTO == null || (responseDTO.getErrors() != null && !responseDTO.getErrors().isEmpty())) {
				LOGGER.error("error in getDAPreference for department_uuid={} da_uuid={} preference={} response={}",
					departmentUuid, daUuid, preferenceKey, objectMapper.writeValueAsString(responseDTO));
			}
			Set<PreferenceDTO> preferenceDtos = responseDTO.getDealerAssociatesPreferencesDTO().get(daUuid);
			if(preferenceDtos == null || preferenceDtos.isEmpty()) {
				return null;
			}
		return preferenceDtos.stream().filter(preference -> preference != null && preferenceKey.equalsIgnoreCase(preference.getKeyName())).findFirst().orElse(null);
		} catch (Exception e) {
			LOGGER.error("error in getDAPreference for department_uuid={} da_uuid={} preference={}",
				departmentUuid, daUuid, preferenceKey);
			return null;
		}
	}

	public Map<String, Set<AuthorityDTO>> sortInputAndGetDealerAssociatesAuthoritiesDTO(List<String> daUuids, List<String> departmentUuids, List<String> keys) throws Exception {
		List<String> daUuidsSorted = new ArrayList<>(daUuids);
		List<String> departmentUuidsSorted = new ArrayList<>(departmentUuids);
		List<String> keysSorted = new ArrayList<>(keys);
		Collections.sort(daUuidsSorted);
		Collections.sort(departmentUuidsSorted);
		Collections.sort(keysSorted);
		return getDealerAssociatesAuthoritiesDTO(daUuidsSorted, departmentUuidsSorted, keysSorted);
	}

	@Cacheable(value = CacheConfig.DEALER_ASSOCIATES_AUTHORITIES_CACHE, keyGenerator = "customKeyGenerator")
	public Map<String, Set<AuthorityDTO>> getDealerAssociatesAuthoritiesDTO(List<String> daUuids, List<String> departmentUuids, List<String> keys) throws Exception {

		Set<String> daUuidsSet = new HashSet<>(daUuids);
		Set<String> departmentUuidsSet = new HashSet<>(departmentUuids);
		Set<String> keysSet = new HashSet<>(keys);

		GetDealerAssociatesAuthoritiesResponse gdaar;

		try {
			gdaar = getDealerAssociatesAuthorities(daUuidsSet, departmentUuidsSet, keysSet);
		} catch (Exception e) {
			LOGGER.error(String.format("Error occured while fetching DealerAssociates from kmanage for dealerDepartmentUUIDsSet = %s", departmentUuids.toString(), e));
			return null;
		}

		GetDealerAssociatesAuthoritiesResponseDTO dtoResp = (GetDealerAssociatesAuthoritiesResponseDTO) getDTOfromProto(gdaar, GetDealerAssociatesAuthoritiesResponseDTO.class);
		return (dtoResp == null) ? null : dtoResp.getDealerAssociatesAuthoritiesDTO();
	}

	public GetDealerAssociatesAuthoritiesResponse getDealerAssociatesAuthorities(Set<String> daUuids, Set<String> departmentUuids, Set<String> keys) throws Exception {
		LOGGER.info(String.format("Sending get dealerAssociates authorities request to kmanage, uuids=%s and keys=%s", daUuids, keys));
		if (daUuids != null && !daUuids.isEmpty()) {
			GetDealerAssociatesAuthoritiesResponse response = getKManageClientService().getDealerAssociatesAuthorities(daUuids, departmentUuids, keys);
			LOGGER.info(String.format("Response received for get dealerAssociates authorities request , response = %s", response));
			ErrorProto.Error errorToThrow = null;
			if (response.getErrorsList().size() > 0) {
				for (ErrorProto.Error e : response.getErrorsList()) {
					if ((e.getErrorCode() == ErrorCodes.USER_NOT_MIGRATED.getErrorCode()) || (e.getErrorCode() == ErrorCodes.USER_NOT_FOUND.getErrorCode())) {
						return null;
					}
					if (errorToThrow == null)
						errorToThrow = e;
				}

				if (errorToThrow != null) {
					throw new Exception(errorToThrow.getErrorMessage());
				}
			}
			return response;
		}
		throw new Exception(ErrorCodes.INVALID_REQUEST_DATA.getErrorMessage());
	}

}

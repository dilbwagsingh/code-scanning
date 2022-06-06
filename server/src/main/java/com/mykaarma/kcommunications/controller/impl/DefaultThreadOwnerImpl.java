package com.mykaarma.kcommunications.controller.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.redis.RedisService;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications_model.response.DefaultThreadOwnerForDealerResponse;
import com.mykaarma.kmanage.model.dto.json.DealerAssociateExtendedDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDealerAssociatesForDealerUUIDsResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDealerAssociatesResponseDTO;

@Service
public class DefaultThreadOwnerImpl {

	private final static Logger LOGGER = LoggerFactory.getLogger(DefaultThreadOwnerImpl.class);	
	
	@Autowired
	private RedisService redisService;
	
	@Autowired
	private KManageApiHelper kManageApiHelper;
	
	private ObjectMapper objectMapper= new ObjectMapper();
	
	public String getDefaultThreadOwner(String departmentUUID,String userUUID) throws Exception{
		String defaultThreadOwnerUserUUID=null;
		defaultThreadOwnerUserUUID=redisService.getDefaultThreadOwnerUserUUIDForUserUUIDAndDealerAssociateUUID(departmentUUID, userUUID);
		if(defaultThreadOwnerUserUUID!=null && !defaultThreadOwnerUserUUID.isEmpty()){
			LOGGER.info("in getDefaultThreadOwner for department_uuid={} user_uuid={} daid_default_owner_user_uuid={}",departmentUUID,userUUID, defaultThreadOwnerUserUUID);
			return defaultThreadOwnerUserUUID;
		}
		Long dealerAssociateId=null;
		ArrayList<DealerAssociateExtendedDTO> dealerAssociatesList=getAllDealerAssociatesFoDepartmentUUID(departmentUUID);
		if(dealerAssociatesList==null || dealerAssociatesList.isEmpty()){
			LOGGER.info("in getDefaultThreadOwner unable to fetched all dealer associates for department_uuid={} user_uuid={}",departmentUUID,userUUID);
			return defaultThreadOwnerUserUUID;
		}
		HashMap<String,String> userUUIDDefaultThreadOwnerUserUUIDMap=new HashMap<String,String>();
	
		for(DealerAssociateExtendedDTO dealerAssociateIterator:dealerAssociatesList){
			Long daId=null;
			String tempUserUUID=null;
			String tempDefaultThreadOwnerUserUUID=null;
			if(dealerAssociateIterator.getId()!=null){
				daId=dealerAssociateIterator.getId();
			}
			if(dealerAssociateIterator.getUserUuid()!=null){
				tempUserUUID=dealerAssociateIterator.getUserUuid();
			}
			if(dealerAssociateIterator.getDefaultThreadOwnerUserUUID()!=null){
				tempDefaultThreadOwnerUserUUID=dealerAssociateIterator.getDefaultThreadOwnerUserUUID();
			}
			if(userUUID.equals(tempUserUUID)){
				dealerAssociateId=daId;
			}
			LOGGER.info("in getDefaultThreadOwner iterating for dealer_associate_id={} user_uuid={} default_thread_owner_uuid={}",daId,tempUserUUID,tempDefaultThreadOwnerUserUUID);
			if(tempUserUUID!=null && !tempUserUUID.isEmpty() && tempDefaultThreadOwnerUserUUID!=null &&  !tempDefaultThreadOwnerUserUUID.isEmpty()){
				userUUIDDefaultThreadOwnerUserUUIDMap.put(tempUserUUID, tempDefaultThreadOwnerUserUUID);
			}
		}
		
		LOGGER.info("in getDefaultThreadOwner for department_uuid={} request_da_id={} user_uuid_default_owner_map={}",departmentUUID,dealerAssociateId,objectMapper.writeValueAsString(userUUIDDefaultThreadOwnerUserUUIDMap));
	
		if(userUUIDDefaultThreadOwnerUserUUIDMap==null || userUUIDDefaultThreadOwnerUserUUIDMap.isEmpty()
				|| userUUIDDefaultThreadOwnerUserUUIDMap.get(userUUID)==null || userUUIDDefaultThreadOwnerUserUUIDMap.get(userUUID).isEmpty()){
			LOGGER.info("in getDefaultThreadOwner no default thread owner configured for department_uuid={} request_da_id={} user_uuid={}",departmentUUID,dealerAssociateId,userUUID);
			return defaultThreadOwnerUserUUID; 
		}
	
		defaultThreadOwnerUserUUID=getDefaultThreadOwnerForUserUUID(userUUIDDefaultThreadOwnerUserUUIDMap,userUUID);
		LOGGER.info("in getDefaultThreadOwner default thread owner configured for department_uuid={} request_da_id={} user_uuid={} "
				+ "is  default_thread_owner_user_uuid={}",departmentUUID,dealerAssociateId,
				userUUID,defaultThreadOwnerUserUUID);
		
		
		if(defaultThreadOwnerUserUUID!=null){
			redisService.pushDefaultThreadOwnerUserUUIDForDepartmentUUIDAndUserUUID(departmentUUID,userUUID, defaultThreadOwnerUserUUID);
		}
		return defaultThreadOwnerUserUUID;
	}
	
	private String getDefaultThreadOwnerForUserUUID(HashMap<String,String> userUUIDDefaultThreadOwnerUserUUIDMap,String userUUID) throws Exception{
		HashMap<String,Boolean> userUUIDsInThreadOwnershipChain=new HashMap<String,Boolean>();
		userUUIDsInThreadOwnershipChain.put(userUUID, Boolean.TRUE);
		String defaultThreadOwnerUserUUID=userUUIDDefaultThreadOwnerUserUUIDMap.get(userUUID);
		while(userUUIDDefaultThreadOwnerUserUUIDMap.get(defaultThreadOwnerUserUUID)!=null){
			String tempDefaultThreadOwnerUserUUID=userUUIDDefaultThreadOwnerUserUUIDMap.get(defaultThreadOwnerUserUUID);
			if(userUUIDsInThreadOwnershipChain.get(tempDefaultThreadOwnerUserUUID)!=null && userUUIDsInThreadOwnershipChain.get(tempDefaultThreadOwnerUserUUID)){
				LOGGER.warn(String.format("loop detected in getDefaultThreadOwnerForUserUUID for user_uuid=%s user_uuid_default_owner_map=%s",userUUID,objectMapper.writeValueAsString(userUUIDDefaultThreadOwnerUserUUIDMap)));
				return null;
			}
			
			userUUIDsInThreadOwnershipChain.put(tempDefaultThreadOwnerUserUUID, Boolean.TRUE);
			defaultThreadOwnerUserUUID=tempDefaultThreadOwnerUserUUID;
		}
		return defaultThreadOwnerUserUUID;
	}
	
	public DefaultThreadOwnerForDealerResponse getDefaultThreadOwnerInfoForDealer(String dealerUUID) throws Exception{
		DefaultThreadOwnerForDealerResponse defaultThreadOwnerForDealerResponse=new DefaultThreadOwnerForDealerResponse();
		ArrayList<DealerAssociateExtendedDTO> dealerAssociatesList=getAllDealerAssociatesFoDealerUUID(dealerUUID);
		if(dealerAssociatesList==null || dealerAssociatesList.isEmpty()){
			LOGGER.info("in getDealerAssociateIDAndDefaultThreadOwnerDAIDMap unable to fetched all dealer associates for dealer_uuid={}",dealerUUID);
			return defaultThreadOwnerForDealerResponse;
		}
		HashMap<String,String> userUUIDDefaultThreadOwnerUserUUIDMap=new HashMap<String,String>();
		HashMap<Long,Long> dealerAssociateIDDefaultThreadOwnerIDMap=new HashMap<Long,Long>();
		HashMap<String,Long> userUUIDDealerAssociateIDMap=new HashMap<String,Long>();
		List<Long> emptyDefaultThreadOwnerDAIDList=new ArrayList<Long>();
		for(DealerAssociateExtendedDTO dealerAssociateIterator:dealerAssociatesList){
			Long daId=null;
			String tempUserUUID=null;
			String tempDefaultThreadOwnerUserUUID=null;
			if(dealerAssociateIterator.getId()!=null){
				daId=dealerAssociateIterator.getId();
			}
			if(dealerAssociateIterator.getUserUuid()!=null){
				tempUserUUID=dealerAssociateIterator.getUserUuid();
			}
			if(dealerAssociateIterator.getDefaultThreadOwnerUserUUID()!=null){
				tempDefaultThreadOwnerUserUUID=dealerAssociateIterator.getDefaultThreadOwnerUserUUID();
			}
			userUUIDDealerAssociateIDMap.put( tempUserUUID,daId);
			LOGGER.info("in getDealerAssociateIDAndDefaultThreadOwnerDAIDMap iterating for dealer_uuid={} user_uuid={} default_thread_owner_uuid={}",dealerUUID,tempUserUUID,tempDefaultThreadOwnerUserUUID);
			if(tempUserUUID!=null && !tempUserUUID.isEmpty() && tempDefaultThreadOwnerUserUUID!=null &&  !tempDefaultThreadOwnerUserUUID.isEmpty()){
				userUUIDDefaultThreadOwnerUserUUIDMap.put(tempUserUUID, tempDefaultThreadOwnerUserUUID);
			} else if(tempDefaultThreadOwnerUserUUID==null ||  tempDefaultThreadOwnerUserUUID.isEmpty()){
				emptyDefaultThreadOwnerDAIDList.add(daId);
			}
			
		}
		HashMap<Long,Long> finalDealerAssociateIDDefaultThreadOwnerIDMap=new HashMap<Long,Long>();
		if(userUUIDDefaultThreadOwnerUserUUIDMap!=null){
			for(String userUUID:userUUIDDefaultThreadOwnerUserUUIDMap.keySet()){
				String defaultThreadOwnerUserUUID=userUUIDDefaultThreadOwnerUserUUIDMap.get(userUUID);
				Long dealerAssociateID=userUUIDDealerAssociateIDMap.get(userUUID);
				Long defaultThreadOwnerDAID=userUUIDDealerAssociateIDMap.get(defaultThreadOwnerUserUUID);
				dealerAssociateIDDefaultThreadOwnerIDMap.put(dealerAssociateID, defaultThreadOwnerDAID);
				LOGGER.info("in getDealerAssociateIDAndDefaultThreadOwnerDAIDMap iterating for dealer_uuid={}"
						+ " dealer_associate_id={} default_thread_owner_da_id={}",dealerUUID,dealerAssociateID,defaultThreadOwnerDAID);
				
			}
			
			for(Long dealerAssociateId:dealerAssociateIDDefaultThreadOwnerIDMap.keySet()){
				if(finalDealerAssociateIDDefaultThreadOwnerIDMap.get(dealerAssociateId)==null){
					getFinalDealerAssociateIdAndDefaultThreadOwnerIDMap(finalDealerAssociateIDDefaultThreadOwnerIDMap,dealerAssociateIDDefaultThreadOwnerIDMap,
							dealerAssociateId,emptyDefaultThreadOwnerDAIDList);
				}
				LOGGER.info("in getDealerAssociateIDAndDefaultThreadOwnerDAIDMap iterating for dealer_uuid={}"
						+ " final default_thread_owner_da_id={} for dealer_associate_id={}",dealerUUID,
						finalDealerAssociateIDDefaultThreadOwnerIDMap.get(dealerAssociateId),dealerAssociateId);
			}
		}
		
		defaultThreadOwnerForDealerResponse.setDealerAssociateDefaultThreadOwnerMap(finalDealerAssociateIDDefaultThreadOwnerIDMap);
		defaultThreadOwnerForDealerResponse.setEmptyDefaultThreadOwnerDealerAssociateList(emptyDefaultThreadOwnerDAIDList);
		return defaultThreadOwnerForDealerResponse;
	}
	
	private void getFinalDealerAssociateIdAndDefaultThreadOwnerIDMap(HashMap<Long,Long> finalDealerAssociateIDDefaultThreadOwnerIDMap,
			HashMap<Long,Long> dealerAssociateIDDefaultThreadOwnerIDMap,Long dealerAssociateId,List<Long> emptyDefaultThreadOwnerDAIDList){
		if(dealerAssociateIDDefaultThreadOwnerIDMap==null || dealerAssociateIDDefaultThreadOwnerIDMap.get(dealerAssociateId)==null){
			if(!emptyDefaultThreadOwnerDAIDList.contains(dealerAssociateId)){
				emptyDefaultThreadOwnerDAIDList.add(dealerAssociateId);
			}
			return;
		}
		if(dealerAssociateIDDefaultThreadOwnerIDMap.get(dealerAssociateId).equals(dealerAssociateId)){
			finalDealerAssociateIDDefaultThreadOwnerIDMap.put(dealerAssociateId, dealerAssociateId);
		}
		List<Long> dealerAssociateList=new ArrayList<Long>();
		while(dealerAssociateIDDefaultThreadOwnerIDMap.get(dealerAssociateId)!=null 
				&& dealerAssociateIDDefaultThreadOwnerIDMap.get(dealerAssociateId)!=dealerAssociateId){
			dealerAssociateList.add(dealerAssociateId);
			Long defaultThreadOwner=dealerAssociateIDDefaultThreadOwnerIDMap.get(dealerAssociateId);
			LOGGER.info("in getFinalDealerAssociateIdAndDefaultThreadOwnerIDMap iterating for "
					+ "  for dealer_associate_id={} default_thread_owner_da_id={} ",dealerAssociateId,defaultThreadOwner);
			if(dealerAssociateList!=null && dealerAssociateList.contains(defaultThreadOwner)){
				LOGGER.info("loop deteceted in getFinalDealerAssociateIdAndDefaultThreadOwnerIDMap iterating for "
						+ "  for dealer_associate_id={}",dealerAssociateId);
				break;
			}
			dealerAssociateId=defaultThreadOwner;
			
		}
		for(Long dealerAssociateIterator:dealerAssociateList){
			finalDealerAssociateIDDefaultThreadOwnerIDMap.put(dealerAssociateIterator, dealerAssociateId);
			LOGGER.info("in getFinalDealerAssociateIdAndDefaultThreadOwnerIDMap final default_thread_owner_da_id={} for "
					+ "  for dealer_associate_id={}",dealerAssociateId,dealerAssociateIterator);
		}
		
	}
	
	private ArrayList<DealerAssociateExtendedDTO> getAllDealerAssociatesFoDealerUUID(String dealerUUID) throws Exception{
		if(dealerUUID==null || dealerUUID.isEmpty()){
			return null;
		}
		GetDealerAssociatesForDealerUUIDsResponseDTO dealerAssociatesResponseDTO=kManageApiHelper.getAllDealerAssociatesForDealerUUID(dealerUUID);
		if(dealerAssociatesResponseDTO!=null && dealerAssociatesResponseDTO.getDealerAssociates()!=null){
			ArrayList<DealerAssociateExtendedDTO> dealerAssociatesList=new ArrayList<DealerAssociateExtendedDTO>();
			if(dealerAssociatesResponseDTO!=null &&  dealerAssociatesResponseDTO.getDealerAssociates()!=null){
				dealerAssociatesList.addAll(dealerAssociatesResponseDTO.getDealerAssociates().get(dealerUUID));
				return dealerAssociatesList;
			}
		}
		return null;
		
	}
	
	private ArrayList<DealerAssociateExtendedDTO> getAllDealerAssociatesFoDepartmentUUID(String departmentUUID) throws Exception{
		if(departmentUUID==null || departmentUUID.isEmpty()){
			return null;
		}
		GetDealerAssociatesResponseDTO dealerAssociatesResponseDTO=kManageApiHelper.getAllDealerAssociatesForDepartmentUUID(departmentUUID);
		if(dealerAssociatesResponseDTO!=null && dealerAssociatesResponseDTO.getDealerAssociates()!=null){
			ArrayList<DealerAssociateExtendedDTO> dealerAssociatesList=new ArrayList<DealerAssociateExtendedDTO>();
			if(dealerAssociatesResponseDTO!=null &&  dealerAssociatesResponseDTO.getDealerAssociates()!=null){
				dealerAssociatesList.addAll(dealerAssociatesResponseDTO.getDealerAssociates().values());
				return dealerAssociatesList;
			}
		}
		return null;
		
	}
	
}

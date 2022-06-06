package com.mykaarma.kcommunications.controller.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mykaarma.kcommunications.jpa.repository.ThreadRepository;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications_model.request.ThreadCountRequest;
import com.mykaarma.kcommunications_model.response.ThreadCountResponse;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;
import com.mykaarma.kmanage.model.dto.json.response.GetDepartmentResponseDTO;

@Service
public class ThreadImpl {
	
	@Autowired
	private ThreadRepository threadRepository;
	
	@Autowired
	private KManageApiHelper kManageApiHelper;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(ThreadImpl.class);	
	
	public ThreadCountResponse getNumberOfThreads(String userUUID, ThreadCountRequest request) throws Exception{

		ThreadCountResponse response = new ThreadCountResponse();
		Map<String,Long> countMap = new HashMap<String,Long>();
		if(request!=null && request.getDepartmentUuids()!=null) {
			Set<String> deptUuidList = request.getDepartmentUuids();
			for(String deptUUID: deptUuidList) {
				GetDepartmentResponseDTO dealerDepartment = kManageApiHelper.getDealerDepartment(deptUUID);
				LOGGER.info("in getNumberOfThreads for departmet_uuid={} user_uuid={}",deptUUID, userUUID);
				if(dealerDepartment!=null && dealerDepartment.getDepartmentExtendedDTO()!=null) {
					GetDealerAssociateResponseDTO dealerAssociate = kManageApiHelper.getDealerAssociate(deptUUID, userUUID);
					if(dealerAssociate!=null && dealerAssociate.getDealerAssociate()!=null && dealerAssociate.getDealerAssociate().getId()!=null) {
						Long count = threadRepository.getThreadCountForDealerAssociateId(dealerAssociate.getDealerAssociate().getId());
						countMap.put(deptUUID, count);
					}
				}
			}
			response.setCount(countMap);
			return response;
		}
		return null;
	}
}

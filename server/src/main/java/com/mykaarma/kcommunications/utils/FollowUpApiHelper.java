package com.mykaarma.kcommunications.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.followup.client.FollowUpApiClientService;
import com.mykaarma.followup.model.request.MultipleFollowupUpdateRequest;
import com.mykaarma.followup.model.response.FollowUpUpdateResponse;

@Service
public class FollowUpApiHelper {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(FollowUpApiHelper.class);
	private static String username ;
	private static String password ;
	private static String base_url ;
	
	private static ObjectMapper objectMapper= new ObjectMapper();
	
	private static FollowUpApiClientService clientService = null;

	@Value("${kcommunications_basic_auth_user}")
	public  void setUsername(String username) {
		FollowUpApiHelper.username = username;
	}

	
    @Value("${kcommunications_basic_auth_pass}")
	public  void setPassword(String password) {
    	FollowUpApiHelper.password = password;
	}

    @Value("${follow_up_api_url}")
	public  void setBase_url(String base_url) {
    	FollowUpApiHelper.base_url = base_url;
	}

    private static FollowUpApiClientService getFollowUpApiClientService(){
		if(clientService==null){
			clientService = new FollowUpApiClientService(base_url,username,password);
		}
		return clientService;
		
	}
    
    public FollowUpUpdateResponse updateFollowUps(String dealerDepartmentUUID,String userUUID,MultipleFollowupUpdateRequest multipleFollowupUpdateRequest) throws Exception{
    	try {
    		LOGGER.info(String.format("in updateFollowUps for department_uuid=%s user_uuid=%s request=%s", dealerDepartmentUUID,
					userUUID,new ObjectMapper().writeValueAsString(multipleFollowupUpdateRequest)));
			FollowUpUpdateResponse followUpUpdateResponse=getFollowUpApiClientService().updateFollowUps(dealerDepartmentUUID, userUUID, multipleFollowupUpdateRequest);
			return followUpUpdateResponse;
		} catch (Exception e) {
			LOGGER.error(String.format("error in updateFollowUps for department_uuid=%s user_uuid=%s request=%s", dealerDepartmentUUID,
					userUUID,new ObjectMapper().writeValueAsString(multipleFollowupUpdateRequest)), e);
			e.printStackTrace();
		} catch (Throwable e) {
			LOGGER.error(String.format("error in updateFollowUps for department_uuid=%s user_uuid=%s request=%s", dealerDepartmentUUID,
					userUUID,new ObjectMapper().writeValueAsString(multipleFollowupUpdateRequest)), e);
		}
    	return null;
    }

}

package com.mykaarma.kcommunications.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.mykaarma.korder_client.KorderApiClientService;
import com.mykaarma.korder_model.common.StandardOrder;
import com.mykaarma.korder_model.response.GetStandardOrderResponse;

@Component
@Service
public class KOrderApiHelper {
	private final static Logger LOGGER = LoggerFactory.getLogger(KOrderApiHelper.class);
	private static String username ;
	private static String password ;
	private static String base_url ;
	
	private static KorderApiClientService clientService = null;
	
	@Value("${kcommunications_basic_auth_user}")
	public void setUsername(String username) {
		KOrderApiHelper.username = username;
	}

	
    @Value("${kcommunications_basic_auth_pass}")
	public void setPassword(String password) {
    	KOrderApiHelper.password = password;
	}

    @Value("${korder_api_url}")
	public void setBase_url(String base_url) {
    	KOrderApiHelper.base_url = base_url;
	}


	private static KorderApiClientService getKOrderClientService(){
		if(clientService==null){
			clientService = new KorderApiClientService(base_url,username,password);
		}
		return clientService;
		
	}
	

	
	public static StandardOrder getDealerOrder(String departmentUUID, String dealerOrderUUID) {
		try {
			GetStandardOrderResponse dealerOrder = getKOrderClientService().getStandardOrder(dealerOrderUUID, departmentUUID);
			return dealerOrder.getOrder();
		} catch (Exception e) {
			LOGGER.error(String.format("Error while getDealerOrder for dealer_order_uuid=%s department_uuid=%s ", 
					dealerOrderUUID,departmentUUID),e);
			return null;
		}
	}
	

}

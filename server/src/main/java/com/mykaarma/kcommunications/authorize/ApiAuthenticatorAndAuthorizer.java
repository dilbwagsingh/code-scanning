package com.mykaarma.kcommunications.authorize;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerMapping;

import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.utils.APIConstants;
import com.mykaarma.kcommunications.utils.KCommunicationsException;
import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;
import com.mykaarma.kcommunications_model.enums.ErrorCode;

@Service
public class ApiAuthenticatorAndAuthorizer {

	private final static Logger LOGGER = LoggerFactory.getLogger(ApiAuthenticatorAndAuthorizer.class);
	
	@Autowired
	GeneralRepository generalRepository;
	
	@SuppressWarnings("unchecked")
    public Boolean isAuthenticated(HttpServletRequest httpServletRequest,
                                   Annotation[] annotations) throws Exception {

        String authString = httpServletRequest.getHeader("authorization");
        if (authString == null) {
        	
        		LOGGER.warn("Login credentials missing from header.");
            throw new KCommunicationsException(ErrorCode.CREDENTIALS_ARE_NULL);
        } else {
            String[] authParts = authString.split("\\s+");
            String authInfo = authParts[1];
            byte[] bytes;
            try {
                bytes = java.util.Base64.getDecoder().decode(authInfo);
            } catch (Exception e) {
            	
            		LOGGER.warn("Exception caught while decoding credentials: " + e);
                throw new KCommunicationsException(ErrorCode.WRONG_CREDENTIAL);
            }
            String decodedAuth = new String(bytes);

            final String[] values = decodedAuth.split(":", 2);
            
            String username = values[0];
            String password = values[1];
            HashMap<String, String> serviceSubscriber = null;
            try {
            		LOGGER.info("username={}",username);
            		serviceSubscriber  = generalRepository.findFirstServiceSubscriberByUserName(username);
         
            } catch (Exception e) {
				
            		LOGGER.warn("Error while fetching service subscriber credentials .Aborting ..",e);
            		throw new KCommunicationsException(ErrorCode.WRONG_CREDENTIAL);
			}
        	if (serviceSubscriber == null) {
        				
        				LOGGER.warn("Login credentials are wrong for username={}", username);
        				throw new KCommunicationsException(ErrorCode.WRONG_CREDENTIAL);
        	}
        				
        	if (!serviceSubscriber.get("password").equalsIgnoreCase(password)) {
        				 
        				 LOGGER.warn("Password do not match for username={}", username);
        				 throw new KCommunicationsException(ErrorCode.WRONG_CREDENTIAL);
        	}
        			 
        	if( !serviceSubscriber.get("valid").equalsIgnoreCase("true")) {
        				 
        				 LOGGER.warn("Service Subscriber is invalid username={}", username);
        				 throw new KCommunicationsException(ErrorCode.INVALID_SERVICE_SUBSCRIBER);
        	}                            
          
            //Authenticated , proceeding with Authorization
            LOGGER.info("Successfully Authenticated and now authorizing");
            HashMap<String, String> pathVariables =
		 			(HashMap<String, String>) httpServletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            return checkAuthorization(httpServletRequest, username, pathVariables, annotations);
        }
            
    }
	
    public String getServiceSubscriberName(String authString) throws Exception {

        
        if (authString == null) {
        	
        		LOGGER.warn("Login credentials missing from header.");
            throw new KCommunicationsException(ErrorCode.CREDENTIALS_ARE_NULL);
        } else {
            String[] authParts = authString.split("\\s+");
            String authInfo = authParts[1];
            byte[] bytes;
            try {
                bytes = java.util.Base64.getDecoder().decode(authInfo);
            } catch (Exception e) {
            	
            		LOGGER.warn("Exception caught while decoding credentials: " + e);
                throw new KCommunicationsException(ErrorCode.WRONG_CREDENTIAL);
            }
            String decodedAuth = new String(bytes);

            final String[] values = decodedAuth.split(":", 2);
            
            String username = values[0];
            String password = values[1];
            HashMap<String, String> serviceSubscriber = null;
            try {
            			serviceSubscriber  = generalRepository.findFirstServiceSubscriberByUserName(username);
         
            }catch (Exception e) {
				
            		LOGGER.warn("Error while fetching service subscriber credentials .Aborting ..",e);
            		throw new KCommunicationsException(ErrorCode.WRONG_CREDENTIAL);
			}
            	if (serviceSubscriber == null) {
            				
            				LOGGER.warn("Login credentials are wrong");
            				throw new KCommunicationsException(ErrorCode.WRONG_CREDENTIAL);
            	}
            				
            	if (!serviceSubscriber.get("password").equalsIgnoreCase(password)) {
            				 
            				 LOGGER.warn("Password do no match");
            				 throw new KCommunicationsException(ErrorCode.WRONG_CREDENTIAL);
            	}
            			 
            	if( !serviceSubscriber.get("valid").equalsIgnoreCase("true")) {
            				 
            				 LOGGER.warn("Service Subscriber is invalid");
            				 throw new KCommunicationsException(ErrorCode.INVALID_SERVICE_SUBSCRIBER);
            	}                            
          
           return serviceSubscriber.get("name");
        }
            
    }
	
	
	private Boolean checkAuthorization(HttpServletRequest httpServletRequest,String username,HashMap<String, String> pathVariables,
             Annotation[] annotations) throws Exception {

		 	
		 	ApiScope apiScope = null;
		 	ApiScopeLevel apiScopeLevel = null;
		 	if (!ArrayUtils.isEmpty(annotations)) {

		 			for (Annotation annotation : annotations) {
		 				if (annotation instanceof KCommunicationsAuthorize) {
		 					apiScope = ((KCommunicationsAuthorize) annotation).apiScope();
		 					apiScopeLevel = ((KCommunicationsAuthorize) annotation).apiScopeLevel();
		 				}
		 			}
		 	}
		 	if (apiScope != null) {
		 		if (ApiScopeLevel.SERVICE_SUBSCRIBER_LEVEL.equals(apiScopeLevel)) {
		 			String authString = httpServletRequest.getHeader("authorization");
		 			return checkServiceSubscriberToApiScopeAuthorization(getServiceSubscriberName(authString), apiScope);
		 		} else if(apiScopeLevel.equals(ApiScopeLevel.DEPARTMENT_LEVEL)) {
		 				String departmentUuid = pathVariables.get(APIConstants.DEPARTMENT_TOKEN);
		 				return checkDepartmentLevelAuthorization(httpServletRequest,apiScope,username, departmentUuid);	
		 		} else if(apiScopeLevel.equals(ApiScopeLevel.DEALER_LEVEL)){
		 			String dealerUuid=pathVariables.get(APIConstants.DEALER_TOKEN);
		 			return checkDealerLevelAuthorization(httpServletRequest,apiScope,username, dealerUuid);
		 		}
		 	}
		 	return Boolean.FALSE;	
		 	
	}
	
	private Boolean checkDepartmentLevelAuthorization(HttpServletRequest httpServletRequest,
                    ApiScope apiScope,
                    String username, String dealerDepartmentUuid) throws Exception { 	
	       try {
	            	
	    	           if(!generalRepository.authenticateSubscriberForScope(username, dealerDepartmentUuid, apiScope.getApiScopeName())) {
	    	            	 
	    	        	   		LOGGER.warn("Service subscriber doesnot have required api scope api_scope={}",
	    	        	   				apiScope.getApiScopeName());
	    	            	 	throw new KCommunicationsException(ErrorCode.NOT_AUTHORIZED_REQUEST);
	    	           } 	 			
	    	           else {
	    	        	   		
	    	        	   		LOGGER.info("Successfully Authenticated and  authorized");
	    	        	   		httpServletRequest.setAttribute(APIConstants.DEALER_DEPARMENT_TOKEN, dealerDepartmentUuid);
	    	        	   		return  Boolean.TRUE;
	    	           }
	    	            	 
	       }catch (Exception e) {
			
	    	   		LOGGER.warn("Error while validating apiscope for service_sucriber username={} api_scope={} dealer_department_uuid={} ",
	    	   				username,apiScope.getApiScopeName(), dealerDepartmentUuid,e);
	    	   		throw new KCommunicationsException(ErrorCode.NOT_AUTHORIZED_REQUEST);
		} 
	}
	

	private Boolean checkDealerLevelAuthorization(HttpServletRequest httpServletRequest,ApiScope apiScope,
            String username, String dealerUuid) throws Exception{
		
		try {
			
        	if(!generalRepository.authenticateSubscriberForDealerLevelScope(username, dealerUuid, apiScope.getApiScopeName())) {
	            	 LOGGER.warn("Service subscriber doesnot have required api scope api_scope={}",
	        	   				apiScope.getApiScopeName());
	            	 throw new KCommunicationsException(ErrorCode.NOT_AUTHORIZED_REQUEST);
	        } else {
	        	LOGGER.info("Successfully Authenticated and  authorized");
	        	httpServletRequest.setAttribute(APIConstants.DEALER_TOKEN, dealerUuid);
	        	return  Boolean.TRUE;
	           }
	            	 
		}catch (Exception e) {
	
	   		LOGGER.warn("Error while validating apiscope for service_sucriber username={} api_scope={} dealer_uuid={} ",
	   				username,apiScope.getApiScopeName(), dealerUuid,e);
	   		throw new KCommunicationsException(ErrorCode.NOT_AUTHORIZED_REQUEST);
		} 
	}
	
	@Deprecated
	public Boolean checkServiceSubscriberToApiScopeAuthorization(String serviceSubscriberName, ApiScope apiScope)
			throws KCommunicationsException {

		try {
			if (!generalRepository.authenticateSubscriberForScope(serviceSubscriberName, apiScope.getApiScopeName())) {
				LOGGER.warn("service_subscriber={} doesnot have required api scope api_scope={}",serviceSubscriberName, apiScope.getApiScopeName());
				throw new KCommunicationsException(ErrorCode.NOT_AUTHORIZED_REQUEST);
			} else {
				LOGGER.info("Successfully Authenticated and authorized");
				return Boolean.TRUE;
			}
		} catch (Exception e) {
			LOGGER.warn("Error while validating apiscope for service_sucriber username={} api_scope={}",
					serviceSubscriberName, apiScope.getApiScopeName(), e);
			throw new KCommunicationsException(ErrorCode.NOT_AUTHORIZED_REQUEST);
		}

	}
	    
}

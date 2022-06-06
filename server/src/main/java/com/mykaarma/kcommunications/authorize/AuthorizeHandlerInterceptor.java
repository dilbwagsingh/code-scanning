package com.mykaarma.kcommunications.authorize;

import java.util.HashMap;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.google.gson.JsonObject;
import com.mykaarma.kcommunications.utils.APIConstants;


@Service
public class AuthorizeHandlerInterceptor extends HandlerInterceptorAdapter {
	
	 @Autowired
	 ApiAuthenticatorAndAuthorizer apiAuthenticatorAndAuthorizer;
	 
	 private final static Logger LOGGER = LoggerFactory.getLogger(AuthorizeHandlerInterceptor.class);
	 
	 
	 @Override
	 public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			 throws Exception{
		 	HashMap<String, String> pathVariables =
		 			(HashMap<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		 	
		 	JsonObject loguuidJson = new JsonObject();
		 	String requestID = UUID.randomUUID().toString();
		 	loguuidJson.addProperty(APIConstants.IP_ADDRESS , IpUtils.getIpAddress(request));
		 	loguuidJson.addProperty(APIConstants.REQUEST_ID, requestID);
		 	
		 	request.setAttribute(APIConstants.REQUEST_ID, requestID);
		 	request.setAttribute(APIConstants.START_TIME, System.nanoTime());
		 	MDC.put(APIConstants.LogUUID, loguuidJson.toString());
	        
			if(skipAuthentication(request))
	            return true;
			
		 	try {
	        
		 		if (handler instanceof HandlerMethod) {
		 			final HandlerMethod handlerMethod = (HandlerMethod) handler;
		 			final RequestMapping requestMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
		 			if (requestMapping != null) {
		 				LOGGER.info(" Beginning  to Authenticate and Authorize request");
		 				return apiAuthenticatorAndAuthorizer.isAuthenticated(request, handlerMethod.getMethod().getAnnotations());
		 			} else {
	                //Not an API call
		 				LOGGER.debug("Allowing request without request mapping url=", request.getRequestURL().toString());
		 				return true;
		 			}

		 		} else {
		 			return true;
		 		}
		 	}catch (Exception e) {
				// TODO: handle exception
		 		throw e;
			}
	    }
	 
	 	@Override
	    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) {
	       
	 		Long startTime = (Long) request.getAttribute(APIConstants.START_TIME);
            if (startTime == null) {
                startTime = System.nanoTime();
            }
            long timeTakenForRequest = System.nanoTime()-startTime;
            if(timeTakenForRequest>2e9){
            	LOGGER.info("Time taken is >2s for this request");
            }
            LOGGER.info("time_taken={} ns for sending response response_status={}", timeTakenForRequest, response.getStatus());
	        MDC.clear();
	    }

	    @Override
	    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
	        MDC.clear();
	    }

	    private boolean skipAuthentication(HttpServletRequest request) {
	        boolean result = false;
	        LOGGER.info("uri={}", request.getRequestURI());
	        if(request.getRequestURI().contains("message/response")) {	  
	        	 LOGGER.info("version_skip=true");
	                result = true;
	            }
	        return result;
	    }


}

package com.mykaarma.kcommunications.filter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.utils.APIConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {
	
	@Autowired
	GeneralRepository generalRepository;


    private final static Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

    	ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpServletResponse);

        long start = System.currentTimeMillis();
        try {
              filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
        	
        	if("true".equalsIgnoreCase(MDC.get(APIConstants.FILTER_REQUEST)))
            {
        		doLog(requestWrapper, responseWrapper, start);
            }
        	responseWrapper.copyBodyToResponse();
        }


    }

    private void doLog(ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper, long start) {
        long end = System.currentTimeMillis();
        String requestBody = new String(requestWrapper.getContentAsByteArray(), Charset.forName("UTF-8"));
        String responseBody = new String(responseWrapper.getContentAsByteArray(), Charset.forName("UTF-8"));
        String method = requestWrapper.getMethod();
        String uri = requestWrapper.getRequestURI();
        if("GET".equalsIgnoreCase(method)) {
        	requestBody = requestWrapper.getQueryString();
        }
       
        String messageId = MDC.get(APIConstants.LogUUID);
        
        JsonParser parser = new JsonParser();
        JsonObject loguuidJson = parser.parse(messageId).getAsJsonObject();
        String dealerDeptUUID = loguuidJson.get(APIConstants.DEALER_DEPARMENT_TOKEN)!=null?loguuidJson.get(APIConstants.DEALER_DEPARMENT_TOKEN).toString():"";
        Long dealerId = null;
        Long dealerDepartmentId=null;
        
        try{
        	String infoForLogging = generalRepository.getDealerContextForLoggingfromDealerDepartmentUUID(dealerDeptUUID);
        	String[] info = infoForLogging.split(",");
        	dealerDepartmentId=Long.parseLong(info[0]);
            dealerId=Long.parseLong(info[1]);
        }catch(Exception e)
        {
        	LOGGER.info("Error occurred while fetching dealer context");
        }
        
        
        int status = responseWrapper.getStatus();
        long timeTaken = end-start;
        
        JsonObject logJson = new JsonObject();
        logJson.addProperty("message_id", messageId);
        logJson.addProperty("dealer_id", dealerId);
        logJson.addProperty("dealer_department_id", dealerDepartmentId);
        logJson.addProperty("time_taken", timeTaken);
        logJson.addProperty("uri", uri);
        logJson.addProperty("request_method", method);
        logJson.addProperty("response_status", status);
        logJson.addProperty("request_string", requestBody);
        logJson.addProperty("response_string", responseBody);
        
        String logString = logJson.toString();
        
        LOGGER.info("reconciliation_log={} ", logString);
        MDC.get(APIConstants.LogUUID);
        MDC.remove(APIConstants.FILTER_REQUEST);
    }


}

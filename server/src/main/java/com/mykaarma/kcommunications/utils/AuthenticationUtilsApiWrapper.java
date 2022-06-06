package com.mykaarma.kcommunications.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.api.MkApiClient;
import com.mykaarma.authenticationutilsclient.v2.AuthenticationUtilsV2ClientService;
import com.mykaarma.authenticationutilsmodel.model.v2.request.AuthorizationRequest;
import com.mykaarma.authenticationutilsmodel.model.v2.response.AuthorizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class AuthenticationUtilsApiWrapper {

    @Value("${authentication_utils_api_url}")
    private String url;

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationUtilsApiWrapper.class);

    private AuthenticationUtilsV2ClientService authenticationUtilsV2ClientService = null;

    @PostConstruct
    public void init() {
        authenticationUtilsV2ClientService = new AuthenticationUtilsV2ClientService(url, MkApiClient.MK_COMMUNICATIONS_API);
    }

    public AuthorizationResponse authorize(String requesterUuid, AuthorizationRequest request) {

        try {
            AuthorizationResponse response = authenticationUtilsV2ClientService.authorize(requesterUuid, request);
            LOGGER.info("Response received from Authentication Utils response={}", Helper.toString(response));

            if(response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
                LOGGER.error("Error response from Authentication Utils while authorizing for requesterUuid={} request={} response={}",
                    requesterUuid, Helper.toString(request), Helper.toString(response.getErrors()));
                return null;
            }

            return response;
        } catch (Exception e) {
            LOGGER.error("Error occurred while authorizing requesterUuid={} request={}", requesterUuid, Helper.toString(request), e);
        }

        return null;
    }

}

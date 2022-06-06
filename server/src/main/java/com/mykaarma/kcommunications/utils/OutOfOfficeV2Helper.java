package com.mykaarma.kcommunications.utils;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.MessageProtocol;
import com.mykaarma.outofoffice_client.OutOfOfficeApiClientService;
import com.mykaarma.outofoffice_model.dto.DealerAssociateDto;
import com.mykaarma.outofoffice_model.request.TurnOffMessageRequest;
import com.mykaarma.outofoffice_model.response.DelegateeResponse;
import com.mykaarma.outofoffice_model.response.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OutOfOfficeV2Helper {

    @Value("${outofoffice_url_v2}")
    private String url;

    @Value("${kcommunications_basic_auth_user}")
    private String username;

    @Value("${kcommunications_basic_auth_pass}")
    private String password;

    private OutOfOfficeApiClientService clientService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        clientService = new OutOfOfficeApiClientService(url, username, password);
    }

    public boolean processTurnOffMessage(String departmentUuid, String userUuid, TurnOffMessageRequest request) {
        try {
            log.info("in processTurnOffMessage for department_uuid={} user_uuid={} request={}", departmentUuid, userUuid, objectMapper.writeValueAsString(request));
            Response response = clientService.processTurnOffMessage(departmentUuid, userUuid, request);
            log.info("processTurnOffMessage for department_uuid={} user_uuid={} request={} completed with response={}", departmentUuid, userUuid,
                objectMapper.writeValueAsString(request), objectMapper.writeValueAsString(response));
            if(response == null || !(response.getErrors() == null || response.getErrors().isEmpty())) {
                log.error("Error while processTurnOffMessage for department_uuid={} user_uuid={} request={} response={}", departmentUuid, userUuid,
                    objectMapper.writeValueAsString(request), objectMapper.writeValueAsString(response));
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Error while processTurnOffMessage department_uuid={} user_uuid={}", departmentUuid, userUuid, e);
            return false;
        }
    }

    public DealerAssociateDto getDelegatee(String departmentUuid, String userUuid) {
        try {
            log.info("in getDelegatee for department_uuid={} user_uuid={}", departmentUuid, userUuid);
            DelegateeResponse response = clientService.getDelegatee(departmentUuid, userUuid);
            log.info("getDelegatee for department_uuid={} user_uuid={} completed with response={}", departmentUuid, userUuid,
                objectMapper.writeValueAsString(response));
            if(response == null || !(response.getErrors() == null || response.getErrors().isEmpty())) {
                log.error("Error while getDelegatee for department_uuid={} user_uuid={} response={}", departmentUuid, userUuid,
                    objectMapper.writeValueAsString(response));
                return null;
            }
            return response.getDelegatee();
        } catch (Exception e) {
            log.error("Error while getDelegatee department_uuid={} user_uuid={}", departmentUuid, userUuid, e);
            return null;
        }
    }

    public static TurnOffMessageRequest createTurnOffMessageRequest(String communicationValue, String messageBody, String messageProtocol) {
        TurnOffMessageRequest request = new TurnOffMessageRequest();
        request.setCommunicationValue(communicationValue);
        request.setMessageBody(messageBody);
        request.setMessageProtocol(MessageProtocol.valueOf(messageProtocol));
        return request;
    }

}

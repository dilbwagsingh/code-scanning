package com.mykaarma.kcommunications.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.kcommunications.jpa.repository.CommunicationStatusRepository;
import com.mykaarma.kcommunications.model.jpa.CommunicationStatus;
import com.mykaarma.kcommunications_model.common.CommunicationAttributes;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.OptOutState;
import com.mykaarma.kcommunications_model.enums.OptOutStatusUpdateEvent;

@Service
public class OptOutStatusHelper {
    
    @Autowired
    private CommunicationStatusRepository communicationStatusRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(OptOutStatusHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public CommunicationStatus getOptOutStatusFromDB(Long dealerID, Long departmentID, String messageProtocol, String communicationValue, Boolean doubleOptInEnabled) throws Exception {
        if(doubleOptInEnabled == null) {
            doubleOptInEnabled = false;
        }
        CommunicationStatus communicationStatus = communicationStatusRepository.findByDealerIDAndDealerDepartmentIDAndMessageProtocolAndCommunicationValue(dealerID, departmentID, messageProtocol, communicationValue);
        if(communicationStatus != null) {
            if(OptOutState.OPTED_IN.name().equalsIgnoreCase(communicationStatus.getOptOutState()) && communicationStatus.getCanSendOptinRequest()) {
                LOGGER.error("in getOptOutStatusFromDB found invalid_communication_status={}", objectMapper.writeValueAsString(communicationStatus));
                communicationStatus.setCanSendOptinRequest(false);
                communicationStatusRepository.saveAndFlush(communicationStatus);
            }
        } else {
            communicationStatus = getDefaultCommunicationStatus(dealerID, departmentID, messageProtocol, communicationValue, doubleOptInEnabled);
        }
        return communicationStatus;        
    }

    public List<CommunicationStatus> getOptOutStatusListFromDB(Long dealerID, Long departmentID, List<CommunicationAttributes> communicationAttributesList, Boolean doubleOptInEnabled) throws Exception {
        if(doubleOptInEnabled == null) {
            doubleOptInEnabled = false;
        }
        List<String> communicationValues = communicationAttributesList.stream()
            .map(CommunicationAttributes::getCommunicationValue).collect(Collectors.toList());
        List<CommunicationStatus> communicationStatusList = communicationStatusRepository.findAllByDealerIDAndDealerDepartmentIDAndMessageProtocolsAndCommunicationValues(
            dealerID, departmentID, Arrays.asList(MessageProtocol.TEXT.name(), MessageProtocol.EMAIL.name()),
            communicationValues);
        if(communicationStatusList == null) {
            communicationStatusList = new ArrayList<>();
        }
        List<CommunicationStatus> invalidStateCommunicationStatusList =  communicationStatusList
            .stream().filter(cs -> OptOutState.OPTED_IN.name().equalsIgnoreCase(cs.getOptOutState())
                && cs.getCanSendOptinRequest())
            .collect(Collectors.toList());
        
        if(!invalidStateCommunicationStatusList.isEmpty()) {
            LOGGER.error("in getOptOutStatusFromDB found invalid_communication_status={}", objectMapper.writeValueAsString(invalidStateCommunicationStatusList));
            for(CommunicationStatus communicationStatus : invalidStateCommunicationStatusList) {
                communicationStatus.setCanSendOptinRequest(false);
            }
            communicationStatusRepository.saveAll(invalidStateCommunicationStatusList);
            communicationStatusRepository.flush();
        }
        for(CommunicationAttributes communicationAttributes : communicationAttributesList) {
            boolean communicationStatusDBEntryExists = communicationStatusList.stream().anyMatch(cs ->
                cs.getMessageProtocol().equalsIgnoreCase(communicationAttributes.getCommunicationType()) &&
                cs.getCommunicationValue().equalsIgnoreCase(communicationAttributes.getCommunicationValue())
            );
            if(!communicationStatusDBEntryExists) {
                communicationStatusList.add(getDefaultCommunicationStatus(dealerID, departmentID, communicationAttributes.getCommunicationType(), communicationAttributes.getCommunicationValue(), doubleOptInEnabled));
            }
        }
        return communicationStatusList;
    }

    private CommunicationStatus getDefaultCommunicationStatus(Long dealerID, Long departmentID, String messageProtocol, String communicationValue, boolean doubleOptInEnabled) {
        CommunicationStatus communicationStatus = new CommunicationStatus();
        communicationStatus.setDealerID(dealerID);
        communicationStatus.setDealerDepartmentID(departmentID);
        communicationStatus.setMessageProtocol(messageProtocol);
        communicationStatus.setCommunicationValue(communicationValue);
        communicationStatus.setCanSendOptinRequest(false);
        if(MessageProtocol.TEXT.name().equalsIgnoreCase(messageProtocol)) {
            communicationStatus.setOptOutState(doubleOptInEnabled ? OptOutState.OPTED_OUT.name() : OptOutState.OPTED_IN.name());
        } else {
            communicationStatus.setOptOutState(OptOutState.OPTED_IN.name());
        }
        return communicationStatus;
    }

    public OptOutState getNewOptOutStateForMessageRelatedEvents(OptOutState currentOptOutState, OptOutStatusUpdateEvent event) {
        if(currentOptOutState == null || event == null) {
            return null;
        }
        switch (currentOptOutState) {
            case OPTED_IN:
                switch (event) {
                    case GENERIC_MESSAGE_RECEIVED:
                    case STOP_SUSPECTED_MESSAGE_RECEIVED:
                    case OPTIN_MESSAGE_RECEIVED:
                        return OptOutState.OPTED_IN;
                    case STOP_MESSAGE_RECEIVED:
                        return OptOutState.OPTED_OUT;
                    default:
                        return null;
                }
                case OPTED_OUT:
                switch (event) {
                    case GENERIC_MESSAGE_RECEIVED:
                    case STOP_SUSPECTED_MESSAGE_RECEIVED:
                    case STOP_MESSAGE_RECEIVED:
                        return OptOutState.OPTED_OUT;
                    case OPTIN_MESSAGE_RECEIVED:
                        return OptOutState.OPTED_IN;
                    default:
                        return null;
                }
            default:
                return null;
        }
    }
}

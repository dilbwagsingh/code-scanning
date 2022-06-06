package com.mykaarma.kcommunications.model.rabbit;

import com.mykaarma.kcommunications_model.enums.MessageKeyword;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.OptOutStatusUpdateEvent;
import com.mykaarma.kcommunications_model.enums.UpdateOptOutStatusRequestType;

import lombok.Data;

@Data
public class OptOutStatusUpdate {
    
    private Long messageID;
    private Long customerID;
    private Long dealerAssociateID;
    private Long dealerDepartmentID;
    private Long dealerID;
    private String apiCallSource;
    private MessageProtocol messageProtocol;
    private String communicationValue;
    private UpdateOptOutStatusRequestType updateType;
    private OptOutStatusUpdateEvent event;
    private Boolean doubleOptInEnabled;
    private Boolean incomingMessageFound;
    private String customerCommunicationCreatedBy;
    private Boolean okToCommunicate;
    private MessageKeyword messageKeyword;
    private Double optOutV2Score;
    private Integer expiration;
}

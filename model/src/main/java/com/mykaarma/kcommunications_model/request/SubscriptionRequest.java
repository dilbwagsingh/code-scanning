package com.mykaarma.kcommunications_model.request;

import lombok.Data;

@Data
public class SubscriptionRequest {

    private String customerUUID;
    private Long dealerID;
    private String subscriptionType;
    private Long dealerDepartmentID;
    private Long dealerAssociateID;
    private String dealerAssociateName;

}

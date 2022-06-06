package com.mykaarma.kcommunications.model.rabbit;

import java.util.List;

import com.mykaarma.kcommunications_model.enums.DeploymentEvent;

import lombok.Data;

@Data
public class DoubleOptInDeployment {
    
    private Long dealerID;
    private List<List<Long>> departmentGroups;
    private DeploymentEvent event;
    private Long maxEntriesToBeFetched;
    private Long minCustomerCommunicationID;
    private Integer expiration;

}

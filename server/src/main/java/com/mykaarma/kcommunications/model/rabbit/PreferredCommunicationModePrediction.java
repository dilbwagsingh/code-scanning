package com.mykaarma.kcommunications.model.rabbit;

import java.io.Serializable;

import com.mykaarma.kcommunications.model.jpa.Message;

import lombok.Data;

@Data
public class PreferredCommunicationModePrediction implements Serializable {
    
    private String customerUUID;
    private String departmentUUID;
    private Integer expiration;
    Message message;
}

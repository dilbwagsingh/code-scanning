package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CommunicationAttributes implements Serializable {
    
    @ApiModelProperty(value = "communication protocol of message", allowableValues = "TEXT, EMAIL, VOICE_CALL", required = true)
    private String communicationType;

    @ApiModelProperty(value = "communication value for sending message", required = true)
    private String communicationValue;

    @ApiModelProperty(value = "whether the communication value is ok to communicate with")
    private Boolean okToCommunicate = null;
}

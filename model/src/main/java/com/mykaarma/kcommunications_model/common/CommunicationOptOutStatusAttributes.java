package com.mykaarma.kcommunications_model.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CommunicationOptOutStatusAttributes {
    
    @ApiModelProperty("communication attributes of a customer")
    private CommunicationAttributes communication;
    
    @ApiModelProperty("optout status for the communication attributes")
    private OptOutStatusAttributes optOutStatus;
}

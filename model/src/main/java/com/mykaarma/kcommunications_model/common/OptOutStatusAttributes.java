package com.mykaarma.kcommunications_model.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class OptOutStatusAttributes {
    
    @ApiModelProperty("optout state of the communication attributes")
    private String optOutState;

    @ApiModelProperty("whether an optin request can be for the communication attributes")
    private Boolean canSendOptinRequest;
}

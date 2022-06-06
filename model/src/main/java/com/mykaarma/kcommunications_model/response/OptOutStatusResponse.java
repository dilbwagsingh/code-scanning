package com.mykaarma.kcommunications_model.response;

import com.mykaarma.kcommunications_model.common.OptOutStatusAttributes;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OptOutStatusResponse extends Response {
    
    @ApiModelProperty("optout status of the communication attributes")
    private OptOutStatusAttributes optOutStatus;

    @ApiModelProperty("unique identifier of the request")
    private String requestUUID;
}

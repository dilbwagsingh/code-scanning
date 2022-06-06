package com.mykaarma.kcommunications_model.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ForwardMessageRequest {

    @ApiModelProperty(value = "communication value to forward message to", required = true)
    private String communicationValue;
}

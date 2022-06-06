package com.mykaarma.kcommunications_model.request;

import com.mykaarma.kcommunications_model.enums.DeploymentEvent;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DoubleOptInDeploymentRequest {
    

    @ApiModelProperty(value = "deployment event to be processed", required = true)
    private DeploymentEvent event;
    
    @ApiModelProperty(value = "maximum entries of communication attributes to be fetched from database at a time")
    private Long maxEntriesToBeFetched = 5000l;

}

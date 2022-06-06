package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.mykaarma.kcommunications_model.common.CommunicationAttributes;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CommunicationsOptOutStatusListRequest {
    
    @ApiModelProperty(value = "list of communication attributes to fetch optout status for", required = true)
    private List<CommunicationAttributes> communicationList;
}

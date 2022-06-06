package com.mykaarma.kcommunications_model.response;

import java.util.List;

import com.mykaarma.kcommunications_model.common.CommunicationOptOutStatusAttributes;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OptOutStatusListResponse extends Response {
    
    @ApiModelProperty("list of communication attrbutes with their optout status")
    private List<CommunicationOptOutStatusAttributes> optOutStatusList;

    @ApiModelProperty("unique identifier of the request")
    private String requestUUID;
}

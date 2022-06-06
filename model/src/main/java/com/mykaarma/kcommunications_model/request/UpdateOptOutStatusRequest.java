package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.mykaarma.kcommunications_model.common.CommunicationAttributes;
import com.mykaarma.kcommunications_model.enums.OptOutStatusUpdateEvent;
import com.mykaarma.kcommunications_model.enums.UpdateOptOutStatusRequestType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UpdateOptOutStatusRequest {
    
    @ApiModelProperty("unique identifier of customer")
    private String customerUUID;

    @ApiModelProperty("unique identifier of message")
    private String messageUUID;

    @ApiModelProperty("unique identifier of user")
    private String userUUID;

    @ApiModelProperty("identifier of the source of request")
    private String apiCallSource;

    @ApiModelProperty("list of communication information of customer")
    private List<CommunicationAttributes> communicationAttributesList;

    @ApiModelProperty(value = "optout update event to process", required = true)
    private OptOutStatusUpdateEvent event;
    
    @ApiModelProperty(value = "source of optoutstatus update information", required = true)
    private UpdateOptOutStatusRequestType updateType;
}

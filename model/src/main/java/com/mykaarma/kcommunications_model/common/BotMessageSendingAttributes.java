package com.mykaarma.kcommunications_model.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BotMessageSendingAttributes {

    @ApiModelProperty(value = "communication value of recipient", required = true)
    private String recipientCommunicationValue;

    @ApiModelProperty(value = "name of sender")
    private String senderName;
}

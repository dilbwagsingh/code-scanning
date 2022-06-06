package com.mykaarma.kcommunications_model.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BotMessageSendingAttributesExtended extends BotMessageSendingAttributes {

    @ApiModelProperty("name of recipient")
    private String recipientName;

    @ApiModelProperty(value = "communication value of sender", required = true)
    private String senderCommunicationValue;

}

package com.mykaarma.kcommunications_model.request;

import com.mykaarma.kcommunications_model.common.BotMessageAttributes;
import com.mykaarma.kcommunications_model.common.BotMessageDeliveryAttributes;
import com.mykaarma.kcommunications_model.common.BotMessageSendingAttributesExtended;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SaveBotMessageRequest {

    @ApiModelProperty(value = "unique identifier of bot message", notes = "leave it null if saving new message")
    private String uuid;

    @ApiModelProperty(value = "message properties of bot message", required = true)
    private BotMessageAttributes messageAttributes;

    @ApiModelProperty(value = "message sending properties of bot message", required = true)
    private BotMessageSendingAttributesExtended messageSendingAttributes;

    @ApiModelProperty(value = "delivery properties of bot message", required = true)
    private BotMessageDeliveryAttributes messageDeliveryAttributes;
}

package com.mykaarma.kcommunications_model.request;

import com.mykaarma.kcommunications_model.common.BotMessageAttributes;
import com.mykaarma.kcommunications_model.common.BotMessageSendingAttributes;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SendBotMessageRequest {

    @ApiModelProperty(value = "message properties of bot message", required = true)
    private BotMessageAttributes messageAttributes;

    @ApiModelProperty(value = "message sending properties of bot message", required = true)
    private BotMessageSendingAttributes messageSendingAttributes;
}

package com.mykaarma.kcommunications_model.common;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BotMessageDeliveryAttributes {

    @ApiModelProperty("timestamp when message was sent")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date sentTimestamp;

    @ApiModelProperty("timestamp when message was received")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date receivedTimestamp;

    @ApiModelProperty("delivery status of message")
    private String deliveryStatus;

    @ApiModelProperty("delivery failure message")
    private String deliveryFailureMessage;

}

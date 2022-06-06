package com.mykaarma.kcommunications_model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;
import com.mykaarma.kcommunications_model.enums.MessagePurpose;
import com.mykaarma.kcommunications_model.enums.MessageType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BotMessageAttributes {

    @ApiModelProperty(value = "content of message", required = true)
    private String body;

    @JsonProperty(value = "subject")
    private String subject;

    @ApiModelProperty(value = "type of message", required = true)
    private MessageType type;

    @ApiModelProperty(value = "protocol for sending message", required = true)
    private MessageProtocol protocol;

    @ApiModelProperty("purpose of message")
    private MessagePurpose purpose;

    @ApiModelProperty(value = "is the message manual")
    private Boolean isManual = false; // since this is bot message defaulting to false

    @ApiModelProperty("number of message attachments")
    private Long numberOfMessageAttachments = 0L;
}

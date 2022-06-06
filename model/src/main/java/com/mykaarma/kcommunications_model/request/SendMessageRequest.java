package com.mykaarma.kcommunications_model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mykaarma.kcommunications_model.common.IncomingMessageAttributes;
import com.mykaarma.kcommunications_model.common.InternalCommentAttributes;
import com.mykaarma.kcommunications_model.common.MessageAttributes;
import com.mykaarma.kcommunications_model.common.MessageSendingAttributes;
import com.mykaarma.kcommunications_model.common.NotificationAttributes;
import com.mykaarma.kcommunications_model.common.User;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SendMessageRequest {

	@ApiModelProperty(notes = "entity creating the message")
	@JsonProperty("editor")
	private User editor;

	@ApiModelProperty(notes = "", required = true)
	@JsonProperty("messageAttributes")
	private MessageAttributes messageAttributes;

	@ApiModelProperty(notes = "message sending properties like signature, cc emails, bcc email etc. (only for outgoing communication)")
	@JsonProperty("messageSendingAttributes")
	private MessageSendingAttributes messageSendingAttributes;

	@ApiModelProperty(notes = "message event notification attributes")
	@JsonProperty("notificationAttributes")
	private NotificationAttributes notificationAttributes;

	@ApiModelProperty(notes = "incoming message attributes (only for incoming communication)")
	@JsonProperty("incomingMessageAttributes")
	private IncomingMessageAttributes incomingMessageAttributes;

	@ApiModelProperty(notes = "internal comment attributes (only for internal comment)")
	@JsonProperty("internalCommentAttributes")
	private InternalCommentAttributes internalCommentAttributes;

}

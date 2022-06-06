package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class IncomingMessageAttributes implements Serializable {

	@ApiModelProperty(notes = "phone number from which incoming message has come from")
	@JsonProperty("fromNumber")
	private String fromNumber;

	@ApiModelProperty(notes = "twilio subAccount id")
	@JsonProperty("communicationUID")
	private String communicationUID;

	@ApiModelProperty(notes = "flag whether to forward text")
	@JsonProperty("forwardText")
	private Boolean forwardText;

	@ApiModelProperty(notes = "email address for forward email")
	@JsonProperty("forwardedEmailReference")
	private String forwardedEmailReference;

}

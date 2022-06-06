package com.mykaarma.kcommunications_model.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UpdatePreferredCommunicationModeRequest implements Serializable {

	private static final long serialVersionUID = 1L;

    @ApiModelProperty(notes = "preferred communication mode message protocol for a customer", required=true)
	@JsonProperty("preferredCommunicationMode")
    private String preferredCommunicationMode;

    @ApiModelProperty(notes = "preferred communication mode metaData for a customer", required=false)
    @JsonProperty("preferredCommunicationModeMetaData")
	private String preferredCommunicationModeMetaData;

}
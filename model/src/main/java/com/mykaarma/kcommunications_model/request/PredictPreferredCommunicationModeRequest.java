package com.mykaarma.kcommunications_model.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PredictPreferredCommunicationModeRequest implements Serializable {

    @ApiModelProperty(notes = "message uuid to use to calculate preferred communication mode information for a customer", required=true)
    @JsonProperty("messageUUID")
    private String messageUUID;

}

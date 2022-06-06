package com.mykaarma.kcommunications_model.request;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class MultipleCustomersPreferredCommunicationModeRequest implements Serializable {

    @ApiModelProperty(notes = "list of customer uuids to fetch preferred communication mode message protocol for", required=true)
    @JsonProperty("customerUUIDList")
    private List<String> customerUUIDList;

}
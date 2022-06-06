package com.mykaarma.kcommunications_model.request;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CustomersOptOutStatusListRequest {
    
    @ApiModelProperty(value = "list of customer uuids to fetch optout status", required = true)
    List<String> customerUUIDList;
}

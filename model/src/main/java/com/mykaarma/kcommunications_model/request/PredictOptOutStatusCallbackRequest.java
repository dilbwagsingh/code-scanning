package com.mykaarma.kcommunications_model.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PredictOptOutStatusCallbackRequest {

    @ApiModelProperty("score given by the new opt out message ai")
    private Double optOutV2Score;

    @ApiModelProperty(value = "message prediction keyword", allowableValues = "STOP, STOP_SUSPECTED, GENERIC, OPTIN")
    private String messageKeyword;

}

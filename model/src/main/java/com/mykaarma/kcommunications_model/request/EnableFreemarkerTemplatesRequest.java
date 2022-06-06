package com.mykaarma.kcommunications_model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnableFreemarkerTemplatesRequest {

	@ApiModelProperty(notes = "Initial dealer ID to enable freemarker templates for.")
	@JsonProperty("fromDealerID")
	private Long fromDealerID;

	@ApiModelProperty(notes = "Last dealer ID to enable freemarker templates for.")
	@JsonProperty("toDealerID")
	private Long toDealerID;

	@ApiModelProperty(notes = "service name to enable freemarker templates for.")
	@JsonProperty("serviceName")
	private String serviceName;
}

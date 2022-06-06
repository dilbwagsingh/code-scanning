package com.mykaarma.kcommunications_model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateFreemarkerTemplatesRequest {

	@ApiModelProperty(notes = "Initial dealer ID to create freemarker templates for.")
	@JsonProperty("fromDealerID")
	private Long fromDealerID;

	@ApiModelProperty(notes = "Last dealer ID to create freemarker templates for.")
	@JsonProperty("toDealerID")
	private Long toDealerID;

	@ApiModelProperty(notes = "Template type to create freemarker templates for.")
	@JsonProperty("templateType")
	private String templateType;
}

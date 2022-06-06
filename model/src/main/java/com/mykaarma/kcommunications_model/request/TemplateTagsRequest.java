package com.mykaarma.kcommunications_model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemplateTagsRequest {

	@ApiModelProperty(notes = "List of template type to get all tags for")
	@JsonProperty("templateTypes")
	private List<String> templateTypes;
	
}
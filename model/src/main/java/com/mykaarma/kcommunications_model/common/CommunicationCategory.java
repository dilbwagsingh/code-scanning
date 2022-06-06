package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;
import java.util.List;

import com.mykaarma.kcommunications_model.enums.CategoryEvent;
import com.mykaarma.kcommunications_model.enums.CommunicationCategoryName;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CommunicationCategory implements Serializable {

	@ApiModelProperty(notes = "communication category which can be either manual or automated")
	private CommunicationCategoryName name;
	 
	@ApiModelProperty(notes = "Event name which can be anything such as internal_note, system_note")
	private List<CategoryEvent> events;

}

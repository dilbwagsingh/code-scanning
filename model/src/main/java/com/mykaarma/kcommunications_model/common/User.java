package com.mykaarma.kcommunications_model.common;

import com.mykaarma.kcommunications_model.enums.EditorType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class User implements Serializable {

	@ApiModelProperty(notes = "Unique identifier for the user of mykaarma")
    private String uuid;
	
	@ApiModelProperty(notes = "name of the user")
    private String name;
	
	@ApiModelProperty(notes = "Types of users of mykaarma USER/CUSTOMER")
    private EditorType type;
	
	@ApiModelProperty(notes = "Unique department identifier for user, not mandatory")
    private String departmentUuid;

}

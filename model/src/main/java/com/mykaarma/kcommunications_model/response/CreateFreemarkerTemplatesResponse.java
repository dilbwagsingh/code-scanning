package com.mykaarma.kcommunications_model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class CreateFreemarkerTemplatesResponse extends Response{

	private static final long serialVersionUID = 1L;
	private String requestUuid;
	private boolean requestStatus=false;
}

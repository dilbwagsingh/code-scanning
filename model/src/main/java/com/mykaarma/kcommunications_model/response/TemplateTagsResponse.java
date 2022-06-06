package com.mykaarma.kcommunications_model.response;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class TemplateTagsResponse extends Response {

	private static final long serialVersionUID = 1L;
	private Set<String> tags;
	private String requestUuid;

}

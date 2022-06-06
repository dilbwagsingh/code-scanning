package com.mykaarma.kcommunications_model.response;

import java.util.List;

import com.mykaarma.kcommunications_model.dto.TemplateDTO;

import lombok.Data;

@Data
public class SearchTemplateResponse extends Response{

	private List<TemplateDTO> templateDTOList;
	
	private String requestUuid;
}

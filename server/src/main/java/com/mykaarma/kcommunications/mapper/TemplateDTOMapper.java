package com.mykaarma.kcommunications.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.mykaarma.kcommunications.elasticsearch.model.Template;
import com.mykaarma.kcommunications_model.dto.TemplateDTO;

@Mapper(componentModel = "spring")
public interface TemplateDTOMapper {

	TemplateDTO map(Template template);
	
	List<TemplateDTO> map(List<Template> template);
}

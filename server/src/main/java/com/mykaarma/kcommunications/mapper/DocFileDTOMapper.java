package com.mykaarma.kcommunications.mapper;

import org.mapstruct.Mapper;

import com.mykaarma.kcommunications.model.jpa.DocFile;
import com.mykaarma.kcommunications_model.dto.DocFileDTO;

@Mapper(componentModel = "spring")
public interface DocFileDTOMapper {
	
	DocFileDTO map(DocFile docFile);

}

package com.mykaarma.kcommunications.mapper;

import org.mapstruct.Mapper;

import com.mykaarma.kcommunications.model.jpa.MessageMetaData;
import com.mykaarma.kcommunications_model.dto.MessageMetaDataDTO;

@Mapper(componentModel = "spring")
public interface MessageMetaDataDTOMapper {

	MessageMetaDataDTO map(MessageMetaData messageMetaData);
}

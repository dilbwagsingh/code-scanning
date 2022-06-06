package com.mykaarma.kcommunications.mapper;

import org.mapstruct.Mapper;

import com.mykaarma.kcommunications.model.jpa.DraftMessageMetaData;
import com.mykaarma.kcommunications_model.dto.DraftMessageMetaDataDTO;

@Mapper(componentModel = "spring")
public interface DraftMessageMetaDataDTOMapper {

	DraftMessageMetaDataDTO map(DraftMessageMetaData draftMessageMetaData);
}

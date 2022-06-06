package com.mykaarma.kcommunications.mapper;

import org.mapstruct.Mapper;

import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications_model.dto.MessageExtnDTO;

@Mapper(componentModel = "spring")
public interface MessageExtnDTOMapper {
	
	MessageExtnDTO map(MessageExtn messageExtn);

}

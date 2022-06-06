package com.mykaarma.kcommunications.mapper;

import org.mapstruct.Mapper;

import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications_model.dto.MessageDTO;

@Mapper(componentModel = "spring")
public interface MessageDTOMapper {

	MessageDTO map(Message message);
}

package com.mykaarma.kcommunications.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.mykaarma.kcommunications.model.jpa.MessagePredictionFeedback;
import com.mykaarma.kcommunications_model.dto.MessagePredictionFeedbackDTO;

@Mapper(componentModel = "spring")
public interface MessagePredictionFeedbackDTOMapper {
	
	MessagePredictionFeedbackDTO map(MessagePredictionFeedback messagePredictionFeedback);
	
	List<MessagePredictionFeedbackDTO> map(List<MessagePredictionFeedback> messagePredictionFeedbackList);
}


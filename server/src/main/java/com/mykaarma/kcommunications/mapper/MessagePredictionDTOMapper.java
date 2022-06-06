package com.mykaarma.kcommunications.mapper;

import org.mapstruct.Mapper;

import com.mykaarma.kcommunications.model.jpa.MessagePrediction;
import com.mykaarma.kcommunications.model.jpa.PredictionFeature;
import com.mykaarma.kcommunications_model.dto.MessagePredictionDTO;
import com.mykaarma.kcommunications_model.dto.PredictionFeatureDTO;

@Mapper(componentModel = "spring")
public interface MessagePredictionDTOMapper {
	
	MessagePredictionDTO map(MessagePrediction messagePrediction);
	
	PredictionFeatureDTO map(PredictionFeature predictionFeature);

}

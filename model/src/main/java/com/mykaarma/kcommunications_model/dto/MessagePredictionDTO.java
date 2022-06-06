package com.mykaarma.kcommunications_model.dto;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class MessagePredictionDTO implements Serializable{

	private static final long serialVersionUID = 1L;

    private String messageUuid;

    private PredictionFeatureDTO predictionFeature;

    private String prediction;

    private String metadata;
    
    Set<MessagePredictionFeedbackDTO> messagePredictionFeedback = new HashSet<MessagePredictionFeedbackDTO>(0);

}

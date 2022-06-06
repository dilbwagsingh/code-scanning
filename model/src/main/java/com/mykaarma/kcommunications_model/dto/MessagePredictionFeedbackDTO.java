package com.mykaarma.kcommunications_model.dto;

import java.io.Serializable;

import lombok.Data;

@Data
public class MessagePredictionFeedbackDTO implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private Long messagePredictionID;
	
	private String userFeedback;
	
	private String feedbackReason;
	
	private String userUUID;
	
	private String departmentUUID;

}

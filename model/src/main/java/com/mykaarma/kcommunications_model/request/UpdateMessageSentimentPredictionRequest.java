package com.mykaarma.kcommunications_model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateMessageSentimentPredictionRequest {
	
	@JsonProperty("sentimentScore")
	private Float sentimentScore;
	
	public Float getSentimentScore() {
		return sentimentScore;
	}
	public void setSentimentScore(Float sentimentScore) {
		this.sentimentScore = sentimentScore;
	}
}

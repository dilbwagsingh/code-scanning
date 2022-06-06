package com.mykaarma.kcommunications_model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateMessagePredictionRequest {

	@JsonProperty("messageID")
    private Long messageID;
	
	@JsonProperty("predictionFeature")
    private String predictionFeature;
	
	@JsonProperty("prediction")
    private String prediction;
	
	@JsonProperty("metadata")
    private String metadata;

	public Long getMessageID() {
		return messageID;
	}

	public void setMessageID(Long messageID) {
		this.messageID = messageID;
	}

	public String getPredictionFeature() {
		return predictionFeature;
	}

	public void setPredictionFeature(String predictionFeature) {
		this.predictionFeature = predictionFeature;
	}

	public String getPrediction() {
		return prediction;
	}

	public void setPrediction(String prediction) {
		this.prediction = prediction;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

}

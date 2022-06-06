package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name="MessagePrediction")
public class MessagePrediction implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue
	@Column(name = "ID")
	private Long id;

	@Column(name = "MessageID")
    private Long messageID;

	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="PredictionFeatureID",referencedColumnName="ID")
	private PredictionFeature predictionFeature;

	@Column(name = "Prediction")
    private String prediction;

	@Column(name = "MetaData")
    private String metadata;
}
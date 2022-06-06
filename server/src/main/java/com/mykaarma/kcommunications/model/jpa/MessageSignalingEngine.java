package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Data;

@Entity
@Data
@Table(name = "MessageSignalingEngine", uniqueConstraints =  @UniqueConstraint(columnNames = {"WorkflowUUID"}))
public class MessageSignalingEngine implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	private Long featureID;
	
	private Long dealerID;
	
	private String workflowUUID;
	
	private Boolean isValid;
	
}

package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Entity
@Data
@Table(name="DealerOrderMessage")
public class DealerOrderMessage implements Serializable{
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@JsonIgnore
	private Long id; 
	
	private Long version;
	private Long dealerID;
	private Long dealerOrderID;
	private Long messageID;
	private String messageProtocol;
	private String messageOrigin;
	private String relationType;
	private Boolean isUpdatedManually  =false;
	
}

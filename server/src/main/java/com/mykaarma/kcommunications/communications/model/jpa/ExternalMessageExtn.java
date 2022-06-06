package com.mykaarma.kcommunications.communications.model.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Data;

@Entity
@Data
@Table(name = "ExternalMessageExtn", uniqueConstraints =  @UniqueConstraint(columnNames = {"MessageID"}))
public class ExternalMessageExtn implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="MessageID")
	private Long messageID;
	
	@Column(name="MessageBody")
	private String messageBody;
	
	@Column(name="Subject")
	private String subject;
	
}

package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Data;

@Entity
@Data
@Table(name = "MessageExtn", uniqueConstraints =  @UniqueConstraint(columnNames = {"MessageID"}))
public class MessageExtn implements Serializable{
	
	@Id
	private Long messageID;
	
	private String messageBody;
	
	private String subject;
	
}

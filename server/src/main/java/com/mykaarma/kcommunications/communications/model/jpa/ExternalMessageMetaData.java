package com.mykaarma.kcommunications.communications.model.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "ExternalMessageMetaData")
public class ExternalMessageMetaData  implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="MessageID")
	private Long messageID;
	
	@Column(name="MetaData")
	private String metaData;
		
}

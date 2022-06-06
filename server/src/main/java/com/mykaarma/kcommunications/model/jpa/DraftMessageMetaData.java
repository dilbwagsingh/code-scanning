package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mykaarma.kcommunications_model.enums.DraftStatus;

import lombok.Data;

@Data
@Entity
@Table(name = "DraftMessageMetaData",  uniqueConstraints =  @UniqueConstraint(columnNames = {"MessageID"}))
public class DraftMessageMetaData  implements Serializable{
	

	@Id
	private Long messageID;
	
	private String status;
	
	private Date scheduledOn;
	
	private String reasonForLastFailure;
	
	private Boolean addSignature;
}

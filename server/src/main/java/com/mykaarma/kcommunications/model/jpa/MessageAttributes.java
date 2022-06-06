package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "MessageAttributes")
@SuppressWarnings("serial")
public class MessageAttributes implements Serializable {
	
	@Id
	private Long messageID;
	
	private Boolean countInThreadMessageCount;
	
	private Boolean showInCustomerConversation;

}

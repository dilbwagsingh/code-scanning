package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "MessageThread")
public class MessageThread  implements Serializable{
	
	@Id
	private Long messageID;
	
	private Long threadID;
}

package com.mykaarma.kcommunications.model.jpa;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "ForwardingBrokerNumberMapping")
public class ForwardingBrokerNumberMapping {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	private Long version;
	private Long dealerID;
	private Long customerID;
	private String brokerNumber;
	private String customerPhoneNumber;
	private Long dealerAssociateID;
	private Date createdTimeStamp;
	private Date lastMessageOn;
	private String dealerAssociatePhoneNumber;

}

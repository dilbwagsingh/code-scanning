package com.mykaarma.kcommunications.communications.model.jpa;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "CustomerPreferredCommunicationMode")
public class CustomerPreferredCommunicationMode implements Serializable{

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

    @Column(name = "CustomerUUID")
	private String customerUUID;

    @Column(name = "Protocol")
    private String protocol;

    @Column(name = "MetaData")
    private String metaData;

}
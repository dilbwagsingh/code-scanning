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
@Table(name = "MessagePurpose")
public class MessagePurpose implements Serializable {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="ID")
    private Long id;
    
    @Column(unique=true, name="UUID")
    private String uuid;
    
    @Column(name="PurposeName")
    private String purposeName;
    
    @Column(name="PurposeDescription")
    private String purposeDescription;
}

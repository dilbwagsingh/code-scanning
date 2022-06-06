package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Entity
@Data
@Table(name = "VoiceCredentials")
public class VoiceCredentials implements Serializable{
	

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Version
    private long version;
    
    private Long dealerID;
    
    private String dealerSubaccount;
   
	private String brokerNumber;
    
    private Long deptID;
    
    private Boolean useForOutgoingCommunication;
    
    private Boolean useForStickiness;
    
    private Boolean isHosted=false;

}

package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;
import java.util.Date;

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
@Table(name = "DelegationHistory")
public class DelegationHistory  implements Serializable{

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Version
    private long version;
    
    private Long threadID;
    
    private Long threadOwner;
    
    private Long delegatedFrom;
    
    private Long delegatedTo;
    
    private String delegator;
    
    private Date timeOfChange;
    
    private Boolean isRevoked;

    private Long revokedBy;
    
    private Date revokedOn;
    
}
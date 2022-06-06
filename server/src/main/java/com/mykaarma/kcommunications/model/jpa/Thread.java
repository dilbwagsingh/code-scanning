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
@Table(name = "Thread")
public class Thread  implements Serializable{

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Version
    private long version;

    private Boolean archived;
    
    private Boolean closed;
    
    private Long dealerID;
    
    private Long dealerAssociateID;
    
    private Long customerID;
    
    private Date lastMessageOn;
    
    private Long lastDelegationOn;
    
    private Long dealerDepartmentID;
    
    private Boolean isWaitingForResponse;
}

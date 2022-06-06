package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;

@Data
@Entity
public class CommunicationStatus implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "DealerID")
    private Long dealerID;

    @Column(name = "DealerDepartmentID")
    private Long dealerDepartmentID;

    @Column(name = "CommType")
    private String messageProtocol;

    @Column(name = "CommValue")
    private String communicationValue;    
    
    @Column(name = "OptOutStatus")
    private String optOutState;

    @Column(name = "CanSendOptinRequest")
    private Boolean canSendOptinRequest;

}

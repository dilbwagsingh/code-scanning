package com.mykaarma.kcommunications.model.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "CommunicationStatusHistory")
public class CommunicationStatusHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "MessageID")
    private Long messageID;

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
}

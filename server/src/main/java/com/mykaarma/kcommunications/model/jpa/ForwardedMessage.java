package com.mykaarma.kcommunications.model.jpa;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import lombok.Data;

@Entity
@Table(name = "ForwardedMessage")
@Data
public class ForwardedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;
    private String messageType;
    private String protocol;
    private Long dealerId;
    private Long dealerAssociateId;
    private String messageBody;
    private Integer messageSize;
    private Date timeStamp;
    private Double charge;
    private String uuid;

}

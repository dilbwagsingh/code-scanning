package com.mykaarma.kcommunications.model.jpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import lombok.Data;

@Entity
@Table(name = "ForwardingBrokerNumberPool")
@Data
public class ForwardingBrokerNumberPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String brokerNumber;
    private Boolean isActive;
    private String credentials;
}

package com.mykaarma.kcommunications.communications.model.jpa;

import lombok.Data;

import javax.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Data
@Table(name = "ExternalMessage")
public class ExternalMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="ID")
    private Long id;

    @Version
    @Column(name="Version")
    private Long version;

    @Column(name="UUID")
    private String uuid;
    
//    Protocol used by the message i.e. either TEXT("X") OR EMAIL("E")
    @Column(name="MessageProtocol")
    private String messageProtocol;
    
//    The purpose of this specific message i.e. any one purpose among all the purpose present in MessagePurpose table
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="MessagePurposeUUID", referencedColumnName="UUID")
    private MessagePurpose messagePurpose;
    
//    The number or email or any other value like service sid using which the message was sent
    @Column(name="FromValue")
    private String fromValue;
   
//    the number or email to which the message was sent
    @Column(name="ToValue")
    private String toValue;
    
//    Size of the message body in bytes
    @Column(name="MessageSize")
    private Integer messageSize;
    
//    Timestamp of when it was sent
    @Column(name="SentOn")
    private Date sentOn;
    
//    To tell if the delivery of the message was successful or not
    @Column(name="DeliveryStatus")
    private String deliveryStatus;
    
    @Transient
    private Integer expiration;
    
//    To store the message body and subject which is mapped to the ExternalMessage table's ID
    @Transient
    private ExternalMessageExtn messageExtn;
    
//    Metadata for storing all the trivial message parameters like twilio message ID or message failure reason
    @Transient
    private ExternalMessageMetaData messageMetaData;

}
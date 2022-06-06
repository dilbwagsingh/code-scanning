package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

import lombok.Data;

@Data
@Entity
public class BotMessage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String uuid;
    private Long numberOfMessageAttachments;
    private Long dealerAssociateId;
    private Long dealerId;
    private Long customerId;
    private String messageType;
    private String protocol;
    private String messagePurpose;
    private String fromName;
    private String fromNumber;
    private String toName;
    private String toNumber;
    private String replyTo;
    private String replyToNumber;
    private String subject;
    private String deliveryStatus;
    private String messageBody;
    private Integer messageSize;
    private Date sentOn;
    private Date receivedOn;
    private boolean isRead;
    private String tags;
    private String folder;
    private String emailMessageId;
    private String communicationUid;
    private String action;
    private Long billingId;
    private Double charge;
    private Long dealerDepartmentId;
    private Boolean twilioDeliveryStatus;
    private String twilioDeliveryFailureMessage;
    private Boolean isManual;
}

package com.mykaarma.kcommunications.model.jpa;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "Message")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    private String uuid;

    
    private String messageType;
    
    private String protocol;
    
    private String fromName;
    
    private String fromNumber;
    
    private String toName;
   
    private String toNumber;
    
    private Integer messageSize;
    
    private Date sentOn;
    
    private Date receivedOn;
    
    private Date routedOn;
    
    private String tags;
    
    private String emailMessageId;
    
    private String communicationUid;
    
    private String messagePurpose;
    
    private String deliveryStatus;
    
    private Long dealerDepartmentId;
    
    private String twilioDeliveryFailureMessage;
    
    private Integer numberofMessageAttachments;
    
    private Boolean isManual;

    private String defaultReplyAction;
    
    private Long customerID;
    
    private Long dealerID;
    
    private Long dealerAssociateID;
    
    private Boolean isRead = false;
    
    private Boolean isArchive = false;
    
    @Transient
    private Integer expiration;
    
    @Transient
    private MessageExtn messageExtn;
    
    @Transient
    private DraftMessageMetaData draftMessageMetaData;

    @Transient
    private MessageMetaData messageMetaData;
    
    @Transient
    private MessageAttributes messageAttributes;
    
    @Transient
    private Set<DocFile> docFiles = new HashSet<DocFile>(0);

	public Set<DocFile> getDocFiles() {
		return docFiles;
	}

	public void setDocFiles(Set<DocFile> docFiles) {
		this.docFiles = docFiles;
	}

}
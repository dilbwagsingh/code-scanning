package com.mykaarma.kcommunications_model.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class MessageDTO implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Long id;
	
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
    
    private String dealerDepartmentUuid;
    
    private String twilioDeliveryFailureMessage;
    
    private Integer numberofMessageAttachments;
    
    private Boolean isManual;
    
    private String customerUuid;
    
    private String customerName;
    
    private String dealerUuid;
    
    private String dealerAssociateUuid;
    
    private Boolean isRead ;
    
    private Boolean isArchive ;
    
    private MessageExtnDTO messageExtnDTO;
    
    private DraftMessageMetaDataDTO draftMessageMetaDataDTO;

    private MessageMetaDataDTO messageMetaDataDTO;
    
    private Set<DocFileDTO> docFiles = new HashSet<DocFileDTO>(0);
    
    private Set<MessagePredictionDTO> messagePredictionDTOSet;
}

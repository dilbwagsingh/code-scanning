package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "VoiceCall")

public class VoiceCall implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private long version; 
    
    private char voiceGateway;
    
    private String callingParty;
    
    private String party1;
    
    private String party1Prompt;
    
    private String party2;
    
    private String party2Prompt;
    
    private Long callStatus;
    
    private String callIdentifier;
    
    private Date callDateTime;
    
    private boolean recordCall;
    
    private String recordingUrl;
    
    private boolean transcribeCall;
    
    private String transcribedText;
    
    private int duration;
    
    private String callBroker;
    
    private String party2Delegate;

	private Date insertTS;
    
    private Date updateTS;
    
    private String childCallIdentifier;
    
}

package com.mykaarma.kcommunications.model.utils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import org.joda.time.DateTime;

import lombok.Data;

@Data
public class CommunicationsVerification implements Serializable{
	
	private Long departmentId;
	private String verificationType;
	private Date startDate;
	private Date endDate;
	Integer expiration;
	Boolean registerFailedMessage = false;
}

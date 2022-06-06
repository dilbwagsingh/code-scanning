package com.mykaarma.kcommunications.model.kne;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KNotificationMessage {
	
	private Long dealerID;
	private Long messageID;
	private String eventType;
	private List<String> filtersChanged;
	private Boolean isCountChanged;
	private Long threadID;
	private Set<Long> notificationDAIDSet = new HashSet<Long>();
	private Set<Long> phoneNotificationDAIDSet = new HashSet<Long>();
	private Long departmentID;
	private Boolean notifierNotification =true;
	private Boolean viewNotification = true;
	private Set<Long> viewDAIDSet = new HashSet<Long>();
	private Set<Long> internalSubscriptionDAIDSet = new HashSet<Long>();
	private String messageType= "KMESSAGENOTIFICATION";
	private String deviceID;
	private Long messageDAID;
	private String messageUUID;
	private String notificationMessageUUID;
	private Integer expiration;
	private Set<Long> subscriptionRevokedDAIDSet = new HashSet<Long>();
	private Long sequenceID;
	private Date updatedDate;
	private Long updateDateInMillis;
	private String orderNumber;
	private String eventData;
	private Set<Long> concernedDAIDSet;
	private Long customerID;
	private String messageProtocol;
	private String communicationValue;
}

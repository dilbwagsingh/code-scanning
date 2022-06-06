package com.mykaarma.kcommunications.model.mvc;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@SuppressWarnings("serial")
@Data
public class DealerOrderSaveEventData implements Serializable {
	
	private String eventName;
	private Long dealerID;
	private Long dealerDepartmentID;
	private Long customerID;
	private Long dealerOrderID;
	private Long dealerAssociateID;
	private Long dmsID;
	private String orderStatus;
	private String orderNumber;
	private String orderType; 
	private Date orderDate;
	private Boolean isPaid;
	private Boolean isPaidInKaarma;
	private Boolean isPaymentRequestSent;
	private BigDecimal orderAmount;
	private BigDecimal paidAmount;
	private String invoiceUrl;
	private Date createdDate;
	private Date updatedDate;
	private String deviceID;
	private Date orderOpenedDate;
	private Long eventRaisedBy;

}

package com.mykaarma.kcommunications_model.request;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class CommunicationsBillingRequest implements Serializable {

	List<Long> dealerIds;
	private String verificationType;
	private String startDate;
	private String endDate;
	Boolean registerFailedMessage = false;
	
}

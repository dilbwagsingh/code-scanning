package com.mykaarma.kcommunications.Twilio;

import java.net.URI;
import java.util.List;

import com.mykaarma.kcommunications.utils.OutboundMessageDetail;

public interface TwilioGatewayServiceBO {

	public OutboundMessageDetail placeCall(String departmentUUID, String concernedDAIUUD, Long dealerID, Long departmentID, String party1Number,
			String party1prompt, String party2Number, String party2prompt, String party2Number2,
			boolean recordCall, 
			boolean isSupport, String mode, Long customerID, Boolean announceCall);

	public void setCallStatus(String accountSid,String callSid, int callStatusId) throws Exception;
		
	public String[] getSeparatedDetailsFromDealerSubaccount(String dealerSubaccount);

	com.twilio.rest.api.v2010.account.Message sendText(String accountSid, String authToken, String messageBody, String brokerNumber, String toNumber, List<URI> mediaUrls, String callbackUrl);
}

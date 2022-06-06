package com.mykaarma.kcommunications.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mykaarma.kcommunications.model.kre.InboundTextRequest;
import com.mykaarma.kcommunications.model.kre.KaarmaRoutingResponse;
import com.mykaarma.kmanage.model.dto.json.response.GetDepartmentResponseDTO;

@Service
public class RulesEngineHelper {

	@Autowired
	private RestTemplate restTemplate;
	
	@Value("${krulesengine.url}")
	private String rulesEngineUrl;
	
	public KaarmaRoutingResponse getReponseFromRulesEngine(InboundTextRequest request)
	{
		String url = rulesEngineUrl+"/routing/text";
		return restTemplate.postForObject(url, request, KaarmaRoutingResponse.class);
	}
	
	public KaarmaRoutingResponse getReponseFromRulesEngineForInboundCall(String accountSid, String caller, String callSid) {
		InboundCallRequest inboundCallRequest = new InboundCallRequest();
		inboundCallRequest.setAccountSid(accountSid);
		inboundCallRequest.setFrom(caller);
		inboundCallRequest.setSid(callSid);
		
		String url = rulesEngineUrl+"/routing/call";
		return restTemplate.postForObject(url, inboundCallRequest, KaarmaRoutingResponse.class);
	}
	
	public OutboundCallResponse getOutboundCallResponseFromRulesEngine(Long dealerAssociateID, Long dealerID, Long departmentID) {
		OutboundCallRequest outboundCallRequest = new OutboundCallRequest();
		outboundCallRequest.setDealerAssociateID(dealerAssociateID);
		outboundCallRequest.setDealerID(dealerID);
		outboundCallRequest.setDealerDepartmentID(departmentID);
		
		String url =  rulesEngineUrl + "/outgoing/call/recording";
					
		OutboundCallResponse outboundCallResponse = restTemplate.postForObject(url, outboundCallRequest, OutboundCallResponse.class);
		return outboundCallResponse;
	}
	
	public InboundCallRecordingUrlResponse getInboundCallRecordingResponse(GetDepartmentResponseDTO dealerDepartment) {
		
		InboundCallRecordingUrlRequest inboundCallRecordingUrlRequest = new InboundCallRecordingUrlRequest();
		inboundCallRecordingUrlRequest.setDealerID(dealerDepartment.getDepartmentExtendedDTO().getDealerMinimalDTO().getId());
		inboundCallRecordingUrlRequest.setDealerDepartmentID(dealerDepartment.getDepartmentExtendedDTO().getId());
		
		String url = rulesEngineUrl + "/incoming/call/recording";
		
		InboundCallRecordingUrlResponse inboundCallRecordingUrlResponse = restTemplate.postForObject(url, inboundCallRecordingUrlRequest, InboundCallRecordingUrlResponse.class);
		return inboundCallRecordingUrlResponse;
	}
}

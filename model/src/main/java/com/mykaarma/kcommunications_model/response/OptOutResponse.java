package com.mykaarma.kcommunications_model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OptOutResponse extends Response {
    
    private Boolean isMessageReactionDealerType;
    private Boolean hitOrakleApi;
    private String requestUUID;
    
	public Boolean getMessageReactionDealerType() {
		return isMessageReactionDealerType;
	}

	public void setMessageReactionDealerType(Boolean isMessageReactionDealerType) {
		this.isMessageReactionDealerType = isMessageReactionDealerType;
	}

	public Boolean getHitOrakleApi() {
		return hitOrakleApi;
	}

	public void setHitOrakleApi(Boolean hitOrakleApi) {
		this.hitOrakleApi = hitOrakleApi;
	}

	public String getRequestUUID() {
		return requestUUID;
	}

	public void setRequestUUID(String requestUUID) {
		this.requestUUID = requestUUID;
	}

}
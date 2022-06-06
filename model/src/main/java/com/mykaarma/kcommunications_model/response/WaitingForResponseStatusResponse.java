package com.mykaarma.kcommunications_model.response;

import lombok.Data;

@Data
public class WaitingForResponseStatusResponse extends Response {
	
	private boolean isInWaitingForResponse=false;

}

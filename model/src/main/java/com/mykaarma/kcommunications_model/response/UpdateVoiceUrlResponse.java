package com.mykaarma.kcommunications_model.response;

import java.util.List;
import java.util.Set;

import com.mykaarma.kcommunications_model.common.DealerUrlUpdateFailure;

import lombok.Data;

@Data
public class UpdateVoiceUrlResponse extends Response{
	
	private List<Long> dealerIDsUpdated;
	private List<DealerUrlUpdateFailure> dealerIdsFailed;
	private List<Long> dealerIdSuccessfullyProcessed;

}

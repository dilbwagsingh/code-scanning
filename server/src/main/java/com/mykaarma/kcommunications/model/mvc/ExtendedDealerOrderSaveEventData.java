package com.mykaarma.kcommunications.model.mvc;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
@SuppressWarnings("serial")
public class ExtendedDealerOrderSaveEventData implements Serializable {
	
	private DealerOrderSaveEventData dealerOrderSaveEventData;
	private List<String> collectionNames;

}

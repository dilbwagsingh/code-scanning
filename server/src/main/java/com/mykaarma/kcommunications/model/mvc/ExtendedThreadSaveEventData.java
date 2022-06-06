package com.mykaarma.kcommunications.model.mvc;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@SuppressWarnings("serial")
@Data
public class ExtendedThreadSaveEventData implements Serializable {
	private ThreadSaveEventData threadSaveEventData;
	private List<String> collectionNames;	
}
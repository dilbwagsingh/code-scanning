package com.mykaarma.kcommunications.model.utils;

import java.util.Comparator;

import com.mykaarma.kcommunications_model.request.SaveMessageRequest;

public class MessageComparatorByReceivedOn implements Comparator<SaveMessageRequest> {

	@Override
	public int compare(SaveMessageRequest m1, SaveMessageRequest m2) {
		return (m1.getReceivedOn().after(m2.getReceivedOn()) ? 1 : ((m1
				.getReceivedOn().equals(m2.getReceivedOn())) ? 0 : -1));
	}
}
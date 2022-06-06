package com.mykaarma.kcommunications_model.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum NotificationType {

	INTERNAL("INTERNAL"),
	EXTERNAL("EXTERNAL");
	
	private String notificationType;

}

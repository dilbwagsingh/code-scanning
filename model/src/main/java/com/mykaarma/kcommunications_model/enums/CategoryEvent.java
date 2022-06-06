package com.mykaarma.kcommunications_model.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CategoryEvent {

	INTERNAL_NOTE("INTERNAL_NOTE");
	private String categoryEvent ;

}

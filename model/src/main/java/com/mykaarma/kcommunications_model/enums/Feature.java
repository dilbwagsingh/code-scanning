package com.mykaarma.kcommunications_model.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Feature {

    OPT_IN_ADVANCED("opt-in-advanced"),
    ;

    private final String key;

}

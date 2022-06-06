package com.mykaarma.kcommunications_model.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum EditorType {

    USER("USER"),
    CUSTOMER("CUSTOMER");

    private String editorType;

}

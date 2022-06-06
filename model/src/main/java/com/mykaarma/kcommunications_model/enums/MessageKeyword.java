package com.mykaarma.kcommunications_model.enums;

public enum MessageKeyword {
    STOP,
    STOP_SUSPECTED,
    GENERIC,
    OPTIN,
    ;

    public static MessageKeyword fromString(String messageKeywordStr) {
        for(MessageKeyword messageKeyword : MessageKeyword.values()) {
            if(messageKeyword.name().equalsIgnoreCase(messageKeywordStr))
                return messageKeyword;
        }
        return null;
    }
}

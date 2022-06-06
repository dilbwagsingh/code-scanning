package com.mykaarma.kcommunications_model.enums;

public enum OptOutState {
    
    OPTED_OUT,
    OPTED_IN;

    public static OptOutState fromString(String optOutStateStr) {
        for(OptOutState optOutState : OptOutState.values()) {
            if(optOutState.name().equalsIgnoreCase(optOutStateStr)) {
                return optOutState;
            }
        }
        return null;
    }
}

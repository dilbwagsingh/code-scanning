package com.mykaarma.kcommunications.utils.TranscriptionHandler.dto;

import org.springframework.stereotype.Component;

@Component
public class Result {
    private Alternative[] alternatives;

    public Alternative[] getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(Alternative[] alternatives) {
        this.alternatives = alternatives;
    }

}
package com.mykaarma.kcommunications.utils.TranscriptionHandler.dto;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class Metadata {
    private String startTime;

    private String progressPercent;

    private String lastUpdateTime;
}


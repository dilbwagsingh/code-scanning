package com.mykaarma.kcommunications.utils.TranscriptionHandler.dto;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class JobStatusDto {
    private Response response;

    private boolean done;

    private String name;

    private Metadata metadata;

}

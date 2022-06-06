package com.mykaarma.kcommunications.utils.TranscriptionHandler.dto;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class Response {
	
    private Result[] results;

}
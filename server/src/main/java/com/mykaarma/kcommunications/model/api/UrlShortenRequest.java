package com.mykaarma.kcommunications.model.api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UrlShortenRequest {
    private List<String> urls;
    private String dealerUUID;
    private String dealerDepartmentUUID;
    private String shortUrlDomain;
}

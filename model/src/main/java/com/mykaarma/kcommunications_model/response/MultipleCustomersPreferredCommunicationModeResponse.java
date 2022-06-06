package com.mykaarma.kcommunications_model.response;

import java.util.Map;

public class MultipleCustomersPreferredCommunicationModeResponse extends Response {

    private static final long serialVersionUID = 1L;

    private Map<String, String> customerPreferredCommunicationModeMap;
    
    private String requestUUID;


    public Map<String, String> getCustomerPreferredCommunicationModeMap() {
        return customerPreferredCommunicationModeMap;
    }

    public void setCustomerPreferredCommunicationModeMap(Map<String, String> customerPreferredCommunicationModeMap) {
        this.customerPreferredCommunicationModeMap = customerPreferredCommunicationModeMap;
    }

    public String getRequestUUID() {
        return requestUUID;
    }

    public void setRequestUUID(String requestUUID) {
        this.requestUUID = requestUUID;
    }
}
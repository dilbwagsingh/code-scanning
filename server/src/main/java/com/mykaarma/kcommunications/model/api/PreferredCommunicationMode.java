package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;

public class PreferredCommunicationMode implements Serializable {

    private String protocol;
    private String metaData;

    public String getProtocol() {
        return protocol;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
   
}

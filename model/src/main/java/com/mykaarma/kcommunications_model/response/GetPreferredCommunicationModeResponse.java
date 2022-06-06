package com.mykaarma.kcommunications_model.response;

public class GetPreferredCommunicationModeResponse extends Response {

    private static final long serialVersionUID = 1L;

    private String preferredCommunicationMode;

    private String preferredCommunicationModeMetaData;

    private String requestUUID;

    public String getPreferredCommunicationMode() {
        return preferredCommunicationMode;
    }

    public String getPreferredCommunicationModeMetaData() {
		return preferredCommunicationModeMetaData;
	}

	public void setPreferredCommunicationModeMetaData(String preferredCommunicationModeMetaData) {
		this.preferredCommunicationModeMetaData = preferredCommunicationModeMetaData;
	}

	public void setPreferredCommunicationMode(String preferredCommunicationMode) {
        this.preferredCommunicationMode = preferredCommunicationMode;
    }

    public String getRequestUUID() {
        return requestUUID;
    }

    public void setRequestUUID(String requestUUID) {
        this.requestUUID = requestUUID;
    }

} 
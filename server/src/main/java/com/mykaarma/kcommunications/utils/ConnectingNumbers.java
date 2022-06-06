package com.mykaarma.kcommunications.utils;

import java.util.List;

import lombok.Data;

@Data
public class ConnectingNumbers{
	private List<String> connectingNumbers;
	
	private List<String> connectingDAs;
	
	public List<String> getConnectingNumbers() {
		return connectingNumbers;
	}
	public void setConnectingNumbers(List<String> connectingNumbers) {
		this.connectingNumbers = connectingNumbers;
	}
	public List<String> getConnectingClients() {
		return connectingDAs;
	}
	public void setConnectingClients(List<String> connectingDAs) {
		this.connectingDAs = connectingDAs;
	}
	
}

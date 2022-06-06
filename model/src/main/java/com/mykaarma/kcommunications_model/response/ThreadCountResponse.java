package com.mykaarma.kcommunications_model.response;

import java.util.Map;

import lombok.Data;

@Data
public class ThreadCountResponse extends Response{

	private Map<String,Long> count;
	private String requestUUID;
}

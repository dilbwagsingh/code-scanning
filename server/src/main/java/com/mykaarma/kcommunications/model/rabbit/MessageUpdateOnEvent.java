package com.mykaarma.kcommunications.model.rabbit;

import java.io.Serializable;

import com.mykaarma.kcommunications_model.enums.Event;

import lombok.Data;

@Data
public class MessageUpdateOnEvent implements Serializable{
	String messageUUID;
	Event event;
	Integer expiration;
}

package com.mykaarma.kcommunications_model.request;

import java.io.Serializable;
import java.util.List;

import com.mykaarma.kcommunications_model.common.UserEvent;

import lombok.Data;

@Data
public class ThreadFollowRequest implements Serializable {

	private List<UserEvent> userEvents;
	private String customerUuid;
	private String departmentUuid;

}

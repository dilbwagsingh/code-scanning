package com.mykaarma.kcommunications_model.response;

import java.util.List;

import com.mykaarma.kcommunications_model.common.UserEvent;
import lombok.Data;

@Data
public class ThreadFollowers extends Response{

	private List<UserEvent> userEvents;
	private String customerUuid;
	private String departmentUuid;

}

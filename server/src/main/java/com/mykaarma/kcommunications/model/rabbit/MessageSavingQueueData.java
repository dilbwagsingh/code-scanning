package com.mykaarma.kcommunications.model.rabbit;

import java.io.Serializable;
import java.util.List;

import com.mykaarma.kcommunications.model.jpa.Message;

import com.mykaarma.kcommunications_model.common.User;
import lombok.Data;

@SuppressWarnings("serial")
@Data
public class MessageSavingQueueData implements Serializable {
	private Message message;
	private Boolean updateThreadTimestamp;
	private List<User> usersToNotify;
}

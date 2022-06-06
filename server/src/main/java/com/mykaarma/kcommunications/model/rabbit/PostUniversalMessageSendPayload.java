package com.mykaarma.kcommunications.model.rabbit;

import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications_model.common.User;
import lombok.Data;

import java.util.List;

@Data
public class PostUniversalMessageSendPayload {

    private Message message;
    private List<User> usersToNotify;
    private Boolean updateThreadTimestamp;
    private Integer expiration;

}

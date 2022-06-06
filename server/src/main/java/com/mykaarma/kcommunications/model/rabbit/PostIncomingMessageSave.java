package com.mykaarma.kcommunications.model.rabbit;

import com.mykaarma.kcommunications.model.jpa.Message;
import lombok.Data;

@Data
public class PostIncomingMessageSave {

    private Message message;
    private Long threadDelegatee;
    private Boolean updateThreadTimestamp;
    private Integer expiration;

}

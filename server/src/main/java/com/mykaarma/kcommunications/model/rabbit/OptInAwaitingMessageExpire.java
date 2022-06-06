package com.mykaarma.kcommunications.model.rabbit;

import lombok.Data;

@Data
public class OptInAwaitingMessageExpire {

    private String messageUUID;
    private Integer expiration;

}

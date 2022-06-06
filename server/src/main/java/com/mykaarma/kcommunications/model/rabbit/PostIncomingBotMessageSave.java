package com.mykaarma.kcommunications.model.rabbit;

import com.mykaarma.kcommunications.model.jpa.BotMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PostIncomingBotMessageSave extends BaseRmqMessage {
    private BotMessage botMessage;
}

package com.mykaarma.kcommunications_model.response;

import com.mykaarma.kcommunications_model.enums.Status;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ForwardMessageResponse extends Response {

    private String requestUuid;
    private Status status;
    private String forwardedMessageUuid;
}
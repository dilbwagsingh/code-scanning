package com.mykaarma.kcommunications.model.rabbit;

import java.util.List;
import java.util.Map;

import com.mykaarma.kcommunications_model.enums.OptOutState;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PostOptOutStatusUpdate {
    private OptOutStatusUpdate optOutStatusUpdate;
    private String template;
    private Map<String, Object> templateParams;
    private OptOutState newOptOutState;
    private OptOutState currentOptOutState;
    private PostOptOutStatusUpdateEvent event;
    private List<Long> requestedDepartmentGroupDepartments;
    private Integer expiration;

    public enum PostOptOutStatusUpdateEvent {
        SEND_SYSTEM_NOTIFICATION,
        SEND_AUTORESPONDER,
        SEND_OPTOUT_STATUS_UPDATE_KNOTIFICATION_MESSAGE,
        UPDATE_MESSAGE_META_DATA_AND_MESSAGE_PREDICTION,
        OPTIN_AWAITING_MESSAGE_QUEUE_PROCESSING,
        SEND_MVC_UPDATE,
    }
}

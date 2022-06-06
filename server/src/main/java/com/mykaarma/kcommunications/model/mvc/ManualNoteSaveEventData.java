package com.mykaarma.kcommunications.model.mvc;

import com.mykaarma.kcommunications_model.common.User;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ManualNoteSaveEventData implements Serializable {

    private List<User> usersNotificationList;
    private List<SubscriberView> daNotificationList;
    private String dealerUuid;
    private String dealerDepartmentUuid;
    private String messageUuid;
    private String customerUuid;
    private Long threadId;
    private User eventRaiseBy;
    private String eventName;

}

package com.mykaarma.kcommunications_model.mvc;

import com.mykaarma.kcommunications_model.common.Subscriber;
import lombok.Data;

import java.util.List;

@Data
public class SubscriptionList {

    private Long customerID;
    private Long dealerID;
    List<Subscriber> internalSubscriberList;
    List<Subscriber> externalSubscriberList;

}

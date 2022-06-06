package com.mykaarma.kcommunications.utils;


import java.io.Serializable;

import javax.annotation.Generated;

import org.springframework.stereotype.Component;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@SuppressWarnings("unused")
@Component
@Data
public class MessageRequest implements Serializable{

    @SerializedName("channel")
    private String channel;
    @SerializedName("from")
    private String from;
    @SerializedName("messageBody")
    private String messageBody;


}
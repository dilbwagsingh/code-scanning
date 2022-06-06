package com.mykaarma.kcommunications_client.service;

import com.mykaarma.kcommunications_model.common.RestURIConstants;
import com.mykaarma.kcommunications_model.request.ForwardMessageRequest;
import com.mykaarma.kcommunications_model.request.SaveBotMessageRequest;
import com.mykaarma.kcommunications_model.request.SendBotMessageRequest;
import com.mykaarma.kcommunications_model.response.BotMessageResponse;
import com.mykaarma.kcommunications_model.response.ForwardMessageResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ForwardedAndBotMessageService {

    @POST(RestURIConstants.DEPARTMENT + "/" + RestURIConstants.DEPARTMENT_PATH_VARIABLE + "/"
        + RestURIConstants.USER + "/" + RestURIConstants.USER_PATH_VARIABLE + "/" + RestURIConstants.MESSAGE + "/"
        + RestURIConstants.MESSAGE_PATH_VARIABLE + "/" + RestURIConstants.FORWARD)
    Call<ForwardMessageResponse> forwardMessage(
        @Path(RestURIConstants.DEPARTMENT_UUID) String departmentUuid,
        @Path(RestURIConstants.USER_UUID) String userUuid,
        @Path(RestURIConstants.MESSAGE_UUID) String messageUuid,
        @Body ForwardMessageRequest forwardMessageRequest);

    @POST(RestURIConstants.DEPARTMENT + "/" + RestURIConstants.DEPARTMENT_PATH_VARIABLE + "/"
        + RestURIConstants.USER + "/" + RestURIConstants.USER_PATH_VARIABLE + "/" + RestURIConstants.BOT_MESSAGE
        + "/" + RestURIConstants.SEND)
    Call<BotMessageResponse> sendBotMessage(
        @Path(RestURIConstants.DEPARTMENT_UUID) String departmentUuid,
        @Path(RestURIConstants.USER_UUID) String userUuid,
        @Body SendBotMessageRequest sendBotMessageRequest);

    @PUT(RestURIConstants.DEPARTMENT + "/" + RestURIConstants.DEPARTMENT_PATH_VARIABLE + "/"
        + RestURIConstants.USER + "/" + RestURIConstants.USER_PATH_VARIABLE + "/" + RestURIConstants.BOT_MESSAGE)
    Call<BotMessageResponse> saveBotMessage(
        @Path(RestURIConstants.DEPARTMENT_UUID) String departmentUuid,
        @Path(RestURIConstants.USER_UUID) String userUuid,
        @Body SaveBotMessageRequest saveBotMessageRequest);

}

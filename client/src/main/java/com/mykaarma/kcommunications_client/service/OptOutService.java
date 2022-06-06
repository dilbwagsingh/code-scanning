package com.mykaarma.kcommunications_client.service;

import com.mykaarma.kcommunications_model.request.CommunicationsOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.CustomersOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.PredictOptOutStatusCallbackRequest;
import com.mykaarma.kcommunications_model.request.UpdateOptOutStatusRequest;
import com.mykaarma.kcommunications_model.response.OptOutResponse;
import com.mykaarma.kcommunications_model.response.OptOutStatusListResponse;
import com.mykaarma.kcommunications_model.response.OptOutStatusResponse;
import com.mykaarma.kcommunications_model.response.Response;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface OptOutService {

    @GET("department/{departmentUUID}/communicationType/{communicationType}/communicationValue/{communicationValue}/optoutstatus")
    Call<OptOutStatusResponse> getOptOutStatus(@Path("departmentUUID") String departmentUUID, @Path("communicationType") String communicationType,
        @Path("communicationValue") String communicationValue);

    @POST("department/{departmentUUID}/communications/optoutstatus/list")
    Call<OptOutStatusListResponse> getCommunicationsOutStatusList(@Path("departmentUUID") String departmentUUID,
        @Body CommunicationsOptOutStatusListRequest communicationsOptOutStatusListRequest);

    @POST("department/{departmentUUID}/customers/optoutstatus/list")
    Call<OptOutStatusListResponse> getCustomersOutStatusList(@Path("departmentUUID") String departmentUUID,
        @Body CustomersOptOutStatusListRequest customersOptOutStatusListRequest);

    @POST("department/{departmentUUID}/optoutstatus")
    Call<Response> updateOptOutStatus(@Path("departmentUUID") String departmentUUID,
        @Body UpdateOptOutStatusRequest updateOptOutStatusRequest);

    @POST("department/{departmentUUID}/message/{messageUUID}/optout/predict")
    Call<OptOutResponse> predictOptOutForMessage(@Path("messageUUID") String messageUUID, @Path("departmentUUID") String departmentUUID);

    @POST("department/{departmentUUID}/message/{messageUUID}/optoutstatus/predict/callback")
    Call<Response> predictOptOutStatusCallback(@Path("departmentUUID") String departmentUUID,
        @Path("messageUUID") String messageUUID, @Body PredictOptOutStatusCallbackRequest predictOptOutStatusCallbackRequest);
}

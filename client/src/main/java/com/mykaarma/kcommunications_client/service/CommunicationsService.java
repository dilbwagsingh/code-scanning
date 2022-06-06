package com.mykaarma.kcommunications_client.service;

import com.mykaarma.kcommunications_model.common.RestURIConstants;
import com.mykaarma.kcommunications_model.common.VoiceCallRequest;
import com.mykaarma.kcommunications_model.dto.DocFileDTO;
import com.mykaarma.kcommunications_model.enums.CommunicationsFeature;
import com.mykaarma.kcommunications_model.enums.Event;
import com.mykaarma.kcommunications_model.request.AutoCsiLogEventRequest;
import com.mykaarma.kcommunications_model.request.CommunicationCountRequest;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryRequest;
import com.mykaarma.kcommunications_model.request.DealersTemplateIndexRequest;
import com.mykaarma.kcommunications_model.request.DeleteAttachmentFromS3Request;
import com.mykaarma.kcommunications_model.request.DeleteSubscriptionsRequest;
import com.mykaarma.kcommunications_model.request.FetchCustomerLockRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesForCommunicationIdentifierListRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesForCustomerRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesRequest;
import com.mykaarma.kcommunications_model.request.MultipleCustomersPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.MultipleMessageRequest;
import com.mykaarma.kcommunications_model.request.PredictPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.SendDraftRequest;
import com.mykaarma.kcommunications_model.request.SendEmailRequest;
import com.mykaarma.kcommunications_model.request.SendMessageRequest;
import com.mykaarma.kcommunications_model.request.SendMessageWithoutCustomerRequest;
import com.mykaarma.kcommunications_model.request.SendNotificationWithoutCustomerRequest;
import com.mykaarma.kcommunications_model.request.TemplateSearchRequest;
import com.mykaarma.kcommunications_model.request.ThreadCountRequest;
import com.mykaarma.kcommunications_model.request.ThreadFollowRequest;
import com.mykaarma.kcommunications_model.request.TranslateTextRequest;
import com.mykaarma.kcommunications_model.request.UpdateCustomerSentimentStatusRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionFeedbackRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionFeedbackRequestNew;
import com.mykaarma.kcommunications_model.request.UpdateMessagePredictionRequest;
import com.mykaarma.kcommunications_model.request.UpdateMessageSentimentPredictionRequest;
import com.mykaarma.kcommunications_model.request.UpdatePreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.UploadAttachmentsToS3Request;
import com.mykaarma.kcommunications_model.response.GetDepartmentUUIDResponse;
import com.mykaarma.kcommunications_model.response.GetPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.MediaPreviewURLFetchResponse;
import com.mykaarma.kcommunications_model.response.CommunicationCountResponse;
import com.mykaarma.kcommunications_model.response.CommunicationHistoryResponse;
import com.mykaarma.kcommunications_model.response.CustomerLockListResponse;
import com.mykaarma.kcommunications_model.response.CustomerMessagesFetchResponse;
import com.mykaarma.kcommunications_model.response.DefaultThreadOwnerForDealerResponse;
import com.mykaarma.kcommunications_model.response.DefaultThreadOwnerResponse;
import com.mykaarma.kcommunications_model.response.DeleteAttachmentFromS3Response;
import com.mykaarma.kcommunications_model.response.FileDeleteResponse;
import com.mykaarma.kcommunications_model.response.FileUploadResponse;
import com.mykaarma.kcommunications_model.response.MessageRedactResponse;
import com.mykaarma.kcommunications_model.response.MessagesFetchResponse;
import com.mykaarma.kcommunications_model.response.MultipleCustomersPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.NotifierDeleteResponse;
import com.mykaarma.kcommunications_model.response.PredictPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.Response;
import com.mykaarma.kcommunications_model.response.SearchTemplateResponse;
import com.mykaarma.kcommunications_model.response.SendEmailResponse;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;
import com.mykaarma.kcommunications_model.response.SendMessageWithoutCustomerResponse;
import com.mykaarma.kcommunications_model.response.SendMultipleMessageResponse;
import com.mykaarma.kcommunications_model.response.SendNotificationWithoutCustomerResponse;
import com.mykaarma.kcommunications_model.response.TemplateIndexingResponse;
import com.mykaarma.kcommunications_model.response.ThreadCountResponse;
import com.mykaarma.kcommunications_model.response.ThreadFollowResponse;
import com.mykaarma.kcommunications_model.response.ThreadFollowers;
import com.mykaarma.kcommunications_model.response.TranslateLanguagesResponse;
import com.mykaarma.kcommunications_model.response.TranslateTextResponse;
import com.mykaarma.kcommunications_model.response.UpdateMessagePredictionFeedbackResponse;
import com.mykaarma.kcommunications_model.response.UploadAttachmentResponse;
import com.mykaarma.kcommunications_model.response.VoiceCallResponse;
import com.mykaarma.kcommunications_model.response.WaitingForResponseStatusResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface CommunicationsService {
	
	@POST("department/{departmentUUID}/dealerOrderUUID/{dealerOrderUUID}/autocsi/log")
	Call<Response> logAutoCsiStatus(@Body AutoCsiLogEventRequest requestData,
			@Path("departmentUUID") String departmentUUID, @Path("dealerOrderUUID") String dealerOrderUUID);

	@POST("department/{departmentUUID}/customer/{customerUUID}/message")
	Call<SendMessageResponse> createUniversalMessage(@Body SendMessageRequest requestData,
  			@Path("departmentUUID") String departmentUUID, @Path("customerUUID") String customerUUID);
	
	@POST("department/{departmentUUID}/user/{userUUID}/customer/{customerUUID}/message")
	Call<SendMessageResponse> sendMessage(@Body SendMessageRequest requestData, 
			@Path("departmentUUID") String departmentUUID, @Path("userUUID") String userUUID,@Path("customerUUID") String customerUUID);
	
	@PUT("department/{departmentUUID}/event/{event}/message/{messageUUID}")
	Call<Response> updateMessageOnEvent(@Path("departmentUUID") String departmentUUID, @Path("event") Event event, @Path("messageUUID") String messageUUID);
	
	@PUT("department/{departmentUUID}/message/{messageUUID}/received")
	Call<Response> postMessageReceived(@Path("departmentUUID") String departmentUUID, @Path("messageUUID") String messageUUID);
	
	@POST("department/{departmentUUID}/user/{userUUID}/notifier")
	Call<NotifierDeleteResponse> deleteNotifierEntries(@Path("userUUID") String userUUID, 
			@Path("departmentUUID") String departmentUUID);
	
	@GET("department/{departmentUUID}/user/{userUUID}/defaultThreadOwner")
	Call<DefaultThreadOwnerResponse> getDefaultThreadOwner(@Path("userUUID") String userUUID, 
			@Path("departmentUUID") String departmentUUID);
	
	@GET("dealer/{dealerUUID}/defaultThreadOwner")
	Call<DefaultThreadOwnerForDealerResponse> getDefaultThreadOwnerInfoForDealer(@Path("dealerUUID") String dealerUUID);
	
	
	@POST("department/{departmentUUID}/feature/{feature}/communicationvalue/{communicationValue}/usage")
	Call<Response> postUsage(@Path("departmentUUID") String departmentUUID, 
			@Path("feature") CommunicationsFeature feature, @Path("communicationValue") String communicationValue);
	
	@POST("department/{departmentUUID}/message/{messageUUID}")
	Call<SendMessageResponse> sendMessage(@Body SendDraftRequest sendDraftRequest, @Path("departmentUUID") String departmentUUID, 
			 @Path("messageUUID") String messageUUID);

	@POST("department/{departmentUUID}/message/{messageUUID}/recording/url")
	Call<Response> updateRecordingUrlForMessage(@Path("messageUUID") String messageUUID, @Path("departmentUUID") String departmentUUID);

	@POST("department/{departmentUUID}/customer/{customerUUID}/printThreadForCustomer")
	Call<CommunicationHistoryResponse> printThreadForCustomer(@Body CommunicationHistoryRequest commHistoryRequest, @Path("departmentUUID") String departmentUUID,
															@Path("customerUUID") String customerUUID);
	@POST("department/{departmentUUID}/customer/{customerUUID}/messageCount")
	Call<CommunicationCountResponse> fetchMessageCountForCustomer(@Body CommunicationCountRequest commCountRequest,
			@Path("departmentUUID") String departmentUUID, @Path("customerUUID") String customerUUID);
	
	@POST("department/{departmentUUID}/customerlock/list")
	Call<CustomerLockListResponse> fetchCustomerLockInfo(@Body FetchCustomerLockRequest fetchCustomerLockRequest,
			@Path("departmentUUID") String departmentUUID);

	@POST("department/{departmentUUID}/subscriptions")
	Call<Response> deleteSubscriptionsForUsers(@Body DeleteSubscriptionsRequest deleteSubscriptionsRequest, @Path("departmentUUID") String departmentUUID);

	@POST("department/{departmentUUID}/message/{messageUUID}/redact")
	Call<MessageRedactResponse> redactMessage(@Path("departmentUUID") String departmentUUID, @Path("messageUUID") String messageUUID);

	@PUT("department/{departmentUUID}/user/{userUUID}/messagepredictionfeedback")
	Call<Response> updateMessagePredictionFeedback(@Body UpdateMessagePredictionFeedbackRequest requestData, 
			@Path("departmentUUID") String departmentUUID, @Path("userUUID") String userUUID);

	@PUT("department/{departmentUUID}/user/{userUUID}/messagepredictionfeedbacknew")
	Call<UpdateMessagePredictionFeedbackResponse> updateMessagePredictionFeedbackNew(@Body UpdateMessagePredictionFeedbackRequestNew requestData, 
			@Path("departmentUUID") String departmentUUID, @Path("userUUID") String userUUID);
	
	@POST("department/{departmentUUID}/attachments/delete")
	Call<DeleteAttachmentFromS3Response> deleteAttachmentFromS3(
			@Body DeleteAttachmentFromS3Request deleteAttachmentFromS3Request, 
			@Path("departmentUUID") String departmentUUID
			);

	@POST("department/{departmentUUID}/attachments")
	Call<UploadAttachmentResponse> uploadAttachmentsToS3(@Body UploadAttachmentsToS3Request uploadAttachmentsToS3Request,
			@Path("departmentUUID") String departmentUUID);

	@POST("department/{departmentUUID}/attachment/preview/url/generate")
	Call<MediaPreviewURLFetchResponse> uploadMediaToAWSAndFetchMediaURL(@Body DocFileDTO docFileDTO,
			@Path("departmentUUID") String departmentUUID);
	
	@PUT("dealer/{dealerUuid}/type/{templateType}/template/{templateUuid}/index")
	Call<TemplateIndexingResponse> indexTemplate(@Path("dealerUuid") String dealerUUID,@Path("templateType") String templateType,
			@Path("templateUuid") String templateUuid);
	
	@DELETE("dealer/{dealerUuid}/type/{templateType}/template/{templateUuid}/index")
	Call<TemplateIndexingResponse> deleteTemplateIndex(@Path("dealerUuid") String dealerUUID,@Path("templateType") String templateType,
			@Path("templateUuid") String templateUuid);
	
	@POST("department/{departmentUuid}/template/search")
	Call<SearchTemplateResponse> searchTemplates(@Path("departmentUuid") String departmentUuid,@Body TemplateSearchRequest templateSearchRequest);
	
	@POST("department/{departmentUuid}/template/index")
	Call<TemplateIndexingResponse> indexTemplatesForDepartment(@Path("departmentUuid") String departmentUuid);
	
	@POST("dealer/{dealerUuid}/template/index")
	Call<TemplateIndexingResponse> indexTemplatesForDealer(@Path("dealerUuid") String dealerUuid,@Body DealersTemplateIndexRequest dealersTemplateIndexRequest);

	@GET("voicecredential/{brokerNumber}")
	Call<GetDepartmentUUIDResponse> getDepartmentUUIDForBrokerNumber(@Path("brokerNumber") String brokerNumber);

    @POST("department/{departmentUUID}/user/{userUUID}/multiple/message")
    Call<SendMultipleMessageResponse> createMultipleMessages(@Body MultipleMessageRequest multipleMessageRequest, @Path("departmentUUID") String departmentUUID,
        @Path("userUUID") String userUUID);

    @POST("department/{departmentUUID}/user/{userUUID}/customer/{customerUUID}/message/{messageUUID}")
	Call<SendMessageResponse> editDraft(@Body SendMessageRequest sendMessageRequest, @Path("departmentUUID") String departmentUUID, @Path("userUUID") String userUUID,
        @Path("customerUUID") String customerUUID, @Path("messageUUID") String messageUUID);

	@POST("department/{departmentUUID}/message/{messageUUID}/sentiment/predict")
	Call<Response> sentimentPredictionForMessage(@Path("departmentUUID") String departmentUUID, @Path("messageUUID") String messageUUID);

	@POST("department/{departmentUUID}/message/{messageUUID}/sentiment/prediction")
	Call<Response> updateMessageSentimentPrediction(@Body UpdateMessageSentimentPredictionRequest updateMessageSentimentPredictionRequest, 
			@Path("departmentUUID") String departmentUUID,@Path("messageUUID") String messageUUID);

	@POST("department/{departmentUUID}/customer/sentiment/status")
	Call<Response> updateCustomerSentimentStatus( @Path("departmentUUID") String departmentUUID, @Body UpdateCustomerSentimentStatusRequest updateCustomerSentimentStatusRequest);
	
	@POST("department/{departmentUUID}/message/prediction")
	Call<Response> updateMessagePrediction( @Path("departmentUUID") String departmentUUID, @Body UpdateMessagePredictionRequest updateMessagePredictionRequest);
	
	
	@POST("department/{departmentUUID}/email")
	Call<SendEmailResponse> sendEmailWithoutCustomer(@Body SendEmailRequest requestData, 
			@Path("departmentUUID") String departmentUUID);
	
	@POST("department/{departmentUUID}/user/{userUUID}/notify")
	Call<SendNotificationWithoutCustomerResponse> notifyWithoutCustomer(@Path("departmentUUID") String departmentUUID, @Path("userUUID") String userUUID,
			@Body SendNotificationWithoutCustomerRequest notificationRequest);
	
	@Multipart
	@PUT("file")
	Call<FileUploadResponse> fileUpload(@Part MultipartBody.Part filePart, @Part("fileType") RequestBody fileType, @Part("contentType") RequestBody contentType);
	
	@FormUrlEncoded
	@DELETE("file")
	Call<FileDeleteResponse> fileDelete(@Field("fileUrl") String fileUrl);

    @GET("department/{departmentUUID}/customer/{customerUUID}/preferredcommunicationmode")
    Call<GetPreferredCommunicationModeResponse> getPreferredCommunicationModeResponse(@Path("departmentUUID") String departmentUUID, @Path("customerUUID") String customerUUID);

    @POST("department/{departmentUUID}/customer/{customerUUID}/preferredcommunicationmode")
    Call<Response> updatePreferredCommunicationMode(@Body UpdatePreferredCommunicationModeRequest updatePreferredCommunicationModeRequest, 
        @Path("departmentUUID") String departmentUUID, @Path("customerUUID") String customerUUID);

    @POST("department/{departmentUUID}/customers/preferredcommunicationmode/protocol")
    Call<MultipleCustomersPreferredCommunicationModeResponse> getMultipleCustomersPreferredCommunicationModeProtocol(
        @Body MultipleCustomersPreferredCommunicationModeRequest multipleCustomersPreferredCommunicationModeRequest, @Path("departmentUUID") String departmentUUID);
                
    @POST("department/{departmentUUID}/customer/{customerUUID}/preferredcommunicationmode/predict")
	Call<PredictPreferredCommunicationModeResponse> predictPreferredCommunicationMode(@Body PredictPreferredCommunicationModeRequest predictPreferredCommunicationModeRequest, 
        @Path("departmentUUID") String departmentUUID, @Path("customerUUID") String customerUUID);

    @POST("department/{departmentUUID}/customer/{customerUUID}/twilio/callContact")
	Call<VoiceCallResponse> callContact(@Path("departmentUUID") String departmentUUID, @Path("customerUUID") String customerUUID, @Body VoiceCallRequest voiceCallRequest);
    
    @POST("department/{departmentUUID}/callSid/{callSID}/cancelCall")
    Call<Response> cancelCall(@Path("departmentUUID") String departmentUUID, @Path("callSID") String callSID);

    @POST("department/{departmentUUID}/user/{userUuid}/message/list")
	Call<MessagesFetchResponse> fetchMessages(@Path("departmentUUID") String departmentUUID, @Path("userUuid") String userUuid,
			@Body FetchMessagesRequest fetchMessagesRequest);

    @POST("department/{departmentUUID}/user/{userUuid}/customer/{customerUuid}/message/list")
	Call<CustomerMessagesFetchResponse> fetchMessagesForCustomer(@Path("departmentUUID") String departmentUUID, @Path("userUuid") String userUuid, 
			@Path("customerUuid") String customerUuid,
			@Body FetchMessagesForCustomerRequest fetchMessagesForCustomerRequest);
    
    @POST("department/{departmentUUID}/user/{userUuid}/communicationidentifier/message/list")
	Call<MessagesFetchResponse> fetchMessageForCommunicationIdentifiersList(@Path("departmentUUID") String departmentUUID, @Path("userUuid") String userUuid, 
			@Body FetchMessagesForCommunicationIdentifierListRequest fetchMessagesForCommunicationIdentifierList);
    
    @GET("translate/languages")
	Call<TranslateLanguagesResponse> getAvailableLanguages();
    
    @POST("translate/text")
	Call<TranslateTextResponse> translateText(@Body TranslateTextRequest translateTextRequest);

    @GET("department/{departmentUuid}/customer/{customerUuid}/waitingforresponse/status")
	Call<WaitingForResponseStatusResponse> fetchWaitingForResponseStatus(@Path("departmentUuid") String departmentUuid, 
			@Path("customerUuid") String customerUuid);

    @POST(RestURIConstants.DEPARTMENT+"/"+RestURIConstants.DEPARTMENT_PATH_VARIABLE+"/"+RestURIConstants.CUSTOMER+"/"+RestURIConstants.CUSTOMER_PATH_VARIABLE+"/" + RestURIConstants.FOLLOWERS)
	Call<ThreadFollowResponse> followUnFollowThread(@Path("departmentUUID") String departmentUUID, @Path("customerUUID") String customerUUID,
			@Body ThreadFollowRequest threadFollowRequest);

	@GET(RestURIConstants.DEPARTMENT+"/"+RestURIConstants.DEPARTMENT_PATH_VARIABLE+"/"+RestURIConstants.CUSTOMER+"/"+RestURIConstants.CUSTOMER_PATH_VARIABLE+"/" + RestURIConstants.FOLLOWERS)
	Call<ThreadFollowers> getFollowersToThread(@Path("departmentUUID") String departmentUUID, @Path("customerUUID") String customerUUID);
	
	@POST(RestURIConstants.EXTERNAL + "/" + RestURIConstants.MESSAGE)
	Call<SendMessageWithoutCustomerResponse> sendMessageWithoutCustomer(@Body SendMessageWithoutCustomerRequest requestData);
	
	@POST(RestURIConstants.DEPARTMENT + "/" + RestURIConstants.USER + "/" + RestURIConstants.USER_PATH_VARIABLE + "/" + RestURIConstants.THREAD + "/" + RestURIConstants.COUNT)
	Call<ThreadCountResponse> getNumberOfThreads(@Path("userUUID") String userUUID, @Body ThreadCountRequest request);

}

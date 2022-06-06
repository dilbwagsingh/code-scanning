package com.mykaarma.kcommunications_client;

import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mykaarma.kcommunications_client.service.CommunicationsService;
import com.mykaarma.kcommunications_client.service.ForwardedAndBotMessageService;
import com.mykaarma.kcommunications_client.service.OptOutService;
import com.mykaarma.kcommunications_model.common.VoiceCallRequest;
import com.mykaarma.kcommunications_model.dto.DocFileDTO;
import com.mykaarma.kcommunications_model.enums.CommunicationsFeature;
import com.mykaarma.kcommunications_model.enums.ErrorCode;
import com.mykaarma.kcommunications_model.enums.Event;
import com.mykaarma.kcommunications_model.enums.FileType;
import com.mykaarma.kcommunications_model.request.AutoCsiLogEventRequest;
import com.mykaarma.kcommunications_model.request.CommunicationCountRequest;
import com.mykaarma.kcommunications_model.request.CommunicationHistoryRequest;
import com.mykaarma.kcommunications_model.request.CommunicationsOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.CustomersOptOutStatusListRequest;
import com.mykaarma.kcommunications_model.request.DealersTemplateIndexRequest;
import com.mykaarma.kcommunications_model.request.DeleteAttachmentFromS3Request;
import com.mykaarma.kcommunications_model.request.DeleteSubscriptionsRequest;
import com.mykaarma.kcommunications_model.request.FetchCustomerLockRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesForCommunicationIdentifierListRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesForCustomerRequest;
import com.mykaarma.kcommunications_model.request.FetchMessagesRequest;
import com.mykaarma.kcommunications_model.request.ForwardMessageRequest;
import com.mykaarma.kcommunications_model.request.MultipleCustomersPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.MultipleMessageRequest;
import com.mykaarma.kcommunications_model.request.PredictOptOutStatusCallbackRequest;
import com.mykaarma.kcommunications_model.request.PredictPreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.SaveBotMessageRequest;
import com.mykaarma.kcommunications_model.request.SendBotMessageRequest;
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
import com.mykaarma.kcommunications_model.request.UpdateOptOutStatusRequest;
import com.mykaarma.kcommunications_model.request.UpdatePreferredCommunicationModeRequest;
import com.mykaarma.kcommunications_model.request.UploadAttachmentsToS3Request;
import com.mykaarma.kcommunications_model.response.ApiError;
import com.mykaarma.kcommunications_model.response.BotMessageResponse;
import com.mykaarma.kcommunications_model.response.CommunicationCountResponse;
import com.mykaarma.kcommunications_model.response.CommunicationHistoryResponse;
import com.mykaarma.kcommunications_model.response.CustomerLockListResponse;
import com.mykaarma.kcommunications_model.response.DefaultThreadOwnerForDealerResponse;
import com.mykaarma.kcommunications_model.response.DefaultThreadOwnerResponse;
import com.mykaarma.kcommunications_model.response.DeleteAttachmentFromS3Response;
import com.mykaarma.kcommunications_model.response.FileDeleteResponse;
import com.mykaarma.kcommunications_model.response.FileUploadResponse;
import com.mykaarma.kcommunications_model.response.ForwardMessageResponse;
import com.mykaarma.kcommunications_model.response.GetDepartmentUUIDResponse;
import com.mykaarma.kcommunications_model.response.GetPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.MessageRedactResponse;
import com.mykaarma.kcommunications_model.response.MultipleCustomersPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.NotifierDeleteResponse;
import com.mykaarma.kcommunications_model.response.OptOutResponse;
import com.mykaarma.kcommunications_model.response.OptOutStatusListResponse;
import com.mykaarma.kcommunications_model.response.OptOutStatusResponse;
import com.mykaarma.kcommunications_model.response.PredictPreferredCommunicationModeResponse;
import com.mykaarma.kcommunications_model.response.SendEmailResponse;
import com.mykaarma.kcommunications_model.response.SendMessageResponse;
import com.mykaarma.kcommunications_model.response.SendMessageWithoutCustomerResponse;
import com.mykaarma.kcommunications_model.response.SendMultipleMessageResponse;
import com.mykaarma.kcommunications_model.response.SendNotificationWithoutCustomerResponse;
import com.mykaarma.kcommunications_model.response.ThreadCountResponse;
import com.mykaarma.kcommunications_model.response.ThreadFollowResponse;
import com.mykaarma.kcommunications_model.response.ThreadFollowers;
import com.mykaarma.kcommunications_model.response.TranslateLanguagesResponse;
import com.mykaarma.kcommunications_model.response.TranslateTextResponse;
import com.mykaarma.kcommunications_model.response.VoiceCallResponse;
import com.mykaarma.kcommunications_model.response.WaitingForResponseStatusResponse;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class KCommunicationsApiClientService {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(KCommunicationsApiClientService.class);
	private final static ObjectMapper mapper = new ObjectMapper();
	
	private CommunicationsService communicationsService;
	private OptOutService optOutService;
	private ForwardedAndBotMessageService forwardedAndBotMessageService;
	
	public KCommunicationsApiClientService(String baseUrl, String userName, String password) {
		createRetrofit(baseUrl, userName, password);
	}
	
	/****************************************Communications Service ***********************************************/
	
	public com.mykaarma.kcommunications_model.response.Response logAutoCsi(String departmentUUID, String dealerOrderUUID, AutoCsiLogEventRequest logAutoCsiRequest)
		throws Exception {
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.logAutoCsiStatus(logAutoCsiRequest, departmentUUID, dealerOrderUUID).execute();
		
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}

	public SendMessageResponse createUniversalMessage(String departmentUuid, String customerUuid, SendMessageRequest sendMessageRequest)
			throws Exception {

		Response<SendMessageResponse> response = communicationsService.createUniversalMessage(sendMessageRequest, departmentUuid, customerUuid).execute();
		return processResponse(response, SendMessageResponse.class);
	}
	
	public SendMessageResponse sendMessage(String departmentUUID, String userUUID, String customerUUID, SendMessageRequest sendMessageRequest) 
			throws Exception {
		
		Response<SendMessageResponse> response = communicationsService.sendMessage(sendMessageRequest, departmentUUID, userUUID, customerUUID).execute();
		return processResponse(response, SendMessageResponse.class);
	}
	
	public DefaultThreadOwnerResponse getDefaultThreadOwner(String departmentUUID, String userUUID) throws Exception{
		Response<DefaultThreadOwnerResponse> response=communicationsService.getDefaultThreadOwner(userUUID, departmentUUID).execute();
		return processResponse(response, DefaultThreadOwnerResponse.class);
	}
	
	public DefaultThreadOwnerForDealerResponse getDefaultThreadOwnerInfoForDealer(String dealerUUID) throws Exception{
		Response<DefaultThreadOwnerForDealerResponse> response=communicationsService.getDefaultThreadOwnerInfoForDealer(dealerUUID).execute();
		return processResponse(response, DefaultThreadOwnerForDealerResponse.class);
	}
	
	public ThreadCountResponse getNumberOfThreads(String userUUID, ThreadCountRequest request) throws Exception{
		Response<ThreadCountResponse> response = communicationsService.getNumberOfThreads(userUUID, request).execute();
		return processResponse(response, ThreadCountResponse.class);
	}
	
	public com.mykaarma.kcommunications_model.response.Response postMessageReceived(String departmentUUID, String messageUUID) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.postMessageReceived(departmentUUID, messageUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}
	
	public com.mykaarma.kcommunications_model.response.Response updateMessageOnEvent(String departmentUUID, String messageUUID, Event event) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.updateMessageOnEvent(departmentUUID, event, messageUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}
	
	
	public NotifierDeleteResponse deleteNotifierEntriesForUser(String departmentUUID, String userUUID) 
			throws Exception {
		
		Response<NotifierDeleteResponse> response = communicationsService.deleteNotifierEntries(userUUID, departmentUUID).execute();
		return processResponse(response, NotifierDeleteResponse.class);
	}
	
	public com.mykaarma.kcommunications_model.response.Response  postUsage(String departmentUUID, CommunicationsFeature feature, String communicationValue) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.postUsage(departmentUUID, feature, communicationValue).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}
	
	public SendMessageResponse sendMessage(String departmentUUID, String messageUUID, SendDraftRequest sendDraftRequest) 
			throws Exception {
		
		Response<SendMessageResponse> response = communicationsService.sendMessage(sendDraftRequest, departmentUUID, messageUUID).execute();
		return processResponse(response, SendMessageResponse.class);
	}

	public com.mykaarma.kcommunications_model.response.Response updateRecordingUrlForMessage(String departmentUUID, String messageUUID) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.updateRecordingUrlForMessage(messageUUID, departmentUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}
	
	public OptOutResponse predictOptOutForMessage(String messageUUID, String departmentUUID) 
			throws Exception {
		
		Response<OptOutResponse> response = optOutService.predictOptOutForMessage(messageUUID, departmentUUID).execute();
		return processResponse(response, OptOutResponse.class);
	}
	
	public CommunicationHistoryResponse printThreadForCustomer(CommunicationHistoryRequest commHistoryRequest, String departmentUUID, String customerUUID) 
			throws Exception {
		
		Response<CommunicationHistoryResponse> response = communicationsService.printThreadForCustomer(commHistoryRequest, departmentUUID, customerUUID).execute();
		return processResponse(response, CommunicationHistoryResponse.class);
	} 
	
	public CommunicationCountResponse fetchMessageCountForCustomer(CommunicationCountRequest commCountRequest, String departmentUUID, String customerUUID) 
			throws Exception {

		Response<CommunicationCountResponse> response = communicationsService.fetchMessageCountForCustomer(commCountRequest, departmentUUID, customerUUID).execute();
		return processResponse(response, CommunicationCountResponse.class);
	} 
	
	public CustomerLockListResponse getCustomerLockInfo(FetchCustomerLockRequest fetchCustomerLockRequest, String departmentUuid) 
			throws Exception {

		Response<CustomerLockListResponse> response = communicationsService.fetchCustomerLockInfo(fetchCustomerLockRequest, departmentUuid).execute();
		return processResponse(response, CustomerLockListResponse.class);
	} 
	
	public com.mykaarma.kcommunications_model.response.Response updateMessagePredictionFeedback(UpdateMessagePredictionFeedbackRequest updateMessagePredictionFeedbackRequest, String departmentUUID, String userUUID) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.updateMessagePredictionFeedback(updateMessagePredictionFeedbackRequest, departmentUUID, userUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}
	
	public com.mykaarma.kcommunications_model.response.UpdateMessagePredictionFeedbackResponse updateMessagePredictionFeedbackNew(UpdateMessagePredictionFeedbackRequestNew updateMessagePredictionFeedbackRequest, String departmentUUID, String userUUID) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.UpdateMessagePredictionFeedbackResponse> response = communicationsService.updateMessagePredictionFeedbackNew(updateMessagePredictionFeedbackRequest, departmentUUID, userUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.UpdateMessagePredictionFeedbackResponse.class);
	}
	
	public com.mykaarma.kcommunications_model.response.Response sentimentPredictionForMessage(String departmentUUID, String messageUUID) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.sentimentPredictionForMessage(departmentUUID, messageUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}
	
	public com.mykaarma.kcommunications_model.response.Response updateMessageSentimentPrediction(String departmentUUID, String messageUUID, UpdateMessageSentimentPredictionRequest updateMessageSentimentPredictionRequest) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.updateMessageSentimentPrediction(updateMessageSentimentPredictionRequest, departmentUUID, messageUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}
	
	public com.mykaarma.kcommunications_model.response.Response updateCustomerSentimentStatus(String departmentUUID, UpdateCustomerSentimentStatusRequest updateCustomerSentimentStatusRequest) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.updateCustomerSentimentStatus(departmentUUID, updateCustomerSentimentStatusRequest).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}
	
	public com.mykaarma.kcommunications_model.response.Response updateMessagePrediction(String departmentUUID, UpdateMessagePredictionRequest updateMessagePredictionRequest) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.updateMessagePrediction(departmentUUID, updateMessagePredictionRequest).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}

	public com.mykaarma.kcommunications_model.response.UploadAttachmentResponse uploadAttachmentsToS3(UploadAttachmentsToS3Request uploadAttachmentsToS3Request, String departmentUUID) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.UploadAttachmentResponse> response = communicationsService.uploadAttachmentsToS3(uploadAttachmentsToS3Request, departmentUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.UploadAttachmentResponse.class);
	}

	public com.mykaarma.kcommunications_model.response.MediaPreviewURLFetchResponse uploadMediaToAWSAndFetchMediaURL(DocFileDTO docFileDTO, String departmentUUID) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.MediaPreviewURLFetchResponse> response = communicationsService.uploadMediaToAWSAndFetchMediaURL(docFileDTO, departmentUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.MediaPreviewURLFetchResponse.class);
	}
	
	public com.mykaarma.kcommunications_model.response.TemplateIndexingResponse indexTemplate(String templateType, String dealerUuid,String templateUuid) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.TemplateIndexingResponse> response = communicationsService.indexTemplate(dealerUuid, templateType,templateUuid).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.TemplateIndexingResponse.class);
	}
	
	public com.mykaarma.kcommunications_model.response.TemplateIndexingResponse deleteTemplateIndex(String templateType, String dealerUuid,String templateUuid) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.TemplateIndexingResponse> response = communicationsService.deleteTemplateIndex(dealerUuid, templateType,templateUuid).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.TemplateIndexingResponse.class);
	}
	
	public com.mykaarma.kcommunications_model.response.TemplateIndexingResponse indexTemplatesForDepartment(String departmentUuid) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.TemplateIndexingResponse> response = communicationsService.indexTemplatesForDepartment(departmentUuid).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.TemplateIndexingResponse.class);
	}
	
	public com.mykaarma.kcommunications_model.response.TemplateIndexingResponse indexTemplatesForDealer(String dealerUuid, DealersTemplateIndexRequest dealersTemplateIndexRequest) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.TemplateIndexingResponse> response = communicationsService.indexTemplatesForDealer(dealerUuid, dealersTemplateIndexRequest).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.TemplateIndexingResponse.class);
	}
	
	public com.mykaarma.kcommunications_model.response.SearchTemplateResponse searchTemplates(String departmentUuid,TemplateSearchRequest templateSearchRequest) 
			throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.SearchTemplateResponse> response = communicationsService.searchTemplates(departmentUuid, templateSearchRequest).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.SearchTemplateResponse.class);
	}
	
	public com.mykaarma.kcommunications_model.response.Response deleteSubscriptionsForUsers(List<Long> dealerAssociateIDs, String departmentUUID) 
			throws Exception {
		DeleteSubscriptionsRequest deleteSubscriptionsRequest = new DeleteSubscriptionsRequest();
		deleteSubscriptionsRequest.setDealerAssociates(dealerAssociateIDs);
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.deleteSubscriptionsForUsers(deleteSubscriptionsRequest, departmentUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
		
	}
	
	public MessageRedactResponse redactMessage(String departmentUUID, String messageUUID) 
	throws Exception {

		Response<MessageRedactResponse> response = communicationsService.redactMessage(departmentUUID, messageUUID).execute();
		return processResponse(response, MessageRedactResponse.class);
	}
	
	public GetDepartmentUUIDResponse getDepartmentUUIDForBrokerNumber(String brokerNumber) throws Exception {
		
		Response<GetDepartmentUUIDResponse> response = communicationsService.getDepartmentUUIDForBrokerNumber(brokerNumber).execute();
		return processResponse(response, GetDepartmentUUIDResponse.class);
	}

    public SendMultipleMessageResponse createMultipleMessages(String departmentUUID, String userUUID, MultipleMessageRequest multipleMessageRequest) 
	throws Exception {

		Response<SendMultipleMessageResponse> response = communicationsService.createMultipleMessages(multipleMessageRequest, departmentUUID, userUUID).execute();
		return processResponse(response, SendMultipleMessageResponse.class);
	}

    public SendMessageResponse editDraft(String departmentUUID, String userUUID, String customerUUID, String messageUUID, SendMessageRequest sendMessageRequest) 
			throws Exception {
		
		Response<SendMessageResponse> response = communicationsService.editDraft(sendMessageRequest, departmentUUID, userUUID, customerUUID, messageUUID).execute();
		return processResponse(response, SendMessageResponse.class);
    }

	public DeleteAttachmentFromS3Response deleteAttachmentFromS3(DeleteAttachmentFromS3Request deleteAttachmentFromS3Request, 
			String departmentUUID) throws Exception {
		
		Response<DeleteAttachmentFromS3Response> response = communicationsService.deleteAttachmentFromS3(deleteAttachmentFromS3Request, departmentUUID).execute();
		return processResponse(response, DeleteAttachmentFromS3Response.class);
	}
	
    public GetPreferredCommunicationModeResponse getPreferredCommunicationModeResponse(String departmentUUID, String customerUUID) 
        throws Exception {
		
		Response<GetPreferredCommunicationModeResponse> response = communicationsService.getPreferredCommunicationModeResponse(departmentUUID, customerUUID).execute();
		return processResponse(response, GetPreferredCommunicationModeResponse.class);
    }
    
    public WaitingForResponseStatusResponse fetchWaitingForResponseStatus(String departmentUuid, String customerUuid) 
            throws Exception {
    		
    	Response<WaitingForResponseStatusResponse> response = communicationsService.fetchWaitingForResponseStatus(departmentUuid, customerUuid).execute();
    	return processResponse(response, WaitingForResponseStatusResponse.class);
        }
    
    public com.mykaarma.kcommunications_model.response.Response updatePreferredCommunicationMode(String departmentUUID, String customerUUID, UpdatePreferredCommunicationModeRequest updatePreferredCommunicationModeRequest) 
        throws Exception {
		
		Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.updatePreferredCommunicationMode(updatePreferredCommunicationModeRequest, departmentUUID, customerUUID).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
    }

    public MultipleCustomersPreferredCommunicationModeResponse getMultipleCustomersPreferredCommunicationModeProtocol(String departmentUUID, MultipleCustomersPreferredCommunicationModeRequest multipleCustomersPreferredCommunicationModeRequest) 
        throws Exception {
		
		Response<MultipleCustomersPreferredCommunicationModeResponse> response = communicationsService.getMultipleCustomersPreferredCommunicationModeProtocol(multipleCustomersPreferredCommunicationModeRequest, departmentUUID).execute();
		return processResponse(response, MultipleCustomersPreferredCommunicationModeResponse.class);
    }
    
    public PredictPreferredCommunicationModeResponse predictPreferredCommunicationMode(String departmentUUID, String customerUUID, PredictPreferredCommunicationModeRequest predictPreferredCommunicationModeRequest) 
        throws Exception {
		
		Response<PredictPreferredCommunicationModeResponse> response = communicationsService.predictPreferredCommunicationMode(predictPreferredCommunicationModeRequest, departmentUUID, customerUUID).execute();
		return processResponse(response, PredictPreferredCommunicationModeResponse.class);
    }

	public OptOutStatusResponse getOptOutStatus(String departmentUUID, String communicationType, String communicationValue) throws Exception {
		Response<OptOutStatusResponse> response = optOutService.getOptOutStatus(departmentUUID, communicationType, communicationValue).execute();
		return processResponse(response, OptOutStatusResponse.class);
	}

	public OptOutStatusListResponse getCommunicationsOutStatusList(String departmentUUID, CommunicationsOptOutStatusListRequest communicationsOptOutStatusListRequest) throws Exception {
		Response<OptOutStatusListResponse> response = optOutService.getCommunicationsOutStatusList(departmentUUID, communicationsOptOutStatusListRequest).execute();
		return processResponse(response, OptOutStatusListResponse.class);
	}

	public OptOutStatusListResponse getCustomersOutStatusList(String departmentUUID, CustomersOptOutStatusListRequest customersOptOutStatusListRequest) throws Exception {
		Response<OptOutStatusListResponse> response = optOutService.getCustomersOutStatusList(departmentUUID, customersOptOutStatusListRequest).execute();
		return processResponse(response, OptOutStatusListResponse.class);
	}

	public com.mykaarma.kcommunications_model.response.Response updateOptOutStatus(String departmentUUID, UpdateOptOutStatusRequest updateOptOutStatusRequest) throws Exception {
		Response<com.mykaarma.kcommunications_model.response.Response> response = optOutService.updateOptOutStatus(departmentUUID, updateOptOutStatusRequest).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}

	public com.mykaarma.kcommunications_model.response.Response predictOptOutStatusCallback(String departmentUUID, String messageUUID, PredictOptOutStatusCallbackRequest predictOptOutStatusCallbackRequest) throws Exception {
		Response<com.mykaarma.kcommunications_model.response.Response> response = optOutService.predictOptOutStatusCallback(departmentUUID, messageUUID, predictOptOutStatusCallbackRequest).execute();
		return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class);
	}

    public VoiceCallResponse callContact(String departmentUUID, String customerUUID, VoiceCallRequest voiceCallRequest) throws Exception {
    	Response<VoiceCallResponse> response = communicationsService.callContact(departmentUUID, customerUUID, voiceCallRequest).execute();
    	return processResponse(response, VoiceCallResponse.class);
    } 
    
    public TranslateLanguagesResponse getAvailableLanguages() throws Exception {
    	Response<TranslateLanguagesResponse> response = communicationsService.getAvailableLanguages().execute();
    	return processResponse(response, TranslateLanguagesResponse.class);
    } 
    
    public TranslateTextResponse translateText(TranslateTextRequest translateTextRequest) throws Exception {
    	Response<TranslateTextResponse> response = communicationsService.translateText(translateTextRequest).execute();
    	return processResponse(response, TranslateTextResponse.class);
    }

	public ForwardMessageResponse forwardMessage(String departmentUuid, String userUuid, String messageUuid, ForwardMessageRequest forwardMessageRequest) throws Exception {
		Response<ForwardMessageResponse> response = forwardedAndBotMessageService.forwardMessage(departmentUuid, userUuid, messageUuid, forwardMessageRequest).execute();
		return processResponse(response, ForwardMessageResponse.class);
	}

	public BotMessageResponse sendBotMessage(String departmentUuid, String userUuid, SendBotMessageRequest sendBotMessageRequest) throws Exception {
		Response<BotMessageResponse> response = forwardedAndBotMessageService.sendBotMessage(departmentUuid, userUuid, sendBotMessageRequest).execute();
		return processResponse(response, BotMessageResponse.class);
	}

	public BotMessageResponse saveBotMessage(String departmentUuid, String userUuid, SaveBotMessageRequest saveBotMessageRequest) throws Exception {
		Response<BotMessageResponse> response = forwardedAndBotMessageService.saveBotMessage(departmentUuid, userUuid, saveBotMessageRequest).execute();
		return processResponse(response, BotMessageResponse.class);
	}

    public com.mykaarma.kcommunications_model.response.Response cancelCall(String departmentUUID, String callSid) throws Exception {
    	Response<com.mykaarma.kcommunications_model.response.Response> response = communicationsService.cancelCall(departmentUUID, callSid).execute();
    	return processResponse(response, com.mykaarma.kcommunications_model.response.Response.class); 
    }
    
    public com.mykaarma.kcommunications_model.response.MessagesFetchResponse fetchMessages(String departmentUuid, String userUuid,FetchMessagesRequest fetchMessagesRequest) throws Exception {
    	Response<com.mykaarma.kcommunications_model.response.MessagesFetchResponse> response = communicationsService.fetchMessages(departmentUuid, userUuid,fetchMessagesRequest).execute();
    	return processResponse(response, com.mykaarma.kcommunications_model.response.MessagesFetchResponse.class); 
    }
    
    public com.mykaarma.kcommunications_model.response.MessagesFetchResponse fetchMessageForCommunicationIdentifiersList(String departmentUuid, String userUuid,FetchMessagesForCommunicationIdentifierListRequest fetchMessagesForCommunicationIdentifierListRequest) throws Exception {
    	Response<com.mykaarma.kcommunications_model.response.MessagesFetchResponse> response = communicationsService.fetchMessageForCommunicationIdentifiersList(departmentUuid, userUuid,fetchMessagesForCommunicationIdentifierListRequest).execute();
    	return processResponse(response, com.mykaarma.kcommunications_model.response.MessagesFetchResponse.class); 
    }
    
    public com.mykaarma.kcommunications_model.response.CustomerMessagesFetchResponse fetchMessagesForCustomer(String departmentUuid, String userUuid,String customerUuid,FetchMessagesForCustomerRequest fetchMessagesForCustomerRequest) throws Exception {
    	Response<com.mykaarma.kcommunications_model.response.CustomerMessagesFetchResponse> response = communicationsService.fetchMessagesForCustomer(departmentUuid, userUuid,customerUuid,fetchMessagesForCustomerRequest).execute();
    	return processResponse(response, com.mykaarma.kcommunications_model.response.CustomerMessagesFetchResponse.class); 
    }
    
    public com.mykaarma.kcommunications_model.response.ThreadFollowResponse followUnfollowThread(String departmentUuid, String customerUuid,ThreadFollowRequest threadFollowRequest) throws Exception {
    	Response<ThreadFollowResponse> response = communicationsService.followUnFollowThread(departmentUuid,customerUuid,threadFollowRequest).execute();
    	return processResponse(response, com.mykaarma.kcommunications_model.response.ThreadFollowResponse.class); 
    }
    
    public com.mykaarma.kcommunications_model.response.ThreadFollowers getFollowersToAthread(String departmentUuid, String customerUuid) throws Exception {
    	Response<ThreadFollowers> response = communicationsService.getFollowersToThread(departmentUuid,customerUuid).execute();
    	return processResponse(response, com.mykaarma.kcommunications_model.response.ThreadFollowers.class); 
    }
    
	/****************************************Communications Service ***********************************************/
	
	
	
	public  <T extends com.mykaarma.kcommunications_model.response.Response> T processResponse(Response<T> retrofitResponse,
					Class<T> responseClassName) throws Exception{
		
		T response = null;
		if(retrofitResponse.body()!=null) {
			response = retrofitResponse.body();
			//response.setStatusCode(retrofitResponse.code());
		} 
		else if(retrofitResponse.errorBody()!=null) {
			
			String errorBody = retrofitResponse.errorBody().string();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			try {
				response = mapper.readValue(errorBody, responseClassName);
			} catch (Exception e) {
				
				LOGGER.warn(" Error while parsing error response_code=" +retrofitResponse.code()+" error_body="+errorBody, e);
				
				if(HttpStatus.GATEWAY_TIMEOUT.value()==retrofitResponse.code() || HttpStatus.REQUEST_TIMEOUT.value()==retrofitResponse.code()) {
					LOGGER.warn("request timedout while hitting kcommunications api, throwing timeout exception");
					throw new SocketTimeoutException();
				}
				JsonParser parser = new JsonParser();
				JsonObject jsonObject = parser.parse(errorBody).getAsJsonObject();
				String errorCode = ErrorCode.INTERNAL_SERVER_ERROR.name();
				String errorDesc = "Something went wrong in deserializng response!!!";
				if(HttpStatus.valueOf(retrofitResponse.code()) != null) {
					errorCode = HttpStatus.valueOf(retrofitResponse.code()).name();
				}
				if(jsonObject.get("message") != null && !StringUtils.isBlank(jsonObject.get("message").getAsString())) {
					errorDesc = jsonObject.get("message").getAsString();
				}
				List<ApiError> errors = new ArrayList<>();
				ApiError error = new ApiError(errorCode, errorDesc);
				response = responseClassName.newInstance();
				errors.add(error);
				response.setErrors(errors);
			}
			//response.setStatusCode(retrofitResponse.code());
		} else {
			response = responseClassName.newInstance();
			//response.setStatusCode(retrofitResponse.code());
		}
		
		return response;
	}
	
	private void createRetrofit(String baseUrl, String userName, String password) {
		OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
		addLoggingHeader(httpClient);
		addAuthenticationAndAcceptsHeader(httpClient, userName, password, "application/json");
		httpClient.connectTimeout(1, TimeUnit.MINUTES).readTimeout(30, TimeUnit.SECONDS).
			writeTimeout(15, TimeUnit.SECONDS);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Retrofit jsonRetrofit = new Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(JacksonConverterFactory.create(mapper)).client(httpClient.build()).build();
		communicationsService = jsonRetrofit.create(CommunicationsService.class);
		optOutService = jsonRetrofit.create(OptOutService.class);
		forwardedAndBotMessageService = jsonRetrofit.create(ForwardedAndBotMessageService.class);
	}
	
	private void addLoggingHeader(OkHttpClient.Builder httpClient) {
		HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC);
		if (!httpClient.interceptors().contains(logging)) {
			httpClient.addInterceptor(logging);
		}
	}
	
	private void addAuthenticationAndAcceptsHeader(OkHttpClient.Builder httpClient, String userName, String password, String accept) {
		String auth = userName + ":" + password;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")));
		final String authHeader = "Basic " + new String(encodedAuth);
		httpClient.addInterceptor(chain -> {
			okhttp3.Request request = chain.request();
			Headers headers = request.headers().newBuilder().add("Authorization", authHeader).add("Accept", accept).build();
			request = request.newBuilder().headers(headers).build();
			return chain.proceed(request);
		});
	}
	
	public SendEmailResponse sendEmailWithoutCustomer(String departmentUUID, SendEmailRequest sendEmailRequest) 
			throws Exception {
		
		Response<SendEmailResponse> response = communicationsService.sendEmailWithoutCustomer(sendEmailRequest,departmentUUID).execute();
		return processResponse(response, SendEmailResponse.class);
	}
	
	public SendNotificationWithoutCustomerResponse sendNotificationWithoutCustomer(String departmentUUID, String userUUID,
			SendNotificationWithoutCustomerRequest request) throws Exception {
		return processResponse(communicationsService.notifyWithoutCustomer(departmentUUID, userUUID, request).execute(),
				SendNotificationWithoutCustomerResponse.class);
	}
	
	public FileUploadResponse fileUpload(ByteArrayResource fileData, String contentType, FileType fileType) throws Exception {
		MediaType mediaType = contentType != null ? MediaType.parse(contentType) : null;
		if (mediaType == null) {
			mediaType = MediaType.parse("application/octet-stream");
		}
		MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileData.getFilename(), RequestBody.create(mediaType, fileData.getByteArray()));
		RequestBody fileTypeBody = RequestBody.create(MediaType.parse("text/plain"), fileType.name());
		RequestBody contentTypeBody = RequestBody.create(MediaType.parse("text/plain"), contentType);
		return processResponse(communicationsService.fileUpload(filePart, fileTypeBody, contentTypeBody).execute(), FileUploadResponse.class);
	}
	
	public FileDeleteResponse fileDelete(String fileUrl) throws Exception {
		return processResponse(communicationsService.fileDelete(fileUrl).execute(), FileDeleteResponse.class);
	}
	
	public SendMessageWithoutCustomerResponse sendMessageWithoutCustomer(SendMessageWithoutCustomerRequest sendMessageRequest) 
			throws Exception {
		
		Response<SendMessageWithoutCustomerResponse> response = communicationsService.sendMessageWithoutCustomer(sendMessageRequest).execute();
		return processResponse(response, SendMessageWithoutCustomerResponse.class);
	}

}

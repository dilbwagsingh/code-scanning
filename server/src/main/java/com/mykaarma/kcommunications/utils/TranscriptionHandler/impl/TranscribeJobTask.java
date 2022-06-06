package com.mykaarma.kcommunications.utils.TranscriptionHandler.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageExtnRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.StorageService;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.TranscribeService;


@Service
public class TranscribeJobTask implements Runnable{

    /**
     * The logger instance.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TranscribeJobTask.class);

    /**
     * Pending transcription value.
     */
    private static final String TRANSCRIPTION_PENDING = "Transcription pending";

    /**
     * The google storage service instance.
     */
    @Autowired
    private StorageService storageService;

    /**
     * The google transcribe service instance.
     */
    @Autowired
    private TranscribeService transcribeService;
    
    @Autowired
    private ApplicationContext context;
   
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    KManageApiHelper kManageApiHelper;
    
    @Autowired
    GeneralRepository generalRepository;
    
    @Autowired 
    MessageExtnRepository messageExtnRepository;
    
    @Autowired
    KCommunicationsUtils kCommunicationsUtils;
    /**
     * Job polling thread pool executor instance.
     */
    @Autowired
    @Qualifier("transcribeJobPollExecutor")
    private ThreadPoolTaskExecutor transcribeJobPollExecutor;
    
    @Value("${bucket.name}")
    String BUCKET_NAME;


    /**
     * The twilio recording url.
     */
    private String twilioRecordingUrl;

    /**
     * Communication UID of the message
     */
    private String messageCommunicationUid;

    /**
     * Set the communication UID for the task
     * @param messageCommunicationUid given communcation UID
     */
    public void setMessageCommunicationUid(String messageCommunicationUid) {
        this.messageCommunicationUid = messageCommunicationUid;
    }

    /**
     * Set the recording url.
     *
     * @param twilioRecordingUrl the recording url
     */
    public void setTwilioRecordingUrl(String twilioRecordingUrl) {
        this.twilioRecordingUrl = twilioRecordingUrl;
    }

    /**
     * Upload recording to google cloud followed by transcription.
     */
    @Override
    public void run() {
    	    	
        // retrieving message for the communication uid
    	Message message = messageRepository.getMessageFromCommunicationUidMatch(messageCommunicationUid);
    	Long dealerID = message.getDealerID();
    	LOGGER.info("Transcription initiated for MessageID = {}, dealerID = {}", message.getId(), dealerID);
    	
    	MessageExtn messageExtn = messageExtnRepository.findByMessageID(message.getId());
    	
    	message.setMessageExtn(messageExtn);    	
    	
    	String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
    	String dealerSetupOption = "";
		try {
			dealerSetupOption = kManageApiHelper.getDealerSetupOptionValueForADealer(dealerUUID, DealerSetupOption.TRANSCRIPTION_ENABLE.getOptionKey());
			LOGGER.info("DealerSetupOption for transcription :" + dealerSetupOption);
		} catch (Exception e1) {
			LOGGER.error("Error in getting dso for dealer", e1);
		}

        // checking if the dso is true
        if (dealerSetupOption!=null && dealerSetupOption.equals("true") && twilioRecordingUrl!=null && !twilioRecordingUrl.isEmpty()) {

        	LOGGER.info("transcription_enable = {}, dealer_id= {}", dealerSetupOption, dealerID);

            // update transcript to pending
        	kCommunicationsUtils.updateRecordingTranscript(message, TRANSCRIPTION_PENDING);
        	
            // Name of the content to be uploaded on cloud
            String contentName = "rec_" + UUID.randomUUID().toString();          
        

            // Uploading the recoding on google cloud
            try {
                boolean isUploaded = storageService.uploadToCloud(twilioRecordingUrl, BUCKET_NAME, contentName, "audio/wav");
                if (!isUploaded) {
                	LOGGER.error("Unable to upload recording to Google cloud recording_url=\"" + twilioRecordingUrl + "\"");
                    return;
                }
            } catch (Exception e) {
            	LOGGER.error("Unable to upload recording to Google cloud recording_url=\"" + twilioRecordingUrl + "\"", e);
                return;
            }

            LOGGER.info("Recording uploaded to google cloud successfully recording_url=\"" + twilioRecordingUrl + "\"");

            // Google storage based recording url
            String googleRecordingUrl = "gs://" + BUCKET_NAME + "/" + contentName;

            // Transcribe asynchronous api call
            String transcribeJobName = transcribeService.transcribe(googleRecordingUrl, 8000, "en-US");

            LOGGER.info("Transcription in process on google cloud transcribe_job_name=\"" + transcribeJobName + "\"");

            TranscribeJobPollTask transcribeJobPollTask = getSpringPrototypeScopedTranscribeJobPollTask();
            // setting the transcribe job name
            transcribeJobPollTask.setTranscribeJobName(transcribeJobName);
            // setting communication uid of the corresponding message
            transcribeJobPollTask.setCommunicationUid(messageCommunicationUid);
            // set the name of the recording to be deleted later
            transcribeJobPollTask.setRecordingName(contentName);
            // set the name of the bucket
            transcribeJobPollTask.setBucketName(BUCKET_NAME);

            // Asynchronously executing job polling task for the given job name
            transcribeJobPollExecutor.execute(transcribeJobPollTask);
        }else {
        	if(twilioRecordingUrl==null || twilioRecordingUrl.isEmpty()) {
        		LOGGER.info("No recordings found for message_id = " + message.getId());
        	}
        }
    }

    public TranscribeJobPollTask getSpringPrototypeScopedTranscribeJobPollTask() {
//        return (TranscribeJobPollTask) KService.getInstance().getAppContext().getBean("transcribeJobPollTask");
        return (TranscribeJobPollTask) context.getBean(TranscribeJobPollTask.class);
    }
}
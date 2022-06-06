package com.mykaarma.kcommunications.utils.TranscriptionHandler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageExtnRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.jpa.repository.VoiceCallRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;
import com.mykaarma.kcommunications.utils.KCommunicationsUtils;
import com.mykaarma.kcommunications.utils.KNotificationApiHelper;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.StorageService;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.TranscribeService;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.dto.Alternative;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.dto.JobStatusDto;
import com.mykaarma.kcommunications.utils.TranscriptionHandler.dto.Result;

@Service
public class TranscribeJobPollTask implements Runnable{

    /**
     * The logger instance.
     */
    private static final Logger LOGGER=LoggerFactory.getLogger(TranscribeJobPollTask.class);

    /**
     * Maximum polling attempts.
     */
    private static final int MAX_POLLING_ATTEMPT = 90;

    /**
     * The google transcribe service instance.
     */
    @Autowired
    private TranscribeService transcribeService;


    /**
     * The {@link StorageService} instance.
     */
    @Autowired
    private StorageService storageService;

    /**
     * Name of the transcription job.
     */
    
    @Autowired
    MessageRepository messageRepository;
    
    @Autowired
    VoiceCallRepository voiceCallRepository;
    
    @Autowired
    GeneralRepository generalRepository;
    
    @Autowired
    KNotificationApiHelper kNotificationApiHelper;
    
    @Autowired
    MessageExtnRepository messageExtnRepository;
    
    @Autowired
    KCommunicationsUtils kCommunicationsUtils;
    
    private String transcribeJobName;

    /**
     * Communication UID of the message.
     */
    private String communicationUid;

    /**
     * Name of the recording on google cloud.
     */
    private String recordingName;

    /**
     * Name of the bucket on google cloud.
     */

    private String bucketName;

    /**
     * Set the Job name.
     *
     * @param transcribeJobName name of the google transcription job.
     */
    public void setTranscribeJobName(String transcribeJobName) {
        this.transcribeJobName = transcribeJobName;
    }

    /**
     * Set the Communication UID.
     *
     * @param communicationUid given communication uid.
     */
    public void setCommunicationUid(String communicationUid) {
        this.communicationUid = communicationUid;
    }

    /**
     * Set the google cloud recording name.
     *
     * @param recordingName name of the recording on google cloud.
     */
    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    /**
     * Set name of the google cloud bucket.
     *
     * @param bucketName name of the bucket.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public void run() {
        boolean isTranscribed = false;
        int attempt = 1;

        LOGGER.info("Started polling for the task transcribe_job_name = {}, communicationUid = {}", transcribeJobName, communicationUid);;
        while (!isTranscribed && attempt <= MAX_POLLING_ATTEMPT){
            //waiting 20 sec before next attempt
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            JobStatusDto jobStatus = transcribeService.getJobStatus(transcribeJobName);
            if(jobStatus.isDone()){
                isTranscribed = true;
                LOGGER.info("Transcription completed for transcribe_job_name = {}, communicationUid = {}", transcribeJobName, communicationUid);
                
                String transcriptData = getTranscriptData(jobStatus);
                
                updateTranscript(transcriptData);
                storageService.deleteFromCloud(bucketName,recordingName);
            } else {
                LOGGER.info("Transcription still in process polling_attempt = {}", attempt);
                if(attempt == 6){
                    updateTranscript("Transcription taking longer than usual.");
                }
            }
            attempt++;
        }

        // Checking if transcription still undone
        if(!isTranscribed){
            updateTranscript("Transcription failed!");
        }
    }

    /**
     * Insert transcript in the DB.
     *
     * @param transcriptData transcription data.
     */
    private void updateTranscript(String transcriptData){
        try {

            LOGGER.info("code inside updateTranscript for comunication_uid= {}", communicationUid);
            
            Message message = messageRepository.getMessageFromCommunicationUidMatch(communicationUid);
        	MessageExtn messageExtn = messageExtnRepository.findByMessageID(message.getId());
        	
        	message.setMessageExtn(messageExtn);    	
        	
            // updating the transcription for the recording
            kCommunicationsUtils.updateRecordingTranscript(message, transcriptData);
           
            
            Long callStatus = voiceCallRepository.getVoiceCallStatusFromCommunicationUID(communicationUid);
            if(callStatus==null) {
            	LOGGER.error("updateTranscript Error while inserting transcript in db since call_status=null for comunication_uid={} dealer_id={} call_status_id={} ", communicationUid, message.getDealerID(), callStatus);    
            }
            //broadcasting the update to everyone
            kNotificationApiHelper.broadcastCallUpdateEvent(message.getDealerID(), message.getCommunicationUid(),callStatus.intValue());
            
            LOGGER.info("updateTranscript for comunication_uid={} dealer_id={} call_status_id = {} ", communicationUid, message.getDealerID(), callStatus);
            
        }catch (Exception e){
            LOGGER.error("Error while inserting transcript in db transcribe_job_name= {}", transcribeJobName, e);
        }
    }

    /**
     * Get the transcription data from {@link JobStatusDto} object.
     *
     * @param jobStatusDto the {@link JobStatusDto} object.
     * @return transcription data.
     */
    private String getTranscriptData(JobStatusDto jobStatusDto){
        String zeroLengthTranscript = "Zero length and we don't have transcription.";
        Result[] results = jobStatusDto.getResponse().getResults();
        if(results == null){
            return zeroLengthTranscript;
        }
        String transcriptData = "";
        for(Result result : results){
            Alternative[] alternatives = result.getAlternatives();
            if(alternatives != null) {
                for (Alternative alternative : alternatives) {
                    transcriptData = transcriptData + alternative.getTranscript() + "\n";
                }
            }
        }
        return transcriptData.isEmpty() ? zeroLengthTranscript : transcriptData;
    }
}
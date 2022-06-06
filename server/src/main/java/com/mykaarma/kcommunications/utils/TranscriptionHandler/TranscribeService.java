package com.mykaarma.kcommunications.utils.TranscriptionHandler;

import org.springframework.stereotype.Service;

import com.mykaarma.kcommunications.utils.TranscriptionHandler.dto.JobStatusDto;

@Service
public interface TranscribeService {

    /**
     * Transcribe the recording present at the recording url.
     *
     * @param recordingUrl google cloud based recording url
     * @param sampleRate sampling rate
     * @param languageCode recording language eg en-US
     *
     * @return name of the job
     */
    String transcribe(String recordingUrl, int sampleRate, String languageCode);

    /**
     * Check for the completion of job.
     *
     * @param transcribeJobName name of the job
     */
    JobStatusDto getJobStatus(String transcribeJobName);
}
//package com.mykaarma.communications.scheduler;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.Scheduled;
//
//import com.mykaarma.communications.scheduler.impl.CommunicationsSchedulerImpl;
//
//@Configuration
//public class CommunicationsScheduler {
//
//	@Autowired
//	CommunicationsSchedulerImpl communicationsSchedulerImpl;
//	
//	private static Logger LOGGER = LoggerFactory.getLogger(CommunicationsScheduler.class);
//	private static final String CALLING_VERIFICAITION_PAREMETER = "Calling";
//	private static final String TEXTING_VERIFICAITION_PAREMETER = "Texting";
//	
//	@Scheduled(cron = "0 0 11 * * *")
//    public void verifyCommunicationsCallingBilling() {
//
//          LOGGER.info("running verifiication script for calling");
//          communicationsSchedulerImpl.verifyCommunicationsBilling(CALLING_VERIFICAITION_PAREMETER);
//
//    }
//	
//	@Scheduled(cron = "0 0 12 * * *")
//    public void verifyCommunicationsTextingBilling() {
//
//		LOGGER.info("running verifiication script for texting");
//		communicationsSchedulerImpl.verifyCommunicationsBilling(TEXTING_VERIFICAITION_PAREMETER);
//    } 
//}

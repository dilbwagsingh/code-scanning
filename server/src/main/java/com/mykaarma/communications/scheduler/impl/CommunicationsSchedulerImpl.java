//package com.mykaarma.communications.scheduler.impl;
//
//import java.math.BigInteger;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
//import com.mykaarma.kcommunications.model.utils.CommunicationsVerification;
//import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
//
//@Service
//public class CommunicationsSchedulerImpl {
//	
//	@Autowired
//	private GeneralRepository generalRepo;
//	
//	@Autowired
//	private RabbitHelper rabbitHelper;
//	
//	private static Logger LOGGER = LoggerFactory.getLogger(CommunicationsSchedulerImpl.class);
//	
//	public void verifyCommunicationsBilling(String verificationType) {
//		
//		List<BigInteger> departmentIDs = null;
//		LocalDateTime startTime = LocalDateTime.now().minusHours(24);
//		LocalDateTime endTime = LocalDateTime.now();
//		
//		try {
//			departmentIDs = generalRepo.getAllDepartments();
//			if(departmentIDs!=null && !departmentIDs.isEmpty()) {
//				
//				for(BigInteger deptId : departmentIDs) {
//					
//					try {
//						LOGGER.info("processing request to run verification request for verification_type={} dealer_department_id={}", verificationType, deptId.longValue());
//						CommunicationsVerification communicationsVerification = new CommunicationsVerification();
//						communicationsVerification.setDepartmentId(deptId.longValue());
//						communicationsVerification.setVerificationType(verificationType);
//						communicationsVerification.setStartDate(startTime);
//						communicationsVerification.setEndDate(endTime);
//						rabbitHelper.pushDataForVerifyCommunicationsBilling(communicationsVerification);
//					}
//					catch(Exception e) {
//						LOGGER.error("error processing verification request for verification_type={} dealer_department_id={} ", verificationType, deptId.longValue());
//					}
//				}
//			}
//		}
//		catch(Exception e) {
//			LOGGER.error("error processing verification request for verification_type={}", verificationType);
//		}
//	}	
//}

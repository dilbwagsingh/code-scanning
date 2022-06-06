package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.MessagePredictionFeedback;

@Transactional
public interface MessagePredictionFeedbackRepository extends JpaRepository<MessagePredictionFeedback, Long> {
	
	@Modifying(clearAutomatically = true)
	@Query(value = "Insert Into MessagePredictionFeedback(`MessagePredictionID`,`UserFeedback`,`FeedbackReason`,`UserUUID`,`DepartmentUUID`)" 
			 + 	" values(:messagePredictionID, :userFeedback, :feedbackReason, :userUUID, :departmentUUID) "
			 +  " ON DUPLICATE KEY UPDATE UserFeedback= :userFeedback, FeedbackReason= :feedbackReason" ,
			nativeQuery = true)
	public void upsertMessagePredictionFeedback(@Param("messagePredictionID")Long messagePredictionID, @Param("userFeedback")String userFeedback, 
			@Param("feedbackReason")String feedbackReason, @Param("userUUID")String userUUID, @Param("departmentUUID")String departmentUUID);
	
	public List<MessagePredictionFeedback> findAllByMessagePredictionIDInAndUserUUID(List<Long> messageUuids,String userUuid);
}
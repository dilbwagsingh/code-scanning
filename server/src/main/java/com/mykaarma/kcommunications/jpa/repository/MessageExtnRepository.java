package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.model.jpa.MessageExtn;

@Transactional
public interface MessageExtnRepository extends JpaRepository<MessageExtn, Long>  {

	@SuppressWarnings("unchecked")
	public MessageExtn saveAndFlush(MessageExtn messageExtn);

	@Query(value = "select messageBody from MessageExtn me where messageid = :messageID", nativeQuery = true)
	public String findByMessageId(@Param("messageID") Long messageID);
	
	public MessageExtn findByMessageID(Long messageID);
	
	public List<MessageExtn> findByMessageIDIn(List<Long> messageIdList);
	 
}

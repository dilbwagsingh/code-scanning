package com.mykaarma.kcommunications.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.MessageAttributes;

@Transactional
public interface MessageAttributesRepository extends JpaRepository<MessageAttributes, Long> {
	
	public MessageAttributes findByMessageID(Long messageID);

}

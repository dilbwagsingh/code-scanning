package com.mykaarma.kcommunications.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.MessageThread;

@Transactional
public interface MessageThreadRepository extends JpaRepository<MessageThread, Long> {

	@SuppressWarnings("unchecked")
	public MessageThread saveAndFlush(MessageThread messageThread);

}

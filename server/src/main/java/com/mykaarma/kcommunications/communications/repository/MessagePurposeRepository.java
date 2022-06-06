package com.mykaarma.kcommunications.communications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.communications.model.jpa.MessagePurpose;

@Transactional
public interface MessagePurposeRepository extends JpaRepository<MessagePurpose, Long> {

	MessagePurpose findOneByUuid(String messagePurposeUuid);

}

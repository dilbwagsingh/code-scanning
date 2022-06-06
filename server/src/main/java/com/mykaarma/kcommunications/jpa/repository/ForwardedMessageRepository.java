package com.mykaarma.kcommunications.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.ForwardedMessage;

@Transactional(readOnly = true)
public interface ForwardedMessageRepository extends JpaRepository<ForwardedMessage, Long> {
}

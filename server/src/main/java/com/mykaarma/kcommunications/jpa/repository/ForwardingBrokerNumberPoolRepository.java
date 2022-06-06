package com.mykaarma.kcommunications.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.ForwardingBrokerNumberPool;

@Transactional(readOnly = true)
public interface ForwardingBrokerNumberPoolRepository extends JpaRepository<ForwardingBrokerNumberPool, Long> {

    ForwardingBrokerNumberPool findByBrokerNumber(String brokerNumber);
}

package com.mykaarma.kcommunications.communications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.communications.model.jpa.ExternalMessageMetaData;

@Transactional
public interface ExternalMessageMetaDataRepository extends JpaRepository<ExternalMessageMetaData, Long> {
	
}

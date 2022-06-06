package com.mykaarma.kcommunications.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.EmailTemplateType;

@Transactional
public interface EmailTemplateTypeRepository extends JpaRepository<EmailTemplateType, Long> {

	public EmailTemplateType findByTypeName(String typeName);
}

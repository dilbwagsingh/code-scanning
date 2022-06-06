package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.EmailTemplate;

@Transactional
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

	@Query(value = "select * from EmailTemplate where EmailTemplateTypeID = :emailTemplateTypeId and DealerID = :dealerId", nativeQuery = true)
	public List<EmailTemplate> findAllByEmailTemplateTypeIDAndDealerID(@Param("emailTemplateTypeId") Long emailTemplateTypeId, @Param("dealerId") Long dealerId);

	@Modifying(clearAutomatically = true)
	@Query(value = " INSERT INTO EmailTemplate (`DealerID`, `DealerUserID`, `EmailTemplateTypeID`, `EmailTemplate`, `Locale`, `UUID`) "
			+ " VALUES ( :dealerId , :dealerUserId , :emailTemplateTypeId , :emailTemplate , :locale , :uuid ) "
			+ " ON DUPLICATE KEY UPDATE EmailTemplate = :emailTemplate " 
			, nativeQuery = true)
	public void upsertFreemarkerTemplate(@Param("dealerId") Long dealerId, @Param("dealerUserId") Long dealerUserId, @Param("emailTemplateTypeId") Long emailTemplateTypeId,
			@Param("emailTemplate") String emailTemplate, @Param("locale") String locale, @Param("uuid") String uuid);

	@Query(value = "select * from EmailTemplate where EmailTemplateTypeID = :emailTemplateTypeId", nativeQuery = true)
	public List<EmailTemplate> findAllByEmailTemplateTypeID(@Param("emailTemplateTypeId") Long emailTemplateTypeId);

	@Query(value = "select EmailTemplate from EmailTemplate where EmailTemplateTypeID IN (select ID from EmailTemplateType where TypeName= :typeName )"
			+ "and DealerID = :dealerId and Locale = :locale Limit 1", nativeQuery = true)
	public String findEmailTemplateByEmailTemplateTypeNameAndDealerIDAndLocale(@Param("typeName") String typeName, 
			@Param("dealerId") Long dealerId, @Param("locale") String locale);
}

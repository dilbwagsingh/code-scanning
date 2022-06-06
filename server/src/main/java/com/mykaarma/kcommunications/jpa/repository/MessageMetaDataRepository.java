package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.DraftMessageMetaData;
import com.mykaarma.kcommunications.model.jpa.MessageMetaData;

@Transactional
public interface MessageMetaDataRepository extends JpaRepository<MessageMetaData, Long> {
	
	public MessageMetaData findByMessageID(Long messageID);
	
	public List<MessageMetaData> findByMessageIDIn(List<Long> messageIdList);
	
	@Modifying(clearAutomatically = true)
	@Query(value = "Insert Into MessageMetaData(`MessageID`,`MetaData`)" 
			 + 	" values(:messageID, :metaData) "
			 +  " ON DUPLICATE KEY UPDATE MetaData= :metaData",
			nativeQuery = true)
	public void upsertMessageMetaData(@Param("messageID")Long messageID, @Param("metaData")String metaData);

}

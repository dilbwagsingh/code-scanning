package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.DocFile;

@Transactional
public interface DocFileRepository extends JpaRepository<DocFile, Long> {

	public List<DocFile> findByMessageIdIn(List<Long> messageIdList);
	public List<DocFile> findByMessageId(Long messageId);

	
	@Modifying(clearAutomatically = true)
	@Query(value = "UPDATE DocFile SET MediaPreviewURL = :mediaPreviewURL WHERE ID = :id " ,
			nativeQuery = true)
	public void updateMediaPreviewURLForID(@Param("id")Long id, @Param("mediaPreviewURL")String mediaPreviewURL);
}

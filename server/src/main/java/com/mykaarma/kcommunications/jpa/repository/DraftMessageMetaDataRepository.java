package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.DocFile;
import com.mykaarma.kcommunications.model.jpa.DraftMessageMetaData;

@Transactional
public interface DraftMessageMetaDataRepository extends JpaRepository<DraftMessageMetaData, Long> {

	public DraftMessageMetaData findByMessageID(Long messageID);
	
	public List<DraftMessageMetaData> findByMessageIDIn(List<Long> messageIdList);
	
	@Query(value = "select m.ID, m.DealerID "
			+ "from Message m join DraftMessageMetaData dmmd on m.ID=dmmd.MessageID "
	 		+ "where dmmd.Status = :draftStatus and m.DealerID>= :fromDealerID and m.DealerID<= :toDealerID "
	 		+ "order by ID desc limit :batchSize offset :offSet ;", nativeQuery = true)
	public List<Object[]> fetchMessageForGivenDraftStatusAndDealers(@Param("draftStatus") String draftStatus,
			@Param("fromDealerID") Long fromDealerID ,@Param("toDealerID") Long toDealerID,
			@Param("batchSize") Long batchSize ,@Param("offSet") Long offSet);
}

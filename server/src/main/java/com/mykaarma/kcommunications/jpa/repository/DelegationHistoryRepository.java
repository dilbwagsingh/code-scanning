package com.mykaarma.kcommunications.jpa.repository;

import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.DelegationHistory;

@Transactional
public interface DelegationHistoryRepository extends JpaRepository<DelegationHistory, Long> {
	
	 @Query(value = "select threadOwner from DelegationHistory where threadID = :threadID limit 1", nativeQuery = true)
	 public Long getFirstThreadOwnerForThread(@Param("threadID") Long threadID);

	 @Query(value = "select delegatedFrom from DelegationHistory where threadID = :threadID and timeOfChange > :receivedDate order by id limit 1", nativeQuery = true)
	 public Long checkLatestDelgationAfterReceivedDate(@Param("threadID") Long threadID, @Param("receivedDate") Date receivedDate);

}

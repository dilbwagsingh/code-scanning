package com.mykaarma.kcommunications.jpa.repository;

import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mykaarma.kcommunications.model.jpa.DealerOrderMessage;
import com.mykaarma.kcommunications.model.jpa.MessageSignalingEngine;

import java.util.List;

@Transactional
public interface DealerOrderMessageRepository extends JpaRepository<DealerOrderMessage,Long>{

	@Query(value = "select *  from DealerOrderMessage where messageID= :messageID", nativeQuery = true)
	public List<DealerOrderMessage> findBymessageID(Long messageID);
	
}

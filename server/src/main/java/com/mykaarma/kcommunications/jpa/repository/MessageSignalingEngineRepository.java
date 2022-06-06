package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.MessageSignalingEngine;


@Transactional
public interface MessageSignalingEngineRepository extends JpaRepository<MessageSignalingEngine, Long>  {
	 
		@Query(value = "select *  from MessageSignalingEngine where dealerID= :dealerId and isValid=1 ", nativeQuery = true)
		 public List<MessageSignalingEngine> findAllByDealerID(@Param("dealerId") Long dealerID);

}

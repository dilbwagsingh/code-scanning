package com.mykaarma.kcommunications.jpa.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mykaarma.kcommunications.model.jpa.DealerOrder;

@Transactional
public interface DealerOrderRepository extends JpaRepository<DealerOrder,Long>  {
	
	@Query(value = "select UUID  from DealerOrder where id= :id", nativeQuery = true)
	public String findUUIDByID(Long id);
}

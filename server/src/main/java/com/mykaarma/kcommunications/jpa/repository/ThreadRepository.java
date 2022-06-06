package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.cache.CacheConfig;

@Transactional
public interface ThreadRepository extends JpaRepository<com.mykaarma.kcommunications.model.jpa.Thread, Long>  {
	
	public com.mykaarma.kcommunications.model.jpa.Thread findFirstByCustomerIDAndDealerDepartmentIDAndClosedOrderByIdDesc(Long customerId, Long dealerDepartmentID, Boolean isClosed);
	
	@Query(value="select dealerAssociateId, dealerDepartmentId from Thread where customerId = :customerId",nativeQuery=true)
	public List<Object[]> findDealerAssociateIdAndDealerDepartmentIdByCustomerId(@Param("customerId") Long customerId);

	public com.mykaarma.kcommunications.model.jpa.Thread findFirstByCustomerIDAndClosedOrderByLastMessageOnDesc(Long customerId, Boolean isClosed);
	
	@Cacheable(value=CacheConfig.THREAD_COUNT_FOR_DEALER_ASSOCIATE_ID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	@Query(value="Select Count(*) from Thread where DealerAssociateID= :dealerAssociateId and Closed=0 and Archived=0", nativeQuery=true)
	public Long getThreadCountForDealerAssociateId(@Param("dealerAssociateId") Long dealerAssociateId);

}
 
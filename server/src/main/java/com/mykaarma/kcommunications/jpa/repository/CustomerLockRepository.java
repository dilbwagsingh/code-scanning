package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.CustomerLock;

@Transactional
public interface CustomerLockRepository extends JpaRepository<CustomerLock, Long> {

	public CustomerLock findFirstByCustomerIDAndDealerDepartmentIDAndLockType(Long customerID, Long dealerDepartmentID, String lockType);
	
	public List<CustomerLock> findAllByDealerDepartmentIDAndLockTypeAndCustomerIDIn(Long dealerDepartmentID, String lockType,List<Long> customerIdList);
	
}

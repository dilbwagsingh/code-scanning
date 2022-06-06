package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.ForwardingBrokerNumberMapping;

@Component
@Repository
@Transactional
@SuppressWarnings("unchecked")
public interface ForwardingBrokerNumberMappingHelper extends JpaRepository<ForwardingBrokerNumberMapping, Long>{

	public ForwardingBrokerNumberMapping saveAndFlush(ForwardingBrokerNumberMapping forwardingBrokerNumberMapping);	
	
	@Query(value = "select * from ForwardingBrokerNumberMapping bn where bn.dealerID = :dealerId "
			+ "and bn.dealerAssociateID = :dAID and bn.customerID = :custID and bn.customerPhoneNumber = :custPNumb and "
			+ " bn.dealerAssociatePhoneNumber=:dealerAssociateNumber order by bn.createdTimeStamp desc ", nativeQuery = true)
	public List<ForwardingBrokerNumberMapping> getBrokerNumber(@Param("dealerId")Long dealerId, @Param("dAID")Long dAID, @Param("dealerAssociateNumber") String dealerAssociateNumber,
			@Param("custID") Long customerID, @Param("custPNumb") String customerPhoneNumber);

	@Query(value = "select brokerNumber from ForwardingBrokerNumberMapping where dealerAssociateID = :dAID and"
			+ " dealerAssociatePhoneNumber=:dealerAssociateNumber ", nativeQuery = true)
	public List<String> getNumbersUsedByServiceAdvisor(@Param("dAID")Long dAID, @Param("dealerAssociateNumber") String dealerAssociateNumber);
	
	@Query(value = "select * from ForwardingBrokerNumberMapping bn where bn.dealerAssociateID = :dAID and bn.lastMessageOn is not null  order by bn.lastMessageOn", nativeQuery = true)
	public List<ForwardingBrokerNumberMapping> fetchOldestBrokerNumberForDealerAssociate(@Param("dAID") Long dAID);

}

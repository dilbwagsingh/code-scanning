package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.cache.CacheConfig;
import com.mykaarma.kcommunications.model.jpa.VoiceCredentials;

@Transactional
public interface VoiceCredentialsRepository extends JpaRepository<VoiceCredentials, Long>{

	@Cacheable(value=CacheConfig.VOICE_CREDENTIALS_LIST_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	@Query(value = "select * from VoiceCredentials vc where deptID = :departmentID and (useForOutgoingCommunication = 1 or UseForStickiness = 1)",
			nativeQuery = true)
	public List<VoiceCredentials> findAllByDeptID(@Param("departmentID")Long departmentID);
	
	@Query(value = "select dealerSubAccount from VoiceCredentials vc where DealerSubAccount like :accountID limit 1",
			nativeQuery = true)
	public String findByAccountSid(@Param("accountID")String accountID);
	
	@Query(value = "select dd.UUID from VoiceCredentials vc " + 
			"join DealerDepartment dd on vc.DeptID = dd.ID " + 
			"where BrokerNumber = :brokerNumber order by vc.ID desc limit 1", nativeQuery = true)
	public String getDepartmentUUIDForBrokerNumber(@Param("brokerNumber") String brokerNumber);
	
	@Cacheable(value=CacheConfig.DEALER_ID_FOR_ACCOUNTSID_CACHE,keyGenerator = "customKeyGenerator",unless="#result == null")
	@Query(value = "select dealerID from VoiceCredentials vc where DealerSubAccount like %:accountID% limit 1 ", nativeQuery = true)
	public Long getDealerIDForAccountSid(@Param("accountID")String accountID);
	
	@Query(value = "select * from VoiceCredentials where brokerNumber like :brokerNumber "
			+ "and dealerSubaccount like %:accountSID% limit 1", nativeQuery = true)
	public List<VoiceCredentials> getVoiceCredentialsForAccountSIDAndBroker(@Param("accountSID")String accountSID, @Param("brokerNumber")String brokerNumber);
	
	@Query(value = "select deptID from VoiceCredentials vc where DealerSubAccount like %:accountSid% limit 1",
			nativeQuery = true)
	public Long getDepartmentIDForCallSid(@Param("accountSid")String callSid);
	
	@Query(value = "select DealerSubaccount from VoiceCredentials "
			+ "where UseForOutgoingCommunication = 1 and "
			+ "DealerID = :dealerId and DeptID = :deptID", nativeQuery = true)
	public List<String> getTwilioCredentialsForDealerDept(@Param("dealerId")Long dealerId, @Param("deptID") Long deptId);
	
	@Query(value = "select BrokerNumber from VoiceCredentials where DealerSubaccount like %:accountSid% limit 1", nativeQuery = true)
	public String getBrokerNumberForCaller(@Param("accountSid")String accountSid);

	
	
	
}

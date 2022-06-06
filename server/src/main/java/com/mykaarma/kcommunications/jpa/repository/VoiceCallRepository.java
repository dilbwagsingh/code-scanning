package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.VoiceCall;

@Repository
@Transactional
public interface VoiceCallRepository extends JpaRepository<VoiceCall, Long>{
	
	@Query(value = "Select * from VoiceCall vc "
			+ "where vc.callIdentifier = :callSID", nativeQuery =  true)
	public List<VoiceCall> getVoiceCall(@Param("callSID")String callSID);	
	
	@Modifying
	@Query(value = "update VoiceCall v  "
			+ "set ChildCallIdentifier = :childCallSID  "
			+ "where CallIdentifier = :parentCallSID", nativeQuery =  true)
	public void updateVoiceCallChildSid(@Param("childCallSID")String childCallSID, @Param("parentCallSID")String parentCallSid);
	
	
	@Query(value = "select CallIdentifier from VoiceCall"
			+ " where ChildCallIdentifier = :childCallSid", nativeQuery =  true)
	public String getParentSidFromChildCallSid(@Param("childCallSid")String childCallSid);
	
	@Modifying
	@Query(value = "update VoiceCall set CallStatus = :callStatusId where CallIdentifier= :callSID", nativeQuery =  true)
	public void setCallStatus(@Param("callSID")String callSid, @Param("callStatusId")int callStatusId);
	
	@Query(value = "select vc.callStatus from VoiceCall vc where callIdentifier = :communicationId ", nativeQuery =  true)
	public Long getVoiceCallStatusFromCommunicationUID(@Param("communicationId")String communicationUID);
}

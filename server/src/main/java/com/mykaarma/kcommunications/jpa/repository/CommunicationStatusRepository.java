package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import com.mykaarma.kcommunications.model.jpa.CommunicationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface CommunicationStatusRepository extends JpaRepository<CommunicationStatus, Long> {
    
    public CommunicationStatus findByDealerIDAndDealerDepartmentIDAndMessageProtocolAndCommunicationValue(Long dealerID, Long dealerDepartmentID, String messageProtocol, String communicationValue);

    @Query(value = "SELECT cs.* FROM CommunicationStatus cs WHERE"
    + " DealerID = :dealerID AND DealerDepartmentID = :dealerDepartmentID AND"
    + " CommType IN (:messageProtocolList) AND CommValue IN (:communicationValues)", nativeQuery = true)
    public List<CommunicationStatus> findAllByDealerIDAndDealerDepartmentIDAndMessageProtocolsAndCommunicationValues(
            @Param("dealerID") Long dealerID, @Param("dealerDepartmentID") Long departmentID, @Param("messageProtocolList") List<String> messageProtocolList, @Param("communicationValues") List<String> communicationValues);

    @Transactional(readOnly = false)
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO CommunicationStatus (DealerID, DealerDepartmentID, CommType, CommValue, OptOutStatus, CanSendOptinRequest)"
    + " SELECT dd.DealerID, dd.ID, :messageProtocol, :communicationValue, :optOutState, :canSendOptinRequest FROM DealerDepartment dd WHERE dd.ID IN (:departmentIDList)"
    + " ON DUPLICATE KEY UPDATE OptOutStatus = :optOutState, CanSendOptinRequest = :canSendOptinRequest", nativeQuery = true)
    public void upsertCommunicationStatusForDepartments(@Param("departmentIDList") List<Long> departmentIDList, @Param("messageProtocol") String messageProtocol, @Param("communicationValue") String communicationValue, @Param("optOutState") String optOutState, @Param("canSendOptinRequest") Boolean canSendOptinRequest);

    @Query(value = "SELECT cs.* FROM CommunicationStatus cs WHERE"
    + " DealerID = :dealerID AND DealerDepartmentID IN (:dealerDepartmentIDList) AND"
    + " CommType = :messageProtocol AND CommValue = :communicationValue", nativeQuery = true)
    public List<CommunicationStatus> findAllByDealerIDAndDealerDepartmentIDListAndMessageProtocolAndCommunicationValue(@Param("dealerID") Long dealerID, @Param("dealerDepartmentIDList") List<Long> dealerDepartmentIDList, @Param("messageProtocol") String messageProtocol, @Param("communicationValue") String communicationValue);
}

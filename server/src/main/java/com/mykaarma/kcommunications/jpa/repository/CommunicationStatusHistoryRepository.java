package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.CommunicationStatusHistory;

@Transactional(readOnly = true)
public interface CommunicationStatusHistoryRepository extends JpaRepository<CommunicationStatusHistory, Long> {

    @Transactional(readOnly = false)
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO CommunicationStatusHistory (DealerID, DealerDepartmentID, MessageID, CommType, CommValue, OptOutStatus)"
    + " SELECT dd.DealerID, dd.ID, :messageID, :messageProtocol, :communicationValue, :optOutState FROM DealerDepartment dd WHERE dd.ID IN (:departmentIDList)"
    + " ON DUPLICATE KEY UPDATE OptOutStatus = :optOutState", nativeQuery = true)
    public void upsertCommunicationStatusForDepartments(@Param("departmentIDList") List<Long> departmentIDList, @Param("messageID") Long messageID, @Param("messageProtocol") String messageProtocol, @Param("communicationValue") String communicationValue, @Param("optOutState") String optOutState);
    
}

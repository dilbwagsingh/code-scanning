package com.mykaarma.kcommunications.communications.repository;

import java.util.List;

import com.mykaarma.kcommunications.communications.model.jpa.CustomerPreferredCommunicationMode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


@Transactional(readOnly = true)
public interface CustomerPreferredCommunicationModeRepository extends JpaRepository<CustomerPreferredCommunicationMode, Long> {

    public CustomerPreferredCommunicationMode findByCustomerUUID(String customerUUID);

    @Transactional(readOnly = false)
    @Modifying(clearAutomatically = true)
    @Query(value = "Insert Into CustomerPreferredCommunicationMode(`CustomerUUID`,`Protocol`,`MetaData`)" 
			+ " values(:customerUUID, :protocol, :metaData) "
			+ " ON DUPLICATE KEY UPDATE Protocol= :protocol, MetaData= :metaData",
			nativeQuery = true)
    public void upsertPreferredCommunicationMode(@Param("customerUUID") String customerUUID, 
        @Param("protocol") String protocol, @Param("metaData") String metaData);

    @Query(value = "Select CustomerUUID, Protocol From CustomerPreferredCommunicationMode"
            + " Where CustomerUUID In (:customerUUIDList)", 
            nativeQuery = true)
    public List<Object[]> getCustomersPreferredCommunicationProtocol(@Param("customerUUIDList") List<String> customerUUIDList);
}
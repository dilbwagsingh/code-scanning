package com.mykaarma.kcommunications.mongo.repository;

import com.mykaarma.kcommunications.model.mongo.DoubleOptInDepartmentGroupConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface DoubleOptInDepartmentGroupConfigurationRepository extends MongoRepository<DoubleOptInDepartmentGroupConfiguration, String> {

    public DoubleOptInDepartmentGroupConfiguration findFirstByFeatureUUIDAndDepartmentGroupUUIDAndKey(String featureUUID, String departmentGroupUUID, String key);
}

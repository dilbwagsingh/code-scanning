package com.mykaarma.kcommunications.model.mongo;

import javax.persistence.Id;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "DOUBLE_OPTIN_DEPARTMENT_GROUP_CONFIGURATION_COLLECTION")
public class DoubleOptInDepartmentGroupConfiguration {
    
    @Id
    @Field("_id")
    private String uuid;

    private String name;

    private String key;

    private String value;

    private String description;

    private String departmentGroupUUID;

    private String featureUUID;

}

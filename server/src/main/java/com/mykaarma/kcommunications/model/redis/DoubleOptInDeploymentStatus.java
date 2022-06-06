package com.mykaarma.kcommunications.model.redis;

import java.util.List;

import com.mykaarma.kcommunications_model.enums.OptOutState;

import lombok.Data;

@Data
public class DoubleOptInDeploymentStatus {
    
    private List<DepartmentDeploymentStatus> departmentDeploymentStatusList;

    @Data
    public static class DepartmentDeploymentStatus {
        private Long departmentID;
        private String departmentName;
        private OptOutState newOptOutState;
        private Boolean optOutStateProcessingDone = false;
    }
}

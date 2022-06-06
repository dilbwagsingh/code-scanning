package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class DealerUrlUpdateFailure implements Serializable{

	Long dealerId;
	List<DepartmentUrlUpdateFailure> departmentsFailed;
	
}

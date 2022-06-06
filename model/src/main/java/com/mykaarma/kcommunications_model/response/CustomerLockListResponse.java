package com.mykaarma.kcommunications_model.response;

import java.io.Serializable;
import java.util.List;

import com.mykaarma.kcommunications_model.dto.CustomerLockDTO;

import lombok.Data;

@Data
public class CustomerLockListResponse extends Response implements Serializable  {

	List<CustomerLockDTO> customerLockInfoList;
	
	String requestUuid;
}

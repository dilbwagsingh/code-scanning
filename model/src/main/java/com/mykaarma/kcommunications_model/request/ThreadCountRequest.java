package com.mykaarma.kcommunications_model.request;

import java.util.Set;

import lombok.Data;

@Data
public class ThreadCountRequest {

	Set<String> departmentUuids;
}

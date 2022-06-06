package com.mykaarma.kcommunications.model.utils;

import java.io.Serializable;

import com.mykaarma.global.AutomationFilterType;
import com.mykaarma.global.FeatureKeys;

import lombok.Data;

@Data
public class LaborOpCodeFilteringParameter implements Serializable{

	private String laborOpTypes;
	private String laborTypes;
	private String departmentType;
	private FeatureKeys feature;
	private String filterfor;
	private String departmentExclusionList;
	private String laborTypeExclusionList;
	private String globalFilter;
	private String featureFilters;
	private String filter;
	private AutomationFilterType filterType;
}

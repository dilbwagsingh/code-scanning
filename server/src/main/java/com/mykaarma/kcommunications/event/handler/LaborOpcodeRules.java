package com.mykaarma.kcommunications.event.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykaarma.global.AutomationFilterType;
import com.mykaarma.global.DealerSetupOption;
import com.mykaarma.global.FeatureKeys;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.model.utils.LaborOpCodeFilteringParameter;
import com.mykaarma.kcommunications.utils.AppConfigHelper;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kmanage.model.dto.json.response.GetDepartmentResponseDTO;

@Service
public class LaborOpcodeRules {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(LaborOpcodeRules.class);
	
	@Autowired
	AppConfigHelper appConfigHelper;
	
	@Autowired
	GeneralRepository generalRepository;
	
	@Autowired
	KManageApiHelper kManageApiHelper;
	
	public LaborOpCodeFilteringParameter getLaborOpCodeFilteringParameters(Long dealerID, FeatureKeys feature) throws Exception {
		LaborOpCodeFilteringParameter laborOpCodeFilteringParameter =  new LaborOpCodeFilteringParameter();
		laborOpCodeFilteringParameter.setFeature(feature);
		String laborOpCodeFilteringParameterKey = null;
		switch(feature) {
		case AUTO_FOLLOW_UP:
			laborOpCodeFilteringParameter.setFilter("D");
			laborOpCodeFilteringParameter.setFilterType(AutomationFilterType.LABOR_OP_FILTER_AUTO_FOLLOWUP);
			laborOpCodeFilteringParameterKey = AutomationFilterType.LABOR_OP_FILTER_AUTO_FOLLOWUP.getKey();
			break;
		case AUTO_PR:
			laborOpCodeFilteringParameter.setFilter("I");
			laborOpCodeFilteringParameter.setFilterType(AutomationFilterType.LABOR_OP_FILTER_AUTO_PR);
			laborOpCodeFilteringParameterKey = AutomationFilterType.LABOR_OP_FILTER_AUTO_PR.getKey();
			break;
		case AUTO_WELCOME_TEXT:
			laborOpCodeFilteringParameter.setFilter("W");
			laborOpCodeFilteringParameter.setFilterType(AutomationFilterType.LABOR_OP_FILTER_WELCOME_TEXT);
			laborOpCodeFilteringParameterKey = AutomationFilterType.LABOR_OP_FILTER_WELCOME_TEXT.getKey();
			break;
		default:
			break;
		}
		
        String dealerUUID = generalRepository.getDealerUUIDFromDealerId(dealerID);
        HashMap<String, String> dsoMap = kManageApiHelper.sortInputAndGetDealerSetupOptionValuesForADealer(dealerUUID, getDSOListForLaborOpCode());
		if(dsoMap!=null) {
			laborOpCodeFilteringParameter.setFilterfor(dsoMap.get(DealerSetupOption.APPLY_LABOR_OP_FILTER_LIST.getOptionKey()));
			laborOpCodeFilteringParameter.setDepartmentExclusionList(dsoMap.get(DealerSetupOption.DEPARTMENTS_EXCLUSION_FILTER.getOptionKey()));
			laborOpCodeFilteringParameter.setLaborTypeExclusionList(dsoMap.get(DealerSetupOption.LABOR_TYPE_EXCLUSION_FILTER.getOptionKey()));
		}
		if(laborOpCodeFilteringParameterKey!=null) {
			String featureFilters = generalRepository.getAutomationFilterValueForAutomationFilter(dealerID, laborOpCodeFilteringParameterKey);
			laborOpCodeFilteringParameter.setFeatureFilters(featureFilters);
		}
		String globalFilters = generalRepository.getAutomationFilterValueForAutomationFilter(dealerID, AutomationFilterType.LABOR_OP_FILTER_GLOBAL.getKey());
		laborOpCodeFilteringParameter.setGlobalFilter(globalFilters);
		return laborOpCodeFilteringParameter;
	}
	
	public  Boolean applyDepartmentLabourOpFilter(String dealerOrderUUID, LaborOpCodeFilteringParameter parameters) throws Exception {
		
		LOGGER.info("applyDepartmentLabourOpFilter for parameters={} dealer_order_uuid={} ", 
				new ObjectMapper().writeValueAsString(parameters),dealerOrderUUID);
		boolean toFilter = applyAutomationFilter(parameters.getLaborOpTypes(), parameters.getFilterType(), "dealerorder_uuid=" + dealerOrderUUID, parameters.getGlobalFilter(),
				parameters.getFeatureFilters());

		if(!toFilter) {
			LOGGER.info("applyDepartmentLabourOpFilter filter rules passed for parameters={} dealer_order_uuid={}  ", 
					new ObjectMapper().writeValueAsString(parameters),dealerOrderUUID);
			if(parameters.getFilterfor() != null && !parameters.getFilterfor().trim().isEmpty()) {	
				String filtervalues[] = parameters.getFilterfor().trim().split(",");

				Boolean toApply = false;

				for (String value : filtervalues) {

					if(value != null && value.trim().equalsIgnoreCase(parameters.getFilter())) {
						toApply = true;
					}
				}

				if(toApply) {
					LOGGER.info("applyDepartmentLabourOpFilter apply department and laborOp filters for parameters={} dealer_order_uuid={}  ", 
							new ObjectMapper().writeValueAsString(parameters),dealerOrderUUID);
					Boolean departmentFilter = false;//  filter to decide whether payment request is to be sent for this department type
					Boolean laborTypeFilter = false;	// labor types based filter.

					if(parameters.getDepartmentType() != null && !parameters.getDepartmentType().trim().isEmpty()) {
						departmentFilter = checkIfDataListContainsAnyOneOfExclusionList(parameters.getDepartmentType(),parameters.getDepartmentExclusionList());
						if(departmentFilter) {
							LOGGER.info("DepartmentType filter returning true because for dealerorder_uuid="+dealerOrderUUID+" Department DSO  is "+parameters.getDepartmentExclusionList());
						}
					}
					if(parameters.getLaborTypes() != null && !parameters.getLaborTypes().trim().isEmpty()) {
						laborTypeFilter = checkIfDataListContainsAnyOneOfExclusionList(parameters.getLaborTypes(), parameters.getLaborTypeExclusionList());
						if(laborTypeFilter) {
							LOGGER.info("LaborType filter returning true because for dealerorder_uuid="+dealerOrderUUID+" LaborOp DSO is "+parameters.getLaborTypeExclusionList());
						}
					}
					toFilter = departmentFilter || laborTypeFilter;
					LOGGER.info("applyDepartmentLabourOpFilter failed for parameters={} dealer_order_uuid={} departmentFilter={} laborTypeFilter={} ", 
							new ObjectMapper().writeValueAsString(parameters),dealerOrderUUID, departmentFilter, laborTypeFilter);
				}
			}
		}
		boolean toSend = !toFilter;

		return toSend;
	}

	public static boolean checkIfDataListContainsAnyOneOfExclusionList(String dataList , String exclusionList) {
		if(dataList!=null && !dataList.isEmpty()
				&& exclusionList!=null && !exclusionList.isEmpty())
		{
			String[] dataArr = dataList.trim().split(",");
			String[] exclusionArr = exclusionList.trim().split(",");
			
		    for(String exclusion : exclusionArr)
			{
		    	boolean isNegative = false;
				if(exclusion.startsWith("-"))
				{
					exclusion = exclusion.substring(1);
					isNegative = true;
				}
				Pattern pattern = Pattern.compile(exclusion);
				for(String data : dataArr)
				{
					Matcher matcher = pattern.matcher(data);
					if(matcher.matches() && isNegative)
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private boolean applyAutomationFilter(String valueList, AutomationFilterType filterType, String logContext,
			String globalFilter, String featureFilter) {
		boolean filterMatches = false;
		if(valueList != null && !valueList.trim().isEmpty()) {
			if(globalFilter != null && !globalFilter.isEmpty()) {
				filterMatches = checkIfDataListContainsAnyOneOfExclusionList(valueList, globalFilter);
				LOGGER.info("applyAutomationFilter failed reason=globalFilter for log_context={} filter_type={} value_list={} global_filter={}",
						logContext, filterType, valueList, globalFilter);
			}
			if(!filterMatches) {
				if(featureFilter != null && !featureFilter.isEmpty()) {
					filterMatches = checkIfDataListContainsAnyOneOfExclusionList(valueList, featureFilter);
					LOGGER.info("applyAutomationFilter failed reason=featureFilter for log_context={} filter_type={} value_list={} feature_filter={}",
							logContext, filterType, valueList, featureFilter);
				}
			}
		}
		return filterMatches;
	}
	
	private Set<String> getDSOListForLaborOpCode() {
		Set<String> dsoList = new HashSet<String>();
		dsoList.add(DealerSetupOption.APPLY_LABOR_OP_FILTER_LIST.getOptionKey());
		dsoList.add(DealerSetupOption.DEPARTMENTS_EXCLUSION_FILTER.getOptionKey());
		dsoList.add(DealerSetupOption.LABOR_TYPE_EXCLUSION_FILTER.getOptionKey());
		return dsoList;
	}
}

package com.mykaarma.kcommunications.utils;

public enum ElasticSearchUtils {

	TEMPLATE_TITLE("title"),
	TEMPLATE_BODY("body"),
	DEPARTMENT_UUID("departmentUuid"),
	DEALER_UUID("dealerUuid"),
	IS_MANUAL("isManual"),
	LOCALE("locale"),
	ASC_SORT("asc"),
	DESC_SORT("desc"),
	SORT_ORDER("sortOrder"),
	ELASTIC_SEARCH_SCORE("_score"),
	ORDER("order"),
	FROM("from"),
	SIZE("size"),
	MUST("must"),
	QUERY_STRING("query_string"),
	QUERY("query"),
	FIELDS("fields"),
	FILTER("filter"),
	SORT("sort"),
	BOOL("bool"),
	DEFAULT_OPERATOR("default_operator"),
	WILDCHAR_REGEX("[^a-zA-Z0-9@$._\\- ]");
	
	private String value;
	
	private ElasticSearchUtils(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}

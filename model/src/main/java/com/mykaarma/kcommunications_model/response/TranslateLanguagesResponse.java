package com.mykaarma.kcommunications_model.response;

import java.util.Map;

import lombok.Data;

@Data
public class TranslateLanguagesResponse extends Response {
	
	Map<String, String> languages;
}
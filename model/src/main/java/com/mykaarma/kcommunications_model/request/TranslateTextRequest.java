package com.mykaarma.kcommunications_model.request;

import lombok.Data;

@Data
public class TranslateTextRequest {

	String text;
	String langCode;
}
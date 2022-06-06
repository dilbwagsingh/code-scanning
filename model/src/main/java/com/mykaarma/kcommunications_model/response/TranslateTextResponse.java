package com.mykaarma.kcommunications_model.response;

import lombok.Data;

@Data
public class TranslateTextResponse extends Response {

	String translatedText;
	String detectedSourceLanguage;
}
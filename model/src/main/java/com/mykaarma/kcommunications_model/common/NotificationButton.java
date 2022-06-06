package com.mykaarma.kcommunications_model.common;

import java.io.Serializable;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mykaarma.kcommunications_model.enums.NotificationButtonTheme;

@SuppressWarnings("serial")
public class NotificationButton implements Serializable {
	
	@JsonProperty("buttonTextTranslationWidgetKey")
	private String buttonTextTranslationWidgetKey = "messagedisplay_widget";
	
	@JsonProperty("buttonTextTranslationKey")
	private String buttonTextTranslationKey = "btnLearnMore";
	
	@JsonProperty("buttonDefaultText")
	private String buttonDefaultText = "LEARN MORE";
	
	@JsonProperty("buttonTheme")
	private NotificationButtonTheme buttonTheme = NotificationButtonTheme.PRIMARY;
	
	@JsonProperty("buttonActionEventData")
	private HashMap<String, String> buttonActionEventData;
	
	@JsonProperty("isPrimaryButton")
	private Boolean isPrimaryButton;
	
	public NotificationButton() {
		
	}

	public String getButtonTextTranslationWidgetKey() {
		return buttonTextTranslationWidgetKey;
	}

	public void setButtonTextTranslationWidgetKey(String buttonTextTranslationWidgetKey) {
		this.buttonTextTranslationWidgetKey = buttonTextTranslationWidgetKey;
	}

	public String getButtonTextTranslationKey() {
		return buttonTextTranslationKey;
	}

	public void setButtonTextTranslationKey(String buttonTextTranslationKey) {
		this.buttonTextTranslationKey = buttonTextTranslationKey;
	}

	public String getButtonDefaultText() {
		return buttonDefaultText;
	}

	public void setButtonDefaultText(String buttonDefaultText) {
		this.buttonDefaultText = buttonDefaultText;
	}

	public NotificationButtonTheme getButtonTheme() {
		return buttonTheme;
	}

	public void setButtonTheme(NotificationButtonTheme buttonTheme) {
		this.buttonTheme = buttonTheme;
	}

	public HashMap<String, String> getButtonActionEventData() {
		return buttonActionEventData;
	}

	public void setButtonActionEventData(HashMap<String, String> buttonActionEventData) {
		this.buttonActionEventData = buttonActionEventData;
	}

	public Boolean getIsPrimaryButton() {
		return isPrimaryButton;
	}

	public void setIsPrimaryButton(Boolean isPrimaryButton) {
		this.isPrimaryButton = isPrimaryButton;
	}
}

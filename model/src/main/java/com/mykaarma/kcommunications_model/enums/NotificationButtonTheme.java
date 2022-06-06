package com.mykaarma.kcommunications_model.enums;

public enum NotificationButtonTheme {
	
	PRIMARY, SECONDARY, TERTIARY, QUATERNARY;
	
	public static NotificationButtonTheme getNotificationButtonTheme(String theme) {
		NotificationButtonTheme result = null;
		for (NotificationButtonTheme buttonTheme : NotificationButtonTheme.values()) {
			if (buttonTheme.name().equals(theme)) {
				result = buttonTheme;
			}
		}
		return result;
	}

}

package com.mykaarma.kcommunications_model.enums;

public class MessagingUtils {

	public static enum PARTNER {
		MK("mykaarma");
		private String value;

		private PARTNER(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}
}

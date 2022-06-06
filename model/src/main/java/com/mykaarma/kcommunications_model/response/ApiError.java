package com.mykaarma.kcommunications_model.response;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement
public class ApiError implements Serializable{
		
		public ApiError() {
			
		}
		
		public ApiError(String errorCode, String errorDescription) {
			super();
			this.errorCode = errorCode;
			this.errorDescription = errorDescription;
		}
		
		private String errorCode;
		
		private String errorDescription;
		
		private String errorUID;

		@JsonProperty("errorCode")
		public String getErrorCode() {
			return errorCode;
		}
		@JsonProperty("errorDescription")
		public String getErrorDescription() {
			return errorDescription;
		}

		public void setErrorCode(String errorCode) {
			this.errorCode = errorCode;
		}

		public void setErrorDescription(String errorDescription) {
			this.errorDescription = errorDescription;
		}

		@JsonProperty("errorUID")
		public String getErrorUID() {
			return errorUID;
		}

		public void setErrorUID(String errorUID) {
			this.errorUID = errorUID;
		}
}



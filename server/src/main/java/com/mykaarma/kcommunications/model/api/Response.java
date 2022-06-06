package com.mykaarma.kcommunications.model.api;

import java.io.Serializable;
import java.util.List;

public class Response implements Serializable {


    private static final long serialVersionUID = 1L;
    private List<Error> errors;
    private List<Warning> warnings;
    
    
    
	public Response() {
		super();
		// TODO Auto-generated constructor stub
	}
	public List<Error> getErrors() {
		return errors;
	}
	public void setErrors(List<Error> errors) {
		this.errors = errors;
	}
	public List<Warning> getWarnings() {
		return warnings;
	}
	public void setWarnings(List<Warning> warnings) {
		this.warnings = warnings;
	}
    
    
}

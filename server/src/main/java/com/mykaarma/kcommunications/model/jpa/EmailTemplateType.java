package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name="EmailTemplateType")
public class EmailTemplateType implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	@Column(name = "ID")
	private Long id;

	@Column(name = "TypeName")
    private String typeName;

	@Column(name = "TypeDesc")
    private String typeDesc;

	@Column(name = "TypeTemplate")
    private String typeTemplate;

	@Column(name = "DefaultTemplate")
    private String defaultTemplate;

	@Column(name = "InSelfAdmin")
    private Boolean inSelfAdmin;

	@Column(name = "MandatoryTCPAFooter")
    private Boolean mandatoryTCPAFooter;

	@Column(name = "UUID")
    private String uuid;

}

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
@Table(name="EmailTemplate")
public class EmailTemplate implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	@Column(name = "ID")
	private Long id;

	@Column(name = "DealerID")
    private Long dealerId;

	@Column(name = "DealerUserID")
    private Long dealerUserId;

	@Column(name = "EmailTemplateTypeID")
    private Long emailTemplateTypeId;

	@Column(name = "EmailTemplate")
    private String emailTemplate;

	@Column(name = "Locale")
    private String locale;

	@Column(name = "UUID")
    private String uuid;
}

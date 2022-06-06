package com.mykaarma.kcommunications.communications.model.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "UIElementTranslation")
public class UIElementTranslation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ElementKey")
    private String elementKey;

    @Column(name = "ElementValue")
    private String elementValue;

    @Column(name = "Locale")
    private String locale;
}

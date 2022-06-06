package com.mykaarma.kcommunications.model.jpa;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Entity
@Data
@Table(name = "DocFile")
public class DocFile implements Serializable {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@Version
	private long version;
	
	private String docFileName;
	
	String fileExtension;
	
	String mimeType;
	
	String originalFileName;
	
	String docSize;
	
	String thumbnailFileName;
	
	String mediaPreviewURL;
	
	private Long messageId;
	
	
}

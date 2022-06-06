package com.mykaarma.kcommunications_model.enums;

import java.io.Serializable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileType implements Serializable {
	
	IMAGE("image"),
	PDF("pdf"),
	XSLT("xslt"),
	AUDIO("audio"),
	VIDEO("video"),
	HTML("html"),
	XML("xml"),
	OTHER("file");
	
	private final String subFolderPrefix;
	
}

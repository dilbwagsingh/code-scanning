package com.mykaarma.kcommunications.utils;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AttachmentUtil {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(AttachmentUtil.class);
	
	private static final MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
	
	private static String VALID_VIDEO_EXTENSION_LIST_URL = "https://static.mykaarma.com/ext/allowed_video_extension.txt";
	private static String VALID_IMAGE_EXTENSION_LIST_URL = "https://static.mykaarma.com/ext/allowed_image_extension.txt";
	
	public static final String SCALED = "/scaled";
	private static final String THUMBNAIL = "_thumbnail";
	private static final String DEFAULT_EXTENSION = "png";
	
	public List<String> fetchValidVideoExtensionsList() throws Exception{
		URL url = new URL(VALID_VIDEO_EXTENSION_LIST_URL);
		String validExtensions=null;
		validExtensions = IOUtils.toString(url.openStream());
		
		List<String> validExtensionsList = null;
		LOGGER.info(String.format("Before parsing valid video extensions list=%s", validExtensionsList));
		
		if(validExtensions != null && !validExtensions.isEmpty()) {
			String[] validExtensionsArray = validExtensions.split(",");
			
			if(validExtensionsArray != null && validExtensionsArray.length > 0){
				validExtensionsList = Arrays.asList(validExtensionsArray);
				
				for(int index=0; index < validExtensionsList.size(); index++){	
					validExtensionsList.set(index, validExtensionsList.get(index).trim().toLowerCase());
				}
			}
		}
		LOGGER.info(String.format("After parsing valid video extensions list=%s", validExtensions));
			 
		return validExtensionsList;   
	}
	
	public List<String> fetchValidImageExtensionsList() throws Exception{
		URL url = new URL(VALID_IMAGE_EXTENSION_LIST_URL);
		String validExtensions=null;
		validExtensions = IOUtils.toString(url.openStream());
		
		List<String> validExtensionsList = null;
		LOGGER.info(String.format("Before parsing valid image extensions list=%s", validExtensionsList));
		
		if(validExtensions != null && !validExtensions.isEmpty()) {
			String[] validExtensionsArray = validExtensions.split(",");
			
			if(validExtensionsArray != null && validExtensionsArray.length > 0){
				validExtensionsList = Arrays.asList(validExtensionsArray);
				
				for(int index=0; index < validExtensionsList.size(); index++){	
					validExtensionsList.set(index, validExtensionsList.get(index).trim().toLowerCase());
				}
			}
		}
		LOGGER.info(String.format("After parsing valid image extensions list=%s", validExtensions));
			 
		return validExtensionsList;   
	}
	
	public String getThumbNailFileName(String fileUrl, String extension, List<String> extensionList) {
	      String substr = fileUrl.substring(0, fileUrl.lastIndexOf("/")) + SCALED;
	      String urlExtension = fileUrl.substring(fileUrl.lastIndexOf("/"), fileUrl.length());
	      String thumbnailUrl = substr + urlExtension;
	      
	      if(!extensionList.contains(extension)) {
	    	  thumbnailUrl = thumbnailUrl.substring(0,thumbnailUrl.lastIndexOf(thumbnailUrl.substring(thumbnailUrl.lastIndexOf('.'))))+ THUMBNAIL+ "." + DEFAULT_EXTENSION;
	      } else {
	    	  thumbnailUrl = thumbnailUrl.substring(0,thumbnailUrl.lastIndexOf(thumbnailUrl.substring(thumbnailUrl.lastIndexOf('.'))))+THUMBNAIL + "." + extension;
	      }
	      
	      return thumbnailUrl;
	}
	
	public boolean isVideo(String contentType) {
		return Arrays.asList(
				"video/mpeg",
				"video/mp4",
				"video/quicktime",
				"video/webm",
				"video/3gpp",
				"video/3gpp2",
				"video/3gpp-tt",
				"video/H261",
				"video/H263",
				"video/H263-1998",
				"video/H263-2000",
				"video/H264").contains(contentType);
	}

	public boolean isVideoFromExtension(String extension) {
		return Arrays.asList(
				"m1v", "m2p",
				"m2t", "m2ts",
				"m2v", "mod",
				"mp2", "mp2v",
				"mp4", "mp4v",
				"mpa", "mpe",
				"mpeg", "mpg",
				"mps", "mpv2",
				"mpv4", "ts",
				"vob", "f4v",
				"mp4", "mp4v",
				"mpg4", "hdmov",
				"mov", "mqv",
				"qt", "webm",
				"3g2", "3g2",
				"3gp", "3gp2",
				"3gp2", "3gpp",
				"sdv", "h261",
				"h263", "h264").contains(extension);
	}

	public boolean isAudioFromExtension(String extension) {
		return Arrays.asList(
				"mp3","wav").contains(extension);
	}
	
	public String getExtensionFromMIME(String mime){
		String extension = "txt";
		MimeType mimeType;
		
		try {
			mimeType = allTypes.forName(mime);
			extension = mimeType.getExtension().replace(".", "");
		} catch (MimeTypeException e) {
			LOGGER.warn("Unknown MIME {}",mime, e);
		}
		
		return extension;
	}
	
	public String getHtmlToRenderInIframe(String url) {
		String htmlString = "<html>"+
				"	<head><meta name='viewport' content='width=device-width'></head>"+
				"	<body style='margin: 0px;'>"+
				"		<iframe id='player' title='player' style='width:100%; height:100%' src='_url' scrolling='no' frameborder='0' allowfullscreen webkitallowfullscreen></iframe>"+
				"	</body>"+
				"</html>";
		
		return htmlString.replace("_url", url);
	}
	
}

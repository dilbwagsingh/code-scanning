package com.mykaarma.kcommunications.utils;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XmlProcessorUtil {

	 public static XPath xpath = XPathFactory.newInstance().newXPath();
	 
	 public static Node getNodeForXpath(String path,Node node) throws XPathExpressionException
		{
			XPathExpression expr = xpath.compile(path);
			Node returnNode =(Node) expr.evaluate(node,XPathConstants.NODE);
			
			return returnNode;
		}
	 
	 public static String transformXmlUsingXslt(Document xml, String xsltPath) throws Exception{
			Source xslt = null;
			if(xsltPath.contains("http"))
			{
				InputStream in = null;
				try {
					in = new URL(xsltPath).openStream();
					xslt = new StreamSource(in);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else
				xslt = new StreamSource(new File(xsltPath));
			
			return transformXmlUsingXslt(xslt, xml);
		}

	public static String transformXmlUsingXslt(Source xsltSource, Document xml) throws Exception {
		
		Transformer trans = TransformerFactory.newInstance().newTransformer(xsltSource);
		StringWriter sw = new StringWriter();
		 
		Source xmlSource = new DOMSource(xml);
		Result xmlResult = (Result)new StreamResult(sw);

		trans.transform(xmlSource, xmlResult);
		
  	    return sw.toString();
	}
}

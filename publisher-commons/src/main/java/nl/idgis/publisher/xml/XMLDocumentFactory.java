package nl.idgis.publisher.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.idgis.publisher.xml.exceptions.NotParseable;

import org.xml.sax.SAXException;

public class XMLDocumentFactory {
	
	private final DocumentBuilder documentBuilder;	
	
	public XMLDocumentFactory() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		documentBuilder = dbf.newDocumentBuilder();
	}

	public XMLDocument parseDocument(byte[] content) throws Exception {
		try {
			return new XMLDocument(documentBuilder.parse(new ByteArrayInputStream(content)));
		} catch (SAXException | IOException e) {
			throw new NotParseable(e);
		}
	}
}

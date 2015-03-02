package nl.idgis.publisher.service;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class TestStyle {

	public static Document getGreenSld() throws Exception {
		
		InputStream green = TestStyle.class.getResourceAsStream("green.sld");
		assertNotNull(green);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document sld = db.parse(green);
			
		return sld;
	}
}

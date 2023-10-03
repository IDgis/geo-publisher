package nl.idgis.publisher.service.style;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class TestStyle {
	
	public static Document getRasterSld() throws Exception {
		return getSld("raster.sld");
	}
	
	public static Document getRegiosSld() throws Exception {
		return getSld("regios.sld");
	}
	
	public static Document getGreenSld() throws Exception {
		return getSld("green.sld");
	}

	private static Document getSld(String name) throws Exception {
		
		InputStream content = TestStyle.class.getResourceAsStream(name);
		assertNotNull(content);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(content);
	}
}

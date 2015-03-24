package nl.idgis.publisher.service;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.w3c.dom.Document;

import nl.idgis.publisher.utils.XMLUtils;

public class StyleUtilsTest {

	@Test
	public void testToLowerCasePropertyName() throws Exception {
		Document regios = TestStyle.getRegiosSld();
		
		Document regiosLowerCase = StyleUtils.toLowerCasePropertyName(regios);
		
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("ogc", "http://www.opengis.net/ogc");
		
		XMLUtils.xpath(regios, Optional.of(namespaces)).strings("//ogc:PropertyName").stream()
			.forEach(propertyName -> assertEquals("NAAM", propertyName));
	
		XMLUtils.xpath(regiosLowerCase, Optional.of(namespaces)).strings("//ogc:PropertyName").stream()
			.forEach(propertyName -> assertEquals("naam", propertyName));
	}
}

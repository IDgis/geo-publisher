package nl.idgis.publisher.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


import org.w3c.dom.Document;


import nl.idgis.publisher.utils.XMLUtils;

public class StyleUtils {	
	
	public static Document toLowerCasePropertyName(Document sld) { 
		Document retval = (Document)sld.cloneNode(true);
		
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("ogc", "http://www.opengis.net/ogc");
		
		XMLUtils.xpath(retval, Optional.of(namespaces)).nodes("//ogc:PropertyName")
			.forEach(node -> {
				Optional<String> currentTextContent = node.string();
				if(currentTextContent.isPresent()) {
					node.setTextContent(currentTextContent.get().toLowerCase());
				}
			});
		
		return retval;
	}
}

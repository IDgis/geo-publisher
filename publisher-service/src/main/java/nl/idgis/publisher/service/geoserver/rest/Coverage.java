package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class Coverage extends Dataset {	
	
	public Coverage(String name, String nativeName, String title, String abstr, List<String> keywords, List<MetadataLink> metadataLinks) {
		super(name, nativeName, title, abstr, keywords, metadataLinks);
	}
}

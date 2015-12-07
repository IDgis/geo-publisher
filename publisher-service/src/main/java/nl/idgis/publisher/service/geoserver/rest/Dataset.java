package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public abstract class Dataset {

	protected final String name, nativeName, title, abstr;
	
	protected final List<String> keywords;
	
	protected final List<MetadataLink> metadataLinks;
	
	protected Dataset(String name, String nativeName, String title, String abstr, List<String> keywords, List<MetadataLink> metadataLinks) {
		this.name = name;
		this.nativeName = nativeName;
		this.title = title;
		this.abstr = abstr;		
		this.keywords = keywords;
		this.metadataLinks = metadataLinks;
	}

	public String getName() {
		return name;
	}

	public String getNativeName() {
		return nativeName;
	}

	public String getTitle() {
		return title;
	}

	public String getAbstract() {
		return abstr;
	}

	public List<String> getKeywords() {
		return keywords;
	}
	
	public List<MetadataLink> getMetadataLinks() {
		return metadataLinks;
	}
}

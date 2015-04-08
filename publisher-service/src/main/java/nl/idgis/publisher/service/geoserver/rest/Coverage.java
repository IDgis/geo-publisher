package nl.idgis.publisher.service.geoserver.rest;

public class Coverage {

	private final String name, nativeName;
	
	public Coverage(String name, String nativeName) {
		this.name = name;
		this.nativeName = nativeName;
	}
	
	public String getName() {
		return name;
	}
	
	public String getNativeName() {
		return nativeName;
	}
}

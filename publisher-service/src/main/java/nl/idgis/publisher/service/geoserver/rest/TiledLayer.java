package nl.idgis.publisher.service.geoserver.rest;

public class TiledLayer {
	
	private final String name;
	
	public TiledLayer(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "TiledLayer [name=" + name + "]";
	} 
}

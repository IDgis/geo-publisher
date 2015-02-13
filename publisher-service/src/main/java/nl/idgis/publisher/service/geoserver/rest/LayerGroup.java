package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class LayerGroup {

	private final String name;
	
	private final List<String> layers;
	
	public LayerGroup(String name, List<String> layers) {
		this.name = name;
		this.layers = layers;
	}

	public String getName() {
		return name;
	}
	
	public List<String> getLayers() {
		return layers;
	}
}

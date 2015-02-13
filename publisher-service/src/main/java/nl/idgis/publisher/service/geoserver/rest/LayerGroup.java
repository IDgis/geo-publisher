package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class LayerGroup {

	private final String name, title, abstr;
	
	private final List<String> layers;
	
	public LayerGroup(String name, String title, String abstr, List<String> layers) {
		this.name = name;
		this.title = title;
		this.abstr = abstr;
		this.layers = layers;
	}

	public String getName() {
		return name;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getAbstract() {
		return abstr;
	}
	
	public List<String> getLayers() {
		return layers;
	}
}

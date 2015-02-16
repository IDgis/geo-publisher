package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class LayerGroup {

	private final String name, title, abstr;
	
	private final List<LayerRef> layers;
	
	public LayerGroup(String name, String title, String abstr, List<LayerRef> layers) {
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
	
	public List<LayerRef> getLayers() {
		return layers;
	}
}

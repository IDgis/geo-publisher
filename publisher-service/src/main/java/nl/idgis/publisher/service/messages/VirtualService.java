package nl.idgis.publisher.service.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class VirtualService implements Serializable {

	private static final long serialVersionUID = -5205835846736660875L;

	private final String name;
	
	private final List<Layer> layers;
	
	public VirtualService(String name, List<Layer> layers) {
		this.name = name;
		this.layers = layers;
	}
	
	public String getName() {
		return name;
	}
	
	public List<Layer> getLayers() {
		return Collections.unmodifiableList(layers);
	}
}

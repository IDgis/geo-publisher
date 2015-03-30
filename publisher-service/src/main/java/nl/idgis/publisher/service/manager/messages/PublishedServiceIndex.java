package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class PublishedServiceIndex implements Serializable {	

	private static final long serialVersionUID = 3315418160630655190L;
	
	private final Map<String, List<String>> services;
	
	public PublishedServiceIndex(Map<String, List<String>> services) {
		this.services = services;
	}
	
	public Map<String, List<String>> getServices() {
		return services;
	}

	@Override
	public String toString() {
		return "PublishedServiceIndex [services=" + services + "]";
	}
}

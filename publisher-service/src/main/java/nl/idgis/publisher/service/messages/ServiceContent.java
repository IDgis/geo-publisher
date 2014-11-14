package nl.idgis.publisher.service.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class ServiceContent implements Serializable {
	
	private static final long serialVersionUID = -677980964403996471L;
	
	private final List<VirtualService> services;
	
	public ServiceContent(List<VirtualService> services) {
		this.services = services;
	}
	
	public List<VirtualService> getServices() {
		return Collections.unmodifiableList(services);
	}
}

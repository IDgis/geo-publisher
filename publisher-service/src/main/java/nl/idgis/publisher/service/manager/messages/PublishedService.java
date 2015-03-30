package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.Set;

import nl.idgis.publisher.domain.web.tree.Service;

public class PublishedService implements Serializable {

	private static final long serialVersionUID = 5975477747515668824L;
	
	private final Service service;
	
	private final Set<String> environmentIds;
	
	public PublishedService(Service service, Set<String> environmentIds) {
		this.service = service;
		this.environmentIds = environmentIds;
	}

	public Service getService() {
		return service;
	}

	public Set<String> getEnvironmentIds() {
		return environmentIds;
	}

	@Override
	public String toString() {
		return "PublishedService [service=" + service + ", environmentIds="
				+ environmentIds + "]";
	}
}

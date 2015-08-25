package nl.idgis.publisher.metadata.messages;

import java.util.Objects;
import java.util.Set;

public class ServiceRef {
	
	private final String serviceId;
	
	private final String serviceName;
	
	private final Set<String> layerNames;
	
	public ServiceRef(String serviceId, String serviceName, Set<String> layerNames) {		
		this.serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null");
		this.serviceName = Objects.requireNonNull(serviceName, "serviceName must not be null");
		this.layerNames = Objects.requireNonNull(layerNames, "layerNames must not be null");
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	public Set<String> getLayerNames() {
		return layerNames;
	}
}
package nl.idgis.publisher.metadata.messages;

import java.util.Objects;
import java.util.Set;

public class ServiceRef {
	
	private final String environmentId;
	
	private final String serviceId;
	
	private final Set<String> layerNames;
	
	public ServiceRef(String environmentId, String serviceId, Set<String> layerNames) {
		this.environmentId = Objects.requireNonNull(environmentId, "environmentId must not be null");
		this.serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null");
		this.layerNames = Objects.requireNonNull(layerNames, "layerNames must not be null");
	}
	
	public String getEnvironmentId() {
		return this.environmentId;
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public Set<String> getLayerNames() {
		return layerNames;
	}
}
package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.List;

public class PublishedServiceIndex implements Serializable {
	
	private static final long serialVersionUID = -3765894152552778111L;

	private final String environmentId;

	private final ServiceIndex serviceIndex;
	
	public PublishedServiceIndex(String environmentId, List<String> serviceNames, List<String> styleNames) {
		this(environmentId, new ServiceIndex(serviceNames, styleNames));
	}
	
	public PublishedServiceIndex(String environmentId, ServiceIndex serviceIndex) {
		this.environmentId = environmentId;
		this.serviceIndex = serviceIndex;
	}
	
	public String getEnvironmentId() {
		return environmentId;
	}
	
	public ServiceIndex getServiceIndex() {
		return serviceIndex;
	}

	@Override
	public String toString() {
		return "PublishedServiceIndex [environmentId=" + environmentId
				+ ", serviceIndex=" + serviceIndex + "]";
	}
	
}

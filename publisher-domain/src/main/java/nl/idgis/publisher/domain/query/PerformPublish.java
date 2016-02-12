package nl.idgis.publisher.domain.query;

import java.util.Optional;

public class PerformPublish implements DomainQuery<Boolean> {
	
	private static final long serialVersionUID = 4253088083282466495L;

	private final String serviceId;
	
	private final String environmentId;
	
	public PerformPublish(String serviceId) {
		this(serviceId, null);
	}

	public PerformPublish(String serviceId, String environmentId) {
		this.serviceId = serviceId;
		this.environmentId = environmentId;
	}

	public String getServiceId() {
		return serviceId;
	}

	public Optional<String> getEnvironmentId() {
		return Optional.ofNullable(environmentId);
	}
}

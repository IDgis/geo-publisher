package nl.idgis.publisher.domain.query;

import java.util.Set;

public class PerformPublish implements DomainQuery<Boolean> {
	
	private static final long serialVersionUID = -3425836343101017202L;

	private final String serviceId;
	
	private final Set<String> environmentIds;

	public PerformPublish(String serviceId, Set<String> environmentIds) {
		this.serviceId = serviceId;
		this.environmentIds = environmentIds;
	}

	public String getServiceId() {
		return serviceId;
	}

	public Set<String> getEnvironmentIds() {
		return environmentIds;
	}
}

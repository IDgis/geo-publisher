package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.ServicePublish;

public class ListEnvironments implements DomainQuery<Page<ServicePublish>> {
	private static final long serialVersionUID = -6545747372615378295L;
	
	private final String serviceId;
	
	public ListEnvironments (final String serviceId) {
		this.serviceId = serviceId;
	}

	public String getServiceId () {
		return serviceId;
	}
}

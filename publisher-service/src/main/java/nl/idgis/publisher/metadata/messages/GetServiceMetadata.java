package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;
import java.util.Objects;

public class GetServiceMetadata implements Serializable {

	private static final long serialVersionUID = 2011382870436056188L;
	
	private final String serviceId;
	
	public GetServiceMetadata(String serviceId) {
		this.serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null");
	}

	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "GetServiceMetadata [serviceId=" + serviceId + "]";
	}
}

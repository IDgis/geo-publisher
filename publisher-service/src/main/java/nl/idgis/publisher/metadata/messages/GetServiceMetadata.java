package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;
import java.util.Objects;

import nl.idgis.publisher.metadata.MetadataSource;

/**
 * Request a service metadata document from a {@link MetadataSource}.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
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

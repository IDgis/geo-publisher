package nl.idgis.publisher.metadata.messages;

import java.util.Objects;

import nl.idgis.publisher.metadata.MetadataDocument;

public class PutServiceMetadata extends PutMetadata {
	
	private static final long serialVersionUID = -5211768984975240783L;
	
	private final String serviceId;

	public PutServiceMetadata(String serviceId, MetadataDocument metadataDocument) {
		super(metadataDocument);
		
		this.serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null");
	}

	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "PutServiceMetadata [serviceId=" + serviceId + ", metadataDocument=" + metadataDocument + "]";
	}
}

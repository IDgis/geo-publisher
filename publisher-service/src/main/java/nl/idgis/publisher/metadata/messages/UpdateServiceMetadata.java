package nl.idgis.publisher.metadata.messages;

import java.util.Objects;

import nl.idgis.publisher.metadata.MetadataDocument;

public class UpdateServiceMetadata extends UpdateMetadata {	

	private static final long serialVersionUID = 4105320126397164912L;
	
	private final String serviceId;

	public UpdateServiceMetadata(String serviceId, MetadataDocument metadataDocument) {
		super(metadataDocument);
		
		this.serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null");
	}

	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "UpdateServiceMetadata [serviceId=" + serviceId + ", metadataDocument=" + metadataDocument + "]";
	}
}

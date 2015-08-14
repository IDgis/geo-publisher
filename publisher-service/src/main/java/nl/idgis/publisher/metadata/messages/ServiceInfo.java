package nl.idgis.publisher.metadata.messages;

import java.util.Set;

public class ServiceInfo extends MetadataItemInfo {
	
	private final Set<String> datasetId;
	
	public ServiceInfo(String serviceId, Set<String> datasetId) {
		super(serviceId);
		
		this.datasetId = datasetId;
	}
	
	public Set<String> getDatasetId() {
		return datasetId;
	}
}
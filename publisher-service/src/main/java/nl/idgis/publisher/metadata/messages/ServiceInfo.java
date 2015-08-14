package nl.idgis.publisher.metadata.messages;

import java.util.Set;

public class ServiceInfo extends MetadataItemInfo {	

	private static final long serialVersionUID = 38502973862867534L;
	
	private final Set<String> datasetId;
	
	public ServiceInfo(String serviceId, Set<String> datasetId) {
		super(serviceId);
		
		this.datasetId = datasetId;
	}
	
	public Set<String> getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "ServiceInfo [datasetId=" + datasetId + "]";
	}
}
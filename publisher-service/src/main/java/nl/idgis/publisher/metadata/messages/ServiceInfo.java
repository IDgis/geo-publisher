package nl.idgis.publisher.metadata.messages;

import java.util.Objects;
import java.util.Set;

public class ServiceInfo extends MetadataItemInfo {	
	
	private static final long serialVersionUID = 9220657367346908792L;
	
	private final Set<String> environmentIds;
	
	private final Set<DatasetRef> datasetRefs;
	
	public ServiceInfo(String serviceId, Set<String> environmentIds, Set<DatasetRef> datasetRefs) {
		super(serviceId);
		
		this.environmentIds = Objects.requireNonNull(environmentIds, "environmentIds must not be null");
		this.datasetRefs = Objects.requireNonNull(datasetRefs, "datasetRefs must not be null");
	}
	
	public Set<String> getEnvironmentIds() {
		return environmentIds;
	}
	
	public Set<DatasetRef> getDatasetRefs() {
		return datasetRefs;
	}

	@Override
	public String toString() {
		return "ServiceInfo [datasetRefs=" + datasetRefs + "]";
	}
}
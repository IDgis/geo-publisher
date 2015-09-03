package nl.idgis.publisher.metadata.messages;

import java.util.Objects;
import java.util.Set;

import nl.idgis.publisher.metadata.ServiceMetadataGenerator;

/**
 * Contains all dataset information required by {@link ServiceMetadataGenerator}.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class ServiceInfo extends MetadataItemInfo {	
	
	private static final long serialVersionUID = 9220657367346908792L;

	private final String name;
	
	private final Set<DatasetRef> datasetRefs;
	
	public ServiceInfo(String serviceId, String name, Set<DatasetRef> datasetRefs) {
		super(serviceId);
		
		this.name = name;
		this.datasetRefs = Objects.requireNonNull(datasetRefs, "datasetRefs must not be null");
	}
	
	public String getName() {
		return name;
	}
	
	public Set<DatasetRef> getDatasetRefs() {
		return datasetRefs;
	}

	@Override
	public String toString() {
		return "ServiceInfo [name=" + name + ", datasetRefs=" + datasetRefs + "]";
	}
	
}
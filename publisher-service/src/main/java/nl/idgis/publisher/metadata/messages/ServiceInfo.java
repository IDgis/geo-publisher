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

	private static final long serialVersionUID = -1916959442721541860L;

	private final String name;
	
	private final Set<DatasetRef> datasetRefs;
	
	private final String wmsMetadataId;
	
	private final String wfsMetadataId;
	
	public ServiceInfo(String serviceId, String name, String wmsMetadataId, 
		String wfsMetadataId, Set<DatasetRef> datasetRefs) {
		
		super(serviceId);
		
		this.name = name;
		this.wmsMetadataId = Objects.requireNonNull(wmsMetadataId, "wmsMetadataId must not be null");
		this.wfsMetadataId = Objects.requireNonNull(wfsMetadataId, "wfsMetadataId must not be null");
		this.datasetRefs = Objects.requireNonNull(datasetRefs, "datasetRefs must not be null");
	}
	
	public String getName() {
		return name;
	}

	public String getWMSMetadataId() {
		return wmsMetadataId;
	}

	public String getWFSMetadataId() {
		return wfsMetadataId;
	}

	public Set<DatasetRef> getDatasetRefs() {
		return datasetRefs;
	}

	@Override
	public String toString() {
		return "ServiceInfo [name=" + name + ", datasetRefs=" + datasetRefs + ", wmsMetadataId=" + wmsMetadataId
				+ ", wfsMetadataId=" + wfsMetadataId + "]";
	}	
}
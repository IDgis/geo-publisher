package nl.idgis.publisher.metadata.messages;

import java.util.Objects;
import java.util.Set;

import nl.idgis.publisher.metadata.DatasetMetadataGenerator;

/**
 * Contains all dataset information required by {@link DatasetMetadataGenerator}. 
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class DatasetInfo extends MetadataItemInfo {

	private static final long serialVersionUID = 7865206226945404461L;

	private final String dataSourceId;
	
	private final String externalDatasetId;
	
	private final String metadataId;
	
	private final String metadataFileId;
	
	private final Set<ServiceRef> serviceRefs;
		
	public DatasetInfo(String datasetId, String dataSourceId, String externalDatasetId, 
		String metadataId, String metadataFileId, Set<ServiceRef> serviceRefs) {
		
		super(datasetId);
		
		this.dataSourceId = Objects.requireNonNull(dataSourceId, "dataSourceId must not be null");
		this.externalDatasetId = Objects.requireNonNull(externalDatasetId, "externalDatasetId must not be null");
		this.metadataId = Objects.requireNonNull(metadataId, "metadataId must not be null");
		this.metadataFileId = Objects.requireNonNull(metadataFileId, "metadataFileId must not be null");
		this.serviceRefs = Objects.requireNonNull(serviceRefs, "serviceRefs must not be null");
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getExternalDatasetId() {
		return externalDatasetId;
	}	
	
	public String getMetadataId() {
		return metadataId;
	}

	public String getMetadataFileId() {
		return metadataFileId;
	}

	/**	 
	 * @return references to services providing this dataset.
	 */
	public Set<ServiceRef> getServiceRefs() {
		return serviceRefs;
	}

	@Override
	public String toString() {
		return "DatasetInfo [dataSourceId=" + dataSourceId + ", externalDatasetId=" + externalDatasetId
				+ ", metadataId=" + metadataId + ", metadataFileId=" + metadataFileId + ", serviceRefs=" + serviceRefs
				+ "]";
	}
}
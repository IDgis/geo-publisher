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

	private static final long serialVersionUID = -6573597622103892506L;

	private final String dataSourceId;
	
	private final String externalDatasetId;
	
	private final String datasetUuid;
	
	private final String fileUuid;
	
	private final Set<ServiceRef> serviceRefs;
		
	public DatasetInfo(String datasetId, String dataSourceId, String externalDatasetId, 
			String datasetUuid, String fileUuid, Set<ServiceRef> serviceRefs) {
		super(datasetId);
		
		this.dataSourceId = Objects.requireNonNull(dataSourceId, "dataSourceId must not be null");
		this.externalDatasetId = Objects.requireNonNull(externalDatasetId, "externalDatasetId must not be null");
		this.datasetUuid = Objects.requireNonNull(datasetUuid, "datasetUuid must not be null");
		this.fileUuid = Objects.requireNonNull(fileUuid, "fileUuid must not be null");
		this.serviceRefs = Objects.requireNonNull(serviceRefs, "serviceRefs must not be null");
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getExternalDatasetId() {
		return externalDatasetId;
	}		

	public String getDatasetUuid() {
		return datasetUuid;
	}

	public String getFileUuid() {
		return fileUuid;
	}
	
	/**	 
	 * @return references to services providing this dataset.
	 */
	public Set<ServiceRef> getServiceRefs() {
		return serviceRefs;
	}
}
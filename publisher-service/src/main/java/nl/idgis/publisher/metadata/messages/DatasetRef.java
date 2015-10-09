package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * A reference from a service to a dataset. An instance of this class 
 * always belong to a single instance of {@link ServiceInfo}.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class DatasetRef implements Serializable {	

	private static final long serialVersionUID = 8639792864243948510L;

	private final String datasetId;

	private final String metadataIdentification;
	
	private final String metadataFileIdentification;
	
	private final Set<String> layerNames;
	
	public DatasetRef(String datasetId, String metadataIdentification, String metadataFileIdentification, Set<String> layerNames) {
		this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
		this.metadataIdentification = Objects.requireNonNull(metadataIdentification, "metadataIdentification must not be null");
		this.metadataFileIdentification = Objects.requireNonNull(metadataFileIdentification, "metadataFileIdentification must not be null");
		this.layerNames = Objects.requireNonNull(layerNames, "layerNames must not be null");
	}
	
	public String getDatasetId() {
		return datasetId;
	}

	public String getMetadataIdentification() {
		return metadataIdentification;
	}

	public String getMetadataFileIdentification() {
		return metadataFileIdentification;
	}
	
	/**	 
	 * @return the name of the layers of the service based on this dataset.
	 */
	public Set<String> getLayerNames() {
		return layerNames;
	}

	@Override
	public String toString() {
		return "DatasetRef [datasetId=" + datasetId + ", metadataIdentification=" + metadataIdentification
				+ ", metadataFileIdentification=" + metadataFileIdentification + ", layerNames=" + layerNames + "]";
	}
}

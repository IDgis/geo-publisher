package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;
import java.util.Objects;

import nl.idgis.publisher.metadata.MetadataSource;

/**
 * Request a dataset metadata document from a {@link MetadataSource}.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class GetDatasetMetadata implements Serializable {

	private static final long serialVersionUID = -9013649122916128842L;

	private final String dataSourceId;
	
	private final String externalDatasetId;

	public GetDatasetMetadata(String dataSourceId, String externalDatasetId) {
		this.dataSourceId =  Objects.requireNonNull(dataSourceId, "dataSourceId must not be null");
		this.externalDatasetId =  Objects.requireNonNull(externalDatasetId, "externalDatasetId must not be null");
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getExternalDatasetId() {
		return externalDatasetId;
	}

	@Override
	public String toString() {
		return "GetDatasetMetadata [dataSourceId=" + dataSourceId + ", externalDatasetId=" + externalDatasetId + "]";
	}
}

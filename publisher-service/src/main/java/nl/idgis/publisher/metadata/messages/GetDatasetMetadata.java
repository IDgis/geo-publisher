package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;

public class GetDatasetMetadata implements Serializable {

	private static final long serialVersionUID = -9013649122916128842L;

	private final String dataSourceId;
	
	private final String externalDatasetId;

	public GetDatasetMetadata(String dataSourceId, String externalDatasetId) {
		this.dataSourceId = dataSourceId;
		this.externalDatasetId = externalDatasetId;
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

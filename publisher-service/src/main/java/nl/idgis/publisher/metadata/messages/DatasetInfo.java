package nl.idgis.publisher.metadata.messages;

import java.util.Objects;

public class DatasetInfo extends MetadataItemInfo {		

	private static final long serialVersionUID = -7123957524471751538L;

	private final String dataSourceId;
	
	private final String externalDatasetId;
	
	public DatasetInfo(String datasetId, String dataSourceId, String externalDatasetId) {
		super(datasetId);
		
		this.dataSourceId = Objects.requireNonNull(dataSourceId, "dataSourceId must not be null");
		this.externalDatasetId = Objects.requireNonNull(externalDatasetId, "externalDatasetId must not be null");
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getExternalDatasetId() {
		return externalDatasetId;
	}

	@Override
	public String toString() {
		return "DatasetInfo [dataSourceId=" + dataSourceId + ", externalDatasetId=" + externalDatasetId + "]";
	}		
}
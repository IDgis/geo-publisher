package nl.idgis.publisher.metadata.messages;

public class DatasetInfo extends MetadataItemInfo {
		
	private final String dataSourceId;
	
	private final String externalDatasetId;
	
	public DatasetInfo(String datasetId, String dataSourceId, String externalDatasetId) {
		super(datasetId);
		
		this.dataSourceId = dataSourceId;
		this.externalDatasetId = externalDatasetId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getExternalDatasetId() {
		return externalDatasetId;
	}		
}
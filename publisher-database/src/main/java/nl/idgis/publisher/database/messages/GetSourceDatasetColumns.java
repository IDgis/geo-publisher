package nl.idgis.publisher.database.messages;

public class GetSourceDatasetColumns extends Query {

	private static final long serialVersionUID = 2512817194752601975L;
	
	private final String dataSourceId, sourceDatasetId;
	
	public GetSourceDatasetColumns(String dataSourceId, String sourceDatasetId) {
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;
	}
	
	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}

	@Override
	public String toString() {
		return "GetSourceDatasetInfo [dataSourceId=" + dataSourceId
				+ ", sourceDatasetId=" + sourceDatasetId + "]";
	}
}

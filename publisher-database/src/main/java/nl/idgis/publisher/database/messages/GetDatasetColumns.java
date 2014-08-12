package nl.idgis.publisher.database.messages;

public class GetDatasetColumns extends Query {

	private static final long serialVersionUID = 1631761982151836240L;
	
	private final String datasetId;
	
	public GetDatasetColumns(String datasetId) {
		this.datasetId = datasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "GetDatasetInfo [datasetId=" + datasetId + "]";
	}
}

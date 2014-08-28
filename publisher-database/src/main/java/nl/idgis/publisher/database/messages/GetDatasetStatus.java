package nl.idgis.publisher.database.messages;

public class GetDatasetStatus extends Query {
	
	private static final long serialVersionUID = -5526921345754616702L;
	
	private final String datasetId;
	
	public GetDatasetStatus() {
		this(null);
	}
	
	public GetDatasetStatus(String datasetId) {
		this.datasetId = datasetId;
	}	

	public String getDatasetId() {
		return datasetId;
	}
	
	@Override
	public String toString() {
		return "GetDatasetStatus [datasetId=" + datasetId + "]";
	}

}

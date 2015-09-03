package nl.idgis.publisher.metadata.messages;

public class KeepDatasetMetadata extends KeepMetadata {

	private static final long serialVersionUID = -6745682052818170218L;
	
	private final String datasetId;
	
	public KeepDatasetMetadata(String datasetId) {
		this.datasetId = datasetId;
	}

	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "KeepDatasetMetadata [datasetId=" + datasetId + "]";
	}
}

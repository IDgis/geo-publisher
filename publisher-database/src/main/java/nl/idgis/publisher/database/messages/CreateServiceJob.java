package nl.idgis.publisher.database.messages;

public class CreateServiceJob extends Query {

	private static final long serialVersionUID = -1831407439621996832L;
	
	private final String datasetId;
	
	public CreateServiceJob(String datasetId) {
		this.datasetId = datasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "CreateServiceJob [datasetId=" + datasetId + "]";
	}
}

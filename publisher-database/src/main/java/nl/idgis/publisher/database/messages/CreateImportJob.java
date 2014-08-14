package nl.idgis.publisher.database.messages;

public class CreateImportJob extends Query {

	private static final long serialVersionUID = -7342920894865169011L;
	
	private final String datasetId;
	
	public CreateImportJob(String datasetId) {
		this.datasetId = datasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "CreateImportJob [datasetId=" + datasetId + "]";
	}
}

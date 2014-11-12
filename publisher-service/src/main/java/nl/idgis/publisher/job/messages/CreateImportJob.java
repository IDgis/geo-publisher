package nl.idgis.publisher.job.messages;

public class CreateImportJob extends CreateJob {	
	
	private static final long serialVersionUID = 7889228089587786764L;
	
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

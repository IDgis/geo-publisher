package nl.idgis.publisher.job.messages;

public class CreateServiceJob extends CreateJob {	

	private static final long serialVersionUID = 4388920021981599252L;
	
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

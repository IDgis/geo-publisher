package nl.idgis.publisher.job.manager.messages;

public class CreateRemoveJob extends CreateJob {
	
	private static final long serialVersionUID = 3509800062614635099L;	

	private final String datasetId;
	
	public CreateRemoveJob(String datasetId) {
		this.datasetId = datasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}
	
	@Override
	public String toString() {
		return "CreateRemoveJob [datasetId=" + datasetId + "]";
	}
}

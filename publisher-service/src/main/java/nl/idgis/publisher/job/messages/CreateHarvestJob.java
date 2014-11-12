package nl.idgis.publisher.job.messages;

public class CreateHarvestJob extends CreateJob {	

	private static final long serialVersionUID = -2292102304377571130L;
	
	private final String dataSourceId;
	
	public CreateHarvestJob(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}
	
	public String getDataSourceId() {
		return dataSourceId;
	}

	@Override
	public String toString() {
		return "CreateHarvestJob [dataSourceId=" + dataSourceId + "]";
	}
}

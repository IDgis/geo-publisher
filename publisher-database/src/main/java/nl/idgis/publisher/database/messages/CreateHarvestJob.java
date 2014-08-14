package nl.idgis.publisher.database.messages;

public class CreateHarvestJob extends Query {

	private static final long serialVersionUID = -8790717151606018078L;
	
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

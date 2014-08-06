package nl.idgis.publisher.domain.query;

public class RefreshDataset implements DomainQuery<Boolean> {
	
	private static final long serialVersionUID = -7528142328578064307L;
	
	private final String datasetId;
	
	public RefreshDataset(String datasetId) {
		this.datasetId = datasetId;
	}

	public String getDatasetId() {
		return datasetId;
	}
}

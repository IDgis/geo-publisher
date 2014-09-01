package nl.idgis.publisher.database.messages;

public class GetDatasetColumnDiff extends Query {
	private static final long serialVersionUID = 4333501278634945578L;
	
	private final String datasetIdentification;
	
	public GetDatasetColumnDiff (final String datasetIdentification) {
		if (datasetIdentification == null) {
			throw new NullPointerException ("datasetIdentification cannot be null");
		}
		
		this.datasetIdentification = datasetIdentification;
	}

	public String getDatasetIdentification () {
		return datasetIdentification;
	}
}

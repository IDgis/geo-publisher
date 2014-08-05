package nl.idgis.publisher.domain.log;

public class ImportLogLine extends LogLine {
	
	private static final long serialVersionUID = 1631388870231783422L;
	
	private final String datasetId;
	
	public ImportLogLine(Enum<?> event, String datasetId) {
		super(event);
		
		this.datasetId = datasetId;
	}

	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "ImportLogLine [datasetId=" + datasetId + "]";
	}
}

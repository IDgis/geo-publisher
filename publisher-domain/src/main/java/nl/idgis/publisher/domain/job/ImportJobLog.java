package nl.idgis.publisher.domain.job;

public class ImportJobLog extends JobLog {
	
	private static final long serialVersionUID = 1631388870231783422L;
	
	private final String datasetId;
	
	public ImportJobLog(JobState state, LogLevel level, Enum<?> type, String datasetId) {
		super(state, level, type);
		
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

package nl.idgis.publisher.domain.job;

public class HarvestJobLog extends JobLog {
	
	private static final long serialVersionUID = 6363380470910198289L;
	
	private final String dataSourceId;

	public HarvestJobLog(JobState state, LogLevel level, Enum<?> type, String dataSourceId) {
		super(state, level, type);
		
		this.dataSourceId = dataSourceId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	@Override
	public String toString() {
		return "HarvestLogLine [dataSourceId=" + dataSourceId + "]";
	}	
	
	
}

package nl.idgis.publisher.domain.log;

public class HarvestLogLine extends LogLine {
	
	private static final long serialVersionUID = 6363380470910198289L;
	
	private final String dataSourceId;

	public HarvestLogLine(Enum<?> event, String dataSourceId) {
		super(event);
		
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

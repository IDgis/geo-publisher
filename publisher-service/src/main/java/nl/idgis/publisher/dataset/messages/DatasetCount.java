package nl.idgis.publisher.dataset.messages;

import java.io.Serializable;
import java.util.Map;

public class DatasetCount implements Serializable {
	
	private static final long serialVersionUID = 3232133888657673833L;
	
	private final String dataSourceId;
	
	private final Map<String, Integer> datasetCount;
	
	public DatasetCount(String dataSourceId, Map<String, Integer> datasetCount) {
		this.dataSourceId = dataSourceId;
		this.datasetCount = datasetCount;
	}
	
	public String getDatasourceId() {
		return dataSourceId;
	}
	
	public Map<String, Integer> getDatasetCount() {
		return datasetCount;
	}
	
	@Override
	public String toString() {
		return "DatasetCount [dataSourceId=" + dataSourceId + ", datasetCount=" + datasetCount + "]";
	}
}

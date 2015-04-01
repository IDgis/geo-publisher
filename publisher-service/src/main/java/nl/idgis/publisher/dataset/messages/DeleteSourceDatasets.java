package nl.idgis.publisher.dataset.messages;

import java.io.Serializable;
import java.util.Set;

public class DeleteSourceDatasets implements Serializable {

	private static final long serialVersionUID = -592535385644255711L;

	private final String dataSourceId;
	
	private final Set<String> datasetIds;
	
	public DeleteSourceDatasets(String dataSourceId, Set<String> datasetIds) {
		this.dataSourceId = dataSourceId;
		this.datasetIds = datasetIds;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public Set<String> getDatasetIds() {
		return datasetIds;
	}

	@Override
	public String toString() {
		return "DeleteSourceDatasets [dataSourceId=" + dataSourceId
				+ ", datasetIds=" + datasetIds + "]";
	}
}

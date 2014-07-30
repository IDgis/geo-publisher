package nl.idgis.publisher.database.messages;

import java.io.Serializable;

public class ImportJob implements Serializable {
	
	private static final long serialVersionUID = 2210035869207085551L;
	
	public final String dataSourceId, sourceDatasetId, datasetId;
	
	public ImportJob(String dataSourceId, String sourceDatasetId, String datasetId) {
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;
		this.datasetId = datasetId;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}

	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "ImportJob [sourceDatasetId=" + sourceDatasetId + ", datasetId="
				+ datasetId + "]";
	}

	public String getDataSourceId() {
		return dataSourceId;
	}
}

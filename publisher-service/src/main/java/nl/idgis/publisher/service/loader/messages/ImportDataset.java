package nl.idgis.publisher.service.loader.messages;

import java.io.Serializable;

public class ImportDataset implements Serializable {
	
	private static final long serialVersionUID = 3244417913532187724L;
	
	private final String dataSourceId, sourceDatasetId, datasetId;
	
	public ImportDataset(String dataSourceId, String sourceDatasetId, String datasetId) {
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;
		this.datasetId = datasetId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "ImportDataset [dataSourceId=" + dataSourceId
				+ ", sourceDatasetId=" + sourceDatasetId + ", datasetId="
				+ datasetId + "]";
	}
	
}

package nl.idgis.publisher.service.loader.messages;

import java.io.Serializable;

public class ImportDataset implements Serializable {
	
	private static final long serialVersionUID = 3244417913532187724L;
	
	private final String dataSourceId, sourceDatasetId;
	
	public ImportDataset(String dataSourceId, String sourceDatasetId) {
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}

	@Override
	public String toString() {
		return "ImportTable [dataSourceId=" + dataSourceId
				+ ", sourceDatasetId=" + sourceDatasetId + "]";
	}
}

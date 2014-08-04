package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class ImportJob implements Serializable {
	
	private static final long serialVersionUID = 2713600638521446785L;
	
	private final String dataSourceId, sourceDatasetId, datasetId;
	private final List<Column> columns; 
	
	public ImportJob(String dataSourceId, String sourceDatasetId, String datasetId, List<Column> columns) {
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;
		this.datasetId = datasetId;
		this.columns = columns;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}	

	public String getDataSourceId() {
		return dataSourceId;
	}
	
	public List<Column> getColumns() {
		return Collections.unmodifiableList(columns);
	}

	@Override
	public String toString() {
		return "ImportJob [dataSourceId=" + dataSourceId + ", sourceDatasetId="
				+ sourceDatasetId + ", datasetId=" + datasetId + ", columns="
				+ columns + "]";
	}

	
}

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columns == null) ? 0 : columns.hashCode());
		result = prime * result
				+ ((dataSourceId == null) ? 0 : dataSourceId.hashCode());
		result = prime * result
				+ ((datasetId == null) ? 0 : datasetId.hashCode());
		result = prime * result
				+ ((sourceDatasetId == null) ? 0 : sourceDatasetId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImportJob other = (ImportJob) obj;
		if (columns == null) {
			if (other.columns != null)
				return false;
		} else if (!columns.equals(other.columns))
			return false;
		if (dataSourceId == null) {
			if (other.dataSourceId != null)
				return false;
		} else if (!dataSourceId.equals(other.dataSourceId))
			return false;
		if (datasetId == null) {
			if (other.datasetId != null)
				return false;
		} else if (!datasetId.equals(other.datasetId))
			return false;
		if (sourceDatasetId == null) {
			if (other.sourceDatasetId != null)
				return false;
		} else if (!sourceDatasetId.equals(other.sourceDatasetId))
			return false;
		return true;
	}

	
}

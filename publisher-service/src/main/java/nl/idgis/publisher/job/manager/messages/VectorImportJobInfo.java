package nl.idgis.publisher.job.manager.messages;

import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.service.Column;

public class VectorImportJobInfo extends ImportJobInfo {		

	private static final long serialVersionUID = 2489387779878772008L;

	private final String filterCondition;
	
	private final List<Column> columns, sourceDatasetColumns; 
	
	public VectorImportJobInfo(int id, String categoryId, String dataSourceId, String sourceDatasetId, String externalSourceDatasetId, String datasetId, String datasetName, String filterCondition, List<Column> columns, List<Column> sourceDatasetColumns, List<Notification> notifications) {
		super(id, categoryId, dataSourceId, sourceDatasetId, externalSourceDatasetId, datasetId, datasetName, notifications);		
		
		this.filterCondition = filterCondition;
		this.columns = columns;
		this.sourceDatasetColumns = sourceDatasetColumns;
	}

	public List<Column> getColumns() {
		return Collections.unmodifiableList(columns);
	}
	
	public List<Column> getSourceDatasetColumns() {
		return Collections.unmodifiableList(sourceDatasetColumns);
	}

	public String getFilterCondition() {
		return filterCondition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((columns == null) ? 0 : columns.hashCode());
		result = prime * result
				+ ((filterCondition == null) ? 0 : filterCondition.hashCode());
		result = prime
				* result
				+ ((sourceDatasetColumns == null) ? 0 : sourceDatasetColumns
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		VectorImportJobInfo other = (VectorImportJobInfo) obj;
		if (columns == null) {
			if (other.columns != null)
				return false;
		} else if (!columns.equals(other.columns))
			return false;
		if (filterCondition == null) {
			if (other.filterCondition != null)
				return false;
		} else if (!filterCondition.equals(other.filterCondition))
			return false;
		if (sourceDatasetColumns == null) {
			if (other.sourceDatasetColumns != null)
				return false;
		} else if (!sourceDatasetColumns.equals(other.sourceDatasetColumns))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "VectorImportJobInfo [filterCondition=" + filterCondition
				+ ", columns=" + columns + ", sourceDatasetColumns="
				+ sourceDatasetColumns + ", categoryId=" + categoryId
				+ ", dataSourceId=" + dataSourceId + ", sourceDatasetId="
				+ sourceDatasetId + ", externalSourceDatasetId="
				+ externalSourceDatasetId + ", datasetId=" + datasetId
				+ ", datasetName=" + datasetName + ", id=" + id + ", jobType="
				+ jobType + ", notifications=" + notifications + "]";
	}	
}

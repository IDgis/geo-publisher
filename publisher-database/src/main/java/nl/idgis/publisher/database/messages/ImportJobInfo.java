package nl.idgis.publisher.database.messages;

import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.domain.job.JobType;
import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.service.Column;

public class ImportJobInfo extends JobInfo {
	
	private static final long serialVersionUID = -7099595558614549074L;
	
	private final String categoryId, dataSourceId, sourceDatasetId, 
		datasetId, datasetName, filterCondition;
	private final List<Column> columns, sourceDatasetColumns; 
	
	public ImportJobInfo(int id, String categoryId, String dataSourceId, String sourceDatasetId, String datasetId, String datasetName, String filterCondition, List<Column> columns, List<Column> sourceDatasetColumns, List<Notification> notifications) {
		super(id, JobType.HARVEST, notifications);
		
		this.categoryId = categoryId;
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;
		this.datasetId = datasetId;
		this.datasetName = datasetName;
		this.filterCondition = filterCondition;
		this.columns = columns;
		this.sourceDatasetColumns = sourceDatasetColumns;
	}
	
	public String getCategoryId() {
		return categoryId;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}
	
	public String getDatasetName() {
		return datasetName;
	}	

	public String getDataSourceId() {
		return dataSourceId;
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
		int result = 1;
		result = prime * result
				+ ((categoryId == null) ? 0 : categoryId.hashCode());
		result = prime * result + ((columns == null) ? 0 : columns.hashCode());
		result = prime * result
				+ ((dataSourceId == null) ? 0 : dataSourceId.hashCode());
		result = prime * result
				+ ((datasetId == null) ? 0 : datasetId.hashCode());
		result = prime * result
				+ ((datasetName == null) ? 0 : datasetName.hashCode());
		result = prime * result
				+ ((filterCondition == null) ? 0 : filterCondition.hashCode());
		result = prime
				* result
				+ ((sourceDatasetColumns == null) ? 0 : sourceDatasetColumns
						.hashCode());
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
		ImportJobInfo other = (ImportJobInfo) obj;
		if (categoryId == null) {
			if (other.categoryId != null)
				return false;
		} else if (!categoryId.equals(other.categoryId))
			return false;
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
		if (datasetName == null) {
			if (other.datasetName != null)
				return false;
		} else if (!datasetName.equals(other.datasetName))
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
		if (sourceDatasetId == null) {
			if (other.sourceDatasetId != null)
				return false;
		} else if (!sourceDatasetId.equals(other.sourceDatasetId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ImportJobInfo [categoryId=" + categoryId + ", dataSourceId="
				+ dataSourceId + ", sourceDatasetId=" + sourceDatasetId
				+ ", datasetId=" + datasetId + ", datasetName=" + datasetName
				+ ", filterCondition=" + filterCondition + ", columns="
				+ columns + ", sourceDatasetColumns=" + sourceDatasetColumns
				+ "]";
	}	
	
}

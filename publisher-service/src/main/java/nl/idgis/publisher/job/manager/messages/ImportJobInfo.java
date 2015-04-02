package nl.idgis.publisher.job.manager.messages;

import java.util.List;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.job.JobType;
import nl.idgis.publisher.domain.job.Notification;

public abstract class ImportJobInfo extends JobInfo {

	private static final long serialVersionUID = -2957923190022287737L;
	
	protected final String categoryId, dataSourceId, sourceDatasetId, datasetId, datasetName;

	public ImportJobInfo(int id, String categoryId, String dataSourceId, String sourceDatasetId, 
		String datasetId, String datasetName, List<Notification> notifications) {
		
		super(id, JobType.IMPORT, notifications);
		
		this.categoryId = categoryId;
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;
		this.datasetId = datasetId;
		this.datasetName = datasetName;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getCategoryId() {
		return categoryId;
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

	public String getDatasetName() {
		return datasetName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((categoryId == null) ? 0 : categoryId.hashCode());
		result = prime * result
				+ ((dataSourceId == null) ? 0 : dataSourceId.hashCode());
		result = prime * result
				+ ((datasetId == null) ? 0 : datasetId.hashCode());
		result = prime * result
				+ ((datasetName == null) ? 0 : datasetName.hashCode());
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
		if (sourceDatasetId == null) {
			if (other.sourceDatasetId != null)
				return false;
		} else if (!sourceDatasetId.equals(other.sourceDatasetId))
			return false;
		return true;
	}

}

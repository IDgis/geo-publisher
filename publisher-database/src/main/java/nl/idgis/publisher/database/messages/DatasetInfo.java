package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.sql.Timestamp;

import nl.idgis.publisher.domain.job.JobState;

import com.mysema.query.annotations.QueryProjection;

public class DatasetInfo implements Serializable {

	private static final long serialVersionUID = 1483600283295264723L;
	
	private final String id;
	private final String name;
	private String sourceDatasetId, sourceDatasetName;
	private String categoryId, categoryName;
	private final String filterConditions;
	private final Boolean imported;
	private final Boolean serviceCreated;
	private final Boolean sourceDatasetColumnsChanged;
	private final Timestamp lastImportTime;
	private final JobState lastJobState;

	@QueryProjection
	public DatasetInfo(String id, String name, String sourceDatasetId,
			String sourceDatasetName, String categoryId, String categoryName, 
			final String filterConditions,
			final Boolean imported,
			final Boolean serviceCreated,
			final Boolean sourceDatasetColumnsChanged,
			final Timestamp lastImportTime,
			final String lastJobState) {
		super();
		this.id = id;
		this.name = name;
		this.sourceDatasetId = sourceDatasetId;
		this.sourceDatasetName = sourceDatasetName;
		this.categoryId = categoryId;
		this.categoryName = categoryName;
		this.filterConditions = filterConditions;
		this.imported = imported;
		this.serviceCreated = serviceCreated;
		this.sourceDatasetColumnsChanged = sourceDatasetColumnsChanged;
		this.lastImportTime = lastImportTime;
		this.lastJobState = lastJobState == null ? null : JobState.valueOf (lastJobState);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}

	public String getSourceDatasetName() {
		return sourceDatasetName;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}
	
	public String getFilterConditions () {
		return filterConditions;
	}

	public Boolean getImported() {
		return imported;
	}

	public Boolean getServiceCreated() {
		return serviceCreated;
	}

	public Boolean getSourceDatasetColumnsChanged() {
		return sourceDatasetColumnsChanged;
	}

	public Timestamp getLastImportTime() {
		return lastImportTime;
	}

	public JobState getLastJobState() {
		return lastJobState;
	}

	@Override
	public String toString() {
		return "DatasetInfo [id=" + id + ", name=" + name
				+ ", sourceDatasetId=" + sourceDatasetId
				+ ", sourceDatasetName=" + sourceDatasetName + ", categoryId="
				+ categoryId + ", categoryName=" + categoryName + "]";
	}

}

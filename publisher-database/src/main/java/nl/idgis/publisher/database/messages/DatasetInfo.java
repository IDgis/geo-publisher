package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.domain.job.JobState;

public class DatasetInfo extends BaseDatasetInfo implements Serializable {
	
	private static final long serialVersionUID = 5854139357908268835L;
	
	private String sourceDatasetId, sourceDatasetName;
	private String categoryId, categoryName;
	private final String filterConditions, metadataFileId;
	private final Boolean imported;	
	private final Boolean sourceDatasetColumnsChanged;
	private final Timestamp lastImportTime;
	private final JobState lastImportJobState;
	private final List<StoredNotification> notifications;
	private final long layerCount;
	private final long publishedServiceCount;
	private final boolean confidential;
	private final boolean wmsOnly;
	private final String physicalName;

	public DatasetInfo(String id, String name, String sourceDatasetId,
			String sourceDatasetName, String categoryId, String categoryName,
			final String filterConditions,
			final Boolean imported,
			final Boolean sourceDatasetColumnsChanged,
			final Timestamp lastImportTime,
			final String lastImportJobState,
			final List<StoredNotification> notifications,
			final long layerCount,
			final long publishedServiceCount,
			final boolean confidential,
			final boolean wmsOnly,
			final String metadataFileId,
			final String physicalName) {
		super(id, name);
		
		this.sourceDatasetId = sourceDatasetId;
		this.sourceDatasetName = sourceDatasetName;
		this.categoryId = categoryId;
		this.categoryName = categoryName;
		this.filterConditions = filterConditions;
		this.imported = imported;
		this.sourceDatasetColumnsChanged = sourceDatasetColumnsChanged;
		this.lastImportTime = lastImportTime;
		this.lastImportJobState = toJobState(lastImportJobState);		
		this.notifications = notifications == null ? Collections.<StoredNotification>emptyList () : new ArrayList<> (notifications);
		this.layerCount = layerCount;
		this.publishedServiceCount = publishedServiceCount;
		this.confidential = confidential;
		this.wmsOnly = wmsOnly;
		this.metadataFileId = metadataFileId;
		this.physicalName = physicalName;
	}
	
	private static JobState toJobState(String jobStateName) {
		if(jobStateName == null) {
			return null;
		} else {
			return JobState.valueOf(jobStateName);
		}
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

	public Boolean getSourceDatasetColumnsChanged() {
		return sourceDatasetColumnsChanged;
	}

	public Timestamp getLastImportTime() {
		return lastImportTime;
	}

	public JobState getLastImportJobState() {
		return lastImportJobState;
	}

	public List<StoredNotification> getNotifications () {
		return Collections.unmodifiableList (notifications);
	}
	
	public long getLayerCount () {
		return layerCount;
	}
	
	public long getPublishedServiceCount () {
		return publishedServiceCount;
	}
	
	public boolean isConfidential () {
		return confidential;
	}
	
	public boolean isWmsOnly () {
		return wmsOnly;
	}
	
	public String getMetadataFileId () {
		return metadataFileId;
	}
	
	public String getPhysicalName () {
		return physicalName;
	}

	@Override
	public String toString() {
		return "DatasetInfo [sourceDatasetId=" + sourceDatasetId + ", sourceDatasetName=" + sourceDatasetName
				+ ", categoryId=" + categoryId + ", categoryName=" + categoryName + ", filterConditions="
				+ filterConditions + ", metadataFileId=" + metadataFileId + ", imported=" + imported
				+ ", sourceDatasetColumnsChanged=" + sourceDatasetColumnsChanged + ", lastImportTime=" + lastImportTime
				+ ", lastImportJobState=" + lastImportJobState + ", notifications=" + notifications + ", layerCount="
				+ layerCount + ", confidential=" + confidential + ", wmsOnly=" + wmsOnly + ", physicalName=" + physicalName + "]";
	}
}

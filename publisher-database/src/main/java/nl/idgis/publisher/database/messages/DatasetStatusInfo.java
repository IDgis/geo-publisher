package nl.idgis.publisher.database.messages;

import java.io.Serializable;

import com.mysema.query.annotations.QueryProjection;

public class DatasetStatusInfo implements Serializable {	

	private static final long serialVersionUID = -4030206407449916616L;
	
	private final String datasetId;
	private final boolean 
		
		columnsChanged,
		filterConditionChanged,
		sourceDatasetChanged,
		
		imported,
		serviceCreated,
		
		sourceDatasetColumnsChanged,
		sourceDatasetRevisionChanged; 
	
	@QueryProjection
	public DatasetStatusInfo(
			String datasetId,
			
			boolean columnsChanged,
			boolean filterConditionChanged,
			boolean sourceDatasetChanged,
			
			boolean imported,
			boolean serviceCreated,
			
			boolean sourceDatasetColumnsChanged,
			boolean sourceDatasetRevisionChanged) {
		
		this.datasetId = datasetId;

		this.columnsChanged = columnsChanged;
		this.filterConditionChanged = filterConditionChanged;
		this.sourceDatasetChanged = sourceDatasetChanged;
		
		this.imported = imported;
		this.serviceCreated = serviceCreated;
		
		this.sourceDatasetColumnsChanged = sourceDatasetColumnsChanged;
		this.sourceDatasetRevisionChanged = sourceDatasetRevisionChanged;
	}

	public String getDatasetId() {
		return datasetId;
	}	
	
	public boolean isServiceCreated() {
		return serviceCreated;
	}
	
	public boolean isImported() {
		return imported;
	}
	
	public boolean isSourceDatasetRevisionChanged() {
		return imported && sourceDatasetRevisionChanged;
	}
	
	public boolean isSourceDatasetColumnsChanged() {
		return imported && sourceDatasetColumnsChanged;		
	}
	
	public boolean isColumnsChanged() {
		return imported && columnsChanged;
	}
	
	public boolean isFilterConditionChanged() {
		return imported && filterConditionChanged;
	}
	
	public boolean isSourceDatasetChanged() {
		return imported && sourceDatasetChanged;
	}

	@Override
	public String toString() {
		return "DatasetStatus [datasetId=" + datasetId + ", columnsChanged="
				+ columnsChanged + ", filterConditionChanged="
				+ filterConditionChanged + ", sourceDatasetChanged="
				+ sourceDatasetChanged + ", imported=" + imported
				+ ", serviceCreated=" + serviceCreated
				+ ", sourceDatasetColumnsChanged="
				+ sourceDatasetColumnsChanged
				+ ", sourceDatasetRevisionChanged="
				+ sourceDatasetRevisionChanged + "]";
	}	
	
	
}

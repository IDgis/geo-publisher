package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.sql.Timestamp;

import nl.idgis.publisher.domain.job.JobState;

import com.mysema.query.annotations.QueryProjection;

public class DataSourceStatus implements Serializable {

	private static final long serialVersionUID = 563568110154119561L;
	
	private final String dataSourceId;
	private final Timestamp lastHarvested;
	private final JobState finishedState;	
	
	@QueryProjection
	public DataSourceStatus(String dataSourceId, Timestamp lastHarvested, String finishedState) {
		this.dataSourceId = dataSourceId;
		this.lastHarvested = lastHarvested;
		this.finishedState = finishedState == null ? null : JobState.valueOf(finishedState);		
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public Timestamp getLastHarvested() {
		return lastHarvested;
	}
	
	public JobState getFinishedState() {
		return finishedState;
	}
	
	@Override
	public String toString() {
		return "HarvestStatus [dataSourceId=" + dataSourceId
				+ ", lastHarvested=" + lastHarvested + ", finishedState="
				+ finishedState + "]";
	}
	
}

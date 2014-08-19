package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.sql.Timestamp;

import nl.idgis.publisher.domain.job.JobState;

import com.mysema.query.annotations.QueryProjection;

public class HarvestStatus implements Serializable {

	private static final long serialVersionUID = 563568110154119561L;
	
	private final String dataSourceId;
	private final Timestamp lastFinished;
	private final JobState finishedState;	
	
	@QueryProjection
	public HarvestStatus(String dataSourceId, Timestamp lastFinished, String finishedState) {
		this.dataSourceId = dataSourceId;
		this.lastFinished = lastFinished;
		this.finishedState = finishedState == null ? null : JobState.valueOf(finishedState);		
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public Timestamp getLastFinished() {
		return lastFinished;
	}
	
	public JobState getFinishedState() {
		return finishedState;
	}
	
	@Override
	public String toString() {
		return "HarvestStatus [dataSourceId=" + dataSourceId
				+ ", lastFinished=" + lastFinished + ", finishedState="
				+ finishedState + "]";
	}
	
}

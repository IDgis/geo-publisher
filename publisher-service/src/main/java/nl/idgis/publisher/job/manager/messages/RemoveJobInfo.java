package nl.idgis.publisher.job.manager.messages;

import com.mysema.query.annotations.QueryProjection;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.job.JobType;

public class RemoveJobInfo extends JobInfo {	
	
	private static final long serialVersionUID = 7269768063612150838L;
	
	private final String datasetId;

	@QueryProjection
	public RemoveJobInfo(int id, String datasetId) {
		super(id, JobType.REMOVE);
		
		this.datasetId = datasetId;
	}

	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "RemoveJobInfo [datasetId=" + datasetId + "]";
	}
}

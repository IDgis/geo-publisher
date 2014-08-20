package nl.idgis.publisher.database.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.job.JobType;

public class JobInfo implements Serializable {

	private static final long serialVersionUID = 6340217799882050992L;
	
	protected final int id;
	protected final JobType jobType;
	
	public JobInfo(int id, JobType jobType) {
		this.id = id;
		this.jobType = jobType;
	}
	
	public int getId() {
		return id;
	}
	
	public JobType getJobType() {
		return jobType;
	}

	@Override
	public String toString() {
		return "JobInfo [id=" + id + ", jobType=" + jobType + "]";
	}
}
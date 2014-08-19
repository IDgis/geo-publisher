package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.job.JobLog;

public class StoreLog extends Query {
	
	private static final long serialVersionUID = -1228166659611717157L;
	
	private final JobInfo job;
	private final JobLog jobLog;
	
	public StoreLog(JobInfo job, JobLog jobLog) {
		this.job = job;
		this.jobLog = jobLog;
	}
	
	public JobInfo getJob() {
		return job;
	}
	
	public JobLog getJobLog() {
		return jobLog;
	}

	@Override
	public String toString() {
		return "StoreLog [job=" + job + ", jobLog=" + jobLog + "]";
	}	
}

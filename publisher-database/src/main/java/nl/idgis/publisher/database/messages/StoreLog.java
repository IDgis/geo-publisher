package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.Log;

public class StoreLog extends Query {
	
	private static final long serialVersionUID = -1228166659611717157L;
	
	private final JobInfo job;
	private final Log jobLog;
	
	public StoreLog(JobInfo job, Log jobLog) {
		this.job = job;
		this.jobLog = jobLog;
	}
	
	public JobInfo getJob() {
		return job;
	}
	
	public Log getJobLog() {
		return jobLog;
	}

	@Override
	public String toString() {
		return "StoreLog [job=" + job + ", jobLog=" + jobLog + "]";
	}	
}

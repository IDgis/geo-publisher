package nl.idgis.publisher.job.manager.messages;

import java.io.Serializable;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.Log;

public class StoreLog implements Serializable {
	
	private static final long serialVersionUID = -2671345917389565328L;

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

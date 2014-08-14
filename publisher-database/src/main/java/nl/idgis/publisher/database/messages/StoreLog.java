package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.job.JobLog;

public class StoreLog extends Query {

	private static final long serialVersionUID = 7563644866851810078L;
	
	private final JobLog jobLog;
	
	public StoreLog(JobLog jobLog) {
		this.jobLog = jobLog;
	}
	
	public JobLog getJobLog() {
		return jobLog;
	}

	@Override
	public String toString() {
		return "StoreLog [jobLog=" + jobLog + "]";
	}
}
